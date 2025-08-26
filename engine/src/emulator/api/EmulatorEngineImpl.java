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
        List<Instruction> instructions = base.getInstructions();
        List<InstructionView> views = new ArrayList<>(instructions.size());
        int maxDegree = base.calculateMaxDegree();

        IdentityHashMap<Instruction, Integer> indexOf = new IdentityHashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            indexOf.put(instructions.get(i), i);
        }

        IdentityHashMap<Instruction, Integer> firstChildIndexOfAncestor = new IdentityHashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            Instruction anc = ins.getCreatedFrom();
            while (anc != null) {
                firstChildIndexOfAncestor.putIfAbsent(anc, i);
                anc = anc.getCreatedFrom();
            }
        }

        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
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
                for (Map.Entry<String, String> entry : extraArgs.entrySet()) {
                    String k = entry.getKey();
                    String v = entry.getValue();
                    args.add(k + "=" + (v == null ? "" : v));
                }
            }

            List<Integer> chain = new ArrayList<>();
            Instruction cur = ins.getCreatedFrom();
            while (cur != null) {
                Integer idx0 = indexOf.get(cur);
                if (idx0 == null) {
                    idx0 = firstChildIndexOfAncestor.get(cur);
                }
                if (idx0 == null) break;
                chain.add(idx0 + 1);
                cur = cur.getCreatedFrom();
            }

            views.add(new InstructionView(i + 1, opcode, label, basic, cycles, args, chain));
        }

        return new ProgramView(views, maxDegree);
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
