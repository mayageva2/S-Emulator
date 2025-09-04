package emulator.logic.instruction.quote;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.instruction.AbstractInstruction;
import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.InstructionData;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;

import java.util.*;

public class QuotationInstruction extends AbstractInstruction implements Expandable {

    private final String functionName;
    private final String functionArguments;
    private final List<String> rawArgs;
    private final QuoteParser parser;
    private final QuotationRegistry registry;
    private final VarResolver varResolver;

    private QuotationInstruction(Builder builder) {
        super(emulator.logic.instruction.InstructionData.QUOTATION, Objects.requireNonNull(builder.variable, "variable"), builder.myLabel);
        this.functionName = Objects.requireNonNull(builder.funcName, "functionName").trim();
        this.functionArguments = (builder.funcArguments == null) ? "" : builder.funcArguments.trim();
        this.parser = (builder.parser != null) ? builder.parser : new QuoteParserImpl();
        this.rawArgs = List.copyOf(this.parser.parseTopLevelArgs(this.functionArguments));
        this.registry = Objects.requireNonNull(builder.registry, "registry");
        this.varResolver = Objects.requireNonNull(builder.varResolver, "varResolver");

        setArgument("functionName", this.functionName);
        setArgument("functionArguments", this.functionArguments);
    }

    //This func builds instruction
    public static class Builder {
        private Variable variable;
        private  String funcName;
        private  String funcArguments;
        private Label myLabel;
        private QuoteParser parser;
        private QuotationRegistry registry;
        private VarResolver varResolver;

        public Builder variable(Variable variable) { this.variable = variable; return this; }
        public Builder funcName(String funcName) { this.funcName = funcName; return this; }
        public Builder funcArguments(String funcArguments) { this.funcArguments = funcArguments; return this; }
        public Builder myLabel(Label label) { this.myLabel = label; return this; }
        public Builder parser(QuoteParser parser) { this.parser = parser; return this; }
        public Builder registry(QuotationRegistry r) { this.registry = r; return this; }
        public Builder varResolver(VarResolver r) { this.varResolver = r; return this; }

        public QuotationInstruction build() { return new QuotationInstruction(this); }
    }

    @Override
    public Label execute(ExecutionContext ctx) {
        long resultY = runQuotedEval(functionName, functionArguments, ctx);
        ctx.updateVariable(getVariable(), resultY);
        return FixedLabel.EMPTY;
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {
        List<Instruction> out = new ArrayList<>();
        List<String> flatArgs = new ArrayList<>(rawArgs.size());

        Variable var = getVariable();
        if (var == null) {
            throw new IllegalStateException("QUOTATION missing variable");
        }

        Label orig = getLabel();
        boolean hasLabel = (orig != null && !FixedLabel.EMPTY.equals(orig));
        boolean labelUsed = false;
        boolean anyNested = false;

        for (String tok : rawArgs) {
            if (parser.isNestedCall(tok)) {
                anyNested = true;

                QuoteParser.NestedCall nc = parser.parseNestedCall(tok);
                Variable tmp = helper.freshVar();

                Label nestedLabel = (hasLabel && !labelUsed) ? orig : FixedLabel.EMPTY;

                QuotationInstruction nested = new QuotationInstruction.Builder()
                        .variable(tmp)
                        .funcName(nc.name())
                        .funcArguments(nc.argsCsv())
                        .myLabel(nestedLabel)
                        .parser(this.parser)
                        .registry(this.registry)
                        .varResolver(this.varResolver)
                        .build();

                List<Instruction> nestedOut = nested.expand(helper);
                if (nestedOut.isEmpty()) {
                    out.add(nested);
                } else {
                    out.addAll(nestedOut);
                }

                if (nestedLabel != FixedLabel.EMPTY) {
                    labelUsed = true;
                }

                flatArgs.add(tmp.getRepresentation());
            } else {
                flatArgs.add(tok);
            }
        }

        if (!anyNested) {
            return List.of(this);
        }

        String newCsv = String.join(", ", flatArgs);
        Label topLabel = (hasLabel && !labelUsed) ? orig : FixedLabel.EMPTY;

        QuotationInstruction top = new QuotationInstruction.Builder()
                .variable(var)
                .funcName(this.functionName)
                .funcArguments(newCsv)
                .myLabel(topLabel)
                .parser(this.parser)
                .registry(this.registry)
                .varResolver(this.varResolver)
                .build();

        out.add(top);
        return out;
    }

    private long runQuotedEval(String fname, String argsCsv, ExecutionContext ctx) {
        Program qProgram = registry.getProgramByName(fname);
        int need = requiredInputCount(qProgram);

        List<String> args = parser.parseTopLevelArgs(argsCsv);
        Long[] inputs = new Long[need];
        java.util.Arrays.fill(inputs, 0L);

        int copy = Math.min(need, args.size());
        for (int i = 0; i < copy; i++) {
            inputs[i] = evalArgToValue(args.get(i), ctx);
        }

        ProgramExecutorImpl subExec = new ProgramExecutorImpl(qProgram);
        return subExec.run(inputs);
    }

    private Long evalArgToValue(String token, ExecutionContext ctx) {
        if (parser.isNestedCall(token)) {
            QuoteParser.NestedCall nc = parser.parseNestedCall(token);
            return runQuotedEval(nc.name(), nc.argsCsv(), ctx);
        } else {
            var src = varResolver.resolve(token);
            return ctx.getVariableValue(src);
        }
    }

    private static int requiredInputCount(Program p) {
        int max = 0;
        for (var v : p.getVariables()) {
            if (v.getType() == emulator.logic.variable.VariableType.INPUT) {
                max = Math.max(max, v.getNumber());
            }
        }
        return max;
    }

    public String functionName() { return functionName; }
    public String functionArguments() { return functionArguments; }
    public List<String> rawArgs() { return rawArgs; }

}
