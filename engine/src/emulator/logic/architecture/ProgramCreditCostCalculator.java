package emulator.logic.architecture;

import emulator.logic.program.Program;
import emulator.logic.program.ProgramCost;
import emulator.logic.instruction.quote.QuotationRegistry;

public class ProgramCreditCostCalculator {

    private final QuotationRegistry quotationRegistry;

    public ProgramCreditCostCalculator(QuotationRegistry quotationRegistry) {
        this.quotationRegistry = quotationRegistry;
    }

    public long calculateTotalCost(Program program, ArchitectureType architectureType) {
        if (program == null)
            throw new IllegalArgumentException("Program cannot be null");
        long architectureCost = (architectureType != null) ? architectureType.getCost() : 0;
        long baseCost = new ProgramCost(quotationRegistry).cyclesAtDegree(program, 0);
        return baseCost + architectureCost;
    }
}
