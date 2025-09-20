package emulator.api;

import emulator.api.dto.*;
import emulator.exception.*;
import emulator.logic.compose.Composer;
import emulator.logic.execution.ProgramExecutor;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.execution.QuoteEvaluator;
import emulator.logic.expansion.Expander;
import emulator.logic.expansion.ProgramExpander;
import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.InstructionData;
import emulator.logic.instruction.quote.MapBackedQuotationRegistry;
import emulator.logic.instruction.quote.QuotationRegistry;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.program.ProgramCost;
import emulator.logic.xml.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static java.util.Locale.ROOT;

public class EmulatorEngineImpl implements EmulatorEngine {

    private Program current;
    private Program lastViewProgram;
    private transient ProgramExecutor executor;
    private final List<RunRecord> history = new ArrayList<>();
    private int runCounter = 0;
    private transient ProgramExpander programExpander = new ProgramExpander();
    private transient Expander expander = new Expander();
    private static final long serialVersionUID = 1L;
    private final Map<String, Program> functionLibrary = new HashMap<>();
    private final Map<String, Program> functionsOnly   = new HashMap<>();
    private final Map<String, String> fnDisplayMap = new HashMap<>();
    private transient QuotationRegistry quotationRegistry = new MapBackedQuotationRegistry(functionLibrary);
    private final XmlProgramValidator xmlProgramValidator = new XmlProgramValidator();

    //This func returns a ProgramView of the currently loaded program
    @Override
    public ProgramView programView() {
        requireLoaded();
        List<Program> only0 = List.of(current);
        int max0 = expander.calculateMaxDegree(current.getInstructions());
        return buildProgramView(current, 0, only0, max0);
    }

    //This func returns a ProgramView of the currently loaded program at a specified degree
    @Override
    public ProgramView programView(int degree) {
        requireLoaded();
        int max = expander.calculateMaxDegree(current.getInstructions());
        if (degree < 0 || degree > max) {
            throw new IllegalArgumentException("Invalid expansion degree: " + degree + " (0-" + max + ")");
        }

        Program base = (degree <= 0) ? current : programExpander.expandToDegree(current, degree);
        List<Program> byDegree = new ArrayList<>(degree + 1);
        byDegree.add(current);

        //Expand according to degree
        for (int d = 1; d <= degree; d++) {
            byDegree.add(programExpander.expandToDegree(current, d));
        }

        if (degree > 0) byDegree.add(base);
        return buildProgramView(base, degree, byDegree, max);
    }

    @Override
    public ProgramView programView(String programName, int degree) {
        Objects.requireNonNull(programName, "programName");
        Program target = functionLibrary.get(programName);
        if (target == null) {
            // try exact and uppercase alias the registry stores
            target = functionLibrary.get(programName.toUpperCase(ROOT));
        }
        if (target == null) {
            throw new IllegalArgumentException("Unknown program: " + programName);
        }
        int max = expander.calculateMaxDegree(target.getInstructions());
        if (degree < 0 || degree > max) {
            throw new IllegalArgumentException("Invalid expansion degree: " + degree + " (0-" + max + ")");
        }

        // Build degree ladder for provenance (like your current programView(int))
        List<Program> byDegree = new ArrayList<>(degree + 1);
        byDegree.add(target);
        for (int d = 1; d <= degree; d++) {
            byDegree.add(programExpander.expandOnce(byDegree.get(d - 1)));
        }
        Program base = byDegree.get(degree);
        return buildProgramView(base, degree, byDegree, max);
    }

    private ProgramXml parseXmlToProgram(String xml) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(ProgramXml.class);
        Unmarshaller um = ctx.createUnmarshaller();
        try (StringReader reader = new StringReader(xml)) {
            return (ProgramXml) um.unmarshal(reader);
        }
    }

    public LoadResult loadProgram(Path xmlPath, ProgressListener cb) throws Exception {
        ProgressListener listener = (cb != null) ? cb : (stage, frac) -> {};

        final String p = String.valueOf(xmlPath);
        if (!p.toLowerCase(ROOT).endsWith(".xml")) {
            throw new XmlWrongExtensionException("Expected .xml file: " + p);
        }
        if (!java.nio.file.Files.exists(xmlPath)) {
            throw new XmlNotFoundException("File not found: " + p);
        }

        listener.onProgress("Reading file...", 0.10);
        Thread.sleep(300);
        XmlProgramReader reader = new XmlProgramReader();
        ProgramXml pxml = reader.read(xmlPath);
        this.current = XmlToObjects.toProgram(pxml, quotationRegistry);
        this.executor = new ProgramExecutorImpl(this.current, makeQuoteEvaluator());
        functionLibrary.put(this.current.getName().toUpperCase(ROOT), this.current);

        functionsOnly.clear();
        for (var e : functionLibrary.entrySet()) {
            Program p2 = e.getValue();
            if (p2 != null && p2 != this.current) {
                functionsOnly.put(e.getKey().toUpperCase(ROOT), p2);
            }
        }

        listener.onProgress("Validating XML...", 0.60);
        Thread.sleep(150);
        xmlProgramValidator.validate(pxml);

        listener.onProgress("Building program...", 0.85);
        Thread.sleep(200);
        this.current = XmlToObjects.toProgram(pxml, quotationRegistry);
        this.executor = new ProgramExecutorImpl(this.current, makeQuoteEvaluator());
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

    //------programView Helpers------//

    private static final ThreadLocal<Deque<Program>> CALL_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private QuoteEvaluator makeQuoteEvaluator() {
        return (fn, fargs, env, degree) -> {
            var invoker = new Composer.ProgramInvoker() {
                @Override public List<Long> run(String functionName, List<Long> inputs) {
                    Program target = resolveQuotedTarget(functionName);
                    if (target == null) {
                        throw new IllegalArgumentException("Unknown function: " + functionName);
                    }
                    Deque<Program> stack = CALL_STACK.get();
                    if (stack.contains(target)) {
                        String path = stack.stream().map(Program::getName)
                                .reduce((a,b) -> a + " -> " + b).orElse("<start>");
                        throw new IllegalStateException("Recursive composition detected: " + path + " -> " + target.getName());
                    }
                    stack.push(target);
                    try {
                        Program toRun = (degree <= 0) ? target : programExpander.expandToDegree(target, degree);
                        var exec = new ProgramExecutorImpl(toRun, makeQuoteEvaluator()); // allow nested QUOTE
                        long y = exec.run(inputs.toArray(Long[]::new));
                        return List.of(y);
                    } finally {
                        stack.pop();
                        if (stack.isEmpty()) CALL_STACK.remove();
                    }
                }
                @Override public Map<String, Long> currentEnv() { return env; }
            };
            return Composer.evaluateArgs(fn, fargs, invoker);
        };
    }

    private Program resolveQuotedTarget(String name) {
        if (name == null) return null;
        Program p = functionsOnly.get(name.toUpperCase(ROOT)); // prefer declared <S-Function>
        if (p != null) return p;
        for (var e : functionLibrary.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue(); // fallback (external)
        }
        return null;
    }

    //This func builds and returns a ProgramView by converting each instruction into an InstructionView
    private ProgramView buildProgramView(Program base, int degree, List<Program> byDegree, int maxDegree) {
        List<Instruction> real = base.getInstructions();
        int totalCycles = new ProgramCost().cyclesAtDegree(byDegree.get(0), degree);
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

        return new ProgramView(out, base.getName(), degree, maxDegree, totalCycles);
    }

    @Override
    public RunResult run(String programName, int degree, Long... input) {
        Objects.requireNonNull(programName, "programName");
        Program target = functionLibrary.get(programName);
        if (target == null) target = functionLibrary.get(programName.toUpperCase(ROOT));
        if (target == null) throw new IllegalArgumentException("Unknown program: " + programName);

        int maxDegree = target.calculateMaxDegree();
        if (degree < 0 || degree > maxDegree) {
            throw new IllegalArgumentException("Invalid expansion degree: " + degree + ". Allowed range is 0-" + maxDegree);
        }

        Program toRun = (degree <= 0) ? target : programExpander.expandToDegree(target, degree);
        ProgramExecutor exec = new ProgramExecutorImpl(toRun, makeQuoteEvaluator());
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

        return new RunResult(y, cycles, vars);
    }

    @Override
    public List<String> availablePrograms() {
        return functionLibrary.keySet().stream().toList();
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
        List<String> out = new ArrayList<>();

        if (ins.getVariable() != null) {
            out.add(ins.getVariable().getRepresentation());
        }
        Map<String, String> extra = new LinkedHashMap<>();
        if (ins.getArguments() != null) {
            extra.putAll(ins.getArguments());
        }

        String opcode = ins.getInstructionData().getName();
        if ("QUOTE".equals(opcode) || "JUMP_EQUAL_FUNCTION".equals(opcode)) {
            String fn = extra.get("functionName");
            if (fn != null && !fn.isBlank()) {
                String disp = displayOf(fn);
                if (!disp.isBlank() && !disp.equals(fn)) {
                    extra.put("userString", disp);
                    extra.putIfAbsent("functionUserString", disp);
                }
                String fargs = extra.get("functionArguments");
                if (fargs != null && !fargs.isBlank()) {
                    extra.put("functionArguments", mapHeadFunctions(fargs));
                }
            }
        }
        if (!extra.isEmpty()) {
            var keys = new ArrayList<>(extra.keySet());
            Collections.sort(keys);
            for (String k : keys) {
                String v = extra.get(k);
                out.add(k + "=" + (v == null ? "" : v));
            }
        }
        return out;
    }

    private String displayOf(String functionName) {
        if (functionName.isBlank()) return "";
        return fnDisplayMap.getOrDefault(functionName, functionName);
    }

    private String mapHeadFunctions(String s) {
        if (s == null || s.isBlank()) return s;
        StringBuilder out = new StringBuilder();
        int n = s.length(), i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '(') {
                out.append(c);
                i++;
                int start = i;
                while (i < n && s.charAt(i) != ',' && s.charAt(i) != ')') i++;
                String head = s.substring(start, i).trim();
                out.append(displayOf(head));
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
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
            throws emulator.exception.XmlWrongExtensionException,
            emulator.exception.XmlNotFoundException,
            emulator.exception.XmlReadException,
            emulator.exception.XmlInvalidContentException,
            emulator.exception.InvalidInstructionException,
            emulator.exception.MissingLabelException,
            emulator.exception.ProgramException,
            java.io.IOException {
        try {
            // Delegate to the unified loader with a no-op progress listener
            return loadProgram(xmlPath, (stage, fraction) -> {});
        } catch (emulator.exception.ProgramException | java.io.IOException e) {
            // Re-throw the exact types declared by the original signature
            throw e;
        } catch (RuntimeException e) {
            // Don’t wrap runtime exceptions
            throw e;
        } catch (Exception e) {
            // Anything unexpected -> wrap into ProgramException USING THE CORRECT CTOR
            // Adjust the two strings to match your ProgramException semantics.
            throw new emulator.exception.ProgramException(
                    "Unexpected error while loading program",
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
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
        var exec = (degree > 0) ? new ProgramExecutorImpl(toRun, makeQuoteEvaluator()) : this.executor;

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
        this.expander = new Expander();
    }
}
