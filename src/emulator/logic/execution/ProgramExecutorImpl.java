package emulator.logic.execution;

import emulator.logic.instruction.SInstruction;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.program.SProgram;
import emulator.logic.variable.Variable;

import java.util.Map;

public class ProgramExecutorImpl implements ProgramExecutor{

    private final SProgram program;

    public ProgramExecutorImpl(SProgram program) {
        this.program = program;
    }

    @Override
    public long run(Long... input) {

        ExecutionContext context = new ExecutionContextImpl();

        SInstruction currentInstruction = program.getInstructions().get(0);
        Label nextLabel;
        do {
            nextLabel = currentInstruction.execute(context);

            if (nextLabel == FixedLabel.EMPTY) {
                // set currentInstruction to the next instruction in line
            } else if (nextLabel != FixedLabel.EXIT) {
                // need to find the instruction at 'nextLabel' and set current instruction to it
            }
        } while (nextLabel != FixedLabel.EXIT);

        return context.getVariableValue(Variable.RESULT);
    }

    @Override
    public Map<Variable, Long> variableState() {
        return Map.of();
    }
}
