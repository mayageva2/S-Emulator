//package emulator.logic.instruction;
//
//import emulator.logic.label.Label;
//import emulator.logic.label.LabelImpl;
//import emulator.logic.variable.Variable;
//import emulator.logic.variable.VariableImpl;
//
//import java.lang.reflect.Constructor;
//import java.util.Map;
//
//public class InstructionCloner {
//
//    public static Instruction clone(Instruction original) {
//        try {
//            InstructionData data = original.getInstructionData();
//
//            // Clone variable
//            Variable var = null;
//            if (original.getVariable() != null) {
//                var = new VariableImpl(
//                        original.getVariable().getType(),
//                        original.getVariable().getNumber()
//                );
//            }
//
//            // Clone label
//            Label lbl = null;
//            if (original.getLabel() != null) {
//                lbl = new LabelImpl(original.getLabel().getLabelRepresentation());
//            }
//
//            // Find constructor: (InstructionData, Variable, Label)
//            Constructor<?> ctor = original.getClass()
//                    .getConstructor(InstructionData.class, Variable.class, Label.class);
//
//            Instruction cloned = (Instruction) ctor.newInstance(data, var, lbl);
//
//            // Copy arguments
//            if (original.getArguments() != null) {
//                for (Map.Entry<String,String> e : original.getArguments().entrySet()) {
//                    if (cloned instanceof AbstractInstruction ai) {
//                        ai.setArgument(e.getKey(), e.getValue());
//                    }
//                }
//            }
//
//            return cloned;
//
//        } catch (Exception e) {
//            throw new RuntimeException("Cannot clone instruction of type " +
//                    original.getClass().getName(), e);
//        }
//    }
//}
