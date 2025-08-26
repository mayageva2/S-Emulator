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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.atomic.AtomicInteger;

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
        Program base = (lastViewProgram != null) ? lastViewProgram : current;
        return buildProgramView(base);
    }

    @Override
    public ProgramView programView(int degree) {
        requireLoaded();
        int max = current.calculateMaxDegree();
        if (degree < 0 || degree > max) {
            throw new IllegalArgumentException("Invalid expansion degree: " + degree + " (0-" + max + ")");
        }
        Program base = (degree <= 0) ? current : programExpander.expandToDegree(current, degree);
        return buildProgramView(base);
    }

    private ProgramView buildProgramView(Program base) {
        List<Instruction> instructions = base.getInstructions();
        int maxDegree = base.calculateMaxDegree();

        IdentityHashMap<Instruction, Integer> indexOfReal = new IdentityHashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            indexOfReal.put(instructions.get(i), i);
        }

        IdentityHashMap<Instruction, List<Integer>> childrenIndicesOfAncestor = new IdentityHashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            Instruction anc = ins.getCreatedFrom();
            while (anc != null) {
                childrenIndicesOfAncestor
                        .computeIfAbsent(anc, k -> new ArrayList<>())
                        .add(i);
                anc = anc.getCreatedFrom();
            }
        }

        IdentityHashMap<Instruction, Integer> indexOfAll = new IdentityHashMap<>(indexOfReal);

        java.util.function.Function<Instruction, InstructionView> makeViewSkeleton = (Instruction ins) -> {
            InstructionData data = ins.getInstructionData();
            String opcode = data.getName();
            int cycles = data.getCycles();
            boolean basic = data.isBasic();
            Label lbl = ins.getLabel();
            String label = (lbl == null) ? null : lbl.getLabelRepresentation();

            List<String> args = new ArrayList<>();
            if (ins.getVariable() != null) {
                args.add(ins.getVariable().getRepresentation());
            }
            Map<String, String> extraArgs = ins.getArguments();
            if (extraArgs != null && !extraArgs.isEmpty()) {
                var keys = new ArrayList<>(extraArgs.keySet());
                Collections.sort(keys);
                for (String k : keys) {
                    String v = extraArgs.get(k);
                    args.add(k + "=" + (v == null ? "" : v));
                }
            }
            return new InstructionView(-1, opcode, label, basic, cycles, args, List.of());
        };

        List<InstructionView> realViews = new ArrayList<>(instructions.size());
        List<InstructionView> virtualViews = new ArrayList<>();

        final AtomicInteger nextVirtualIndex = new AtomicInteger(instructions.size() + 1);

        java.util.function.BiFunction<Instruction, Integer, List<Integer>> buildChain =
                (Instruction ins, Integer selfZeroBased) -> {
                    List<Integer> chain = new ArrayList<>();
                    IdentityHashMap<Instruction, Boolean> seen = new IdentityHashMap<>();
                    Instruction cur = ins.getCreatedFrom();

                    while (cur != null && !seen.containsKey(cur)) {
                        seen.put(cur, Boolean.TRUE);

                        Integer zeroBased = indexOfAll.get(cur);
                        if (zeroBased == null) {
                            InstructionView vv = makeViewSkeleton.apply(cur);
                            int assigned = nextVirtualIndex.getAndIncrement();
                            vv = new InstructionView(assigned, vv.opcode(), vv.label(),
                                    vv.basic(), vv.cycles(), vv.args(), List.of());
                            virtualViews.add(vv);
                            zeroBased = assigned - 1;
                            indexOfAll.put(cur, zeroBased);
                        }

                        if (selfZeroBased == null || zeroBased.intValue() != selfZeroBased.intValue()) {
                            chain.add(zeroBased + 1);
                        }

                        cur = cur.getCreatedFrom();
                    }
                    return chain;
                };

        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            InstructionView skel = makeViewSkeleton.apply(ins);
            List<Integer> chain = buildChain.apply(ins, i);
            realViews.add(new InstructionView(
                    i + 1,
                    skel.opcode(),
                    skel.label(),
                    skel.basic(),
                    skel.cycles(),
                    skel.args(),
                    chain
            ));
        }

        List<InstructionView> all = new ArrayList<>(realViews.size() + virtualViews.size());
        all.addAll(realViews);
        all.addAll(virtualViews);

        return new ProgramView(all, maxDegree);
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
