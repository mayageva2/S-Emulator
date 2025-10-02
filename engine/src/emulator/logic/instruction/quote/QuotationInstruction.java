package emulator.logic.instruction.quote;

import emulator.logic.execution.ExecutionContext;
import emulator.logic.execution.QuoteEvaluator;
import emulator.logic.expansion.Expandable;
import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.instruction.*;
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
        public Builder varResolver(VarResolver var) { this.varResolver = var; return this; }

        public QuotationInstruction build() { return new QuotationInstruction(this); }
    }

    @Override
    public Label execute(ExecutionContext ctx) {
        QuoteEvaluator evaluator = (ctx != null) ? ctx.getQuoteEvaluator() : null;
        if (evaluator == null) {
            throw new IllegalStateException("QuoteEvaluator is not available in ExecutionContext");
        }
        long resultY = QuoteUtils.runQuotedEval(functionName, functionArguments, ctx, registry, parser, varResolver, evaluator);
        ctx.updateVariable(getVariable(), resultY);
        return FixedLabel.EMPTY;
    }

    @Override
    public List<Instruction> expand(ExpansionHelper helper) {
        List<Instruction> out = new ArrayList<>();
        Label origLbl = getLabel();
        if (origLbl != null && !FixedLabel.EMPTY.equals(origLbl)) {
            out.add(new NeutralInstruction(getVariable(), origLbl));
        }

        Program qProgram = registry.getProgramByName(functionName);
        Map<Variable, Variable> varSub = new HashMap<>();
        Map<Integer, Variable> inputIndexToFresh = new HashMap<>();
        Variable newY = null;

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
                        newY = helper.freshVar();
                        varSub.put(qv, newY);
                    }
                }
            }
        }
        if (newY == null) {
            newY = helper.freshVar();
        }

        Label lend = helper.freshLabel();
        Map<String, Label> labelSub = new HashMap<>();
        int nInputs = inputIndexToFresh.isEmpty() ? 0 : Collections.max(inputIndexToFresh.keySet());
        for (int i = 1; i <= nInputs; i++) {
            Variable dstZi = inputIndexToFresh.get(i);
            if (dstZi == null) {
                Variable filler = helper.freshVar();
                out.add(new ZeroVariableInstruction(filler, FixedLabel.EMPTY));
                continue;
            }

            String tok = (i - 1 < rawArgs.size()) ? rawArgs.get(i - 1) : "";
            if (tok.isBlank()) {
                out.add(new ZeroVariableInstruction(dstZi, FixedLabel.EMPTY));
            } else if (parser.isNestedCall(tok)) {
                QuoteParser.NestedCall nc = parser.parseNestedCall(tok);
                out.add(new Builder()
                        .variable(dstZi)
                        .funcName(nc.name())
                        .funcArguments(nc.argsCsv())
                        .parser(parser)
                        .registry(registry)
                        .varResolver(varResolver)
                        .build());
            } else {
                Variable src = resolveLoose(varResolver, tok);
                out.add(new AssignmentInstruction(dstZi, src, FixedLabel.EMPTY));
            }
        }

        for (Instruction iq : qProgram.getInstructions()) {
            out.add(cloneWithSubs(iq, varSub, labelSub, helper, lend));
        }

        out.add(new AssignmentInstruction(getVariable(), newY, lend));
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

    private Variable resolveLoose(VarResolver varResolver, String tok) {
        try {
            return varResolver.resolve(tok);
        } catch (RuntimeException ex) {
            String t = tok.trim();
            if ("y".equals(t)) {
                return new emulator.logic.variable.VariableImpl(
                        emulator.logic.variable.VariableType.RESULT, 0);
            } else if (t.length() >= 2 && (t.charAt(0) == 'x' || t.charAt(0) == 'z')) {
                char kind = t.charAt(0);
                int idx = Integer.parseInt(t.substring(1));
                if (idx <= 0) throw new IllegalArgumentException("Illegal variable index: " + tok);
                return new emulator.logic.variable.VariableImpl(
                        (kind == 'x') ? emulator.logic.variable.VariableType.INPUT
                                : emulator.logic.variable.VariableType.WORK,
                        idx);
            }
            throw ex;
        }
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

        Label newLbl = subLabel(labelSub, iq.getLabel(), helper, lend);

        if (iq instanceof IncreaseInstruction inc) {
            return new IncreaseInstruction(subVar(varSub, inc.getVariable()), newLbl);
        }
        if (iq instanceof DecreaseInstruction dec) {
            return new DecreaseInstruction(subVar(varSub, dec.getVariable()), newLbl);
        }
        if (iq instanceof NeutralInstruction neu) {
            return new NeutralInstruction(subVar(varSub, neu.getVariable()), newLbl);
        }
        if (iq instanceof ZeroVariableInstruction zv) {
            return new ZeroVariableInstruction(subVar(varSub, zv.getVariable()), newLbl);
        }
        if (iq instanceof GoToLabelInstruction gl) {
            return new GoToLabelInstruction(newLbl, subLabel(labelSub, gl.getgtlLabel(), helper, lend));
        }
        if (iq instanceof JumpNotZeroInstruction jnz) {
            return new JumpNotZeroInstruction(
                    subVar(varSub, jnz.getVariable()),
                    subLabel(labelSub, jnz.getJnzLabel(), helper, lend),
                    newLbl);
        }
        if (iq instanceof AssignmentInstruction asg) {
            return new AssignmentInstruction(
                    subVar(varSub, asg.getVariable()),
                    subVar(varSub, asg.getAssignedVariable()),
                    newLbl);
        }
        if (iq instanceof ConstantAssignmentInstruction ca) {
            return new ConstantAssignmentInstruction(
                    subVar(varSub, ca.getVariable()),
                    ca.getConstantValue(),
                    newLbl);
        }
        if (iq instanceof JumpZeroInstruction jz) {
            return new JumpZeroInstruction(
                    subVar(varSub, jz.getVariable()),
                    subLabel(labelSub, jz.getJzLabel(), helper, lend),
                    newLbl);
        }
        if (iq instanceof JumpEqualConstantInstruction jec) {
            return new JumpEqualConstantInstruction.Builder()
                    .variable(subVar(varSub, jec.getVariable()))
                    .constantValue(jec.getConstantValue())
                    .jeConstantLabel(subLabel(labelSub, jec.getJeConstantLabel(), helper, lend))
                    .myLabel(newLbl)
                    .build();
        }
        if (iq instanceof JumpEqualVariableInstruction jev) {
            return new JumpEqualVariableInstruction.Builder()
                    .variable(subVar(varSub, jev.getVariable()))
                    .compareVariable(subVar(varSub, jev.getCompareVariable()))
                    .jeVariableLabel(subLabel(labelSub, jev.getJeVariableLabel(), helper, lend))
                    .myLabel(newLbl)
                    .build();
        }
        if (iq instanceof QuotationInstruction qi) {
            return new QuotationInstruction.Builder()
                    .variable(subVar(varSub, qi.getVariable()))
                    .funcName(qi.functionName())
                    .funcArguments(qi.functionArguments())
                    .parser(qi.parser)
                    .registry(qi.registry)
                    .varResolver(qi.varResolver)
                    .myLabel(newLbl)
                    .build();
        }

        return new NeutralInstruction(subVar(varSub, iq.getVariable()), newLbl);
    }

    public String functionName() { return functionName; }
    public String functionArguments() { return functionArguments; }
    public List<String> rawArgs() { return rawArgs; }
    public QuoteParser getParser() {return parser;}
    public String getFunctionName() {return functionName;}
    public String getFunctionArguments() {return functionArguments;}

}
