package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public class ZeroVariableInstruction extends AbstractInstruction implements Expandable {

    public ZeroVariableInstruction(Variable variable) {super(InstructionData.ZERO_VARIABLE, variable);}

    public ZeroVariableInstruction(Variable variable, Label label) {
        super(InstructionData.ZERO_VARIABLE, variable, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(getVariable(), 0L);
        return FixedLabel.EMPTY;
    }

   @Override
   public List<Instruction> expand(ExpansionHelper helper) {
       List<Instruction> out = new ArrayList<>();
       if (getVariable() == null) {
           throw new IllegalStateException("ZERO_VARIABLE missing variable");
       }

       Variable var =  getVariable();

       Label original  = getLabel();
       Label loopLabel = (original == null || FixedLabel.EMPTY.equals(original)) ? helper.freshLabel() : original;

       out.add(new DecreaseInstruction(var, loopLabel));
       out.add(new JumpNotZeroInstruction(var, loopLabel));
       return out;
   }

}
