package emulator.api;

import emulator.api.dto.*;
import emulator.exception.*;
import emulator.logic.execution.ProgramExecutor;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.expansion.ProgramExpander;
import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.InstructionData;
import emulator.logic.instruction.quote.MapBackedQuotationRegistry;
import emulator.logic.instruction.quote.QuotationRegistry;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.xml.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static java.util.Locale.ROOT;

public class EmulatorEngineImpl implements EmulatorEngine, Serializable {

    private Program current;
    private Program lastViewProgram;
    private transient ProgramExecutor executor;
    private final List<RunRecord> history = new ArrayList<>();
    private int runCounter = 0;
    private transient ProgramExpander programExpander = new ProgramExpander();
    private static final long serialVersionUID = 1L;
    private final Map<String, Program> functionLibrary = new java.util.HashMap<>();
    private transient QuotationRegistry quotationRegistry = new MapBackedQuotationRegistry(functionLibrary);

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
    public LoadResult loadProgram(Path xmlPath)
            throws XmlWrongExtensionException,
            XmlNotFoundException,
            XmlReadException,
            XmlInvalidContentException,
            InvalidInstructionException,
            MissingLabelException,
            ProgramException,
            IOException {

        final String p = String.valueOf(xmlPath);
        if (!p.toLowerCase(ROOT).endsWith(".xml")) {
            throw new XmlWrongExtensionException("Expected .xml file: " + p);
        }
        if (!java.nio.file.Files.exists(xmlPath)) {
            throw new XmlNotFoundException("File not found: " + p);
        }

        XmlProgramReader reader = new XmlProgramReader();
        ProgramXml pxml = reader.read(xmlPath);
        XmlProgramValidator validator = new XmlProgramValidator();
        validator.validate(pxml);
        this.current = XmlToObjects.toProgram(pxml, quotationRegistry);
        this.executor = new ProgramExecutorImpl(this.current);
        functionLibrary.put(this.current.getName().toUpperCase(ROOT), this.current);
        this.lastViewProgram = null;
        this.history.clear();
        this.runCounter = 0;

        return new LoadResult(
                current.getName(),
                current.getInstructions().size(),
                current.calculateMaxDegree()
        );
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
    public boolean hasProgramLoaded() {
        return current != null;
    }

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

    //This func saves the program's state
    @Override
    public void saveState(Path fileWithoutExt) throws Exception {
        try (var oos = new ObjectOutputStream(
                Files.newOutputStream(fileWithoutExt.resolveSibling(fileWithoutExt.getFileName() + ".semu")))) {
            oos.writeObject(this);
        }
    }

    //This func loads the program's state
    @Override
    public void loadState(Path fileWithoutExt) throws Exception {
        try (var ois = new ObjectInputStream(
                Files.newInputStream(fileWithoutExt.resolveSibling(fileWithoutExt.getFileName() + ".semu")))) {
            EmulatorEngineImpl loaded = (EmulatorEngineImpl) ois.readObject();

            this.current = loaded.current;
            this.lastViewProgram = loaded.lastViewProgram;
            this.executor = loaded.executor;
            this.history.clear();
            this.history.addAll(loaded.history);
            this.runCounter = loaded.runCounter;
        }
    }

    //This func creates heavy objects
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.executor = new ProgramExecutorImpl(this.current);
        this.programExpander = new ProgramExpander();
        this.quotationRegistry = new MapBackedQuotationRegistry(functionLibrary);
    }
}
