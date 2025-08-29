package emulator.api;

import emulator.api.dto.*;
import emulator.exception.ProgramNotLoadedException;
import emulator.exception.XmlInvalidContentException;
import emulator.exception.XmlReadException;
import emulator.logic.execution.ProgramExecutor;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.expansion.ProgramExpander;
import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.InstructionData;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.xml.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class EmulatorEngineImpl implements EmulatorEngine {

    private Program current;
    private Program lastViewProgram;
    private ProgramExecutor executor;
    private final List<RunRecord> history = new ArrayList<>();
    private int runCounter = 0;
    private final ProgramExpander programExpander = new ProgramExpander();

    //This func returns a ProgramView of the currently loaded program
    @Override
    public ProgramView programView() {
        requireLoaded();
        List<Program> only0 = List.of(current);
        return buildProgramView(current, 0, only0);
    }

    //This func returns a ProgramView of the currently loaded program at a specified degree
    @Override
    public ProgramView programView(int degree) {
        requireLoaded();
        int max = current.calculateMaxDegree();
        if (degree < 0 || degree > max) {
            throw new IllegalArgumentException("Invalid expansion degree: " + degree + " (0-" + max + ")");
        }

        List<Program> byDegree = new ArrayList<>(degree + 1);
        byDegree.add(current);

        //Expand according to degree
        for (int d = 1; d <= degree; d++) {
            Program prev = byDegree.get(d - 1);
            Program next = programExpander.expandOnce(prev);
            byDegree.add(next);
        }

        Program base = byDegree.get(degree);
        return buildProgramView(base, degree, byDegree);
    }

    //------programView Helpers------//

    //This func builds and returns a ProgramView by converting each instruction into an InstructionView
    private ProgramView buildProgramView(Program base, int degree, List<Program> byDegree) {
        List<Instruction> real = base.getInstructions();
        int maxDegree = byDegree.get(0).calculateMaxDegree();

        List<IdentityHashMap<Instruction, Integer>> indexMaps = buildIndexMaps(byDegree, degree); // Index maps for each degree
        Function<Instruction, InstructionView> mkViewNoIndex = this::makeInstructionViewNoIndex;

        //Build final views (with provenance)
        List<InstructionView> out = new ArrayList<>(real.size());
        for (int i = 0; i < real.size(); i++) {
            Instruction ins = real.get(i);
            List<InstructionView> provenance = buildProvenanceForInstruction(ins, degree, indexMaps, mkViewNoIndex);

            InstructionView self = mkViewNoIndex.apply(ins);
            out.add(new InstructionView(
                    i + 1,
                    self.opcode(), self.label(),
                    self.basic(), self.cycles(), self.args(),
                    List.of(),
                    provenance
            ));
        }

        return new ProgramView(out, maxDegree);
    }

    //This func builds maps per degree from Instruction identity
    private List<IdentityHashMap<Instruction, Integer>> buildIndexMaps(List<Program> byDegree, int degree) {
        List<IdentityHashMap<Instruction, Integer>> indexMaps = new ArrayList<>(degree + 1);
        for (int d = 0; d <= degree; d++) {
            IdentityHashMap<Instruction, Integer> m = new IdentityHashMap<>();
            List<Instruction> list = byDegree.get(d).getInstructions();
            for (int i = 0; i < list.size(); i++) {
                m.put(list.get(i), i + 1);
            }
            indexMaps.add(m);
        }
        return indexMaps;
    }

    //This func creates an InstructionView without index
    private InstructionView makeInstructionViewNoIndex(Instruction ins) {
        InstructionData data = ins.getInstructionData();
        String opcode = data.getName();
        int cycles = data.getCycles();
        boolean basic = data.isBasic();

        Label lbl = ins.getLabel();
        String label = (lbl == null) ? null : lbl.getLabelRepresentation();

        List<String> args = collectArgs(ins);
        return new InstructionView(-1, opcode, label, basic, cycles, args, List.of(), List.of());
    }

    // This func collects ordered arguments and sorts extra arguments
    private List<String> collectArgs(Instruction ins) {
        List<String> args = new ArrayList<>();
        if (ins.getVariable() != null) {
            args.add(ins.getVariable().getRepresentation());
        }
        Map<String, String> extra = ins.getArguments();
        if (extra != null && !extra.isEmpty()) {
            var keys = new ArrayList<>(extra.keySet());
            Collections.sort(keys);
            for (String k : keys) {
                String v = extra.get(k);
                args.add(k + "=" + (v == null ? "" : v));
            }
        }
        return args;
    }

    // This func builds provenance chain
    private List<InstructionView> buildProvenanceForInstruction(Instruction ins, int degree, List<IdentityHashMap<Instruction, Integer>> indexMaps, Function<Instruction, InstructionView> mkViewNoIndex) {
        List<InstructionView> provenance = new ArrayList<>();
        IdentityHashMap<Instruction, Boolean> seen = new IdentityHashMap<>();
        Instruction cur = ins.getCreatedFrom();
        int guessDeg = degree - 1;

        while (cur != null && !seen.containsKey(cur)) {
            seen.put(cur, Boolean.TRUE);

            int foundDeg = findDegreeForInstruction(cur, indexMaps, Math.min(guessDeg, degree - 1), degree);
            InstructionView v = mkViewNoIndex.apply(cur);
            int idx = (foundDeg >= 0) ? indexMaps.get(foundDeg).get(cur) : -1;

            provenance.add(new InstructionView(
                    idx,
                    v.opcode(), v.label(),
                    v.basic(), v.cycles(), v.args(),
                    List.of(), List.of()
            ));

            cur = cur.getCreatedFrom();
            guessDeg = (foundDeg >= 0) ? (foundDeg - 1) : (guessDeg - 1);
        }

        return provenance;
    }

    //This func locate the degree layer
    private int findDegreeForInstruction(
            Instruction target,
            List<IdentityHashMap<Instruction, Integer>> indexMaps,
            int guessDeg,
            int degree
    ) {
        for (int d = guessDeg; d >= 0; d--) {
            if (indexMaps.get(d).containsKey(target)) return d;
        }
        for (int d = degree - 1; d >= 0; d--) {
            if (indexMaps.get(d).containsKey(target)) return d;
        }
        return -1;
    }

    //This function loads a program from an XML file and validates it
    @Override
    public LoadResult loadProgram(Path xmlPath) {
        try {
            var reader = new XmlProgramReader();
            var pxml = reader.read(xmlPath);

            var validator = new XmlProgramValidator();
            validator.validate(pxml);

            this.current = XmlToObjects.toProgram(pxml);
            this.executor = new ProgramExecutorImpl(this.current);

            this.lastViewProgram = null;
            history.clear();
            runCounter = 0;

            return new LoadResult(
                    current.getName(),
                    current.getInstructions().size(),
                    current.calculateMaxDegree()
            );
        } catch (XmlReadException e) {
            throw new XmlInvalidContentException(
                    e.getMessage(),
                    Map.of("path", String.valueOf(xmlPath), "cause", e.getClass().getSimpleName())
            );
        }
    }

    //This func runs the currently loaded program at expansion degree 0
    @Override
    public RunResult run(Long... input) {
        return run(0, input);
    }

    //This func runs the program expanded to the specified degree
    @Override
    public RunResult run(int degree, Long... input) {
        requireLoaded();

        int maxDegree = current.calculateMaxDegree();
        if (degree < 0 || degree > maxDegree) {
            throw new IllegalArgumentException("Invalid expansion degree: " + degree + ". Allowed range is 0-" + maxDegree);
        }

        Program toRun = (degree <= 0) ? current : programExpander.expandToDegree(current, degree);
        var exec = (degree > 0) ? new ProgramExecutorImpl(toRun) : this.executor;

        this.lastViewProgram = (degree > 0) ? toRun : current;
        long y = exec.run(input);
        int cycles = exec.getLastExecutionCycles();

        //Collect final state of all variables after execution
        var vars = exec.variableState().entrySet().stream()
                .map(e -> new VariableView(
                        e.getKey().getRepresentation(),
                        VarType.valueOf(e.getKey().getType().name()),
                        e.getKey().getNumber(),
                        e.getValue()
                ))
                .toList();

        history.add(new RunRecord(++runCounter, degree, Arrays.asList(input), y, cycles));
        return new RunResult(y, cycles, vars);
    }

    //This func checks whether a program is currently loaded
    @Override
    public boolean hasProgramLoaded() { return current != null; }

    //This func ensures a program is loaded
    private void requireLoaded() {
        if (current == null || executor == null) {
            throw new ProgramNotLoadedException();
        }
    }

    //This func extracts and returns a sorted list of unique input variables
    @Override
    public List<String> extractInputVars(ProgramView pv) {
        Pattern VAR_X = Pattern.compile("\\bx([1-9]\\d*)\\b");
        Set<String> uniq = new LinkedHashSet<>();
        for (InstructionView iv : pv.instructions()) {
            for (String arg : iv.args()) {
                Matcher m = VAR_X.matcher(arg);
                while (m.find()) {
                    uniq.add("x" + m.group(1));
                }
            }
        }
        List<String> out = new ArrayList<>(uniq);
        out.sort(Comparator.comparingInt(s -> Integer.parseInt(s.substring(1))));
        return out;
    }

    //This func returns all runs history list
    @Override
    public List<RunRecord> history() {
        return Collections.unmodifiableList(history);
    }

}
