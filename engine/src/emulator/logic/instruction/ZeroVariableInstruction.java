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

    //This func executes the instruction
    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(getVariable(), 0L);
        return FixedLabel.EMPTY;
    }

    //This func expands an ZERO_VARIABLE instruction
   @Override
   public List<Instruction> expand(ExpansionHelper helper) {
       List<Instruction> out = new ArrayList<>();

       Variable var =  getVariable();
       if (var == null) {
           throw new IllegalStateException("ZERO_VARIABLE missing variable");
       }

       Label carry = getLabel();
       boolean hasCarry = (carry != null && !FixedLabel.EMPTY.equals(carry));
       Label L1 = helper.freshLabel();

       if (hasCarry) {
           out.add(new NeutralInstruction(var, carry));
       }

       out.add(new DecreaseInstruction(var, L1));
       out.add(new JumpNotZeroInstruction(var, L1));

       return out;
   }

}
