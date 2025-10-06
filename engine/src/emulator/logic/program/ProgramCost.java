package emulator.logic.program;

import emulator.logic.expansion.ProgramExpander;
import emulator.logic.instruction.*;
import emulator.logic.instruction.quote.QuoteParser;
import emulator.logic.instruction.quote.QuotationInstruction;
import emulator.logic.instruction.quote.QuotationRegistry;

public final class ProgramCost {

    private final QuotationRegistry registry;

    public ProgramCost(QuotationRegistry registry) {
        this.registry = registry;
    }

    public int cyclesAtDegree(Program program, int degree) {
        Program expanded = new ProgramExpander().expandToDegree(program, degree);
        int total = 0;
        for (Instruction ins : expanded.getInstructions()) {
            total += computeCycles(ins);
        }
        return total;
    }

    public int cyclesFullyExpanded(Program program) {
        int maxDeg = program.calculateMaxDegree();
        return cyclesAtDegree(program, maxDeg);
    }

    private int computeCycles(Instruction instr) {
        if (instr == null) return 0;

        InstructionData data = instr.getInstructionData();

        if (instr instanceof QuotationInstruction qi) {
            return computeQuoteCycles(qi, data);
        }
        if (instr instanceof JumpEqualFunctionInstruction jefi) {
            return computeJumpEqualFunctionCycles(jefi, data);
        }
        return data.getCycles();
    }

    private int computeQuoteCycles(QuotationInstruction qi, InstructionData data) {
        int total = data.getCycles();

        Program inner = registry.getProgramByName(qi.functionName());
        if (inner != null) {
            for (Instruction innerInstr : inner.getInstructions()) {
                total += computeCycles(innerInstr);
            }
        }

        QuoteParser parser = qi.getParser();
        for (String arg : qi.rawArgs()) {
            total += computeNestedArgCycles(parser, arg);
        }

        return total;
    }

    private int computeJumpEqualFunctionCycles(JumpEqualFunctionInstruction jefi, InstructionData data) {
        int total = data.getCycles();

        Program inner = registry.getProgramByName(jefi.getFunctionName());
        if (inner != null) {
            for (Instruction innerInstr : inner.getInstructions()) {
                total += computeCycles(innerInstr);
            }
        }

        QuoteParser parser = jefi.getParser();
        for (String arg : parser.parseTopLevelArgs(jefi.getFunctionArguments())) {
            total += computeNestedArgCycles(parser, arg);
        }

        return total;
    }

    private int computeNestedArgCycles(QuoteParser parser, String arg) {
        int total = 0;
        if (parser.isNestedCall(arg)) {
            var nc = parser.parseNestedCall(arg);

            Program nested = registry.getProgramByName(nc.name());
            if (nested != null) {
                for (Instruction innerInstr : nested.getInstructions()) {
                    total += computeCycles(innerInstr);
                }
            }

            for (String subArg : parser.parseTopLevelArgs(nc.argsCsv())) {
                total += computeNestedArgCycles(parser, subArg);
            }
        }
        return total;
    }

}
