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
        List<Instruction> real = base.getInstructions();
        int maxDegree = base.calculateMaxDegree();

        IdentityHashMap<Instruction, Integer> indexOfReal = new IdentityHashMap<>();
        for (int i = 0; i < real.size(); i++) indexOfReal.put(real.get(i), i);

        IdentityHashMap<Instruction, Integer> firstChildIdx = new IdentityHashMap<>();
        for (int i = 0; i < real.size(); i++) {
            Instruction ins = real.get(i);
            IdentityHashMap<Instruction, Boolean> seen = new IdentityHashMap<>();
            Instruction anc = ins.getCreatedFrom();
            while (anc != null && !seen.containsKey(anc)) {
                seen.put(anc, Boolean.TRUE);
                Integer curMin = firstChildIdx.get(anc);
                if (curMin == null || i < curMin) firstChildIdx.put(anc, i);
                anc = anc.getCreatedFrom();
            }
        }

        List<InstructionView> out = new ArrayList<>();
        IdentityHashMap<Instruction, Integer> outIndex = new IdentityHashMap<>();

        Function<Instruction, InstructionView> mkView = (Instruction ins) -> {
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
                for (String k : keys) {
                    String v = extra.get(k);
                    args.add(k + "=" + (v == null ? "" : v));
                }
            }
            return new InstructionView(-1, opcode, label, basic, cycles, args, List.of());
        };

        for (int i = 0; i < real.size(); i++) {
            Instruction ins = real.get(i);
            List<Instruction> ancNearest = new ArrayList<>();
            IdentityHashMap<Instruction, Boolean> seen = new IdentityHashMap<>();
            Instruction cur = ins.getCreatedFrom();
            while (cur != null && !seen.containsKey(cur)) {
                seen.put(cur, Boolean.TRUE);
                ancNearest.add(cur);
                cur = cur.getCreatedFrom();
            }

            if (!ancNearest.isEmpty()) {
                List<Instruction> farthestFirst = new ArrayList<>(ancNearest);
                Collections.reverse(farthestFirst);
                for (Instruction anc : farthestFirst) {
                    if (outIndex.containsKey(anc)) continue;
                    if (indexOfReal.containsKey(anc)) continue;
                    Integer fci = firstChildIdx.get(anc);
                    if (fci != null && fci == i) {
                        List<Integer> vChain = new ArrayList<>();
                        Instruction up = anc.getCreatedFrom();
                        IdentityHashMap<Instruction, Boolean> seenUp = new IdentityHashMap<>();
                        while (up != null && !seenUp.containsKey(up)) {
                            seenUp.put(up, Boolean.TRUE);
                            Integer idx = outIndex.get(up);
                            if (idx != null) vChain.add(idx);
                            up = up.getCreatedFrom();
                        }
                        InstructionView skel = mkView.apply(anc);
                        List<String> vArgs = new ArrayList<>(skel.args());
                        vArgs.add("__virtual__=1");
                        int newIdx = out.size() + 1;
                        out.add(new InstructionView(newIdx, skel.opcode(), skel.label(),
                                skel.basic(), skel.cycles(), vArgs, vChain));
                        outIndex.put(anc, newIdx);
                    }
                }
            }

            List<Integer> chain = new ArrayList<>();
            for (Instruction anc : ancNearest) {
                Integer idx = outIndex.get(anc);
                if (idx != null && (chain.isEmpty() || !Objects.equals(chain.get(chain.size()-1), idx))) {
                    chain.add(idx);
                }
            }

            InstructionView rSkel = mkView.apply(ins);
            int myIdx = out.size() + 1;
            out.add(new InstructionView(myIdx, rSkel.opcode(), rSkel.label(),
                    rSkel.basic(), rSkel.cycles(), rSkel.args(), chain));
            outIndex.put(ins, myIdx);
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
