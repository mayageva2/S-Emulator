package emulator.api;

import emulator.logic.execution.ProgramExecutor;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.instruction.Instruction;
import emulator.logic.print.InstructionFormatter;
import emulator.logic.program.Program;
import emulator.logic.xml.XmlProgramReader;
import emulator.logic.xml.XmlProgramValidator;
import emulator.logic.xml.XmlReadException;
import emulator.logic.xml.XmlToObjects;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class EmulatorEngineImpl implements EmulatorEngine {

    private Program current;
    private ProgramExecutor executor;
    private final List<RunRecord> history = new ArrayList<>();

    @Override
    public LoadResult loadProgram(Path xmlPath) {
        try {
            var reader = new XmlProgramReader();
            var pxml = reader.read(xmlPath);

            var validator = new XmlProgramValidator();
            validator.validate(pxml);

            this.current = XmlToObjects.toProgram(pxml);
            this.executor = new ProgramExecutorImpl(this.current);

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
        var vars = executor.variableState();
        history.add(new RunRecord(List.of(input), y, cycles));
        return new RunResult(y, cycles, vars);
    }

    @Override
    public List<String> programSummary() {
        requireLoaded();

        var fmt  = new InstructionFormatter();
        var list = current.getInstructions();

        return IntStream.range(0, list.size())
                .mapToObj(i -> fmt.formatInstruction(i + 1, list.get(i)))
                .toList();
    }

    @Override
    public Map<String, Long> variableState() {
        requireLoaded();
        return executor.variableState();
    }

    @Override
    public int lastCycles() {
        requireLoaded();
        return executor.getLastExecutionCycles();
    }

    @Override
    public List<RunRecord> history() { return List.copyOf(history); }

    @Override
    public boolean hasProgramLoaded() { return current != null; }

    private void requireLoaded() {
        if (current == null || executor == null) {
            throw new IllegalStateException("No program loaded");
        }
    }
}
