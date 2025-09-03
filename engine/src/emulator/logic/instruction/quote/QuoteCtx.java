package emulator.logic.instruction.quote;

import emulator.logic.expansion.ExpansionHelper;
import emulator.logic.instruction.Instruction;
import emulator.logic.label.Label;
import emulator.logic.variable.Variable;

import java.util.List;
import java.util.Map;

public final class QuoteCtx {
    final ExpansionHelper h;
    final List<Instruction> qBody;
    final Variable[] zx;
    final Variable zy;
    final Map<String, Label> labelMap;
    final Label lend;
    final List<Instruction> prelude;
    QuoteCtx(ExpansionHelper h, List<Instruction> qBody, Variable[] zx, Variable zy,
             Map<String, Label> labelMap, Label lend, List<Instruction> prelude) {
        this.h = h; this.qBody = qBody; this.zx = zx; this.zy = zy;
        this.labelMap = labelMap; this.lend = lend; this.prelude = prelude;
    }
}
