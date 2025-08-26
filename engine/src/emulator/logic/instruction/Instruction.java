package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface Instruction {

    String getName();
    Label execute(ExecutionContext context);
    int cycles();
    int degree();
    Label getLabel();
    Variable getVariable();
    Map<String, String> getArguments();
    default Collection<Variable> referencedVariables() { return Collections.emptyList(); }
    InstructionData getInstructionData();
    default Instruction getCreatedFrom() { return null; }
}
