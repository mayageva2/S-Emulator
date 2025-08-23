package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

public class QuotationInstruction extends AbstractInstruction {

    private final String functionName;
    private final String functionArguments;

    public  QuotationInstruction(Variable variable, String functionName, String functionArguments) {
        super(InstructionData.QUOTATION, variable);
        this.functionName = functionName;
        this.functionArguments = functionArguments;
    }

    public  QuotationInstruction(Variable variable, String functionName, String functionArguments, Label label) {
        super(InstructionData.QUOTATION, variable, label);
        this.functionName = functionName;
        this.functionArguments = functionArguments;
    }

    @Override
    public Label execute(ExecutionContext context) {  //fix
       /* List<Integer> argValues = parseAndEvalArgs(functionArguments, context);
        int resultY = context.executeFunction(functionName, argValues);
        getVariable().setValue(resultY);
        long childCycles = context.getLastExecutionCycles();
        context.addCycles(childCycles);*/
        return FixedLabel.EMPTY;
    }

    public String getFunctionArguments() { return functionArguments; }
    public String getFunctionName() { return functionName; }
}
