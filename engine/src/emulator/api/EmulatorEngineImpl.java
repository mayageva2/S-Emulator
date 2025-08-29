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
    private final XmlVersionManager versionMgr = new XmlVersionManager(Paths.get("versions"));
    private final ProgramExpander programExpander = new ProgramExpander();

    @Override
    public ProgramView programView() {
        requireLoaded();
        List<Program> only0 = List.of(current);
        return buildProgramView(current, 0, only0);
    }

    @Override
    public ProgramView programView(int degree) {
        requireLoaded();
        int max = current.calculateMaxDegree();
        if (degree < 0 || degree > max) {
            throw new IllegalArgumentException("Invalid expansion degree: " + degree + " (0-" + max + ")");
        }

        List<Program> byDegree = new ArrayList<>(degree + 1);
        byDegree.add(current);

        for (int d = 1; d <= degree; d++) {
            Program prev = byDegree.get(d - 1);
            Program next = programExpander.expandOnce(prev);
            byDegree.add(next);
        }

        Program base = byDegree.get(degree);
        return buildProgramView(base, degree, byDegree);
    }

    private ProgramView buildProgramView(Program base, int degree, List<Program> byDegree) {
        List<Instruction> real = base.getInstructions();
        int maxDegree = byDegree.get(0).calculateMaxDegree();

        List<IdentityHashMap<Instruction, Integer>> indexMaps = new ArrayList<>(degree + 1);
        for (int d = 0; d <= degree; d++) {
            IdentityHashMap<Instruction, Integer> m = new IdentityHashMap<>();
            List<Instruction> list = byDegree.get(d).getInstructions();
            for (int i = 0; i < list.size(); i++) m.put(list.get(i), i + 1);
            indexMaps.add(m);
        }

        Function<Instruction, InstructionView> mkViewNoIndex = (Instruction ins) -> {
            InstructionData data = ins.getInstructionData();
            String opcode = data.getName();
            int cycles = data.getCycles();
            boolean basic = data.isBasic();
            Label lbl = ins.getLabel();
            String label = (lbl == null) ? null : lbl.getLabelRepresentation();

            List<String> args = new ArrayList<>();
            if (ins.getVariable() != null) args.add(ins.getVariable().getRepresentation());
            Map<String, String> extra = ins.getArguments();
            if (extra != null && !extra.isEmpty()) {
                var keys = new ArrayList<>(extra.keySet());
                Collections.sort(keys);
                for (String k : keys) args.add(k + "=" + (extra.get(k) == null ? "" : extra.get(k)));
            }
            return new InstructionView(-1, opcode, label, basic, cycles, args, List.of(), List.of());
        };

        List<InstructionView> out = new ArrayList<>(real.size());

        for (int i = 0; i < real.size(); i++) {
            Instruction ins = real.get(i);
            List<InstructionView> provenance = new ArrayList<>();
            IdentityHashMap<Instruction, Boolean> seen = new IdentityHashMap<>();
            Instruction cur = ins.getCreatedFrom();
            int guessDeg = degree - 1;

            while (cur != null && !seen.containsKey(cur)) {
                seen.put(cur, Boolean.TRUE);

                int foundDeg = -1;
                for (int d = Math.min(guessDeg, degree - 1); d >= 0; d--) {
                    if (indexMaps.get(d).containsKey(cur)) { foundDeg = d; break; }
                }
                if (foundDeg < 0) {
                    for (int d = degree - 1; d >= 0; d--) {
                        if (indexMaps.get(d).containsKey(cur)) { foundDeg = d; break; }
                    }
                }

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

    @Override
    public RunResult run(Long... input) {
        return run(0, input);
    }

    @Override
    public RunResult run(int degree, Long... input) {
        requireLoaded();

        int maxDegree = current.calculateMaxDegree();
        if (degree < 0 || degree > maxDegree) {
            throw new IllegalArgumentException(
                    "Invalid expansion degree: " + degree +
                            ". Allowed range is 0-" + maxDegree
            );
        }

        Program toRun = (degree <= 0) ? current : programExpander.expandToDegree(current, degree);
        var exec = (degree > 0) ? new ProgramExecutorImpl(toRun) : this.executor;

        this.lastViewProgram = (degree > 0) ? toRun : current;
        long y = exec.run(input);
        int cycles = exec.getLastExecutionCycles();

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

    @Override
    public Path saveOrReplaceVersion(Path original, int version) throws IOException {
        Objects.requireNonNull(original, "original must not be null");
        if (version < 0) throw new IllegalArgumentException("version must be non-negative");
        return versionMgr.saveOrReplaceVersion(original, version);
    }

    @Override
    public int lastCycles() {
        requireLoaded();
        return executor.getLastExecutionCycles();
    }

    @Override
    public List<RunRecord> history() { return List.copyOf(history); }

    @Override
    public void clearHistory() {
        history.clear();
        runCounter = 0;
    }

    @Override
    public boolean hasProgramLoaded() { return current != null; }

    private void requireLoaded() {
        if (current == null || executor == null) {
            throw new ProgramNotLoadedException();
        }
    }

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
}
