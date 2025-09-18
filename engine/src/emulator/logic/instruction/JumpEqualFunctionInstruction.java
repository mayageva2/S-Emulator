package emulator.logic.instruction;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.instruction.quote.*;
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

    //This func builds instruction
    public static class Builder {
        private Variable variable;
        private Label jeFunctionLabel;
        private String funcName;
        private String funcArguments;
        private Label myLabel;

        private QuoteParser parser;
        private QuotationRegistry registry;
        private VarResolver varResolver;

        public Builder variable(Variable variable) {
            this.variable = variable;
            return this;
        }

        public Builder jeFunctionLabel(Label label) {
            this.jeFunctionLabel = label;
            return this;
        }

        public Builder funcName(String funcName) {
            this.funcName = funcName;
            return this;
        }

        public Builder funcArguments(String funcArguments) {
            this.funcArguments = funcArguments;
            return this;
        }

        public Builder myLabel(Label label) {
            this.myLabel = label;
            return this;
        }

        public Builder parser(QuoteParser parser) {
            this.parser = parser;
            return this;
        }

        public Builder registry(QuotationRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder varResolver(VarResolver varResolver) {
            this.varResolver = varResolver;
            return this;
        }

        public JumpEqualFunctionInstruction build() {
            return new JumpEqualFunctionInstruction(this);
        }
    }

    //This func executes the instruction
    @Override
    public Label execute(ExecutionContext ctx) {
        long vVal = ctx.getVariableValue(getVariable());
        long qVal = QuoteUtils.runQuotedEval(functionName, functionArguments, ctx, registry, parser, varResolver);
        return (vVal == qVal) ? jeFunctionLabel : FixedLabel.EMPTY;
    }

    //This func expands the instruction
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
            String tok = args.get(i);
            if (parser.isNestedCall(tok)) {
                var nc = parser.parseNestedCall(tok);
                long v = QuoteUtils.runQuotedEval(nc.name(), nc.argsCsv(), QuoteUtils.newScratchCtx(), registry, parser, varResolver);
                out.add(new ConstantAssignmentInstruction(dst, v));
            } else {
                try {
                    Variable src = varResolver.resolve(tok);
                    out.add(new AssignmentInstruction(dst, src));
                } catch (Exception e) {
                    long v = QuoteUtils.safeParseLong(tok);
                    out.add(new ConstantAssignmentInstruction(dst, v));
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
        Program q = registry.getProgramByName(functionName);
        int body = (q == null) ? 0 : q.calculateMaxDegree();

        int argsDepth = 0;
        for (String tok : parser.parseTopLevelArgs(functionArguments)) {
            if (parser.isNestedCall(tok)) {
                QuoteParser.NestedCall nc = parser.parseNestedCall(tok);
                Program p = registry.getProgramByName(nc.name());
                int d = 1 + ((p == null) ? 0 : p.calculateMaxDegree());
                if (d > argsDepth) argsDepth = d;
            }
        }

        return 1 + Math.max(body, argsDepth);
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
}

