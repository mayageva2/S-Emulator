package emulator.api;

import emulator.api.debug.DebugRecord;
import emulator.api.debug.DebugService;
import emulator.api.dto.*;
import emulator.exception.*;
import emulator.global.GlobalRelationsCenter;
import emulator.logic.compose.Composer;
import emulator.logic.debug.EngineDebugAdapter;
import emulator.logic.execution.ProgramExecutor;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.execution.QuoteEvaluator;
import emulator.logic.expansion.Expander;
import emulator.logic.expansion.ProgramExpander;
import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.InstructionData;
import emulator.logic.instruction.quote.MapBackedQuotationRegistry;
import emulator.logic.instruction.quote.QuotationRegistry;
import emulator.logic.instruction.quote.QuoteUtils;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.program.ProgramCost;
//import emulator.logic.program.ProgramDeepCopyFactory;
import emulator.logic.program.ProgramImpl;
import emulator.logic.xml.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static java.util.Locale.ROOT;

public class EmulatorEngineImpl implements EmulatorEngine {

    private Program current;
    private Program lastViewProgram;
    private transient ProgramExecutor executor;
    private final List<RunRecord> history = new ArrayList<>();
    private final Map<String, List<RunRecord>> historyByProgram = new HashMap<>();
    private final Map<String, Integer> runCountersByProgram = new HashMap<>();
    private transient ProgramExpander programExpander = new ProgramExpander();
    private transient Expander expander = new Expander();
    private static final long serialVersionUID = 1L;
    private Map<String, Program> functionLibrary = new HashMap<>();
    private final Map<String, Program> functionsOnly   = new HashMap<>();
    private final Map<String, String> fnDisplayMap = new HashMap<>();
    private transient QuotationRegistry quotationRegistry = new MapBackedQuotationRegistry(functionLibrary);
    private final XmlProgramValidator xmlProgramValidator = new XmlProgramValidator();
    private final List<DebugRecord> debugTrace = new ArrayList<>();
    private Map<String, Long> lastRunVars = Map.of();
    private List<Long> lastRunInputs = List.of();
    private int lastRunDegree = 0;
    private String lastRunProgramName = null;
    private final Map<String,String> displayToInternal = new HashMap<>();
    private final Map<String, List<Long>> programCreditHistory = new HashMap<>();
    private final Map<String, List<Long>> functionCreditHistory = new HashMap<>();
    private final Map<String, Integer> runCountByProgram = new HashMap<>();
    private final Map<String, Double> avgCreditsByProgram = new HashMap<>();
    private transient UserDTO sessionUser;
    private ArchitectureInfo lastArchitecture = new ArchitectureInfo("I", 5, "Basic architecture");
    private final ProgramStatsRepository programStats = new ProgramStatsRepository();
    private final ProgramService programService = new ProgramService(programStats);
    private final FunctionService functionService = new FunctionService(programStats);
    private final LoadService loadService = new LoadService(programService, functionService, programStats);
    Map<String, FunctionInfo> functionStats = new HashMap<>();

    // ----- DEBUG -----
    private transient Thread dbgThread;
    private final Object dbgLock = new Object();
    private volatile boolean dbgAlive = false;
    private volatile boolean dbgFinished = true;
    private volatile boolean dbgStopRequested = false;
    private volatile boolean dbgResumeMode = false;
    private volatile boolean dbgStepOnce = false;
    private volatile Runnable dbgOnFinish;
    private volatile String dbgErrorMessage = null;
    private volatile int dbgPC = 0;
    private volatile int dbgCycles = 0;
    private volatile Map<String,String> dbgVars = Map.of();
    private transient Program dbgProgram;
    private transient ProgramExecutor dbgExecutor;

    public void setSessionUser(UserDTO user) {
        this.sessionUser = user;
    }

    public FunctionService getFunctionService() {
        return functionService;
    }

    public ProgramService getProgramService() {
        return programService;
    }

    public Map<String, FunctionInfo> getFunctionStats() { return functionStats; }

  /*  @Override
    public void setProgramFromGlobal(Program program, String programName) {
        if (program == null) {
            this.current = null;
            this.lastViewProgram = null;
            this.executor = null;
            return;
        }

        ProgramImpl cloned = ProgramDeepCopyFactory.copyProgram((ProgramImpl) program);
        this.current = cloned;
        this.lastViewProgram = cloned;
        this.executor = new ProgramExecutorImpl(cloned, makeQuoteEvaluator());
        System.out.println("[Engine] Loaded program from GlobalDataCenter: " + programName);
    }*/

    @Override
    public UserDTO getSessionUser() {
        return sessionUser;
    }

    @Override
    public void registerProgram(Program program) {
        if (program == null) return;
        if (current == null) current = program;
        if (functionLibrary == null) functionLibrary = new HashMap<>();

        functionLibrary.put(program.getName().toUpperCase(Locale.ROOT), program);
        this.lastViewProgram = program;

        System.out.println("[EmulatorEngineImpl] Registered program: " + program.getName());
    }

    //This func validate degree bounds for a program
    private void validateDegree(Program p, int degree) {
        int maxDegree = p.calculateMaxDegree();
        if (degree < 0 || degree > maxDegree) {
            throw new IllegalArgumentException("Invalid expansion degree: " + degree + " (0-" + maxDegree + ")");
        }
    }

    //This func resolve a program by name
    private Program resolveProgram(String programName) {
        Objects.requireNonNull(programName, "programName");
        String key = programName.toUpperCase(Locale.ROOT);

        if (functionsOnly.containsKey(key)) {
            return functionsOnly.get(key);
        }

        Program p = functionLibrary.get(key);
        if (p == null) throw new IllegalArgumentException("Unknown program: " + programName);
        return p;
    }

    //This func expand if needed
    private Program expandIfNeeded(Program base, int degree) {
        return (degree <= 0) ? base : programExpander.expandToDegree(base, degree);
    }

    //This func attaches debug listener. */
    private void attachDebugListener(ProgramExecutor exec) {
        debugTrace.clear();
        exec.setStepListener((pcAfter, cycles, vars, finished) -> {
            Map<String,String> vv = (vars == null) ? Map.of() : Map.copyOf(vars);
            debugTrace.add(new DebugRecord(pcAfter, cycles, vv, finished, "STEP"));
        });
    }

    // Container for execution outcomes
    private static final class ExecOutcome {
        final long y;
        final int staticCycles;
        final int dynamicCycles;
        ExecOutcome(long y, int s, int d) { this.y = y; this.staticCycles = s; this.dynamicCycles = d; }
        int totalCycles() { return staticCycles + dynamicCycles; }
    }

    // This func execute and capture cycles
    private ExecOutcome runAndCapture(ProgramExecutor exec, Long... input) {
        long y = exec.run(input);
        int s = (exec instanceof ProgramExecutorImpl pei) ? pei.getLastExecutionCycles() : 0;
        int d = (exec instanceof ProgramExecutorImpl pei) ? pei.getLastDynamicCycles()   : 0;
        return new ExecOutcome(y, s, d);
    }

    // This func converts to VariableView list
    private List<VariableView> collectVariableViews(ProgramExecutor exec) {
        return exec.variableState().entrySet().stream()
                .map(e -> new VariableView(
                        e.getKey().getRepresentation(),
                        VarType.valueOf(e.getKey().getType().name()),
                        e.getKey().getNumber(),
                        e.getValue()
                ))
                .toList();
    }

    // This func get the last run variables
    private Map<String, Long> collectLastRunVars(ProgramExecutor exec) {
        return exec.variableState().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getRepresentation(),
                        Map.Entry::getValue,
                        (a,b) -> b,
                        LinkedHashMap::new
                ));
    }

    // This func normalize inputs
    private List<Long> normalizeInputs(Long[] input) {
        return Arrays.stream(input == null ? new Long[0] : input)
                .map(v -> v == null ? 0L : v)
                .toList();
    }

    // This func saves last-run state
    private void saveLastRunState(String programName, int degree, Long[] input, ProgramExecutor exec) {
        this.lastRunVars = collectLastRunVars(exec);
        this.lastRunInputs = normalizeInputs(input);
        this.lastRunDegree = degree;
        this.lastRunProgramName = programName;
    }

    // This func checks if user has enough credits for run
    private void ensureEnoughArchCredits(String programName, ArchitectureInfo arch) {
        long credits   = (sessionUser != null) ? sessionUser.getCredits() : 0L;
        long archCost  = arch.cost();
        double avgCost = avgCreditsByProgram.getOrDefault(programName.toUpperCase(Locale.ROOT), 0.0);
        long required  = archCost + Math.round(avgCost);
        if (credits < required) {
            throw new IllegalStateException(
                    "Not enough credits to start program (required = " + required +
                            ", architecture cost = " + archCost +
                            ", average cost = " + Math.round(avgCost) + ")");
        }
    }

    // This func estimates ProgramCost
    private int estimateAndCharge(Program target, int degree) {
        int estimated = new ProgramCost(quotationRegistry).cyclesAtDegree(target, degree);
        if (sessionUser == null || sessionUser.getCredits() < estimated) {
            throw new IllegalStateException("Not enough credits (" + estimated + " cycles required)");
        }
        sessionUser.setCredits(sessionUser.getCredits() - estimated);
        return estimated;
    }

    //Debug Error message handle
    public String getDebugErrorMessage() { return dbgErrorMessage; }
    public void clearDebugErrorMessage() { dbgErrorMessage = null; }

    //This func flags end of debug
    public void setOnDebugFinish(Runnable r) { this.dbgOnFinish = r; }

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
            throw new IllegalArgumentException(
                    "Invalid expansion degree: " + degree + " (0-" + max + ")"
            );
        }

        // Build proper degree ladder
        List<Program> byDegree = new ArrayList<>(degree + 1);

        Program prev = current;
        byDegree.add(prev);   // degree 0

        for (int d = 1; d <= degree; d++) {
            prev = programExpander.expandOnce(prev);   // expand from previous degree
            byDegree.add(prev);
        }

        Program base = byDegree.get(degree);  // final degree
        return buildProgramView(base, degree, byDegree, max);
    }

    //This func returns a ProgramView according to name and degree
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

    //This func returns last run variables
    @Override
    public Map<String, Long> lastRunVars() {
        return (lastRunVars == null) ? Map.of()
                : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(lastRunVars));
    }

    //This func returns last run inputs
    @Override
    public List<Long> lastRunInputs() {
        return (lastRunInputs == null) ? List.of() : List.copyOf(lastRunInputs);
    }

    //GETTERS
    @Override public int lastRunDegree() {return lastRunDegree;}
    @Override public String lastRunProgramName() {return lastRunProgramName;}

    //This func loads a program from stream
    @Override
    public LoadResult loadProgramFromStream(InputStream xmlStream)
            throws XmlWrongExtensionException,
            XmlNotFoundException,
            XmlReadException,
            XmlInvalidContentException,
            InvalidInstructionException,
            MissingLabelException,
            ProgramException,
            IOException {

        try {
            String xmlContent = new String(xmlStream.readAllBytes(), StandardCharsets.UTF_8);
            XmlProgramReader reader = new XmlProgramReader();
            ProgramXml pxml = reader.readFromString(xmlContent);

            this.current = XmlToObjects.toProgram(pxml, quotationRegistry, makeQuoteEvaluator());
            this.executor = new ProgramExecutorImpl(this.current, makeQuoteEvaluator());
            functionLibrary.put(this.current.getName().toUpperCase(Locale.ROOT), this.current);
            this.lastViewProgram = null;
            this.history.clear();
            this.historyByProgram.clear();
            this.runCountersByProgram.clear();
            this.lastRunVars = Map.of();
            this.lastRunInputs = List.of();
            this.lastRunDegree = 0;
            this.lastRunProgramName = null;

            functionStats.clear();
            String user = (sessionUser != null ? sessionUser.getUsername() : "Unknown");
            if (pxml.getFunctions() != null && pxml.getFunctions().getFunctions() != null) {

                List<FunctionXml> funcs = pxml.getFunctions().getFunctions();
                for (FunctionXml fxml : funcs) {

                    int instructionCount =
                            (fxml.getInstructions() != null &&
                                    fxml.getInstructions().getInstructions() != null)
                                    ? fxml.getInstructions().getInstructions().size()
                                    : 0;

                    int maxDegree = computeMaxDegree(fxml);
                    functionStats.put(
                            fxml.getName(),
                            new FunctionInfo(
                                    fxml.getName(),
                                    pxml.getName(),
                                    user,
                                    instructionCount,
                                    maxDegree,
                                    0.0
                            )
                    );

                    GlobalRelationsCenter.addFunctionToProgram(
                            pxml.getName(),
                            fxml.getName()
                    );
                }

                for (int i = 0; i < funcs.size(); i++) {
                    for (int j = 0; j < funcs.size(); j++) {
                        if (i != j) {
                            GlobalRelationsCenter.addRelatedFunction(
                                    funcs.get(i).getName(),
                                    funcs.get(j).getName()
                            );
                        }
                    }
                }
            }

            LoadResult result = new LoadResult(
                    current.getName(),
                    current.getInstructions().size(),
                    current.calculateMaxDegree(),
                    extractFunctionNames(pxml),
                    current
            );

            loadService.registerProgramType(current.getName(), "PROGRAM");
            for (String fn : result.functions()) {
                if (!fn.equalsIgnoreCase(current.getName())) {
                    loadService.registerProgramType(fn, "FUNCTION");
                }
            }
            return result;

        } catch (ProgramException | IOException e) {
            throw e;

        } catch (Exception e) {
            throw new ProgramException(
                    "Unexpected error while loading program from stream",
                    e.getMessage()
            );
        }
    }

    private int computeMaxDegree(FunctionXml fxml) {

        try {
            // Build a fully valid Program object
            Program func = XmlToObjects.toFunctionProgram(
                    fxml,
                    quotationRegistry,
                    makeQuoteEvaluator()
            );

            ProgramExpander expander = new ProgramExpander();

            int deg = 0;
            Program prev = func;

            while (true) {
                Program next = expander.expandOnce(prev);

                if (next.getInstructions().size() == prev.getInstructions().size()) {
                    // No more expansion possible
                    break;
                }
                deg++;
                prev = next;
            }

            return deg;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    //This func return ArchitectureInfo
    private ArchitectureInfo getArchitectureInfo(InstructionData data) {
        if (data == null)
            return new ArchitectureInfo("?", 0, "Unknown architecture");

        int cost = data.getBaseCreditCost();
        String name = switch (cost) {
            case 5 -> "I";
            case 100 -> "II";
            case 500 -> "III";
            case 1000 -> "IV";
            default -> "?";
        };
        String description = switch (name) {
            case "I" -> "Basic architecture";
            case "II" -> "Optimized architecture";
            case "III" -> "High performance architecture";
            case "IV" -> "Ultimate architecture";
            default -> "Unknown architecture";
        };
        return new ArchitectureInfo(name, cost, description);
    }

    //This func loads program validates it and updates progress bar
    public LoadResult loadProgram(Path xmlPath, ProgressListener cb) throws Exception {
        ProgressListener listener = (cb != null) ? cb : (stage, frac) -> {};

        ProgramXml pxml = validateAndReadXml(xmlPath, listener);
        buildProgramFromXml(pxml, listener);
        return finalizeLoad(pxml, listener);
    }

    //This func validates file and parses the XML to an object
    private ProgramXml validateAndReadXml(Path xmlPath, ProgressListener listener) throws Exception {
        final String pathStr = String.valueOf(xmlPath);

        if (!pathStr.toLowerCase(ROOT).endsWith(".xml")) {
            throw new XmlWrongExtensionException("Expected .xml file: " + pathStr);
        }
        if (!java.nio.file.Files.exists(xmlPath)) {
            throw new XmlNotFoundException("File not found: " + pathStr);
        }

        listener.onProgress("Reading file...", 0.10);
        XmlProgramReader reader = new XmlProgramReader();
        return reader.read(xmlPath);
    }

    //This func builds the program object
    private void buildProgramFromXml(ProgramXml pxml, ProgressListener listener) throws Exception {
        listener.onProgress("Validating XML...", 0.60);
        xmlProgramValidator.validate(pxml);

        // Build and register the programs
        this.current = XmlToObjects.toProgram(pxml, quotationRegistry, makeQuoteEvaluator());
        this.executor = new ProgramExecutorImpl(this.current, makeQuoteEvaluator());
        functionLibrary.put(this.current.getName().toUpperCase(ROOT), this.current);
        functionsOnly.clear();
        for (var e : functionLibrary.entrySet()) {
            Program p2 = e.getValue();
            if (p2 != null && p2 != this.current) {
                functionsOnly.put(e.getKey().toUpperCase(ROOT), p2);
            }
        }

        fnDisplayMap.clear();
        displayToInternal.clear();
        if (pxml.getFunctions() != null && pxml.getFunctions().getFunctions() != null) {
            for (var fxml : pxml.getFunctions().getFunctions()) {
                String internal = (fxml.getName() == null) ? "" : fxml.getName().trim();
                String user = (fxml.getUserString() == null) ? "" : fxml.getUserString().trim();
                if (!internal.isEmpty() && !user.isEmpty()) {
                    fnDisplayMap.put(internal, user);
                    fnDisplayMap.put(internal.toUpperCase(ROOT), user);
                    displayToInternal.put(user, internal);
                    displayToInternal.put(user.toUpperCase(ROOT), internal);
                }
            }
        }
    }

    //This func creates the LoadResult object
    private LoadResult finalizeLoad(ProgramXml pxml, ProgressListener listener) {
        listener.onProgress("Building program...", 0.85);

        this.current = XmlToObjects.toProgram(pxml, quotationRegistry, makeQuoteEvaluator());
        this.executor = new ProgramExecutorImpl(this.current, makeQuoteEvaluator());
        functionLibrary.put(this.current.getName().toUpperCase(ROOT), this.current);

        this.lastViewProgram = null;
        this.history.clear();
        this.historyByProgram.clear();
        this.runCountersByProgram.clear();
        this.lastRunVars = Map.of();
        this.lastRunInputs = List.of();
        this.lastRunDegree = 0;
        this.lastRunProgramName = null;

        LoadResult result = new LoadResult(
                current.getName(),
                current.getInstructions().size(),
                current.calculateMaxDegree(),
                extractFunctionNames(pxml),
                current
        );

        loadService.registerProgramType(current.getName(), "PROGRAM");
        for (String fn : result.functions()) {
            if (!fn.equalsIgnoreCase(current.getName())) {
                loadService.registerProgramType(fn, "FUNCTION");
            }
        }

        return result;
    }

    //This func extract function names
    private List<String> extractFunctionNames(ProgramXml pxml) {
        try {
            if (pxml.getFunctions() == null || pxml.getFunctions().getFunctions() == null)
                return List.of();
            return pxml.getFunctions().getFunctions().stream()
                    .map(f -> (f.getName() != null) ? f.getName().trim() : "")
                    .filter(s -> !s.isEmpty())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    // This func converts a display name to its internal function name
    private String internalOf(String maybeDisplayOrInternal) {
        if (maybeDisplayOrInternal == null) return null;
        String s = maybeDisplayOrInternal.trim();
        if (s.isEmpty()) return s;
        if (functionLibrary.containsKey(s.toUpperCase(ROOT))) return s;
        return displayToInternal.getOrDefault(s, s);
    }

    private final ThreadLocal<Deque<Program>> CALL_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    // This func creates a QuoteEvaluator used for evaluating QUOTE instructions
    private QuoteEvaluator makeQuoteEvaluator() {
        return (fn, fargs, env, degree) -> {
            Composer.ProgramInvoker invoker = createProgramInvoker(env, degree);
            return Composer.evaluateArgs(fn, fargs, invoker);
        };
    }

    // This func creates a ProgramInvoker that manages quoted function execution
    private Composer.ProgramInvoker createProgramInvoker(Map<String, Long> env, int degree) {
        return new Composer.ProgramInvoker() {
            @Override
            public List<Long> run(String functionName, List<Long> inputs) {
                return executeQuotedFunction(functionName, inputs, env, degree);
            }

            @Override
            public Map<String, Long> currentEnv() {
                return env;
            }
        };
    }

    // This func runs a quoted function safely with recursion protection, cycle tracking, and debug tracing
    private List<Long> executeQuotedFunction(String functionName, List<Long> inputs,
                                             Map<String, Long> env, int degree) {

        Program target = resolveQuotedTarget(functionName);  // Resolve target program
        if (target == null) {
            throw new IllegalArgumentException("Unknown function: " + functionName);
        }

        Deque<Program> stack = CALL_STACK.get();
        if (stack.contains(target)) {
            String path = stack.stream().map(Program::getName)
                    .reduce((a, b) -> a + " -> " + b).orElse("<start>");
            throw new IllegalStateException("Recursive composition detected: " + path + " -> " + target.getName());
        }

        stack.push(target);
        try {
            Program toRun = (degree <= 0) ? target : programExpander.expandToDegree(target, degree);
            var exec = new ProgramExecutorImpl(toRun, makeQuoteEvaluator());
            if (degree > 0 && exec instanceof ProgramExecutorImpl pei) {
                pei.setBaseCycles(QuoteUtils.getCurrentCycles());
            }
            exec.setBaseCycles(QuoteUtils.getCurrentCycles());

            // Debug listener
            debugTrace.clear();
            exec.setStepListener((pcAfter, cycles, vars, finished) -> {
                Map<String, String> vv = (vars == null) ? Map.of() : Map.copyOf(vars);
                debugTrace.add(new DebugRecord(pcAfter, cycles, vv, finished, "STEP"));
            });

            int carried = QuoteUtils.drainCycles();
            long y = exec.run(inputs.toArray(Long[]::new));
            QuoteUtils.addCycles(carried + exec.getLastExecutionCycles() + exec.getLastDynamicCycles());
            return List.of(y);

        } finally {
            stack.pop();
            if (stack.isEmpty()) CALL_STACK.remove();
        }
    }

    // This func resolves a quoted function name
    private Program resolveQuotedTarget(String name) {
        if (name == null) return null;
        String norm = internalOf(name);
        Program p = functionsOnly.get(name.toUpperCase(ROOT));
        if (p != null) return p;
        for (var e : functionLibrary.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        }
        return null;
    }

    //This func builds and returns a ProgramView by converting each instruction into an InstructionView
    private ProgramView buildProgramView(Program base, int degree, List<Program> byDegree, int maxDegree) {
        List<Instruction> real = base.getInstructions();
        int totalCycles = new ProgramCost(quotationRegistry).cyclesAtDegree(byDegree.get(0), degree);
        List<IdentityHashMap<Instruction, Integer>> indexMaps = buildIndexMaps(byDegree, degree); // Index maps for each degree
        Function<Instruction, InstructionView> mkViewNoIndex = this::makeInstructionViewNoIndex;

        // Build final views
        List<InstructionView> out = new ArrayList<>(real.size());
        for (int i = 0; i < real.size(); i++) {
            Instruction ins = real.get(i);
            List<InstructionView> provenance = buildProvenanceForInstruction(ins, degree, indexMaps, mkViewNoIndex);

            // numeric chain
            List<Integer> createdFromChain = new ArrayList<>();
            Instruction cur = ins.getCreatedFrom();
            int guessDeg = degree - 1;
            while (cur != null) {
                int foundDeg = findDegreeForInstruction(cur, indexMaps, Math.min(guessDeg, degree - 1), degree);
                Integer idx = (foundDeg >= 0) ? indexMaps.get(foundDeg).get(cur) : null;
                if (idx != null) createdFromChain.add(idx);
                cur = cur.getCreatedFrom();
                guessDeg--;
            }

            InstructionView self = mkViewNoIndex.apply(ins);
            InstructionData data = ins.getInstructionData();
            ArchitectureInfo info = getArchitectureInfo(data);

            out.add(new InstructionView(
                    i,
                    self.opcode(),
                    self.label(),
                    self.basic(),
                    self.cycles(),
                    self.args(),
                    createdFromChain,
                    provenance,
                    info.cost(),
                    info.name()
            ));
        }
        List<String> inputs = extractInputVars(new ProgramView(out, displayOf(base.getName()), degree, maxDegree, totalCycles, List.of()));
        return new ProgramView(out, displayOf(base.getName()), degree, maxDegree, totalCycles,  inputs);
    }

    //This func runs program
    @Override
    public RunResult run(String programName, int degree, Long... input) {
        Program target = resolveProgram(programName);
        validateDegree(target, degree);
        estimateAndCharge(target, degree);

        Program toRun = expandIfNeeded(target, degree);
        ProgramExecutor exec = new ProgramExecutorImpl(toRun, makeQuoteEvaluator());
        attachDebugListener(exec);

        ExecOutcome out = runAndCapture(exec, input);

        List<VariableView> views = collectVariableViews(exec);
        saveLastRunState(target.getName(), degree, input, exec);

        recordRun(this.lastRunProgramName, degree, input, out.y, out.totalCycles(),
                (lastArchitecture != null ? lastArchitecture.name() : "N/A"));

        return new RunResult(out.y, out.totalCycles(), views);
    }

    //This func runs the currently loaded program at expansion degree 0
    @Override
    public RunResult run(Long... input) { return run(0, input); }

    //This func runs the program expanded to the specified degree
    @Override
    public RunResult run(int degree, Long... input) {
        requireLoaded();
        validateDegree(current, degree);

        Program toRun = expandIfNeeded(current, degree);
        ProgramExecutor exec = (degree > 0)
                ? new ProgramExecutorImpl(toRun, makeQuoteEvaluator())
                : this.executor;

        this.lastViewProgram = (degree > 0) ? toRun : current;
        attachDebugListener(exec);
        ExecOutcome out = runAndCapture(exec, input);

        List<VariableView> views = collectVariableViews(exec);
        saveLastRunState(current.getName(), degree, input, exec);

        recordRun(this.lastRunProgramName, degree, input, out.y, out.totalCycles(),
                (lastArchitecture != null ? lastArchitecture.name() : "N/A"));

        return new RunResult(out.y, out.totalCycles(), views);
    }

    //This func runs the program (arch included version)
    public RunResult run(String programName, int degree, ArchitectureInfo arch, Long... input) {
        Objects.requireNonNull(arch, "architecture cannot be null");
        Program target = resolveProgram(programName);
        validateDegree(target, degree);
        ensureEnoughArchCredits(programName, arch);

        if (sessionUser == null || sessionUser.getCredits() < arch.cost()) {
            throw new IllegalStateException("Not enough credits for architecture cost = " + arch.cost());
        }
        sessionUser.setCredits(sessionUser.getCredits() - arch.cost());
        this.lastArchitecture = arch;

        Program toRun = expandIfNeeded(target, degree);
        ProgramExecutor exec = new ProgramExecutorImpl(toRun, makeQuoteEvaluator());
        attachDebugListener(exec);

        ExecOutcome out;
        try {
            out = runAndCapture(exec, input);
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not enough credits")) {
                System.err.println("Run stopped: not enough credits to continue.");

                int s = (exec instanceof ProgramExecutorImpl pei) ? pei.getLastExecutionCycles() : 0;
                int d = (exec instanceof ProgramExecutorImpl pei) ? pei.getLastDynamicCycles()   : 0;
                out = new ExecOutcome(0L, s, d);

                saveLastRunState(target.getName(), degree, input, exec);
                recordRun(programName, degree, input, out.y, out.totalCycles(), arch.name());
                throw new IllegalStateException("Run stopped due to insufficient credits. returning to Dashboard");
            } else {
                throw ex;
            }
        }

        List<VariableView> views = collectVariableViews(exec);
        saveLastRunState(target.getName(), degree, input, exec);
        recordRun(programName, degree, input, out.y, out.totalCycles(), arch.name());

        return new RunResult(out.y, out.totalCycles(), views);
    }

    // This func records a program run
    private void recordRun(String programName, int degree, Long[] input, long y, int cycles, String arch) {
        String canonical = canonicalProgramName(programName);
        int nextRunNumber = runCountersByProgram.merge(canonical, 1, Integer::sum);

        RunRecord record = createRunRecord(programName, degree, input, y, cycles, arch, nextRunNumber);
        record.setVarsSnapshot(buildVarsSnapshot(input, y));

        // Add to history
        history.add(record);
        historyByProgram.computeIfAbsent(canonical, k -> new ArrayList<>()).add(record);
        updateStatistics(programName, canonical, cycles, degree, input, nextRunNumber);
    }

    // This func creates a RunRecord object for the given run data
    private RunRecord createRunRecord(String programName, int degree, Long[] input, long y, int cycles,
                                      String arch, int nextRunNumber) {
        String currentUser = (sessionUser != null)
                ? sessionUser.getUsername()
                : "Unknown";
        String programType = loadService.getTypeForProgram(programName);

        return new RunRecord(
                currentUser,
                programName,
                nextRunNumber,
                degree,
                Arrays.asList(input),
                y,
                cycles,
                (this.lastRunVars != null)
                        ? new LinkedHashMap<>(this.lastRunVars)
                        : new LinkedHashMap<>(),
                programType,
                arch
        );
    }

    // This func builds a variable snapshot for current run
    private Map<String, Long> buildVarsSnapshot(Long[] input, long y) {
        if (this.lastRunVars != null && !this.lastRunVars.isEmpty()) {
            Map<String, Long> normalizedVars = new LinkedHashMap<>();
            for (var e : this.lastRunVars.entrySet()) {
                long value = e.getValue();
                normalizedVars.put(e.getKey(), (long) (int) value);
            }
            return normalizedVars;
        }

        LinkedHashMap<String, Long> fallback = new LinkedHashMap<>();
        if (input != null) {
            for (int i = 0; i < input.length; i++) {
                fallback.put("x" + (i + 1), input[i] != null ? input[i] : 0L);
            }
        }
        fallback.putIfAbsent("y", y);
        System.out.println("recordRun(): lastRunVars empty, using fallback varsSnapshot = " + fallback);
        return fallback;
    }

    //This func updates program statistics
    private void updateStatistics(String programName, String canonical, int cycles,
                                  int degree, Long[] input, int nextRunNumber) {
        try {
            int prevRuns = runCountByProgram.getOrDefault(canonical, 0);
            double prevAvg = avgCreditsByProgram.getOrDefault(canonical, 0.0);
            double newAvg = ((prevAvg * prevRuns) + cycles) / (prevRuns + 1);

            avgCreditsByProgram.put(canonical, newAvg);
            runCountByProgram.put(canonical, prevRuns + 1);
            programStats.updateAverage(programName, cycles);
            programService.recordRun(programName, cycles);

            if (sessionUser != null) {
                long runs = sessionUser.getRunsCount() + 1;
                sessionUser.setRunsCount(runs);
            }

            System.out.println("recordRun(): Added run #" + nextRunNumber +
                    " for program=" + programName +
                    ", degree=" + degree +
                    ", inputs=" + Arrays.toString(input) +
                    ", cycles=" + cycles +
                    ", avgCredits=" + newAvg);

        } catch (Exception e) {
            System.err.println("recordRun() failed for " + programName + ": " + e);
            e.printStackTrace();
        }
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
        ArchitectureInfo info = getArchitectureInfo(data);

        List<String> args = collectArgs(ins);
        return new InstructionView(-1, opcode, label, basic, cycles, args, List.of(), List.of(), info.cost(), info.name());
    }

    //This func returns the canonical version of a program name
    private String canonicalProgramName(String programName) {
        if (programName == null) return "";
        return programName.trim().toUpperCase(ROOT);
    }

    // This func collects arguments
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

    //This func returns the display name of a function
    private String displayOf(String functionName) {
        if (functionName.isBlank()) return "";
        return fnDisplayMap.getOrDefault(functionName, functionName);
    }

    // This func replaces internal function names with their display names
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
            InstructionData data = cur.getInstructionData();
            ArchitectureInfo info = getArchitectureInfo(data);

            provenance.add(new InstructionView(
                    idx,
                    v.opcode(), v.label(),
                    v.basic(), v.cycles(), v.args(),
                    List.of(), List.of(),
                    info.cost(),
                    info.name()
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
            return loadProgram(xmlPath, (stage, fraction) -> {});
        } catch (emulator.exception.ProgramException | java.io.IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new emulator.exception.ProgramException(
                    "Unexpected error while loading program",
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
            );
        }
    }

    //This func checks whether a program is currently loaded
    @Override
    public boolean hasProgramLoaded() { return current != null; }

    //This func ensures a program is loaded
    private void requireLoaded() {
        if (current == null || executor == null) { throw new ProgramNotLoadedException(); }
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
    public List<RunRecord> history() { return Collections.unmodifiableList(history); }

    //This func returns the run history
    @Override
    public List<RunRecord> history(String programName) {
        if (programName == null || programName.isBlank()) { return history(); }

        String internal = displayToInternal.getOrDefault(programName.toUpperCase(ROOT), programName);
        String canonical = canonicalProgramName(internal);
        List<RunRecord> byProgram = historyByProgram.get(canonical);
        if (byProgram == null || byProgram.isEmpty()) { return List.of(); }
        return Collections.unmodifiableList(byProgram);
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
            this.executor = new ProgramExecutorImpl(this.current, makeQuoteEvaluator());
            this.history.clear();
            this.history.addAll(loaded.history);
            this.historyByProgram.clear();
            this.runCountersByProgram.clear();
            for (RunRecord record : this.history) {
                String canonical = canonicalProgramName(record.programName());
                historyByProgram.computeIfAbsent(canonical, k -> new ArrayList<>()).add(record);
                runCountersByProgram.merge(canonical, record.runNumber(), Math::max);
            }
        }
    }

    //This func restores fields after deserialization
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.executor = new ProgramExecutorImpl(this.current, makeQuoteEvaluator());
        this.programExpander = new ProgramExpander();
        this.quotationRegistry = new MapBackedQuotationRegistry(functionLibrary);
        this.expander = new Expander();
    }

    //This func records a debug run session
    public void recordDebugSession(String programName, int degree, Long[] inputs, Map<String,String> vars, int cycles) {
        if (programName == null || programName.isBlank()) {
            programName = (current != null ? current.getName() : "UNKNOWN");
        }

        this.lastRunInputs = Arrays.stream(inputs == null ? new Long[0] : inputs)
                .map(v -> v == null ? 0L : v)
                .toList();
        this.lastRunDegree = Math.max(0, degree);
        this.lastRunProgramName = programName;

        long y = 0L;
        LinkedHashMap<String, Long> lastVars = new LinkedHashMap<>();
        if (vars != null) {
            for (var e : vars.entrySet()) {
                String k = e.getKey();
                String sv = e.getValue();
                if (k == null || sv == null || sv.isBlank()) continue;
                try {
                    long v = Long.parseLong(sv.trim());
                    lastVars.put(k, v);
                    if ("y".equalsIgnoreCase(k)) y = v;
                } catch (NumberFormatException ignore) {}
            }
        }
        if (this.lastRunVars == null) {
            this.lastRunVars = new LinkedHashMap<>(lastVars);
        }
        recordRun(this.lastRunProgramName, this.lastRunDegree,
                inputs == null ? new Long[0] : inputs, y, Math.max(0, cycles), lastArchitecture.name());
    }

    //This func snapshots variables into a map
    private Map<String,String> snapshotVars(ProgramExecutor ex, Long[] inputs) {
        if (ex == null) return Map.of();
        var state = ex.variableState();
        if (state == null) return Map.of();

        TreeMap<Integer, Long> x = new TreeMap<>();
        TreeMap<Integer, Long> z = new TreeMap<>();
        LinkedHashMap<String,String> other = new LinkedHashMap<>();
        Long yVal = parseStateIntoBuckets(ex, x, z, other);

        return buildSnapshotFromBuckets(x, z, yVal, other, inputs);
    }

    //This func parses executor state into x/z/y buckets
    private Long parseStateIntoBuckets(ProgramExecutor ex, TreeMap<Integer, Long> x,
                                       TreeMap<Integer, Long> z, LinkedHashMap<String,String> other) {
        Long yVal = null;

        for (var e : ex.variableState().entrySet()) {
            var ref = e.getKey();
            long v = e.getValue();
            String name = ref.getRepresentation();
            String low = name.toLowerCase(java.util.Locale.ROOT);

            if ("y".equals(low)) {
                yVal = v;
            } else if (low.startsWith("x")) {
                try { x.put(Integer.parseInt(low.substring(1)), v); } catch (Exception ignore) {}
            } else if (low.startsWith("z")) {
                try { z.put(Integer.parseInt(low.substring(1)), v); } catch (Exception ignore) {}
            } else {
                other.put(name, String.valueOf(v));
            }
        }
        return yVal;
    }

    //This func builds the final snapshot map and merges inputs into x-bucket
    private LinkedHashMap<String,String> buildSnapshotFromBuckets(TreeMap<Integer, Long> x, TreeMap<Integer, Long> z,
                                                                  Long yVal, LinkedHashMap<String,String> other, Long[] inputs) {
        if (inputs != null) {
            for (int i = 0; i < inputs.length; i++) {
                x.putIfAbsent(i + 1, inputs[i] == null ? 0L : inputs[i]);
            }
        }

        LinkedHashMap<String,String> finalMap = new LinkedHashMap<>();
        long y = (yVal == null) ? 0L : yVal;
        finalMap.put("y", String.valueOf(y));
        x.forEach((i,v) -> finalMap.put("x"+i, String.valueOf(v)));
        z.forEach((i,v) -> finalMap.put("z"+i, String.valueOf(v)));
        finalMap.putAll(other);
        return finalMap;
    }

    //This function starts debug run
    public void debugStart(String programName, Long[] inputs, int degree, ArchitectureInfo architectureInfo) {
        Objects.requireNonNull(programName, "programName");
        Program target = functionLibrary.get(programName);
        if (target == null) target = functionLibrary.get(programName.toUpperCase(java.util.Locale.ROOT));
        if (target == null) throw new IllegalArgumentException("Unknown program: " + programName);
        this.lastArchitecture = architectureInfo;
        debugStartCommon(target, inputs, degree, architectureInfo);
    }

    //This function starts debug run
    public void debugStart(Long[] inputs, int degree, ArchitectureInfo architectureInfo) {
        requireLoaded();
        debugStartCommon(current, inputs, degree, architectureInfo);
        this.lastArchitecture = architectureInfo;
    }

    //This func starts a debug session - orchestrator
    private void debugStartCommon(Program target, Long[] inputs, int degree, ArchitectureInfo architectureInfo) {
        Program toRun = prepareDebugSession(target, degree, architectureInfo);
        initDebugState(toRun, architectureInfo, inputs, degree, target.getName());
        installDebugStepListener(inputs);
        launchDebugThread(target, inputs, degree, architectureInfo);
    }

    //This func validates, checks credits, expands program
    private Program prepareDebugSession(Program target, int degree, ArchitectureInfo archInfo) {
        int maxDegree = target.calculateMaxDegree();
        if (degree < 0 || degree > maxDegree) {
            throw new IllegalArgumentException("Invalid expansion degree: " + degree + " (0-" + maxDegree + ")");
        }

        long credits = (sessionUser != null) ? sessionUser.getCredits() : 0L;
        long archCost = archInfo.cost();
        double avgCost = avgCreditsByProgram.getOrDefault(target.getName().toUpperCase(Locale.ROOT), 0.0);
        long totalRequired = archCost + Math.round(avgCost);

        if (credits < archCost) {
            throw new IllegalStateException(
                    "Not enough credits to start program (required = " + totalRequired +
                            ", architecture cost = " + archCost +
                            ", average cost = " + Math.round(avgCost) + ")"
            );
        }

        if (sessionUser == null || sessionUser.getCredits() < archCost) {
            throw new IllegalStateException("Not enough credits for architecture cost = " + archCost);
        }
        sessionUser.setCredits(sessionUser.getCredits() - archCost);
        Program toRun = (degree <= 0) ? target : programExpander.expandToDegree(target, degree);

        debugStopSafe();
        this.lastArchitecture = archInfo;
        return toRun;
    }

    //This func initializes all debug fields
    private void initDebugState(Program toRun, ArchitectureInfo archInfo, Long[] inputs, int degree, String programName) {
        dbgProgram = toRun;
        dbgExecutor = new ProgramExecutorImpl(dbgProgram, makeQuoteEvaluator());
        dbgPC = 0;
        dbgCycles = 0;
        dbgVars = Map.of();
        dbgFinished = false;
        dbgStopRequested = false;
        dbgResumeMode = false;
        dbgStepOnce = false;
        dbgAlive = true;
        this.lastArchitecture = archInfo;

        this.lastRunInputs = Arrays.stream(inputs == null ? new Long[0] : inputs)
                .map(v -> v == null ? 0L : v)
                .toList();
        this.lastRunDegree = degree;
        this.lastRunProgramName = programName;
    }

    //This func installs the step listener for the debug executor
    private void installDebugStepListener(Long[] inputs) {
        dbgExecutor.setStepListener((pc, cycles, vars, finished) -> {
            dbgPC = pc;
            dbgCycles = cycles;
            dbgVars = (vars == null) ? snapshotVars(dbgExecutor, inputs) : Map.copyOf(vars);

            synchronized (dbgLock) {
                if (dbgStopRequested) throw new DebugAbortException();

                if (!dbgResumeMode) {
                    while (!dbgStopRequested && !dbgStepOnce) {
                        try {
                            dbgLock.wait();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new DebugAbortException();
                        }
                    }
                    if (dbgStopRequested) throw new DebugAbortException();
                    dbgStepOnce = false;
                }
            }
        });
    }

    //This func launches the background debug thread
    private void launchDebugThread(Program target, Long[] inputs, int degree, ArchitectureInfo architectureInfo) {
        dbgThread = new Thread(() -> {
            try {
                long y = dbgExecutor.run(inputs == null ? new Long[0] : inputs);

                int cycles = dbgExecutor.getLastExecutionCycles();
                Map<String, String> vars = snapshotVars(dbgExecutor, inputs);

                lastRunVars = dbgExecutor.variableState().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> e.getKey().getRepresentation(),
                                Map.Entry::getValue,
                                (a, b) -> b,
                                LinkedHashMap::new
                        ));

                lastRunInputs = Arrays.stream(inputs == null ? new Long[0] : inputs)
                        .map(v -> v == null ? 0L : v)
                        .toList();
                lastRunDegree = degree;
                lastRunProgramName = target.getName();
                if (sessionUser != null) {
                    long runs = sessionUser.getRunsCount() + 1;
                    sessionUser.setRunsCount(runs);
                }
                recordRun(lastRunProgramName, degree, inputs, y, cycles, architectureInfo.name());

                dbgVars = vars;
                dbgFinished = true;
                dbgAlive = false;

            } catch (DebugAbortException ignore) {
            } catch (Throwable t) {
                if (t instanceof IllegalStateException &&
                        t.getMessage() != null &&
                        t.getMessage().toLowerCase().contains("not enough credits")) {

                    dbgErrorMessage = "You ran out of credits. Program stopped.";
                    dbgFinished = true;
                    dbgAlive = false;

                    try {
                        int cycles = (dbgExecutor != null)
                                ? dbgExecutor.getLastExecutionCycles() + dbgExecutor.getLastDynamicCycles()
                                : 0;
                        Map<String, String> vars = snapshotVars(dbgExecutor, inputs);

                        recordDebugSession(
                                lastRunProgramName != null ? lastRunProgramName : target.getName(),
                                degree,
                                inputs,
                                vars,
                                cycles
                        );
                        System.err.println("Debug stopped: out of credits");
                    } catch (Exception saveEx) {
                        System.err.println("Failed to record partial debug run: " + saveEx);
                    }

                } else {
                    dbgErrorMessage = "Unexpected error during debug: " + t.getMessage();
                    t.printStackTrace();
                }
            } finally {
                dbgFinished = true;
                dbgAlive = false;
                synchronized (dbgLock) {
                    dbgLock.notifyAll();
                }
                if (dbgOnFinish != null) { dbgOnFinish.run(); }
            }
        }, "emu-debug-thread");

        dbgThread.setDaemon(true);
        dbgThread.start();
    }

    //This func does a step in debug
    public void debugStepOver() {
        if (!dbgAlive || dbgFinished) return;
        synchronized (dbgLock) { dbgStepOnce = true; dbgLock.notifyAll(); }
    }

    //This func stops debug and finish running
    public void debugResume() {
        if (!dbgAlive || dbgFinished) return;
        synchronized (dbgLock) { dbgResumeMode = true; dbgStepOnce = true; dbgLock.notifyAll(); }
    }

    //This func stops debug
    public void debugStop() { debugStopSafe(); }

    //This func stops debug
    private void debugStopSafe() {
        synchronized (dbgLock) {
            dbgStopRequested = true;
            dbgResumeMode = true;
            dbgStepOnce = true;
            dbgLock.notifyAll();
        }
        if (dbgThread != null && dbgThread.isAlive()) {
            try { dbgThread.join(100); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        dbgAlive = false;
        dbgFinished = true;
    }

    //This func checks if debug is finished
    public boolean debugIsFinished() {return dbgFinished;}

    //debug getters
    public int debugCurrentPC() {return dbgPC;}
    public int debugCycles() { return dbgCycles; }

    public Map<String,String> debugVarsSnapshot() {return (dbgVars == null) ? Map.of() : dbgVars;}

    //This func builds and returns the current debug session's RunResult snapshot
    public RunResult debugCurrentRunResult() {
        if (dbgExecutor == null) return null;

        var vars = dbgExecutor.variableState().entrySet().stream()
                .map(e -> new VariableView(
                        e.getKey().getRepresentation(),
                        VarType.valueOf(e.getKey().getType().name()),
                        e.getKey().getNumber(),
                        e.getValue()
                ))
                .toList();

        long y = lastRunVars.getOrDefault("y", 0L);
        return new RunResult(y, debugCycles(), vars);
    }

    //This func returns a DebugService adapter for the current engine
    @Override
    public DebugService debugger() { return new EngineDebugAdapter(this); }

    //This func clears all stored run history and counters
    @Override
    public void clearHistory() {
        history.clear();
        historyByProgram.clear();
        runCountersByProgram.clear();
    }

    public RunRecord lastRunRecord() {
        return history().isEmpty() ? null : history().get(history().size() - 1);
    }

    @Override
    public void setCurrentProgram(String name) {
        if (name == null || name.isBlank()) return;

        if (isFunction(name)) {
            Program p = functionsOnly.get(name.toUpperCase());
            if (p != null) {
                this.current = p;
                return;
            }
        }

        try {
            this.current = resolveProgram(name);
        } catch (Exception ignore) {}
    }

    @Override
    public boolean isFunction(String name) {
        if (name == null) return false;
        return functionStats.containsKey(name) || functionStats.containsKey(name.toUpperCase());
    }

}
