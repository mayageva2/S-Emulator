//package emulator.logic.program;
//
//import emulator.logic.instruction.Instruction;
//import emulator.logic.instruction.InstructionCloner;
//
//public class ProgramDeepCopyFactory {
//
//    public static ProgramImpl copyProgram(ProgramImpl original) {
//        ProgramImpl p = new ProgramImpl(original.getName());
//
//        for (Instruction inst : original.getInstructions()) {
//            p.addInstruction(InstructionCloner.clone(inst));
//        }
//
//        return p;
//    }
//}
//
