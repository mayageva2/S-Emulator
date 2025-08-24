package emulator.api;

import emulator.api.dto.*;
import emulator.logic.execution.ProgramExecutor;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.InstructionData;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;
import emulator.logic.xml.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class EmulatorEngineImpl implements EmulatorEngine {

    private Program current;
    private ProgramExecutor executor;
    private final List<RunRecord> history = new ArrayList<>();
    private int runCounter = 0;
    private final XmlVersionManager versionMgr = new XmlVersionManager(Paths.get("versions"));

    @Override
    public ProgramView programView() {
        List<Instruction> instructions = current.getInstructions();
        List<InstructionView> views = new ArrayList<>(instructions.size());

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

            if (ins.getArguments() != null && !ins.getArguments().isEmpty()) {
                for (Map.Entry<String, String> entry : ins.getArguments().entrySet()) {
                    String k = entry.getKey();
                    String v = entry.getValue();
                    args.add(k + "=" + (v == null ? "" : v));
                }
            }

            views.add(new InstructionView(i + 1, opcode, label, basic, cycles, args));
        }

        return new ProgramView(views);
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

            return new LoadResult(true, "Program loaded");
        } catch (XmlReadException e) {
            this.current = null;
            this.executor = null;
            return new LoadResult(false, e.getMessage());
        }
    }

    @Override
    public RunResult run(Long... input) {
        requireLoaded();
        long y = executor.run(input);
        int cycles = executor.getLastExecutionCycles();
        Map<Variable, Long> state = executor.variableState();

        List<VariableView> views = state.entrySet().stream()
                .map(e -> new VariableView(
                        e.getKey().getRepresentation(),
                        switch (e.getKey().getType()) {
                            case RESULT -> VarType.RESULT;
                            case INPUT  -> VarType.INPUT;
                            case WORK   -> VarType.WORK;
                        },
                        e.getKey().getNumber(),
                        e.getValue()
                ))
                .toList();

        long[] in = Arrays.stream(input).mapToLong(Long::longValue).toArray();
        history.add(RunRecord.of(++runCounter, 0, in, y, cycles));

        return new RunResult(y, cycles, views);
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
            throw new IllegalStateException("No program loaded");
        }
    }
}
