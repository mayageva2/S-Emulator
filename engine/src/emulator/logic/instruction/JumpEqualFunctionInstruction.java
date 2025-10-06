package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.execution.QuoteEvaluator;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.instruction.quote.QuoteParser;
import emulator.logic.instruction.quote.QuoteParserImpl;
import emulator.logic.instruction.quote.QuoteUtils;
import emulator.logic.instruction.quote.QuotationInstruction;
import emulator.logic.instruction.quote.QuotationRegistry;
import emulator.logic.instruction.quote.VarResolver;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;

import java.util.*;

public class JumpEqualFunctionInstruction extends AbstractInstruction implements Expandable {

    private final Label jeFunctionLabel;
    private final String functionName;
    private final String functionArguments;
    private final QuoteParser parser;
    private final QuotationRegistry registry;
    private final VarResolver varResolver;

    private JumpEqualFunctionInstruction(Builder builder) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, builder.variable, builder.myLabel);

        this.jeFunctionLabel   = Objects.requireNonNull(builder.jeFunctionLabel, "jeFunctionLabel");
        this.functionName      = Objects.requireNonNull(builder.funcName, "functionName").trim();
        this.functionArguments = (builder.funcArguments == null) ? "" : builder.funcArguments.trim();
        this.parser            = (builder.parser != null) ? builder.parser : new QuoteParserImpl();
        this.registry          = Objects.requireNonNull(builder.registry, "registry");
        this.varResolver       = Objects.requireNonNull(builder.varResolver, "varResolver");

        setArgument("JEFunctionLabel", this.jeFunctionLabel.getLabelRepresentation());
        setArgument("functionName", this.functionName);
        setArgument("functionArguments", this.functionArguments);
    }

    // -------- Builder --------
    public static class Builder {
        private Variable variable;
        private Label jeFunctionLabel;
        private String funcName;
        private String funcArguments;
        private Label myLabel;

        private QuoteParser parser;
        private QuotationRegistry registry;
        private VarResolver varResolver;

        public Builder variable(Variable variable) { this.variable = variable; return this; }
        public Builder jeFunctionLabel(Label label) { this.jeFunctionLabel = label; return this; }
        public Builder funcName(String funcName) { this.funcName = funcName; return this; }
        public Builder funcArguments(String funcArguments) { this.funcArguments = funcArguments; return this; }
        public Builder myLabel(Label label) { this.myLabel = label; return this; }
        public Builder parser(QuoteParser parser) { this.parser = parser; return this; }
        public Builder registry(QuotationRegistry registry) { this.registry = registry; return this; }
        public Builder varResolver(VarResolver varResolver) { this.varResolver = varResolver; return this; }

        public JumpEqualFunctionInstruction build() { return new JumpEqualFunctionInstruction(this); }
    }

    // -------- Execute --------
    @Override
    public Label execute(ExecutionContext ctx) {
        long vVal = ctx.getVariableValue(getVariable());
        QuoteEvaluator evaluator = (ctx != null) ? ctx.getQuoteEvaluator() : null;
        if (evaluator == null) {
            throw new IllegalStateException("QuoteEvaluator is not available in ExecutionContext");
        }
        long qVal = QuoteUtils.runQuotedEval(functionName, functionArguments, ctx, registry, parser, varResolver, evaluator);
        return (vVal == qVal) ? jeFunctionLabel : FixedLabel.EMPTY;
    }

    // -------- Expand --------
    @Override
    public List<Instruction> expand(ExpansionHelper helper) {
        List<Instruction> out = new ArrayList<>();

        Label first = getLabel();
        if (first != null && !FixedLabel.EMPTY.equals(first)) {
            out.add(new NeutralInstruction(getVariable(), first));
        }

        Program qProgram = registry.getProgramByName(functionName);
        Map<Variable, Variable> varSub = new HashMap<>();
        Map<Integer, Variable> inputIndexToFresh = new HashMap<>();
        Map<String, Label> labelSub = new HashMap<>();
        Variable tempZ = null;

        for (Variable qv : qProgram.getVariables()) {
            switch (qv.getType()) {
                case INPUT -> {
                    Variable fresh = helper.freshVar();
                    varSub.put(qv, fresh);
                    inputIndexToFresh.put(qv.getNumber(), fresh);
                }
                case WORK -> varSub.put(qv, helper.freshVar());
                default -> {
                    if (QuoteUtils.isOutputVar(qv)) {
                        tempZ = helper.freshVar();
                        varSub.put(qv, tempZ);
                    } else {
                        varSub.put(qv, qv);
                    }
                }
            }
        }
        if (tempZ == null) tempZ = helper.freshVar();

        // מיפוי לייבלים פנימיים
        for (Instruction iq : qProgram.getInstructions()) {
            Label lq = iq.getLabel();
            if (lq == null || FixedLabel.EMPTY.equals(lq) || FixedLabel.EXIT.equals(lq)) continue;
            labelSub.putIfAbsent(lq.getLabelRepresentation(), helper.freshLabel());
        }
        Label lend = helper.freshLabel();

        List<String> args = parser.parseTopLevelArgs(functionArguments);
        for (int i = 0; i < args.size(); i++) {
            Variable dst = inputIndexToFresh.get(i + 1);
            if (dst == null) break;

            String tok = args.get(i).trim();
            if (tok.isEmpty()) {
                out.add(new ZeroVariableInstruction(dst, FixedLabel.EMPTY));
                continue;
            }

            if (parser.isNestedCall(tok)) {
                QuoteParser.NestedCall nc = parser.parseNestedCall(tok);
                out.add(new QuotationInstruction.Builder()
                        .variable(dst)
                        .funcName(nc.name())
                        .funcArguments(nc.argsCsv())
                        .parser(parser)
                        .registry(registry)
                        .varResolver(varResolver)
                        .myLabel(FixedLabel.EMPTY)
                        .build());
            } else {
                try {
                    Variable src = varResolver.resolve(tok);
                    out.add(new AssignmentInstruction(dst, src, FixedLabel.EMPTY));
                } catch (Exception e) {
                    long v = QuoteUtils.safeParseLong(tok);
                    out.add(new ConstantAssignmentInstruction(dst, v, FixedLabel.EMPTY));
                }
            }
        }

        for (Instruction iq : qProgram.getInstructions()) {
            out.add(cloneWithSubs(iq, varSub, labelSub, helper, lend));
        }

        out.add(new JumpEqualVariableInstruction.Builder()
                .variable(getVariable())
                .compareVariable(tempZ)
                .jeVariableLabel(jeFunctionLabel)
                .build());

        return out;
    }

    @Override
    public int degree() {
        int body = degreeOfProgram(functionName);
        int argsDepth = degreeOfArgs(functionArguments);
        return 1 + Math.max(body, argsDepth);
    }

    private int degreeOfProgram(String name) {
        Program p = registry.getProgramByName(name);
        return (p == null) ? 0 : p.calculateMaxDegree();
    }

    private int degreeOfArgs(String argsCsv) {
        int best = 0;
        for (String tok : parser.parseTopLevelArgs(argsCsv)) {
            best = Math.max(best, degreeOfArg(tok));
        }
        return best;
    }

    private int degreeOfArg(String tok) {
        tok = (tok == null) ? "" : tok.trim();
        if (tok.isEmpty()) return 0;

        if (!parser.isNestedCall(tok)) return 0;

        QuoteParser.NestedCall nc = parser.parseNestedCall(tok);
        int nestedArgsDepth = degreeOfArgs(nc.argsCsv());
        int calleeBody = degreeOfProgram(nc.name());
        return 1 + Math.max(calleeBody, nestedArgsDepth);
    }

    private static Variable subVar(Map<Variable, Variable> map, Variable key) {
        return map.getOrDefault(key, key);
    }

    private Label subLabel(Map<String, Label> map, Label key, ExpansionHelper helper, Label lend) {
        if (key == null || FixedLabel.EMPTY.equals(key)) return FixedLabel.EMPTY;
        if (FixedLabel.EXIT.equals(key)) return lend;
        String name = key.getLabelRepresentation();
        return map.computeIfAbsent(name, n -> helper.freshLabel());
    }

    private Instruction cloneWithSubs(Instruction iq,
                                      Map<Variable, Variable> varSub,
                                      Map<String, Label> labelSub,
                                      ExpansionHelper helper,
                                      Label lend) {
        if (iq == null) return null;
        Label newLbl = subLabel(labelSub, iq.getLabel(), helper, lend);

        if (iq instanceof IncreaseInstruction ii)
            return new IncreaseInstruction(subVar(varSub, ii.getVariable()), newLbl);
        if (iq instanceof DecreaseInstruction di)
            return new DecreaseInstruction(subVar(varSub, di.getVariable()), newLbl);
        if (iq instanceof ZeroVariableInstruction zi)
            return new ZeroVariableInstruction(subVar(varSub, zi.getVariable()), newLbl);
        if (iq instanceof JumpZeroInstruction jz)
            return new JumpZeroInstruction(subVar(varSub, jz.getVariable()),
                    subLabel(labelSub, jz.getJzLabel(), helper, lend), newLbl);
        if (iq instanceof JumpNotZeroInstruction jnz)
            return new JumpNotZeroInstruction(subVar(varSub, jnz.getVariable()),
                    subLabel(labelSub, jnz.getJnzLabel(), helper, lend), newLbl);
        if (iq instanceof GoToLabelInstruction gtl)
            return new GoToLabelInstruction(subLabel(labelSub, gtl.getgtlLabel(), helper, lend), newLbl);
        if (iq instanceof AssignmentInstruction ai)
            return new AssignmentInstruction(subVar(varSub, ai.getVariable()),
                    subVar(varSub, ai.getAssignedVariable()), newLbl);
        if (iq instanceof ConstantAssignmentInstruction cai)
            return new ConstantAssignmentInstruction(subVar(varSub, cai.getVariable()),
                    cai.getConstantValue(), newLbl);
        if (iq instanceof JumpEqualConstantInstruction jec)
            return new JumpEqualConstantInstruction.Builder()
                    .variable(subVar(varSub, jec.getVariable()))
                    .constantValue(jec.getConstantValue())
                    .jeConstantLabel(subLabel(labelSub, jec.getJeConstantLabel(), helper, lend))
                    .myLabel(newLbl)
                    .build();
        if (iq instanceof JumpEqualVariableInstruction jev)
            return new JumpEqualVariableInstruction.Builder()
                    .variable(subVar(varSub, jev.getVariable()))
                    .compareVariable(subVar(varSub, jev.getCompareVariable()))
                    .jeVariableLabel(subLabel(labelSub, jev.getJeVariableLabel(), helper, lend))
                    .myLabel(newLbl)
                    .build();

        return new NeutralInstruction(subVar(varSub, iq.getVariable()), newLbl);
    }

    public String getFunctionName() { return functionName; }
    public String getFunctionArguments() { return functionArguments; }
    public QuoteParser getParser() { return parser; }
}
