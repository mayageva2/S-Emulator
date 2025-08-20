package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.label.LabelImpl;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableImpl;
import emulator.logic.variable.VariableType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
       Variable var =  getVariable();

       Label loopLabel = getLabel();
       if (loopLabel == null || FixedLabel.EMPTY.equals(loopLabel)) {
           loopLabel = helper.freshLabel();
       }

       out.add(new DecreaseInstruction(var, loopLabel));
       out.add(new JumpNotZeroInstruction(var, loopLabel));

       return out;
   }

    @Override
    public int degree() {
        ExpansionHelper helper = ExpansionHelper.fromUsedSets(
                java.util.Set.of(), java.util.Set.of(),
                name -> new VariableImpl(VariableType.WORK, 0, name),
                name -> new LabelImpl(0)
        );

        return 1 + expand(helper).stream()
                .mapToInt(Instruction::degree)
                .max()
                .orElse(0);
    }

}
