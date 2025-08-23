package emulator.logic.execution;

import emulator.logic.instruction.Instruction;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProgramExecutorImpl implements ProgramExecutor{

    private final Program program;

    public ProgramExecutorImpl(Program program) {
        this.program = program;
    }

    @Override
    public long run(Long... input) {

        List<Instruction> instructions = program.getInstructions();
        final int len = instructions.size();

        if (len == 0) {
            throw new IllegalStateException("empty program");
        }

        ExecutionContext context = new ExecutionContextImpl();
        Set<Variable> variables = program.getVariables();
        for (Variable variable : variables) {
            context.updateVariable(variable, variable.getNumber());
        }

        int currentIndex = 0;
        Instruction currentInstruction = instructions.get(currentIndex);
        Label nextLabel;

        do {
            nextLabel = currentInstruction.execute(context);

            if (nextLabel == FixedLabel.EMPTY) {    // Go to next instruction in line
                currentIndex++;
                if (currentIndex < len) {
                    currentInstruction = instructions.get(currentIndex);
                }
            } else if (nextLabel != FixedLabel.EXIT) {   // Go to the instruction at given label
                Instruction target = program.instructionAt(nextLabel);
                if (target == null) {
                    throw new IllegalStateException("No instruction for label: " + nextLabel);
                }
                int idx = instructions.indexOf(target);
                currentInstruction = target;
                currentIndex = idx;
            }
        } while (nextLabel != FixedLabel.EXIT && currentIndex < len);

        return context.getVariableValue(Variable.RESULT);
    }

    @Override
    public Map<Variable, Long> variableState() {
        return Map.of();
    }
}
