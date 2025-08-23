package console;

import emulator.logic.execution.ProgramExecutor;
import emulator.logic.execution.ProgramExecutorImpl;
import emulator.logic.instruction.Instruction;
import emulator.logic.instruction.InstructionData;
import emulator.logic.label.FixedLabel;
import emulator.logic.label.Label;
import emulator.logic.print.InstructionFormatter;
import emulator.logic.program.Program;
import emulator.logic.variable.Variable;
import emulator.logic.variable.VariableType;
import emulator.logic.xml.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ConsoleApp {
    private Program currentProgram = null;
    private final List<String> runHistory = new ArrayList<>();

    public static void main(String[] args) {
        ConsoleIO io = new ConsoleIO(System.in, System.out);
        new ConsoleApp().loop(io);
    }

    public void loop(ConsoleIO io) {
        io.println("S-Emulator (Console)");
        while (true) {
            showMenu(io);
            String choice = io.ask("Choose action [1-5]: ").trim();
            switch (choice) {
                case "1" -> doLoad(io);
                case "2" -> doShowProgram(io);
                case "3" -> doRun(io);
                case "4" -> doHistory(io);
                case "5" -> { io.println("Bye!"); return; }
                default -> io.println("Invalid choice. Try again.");
            }
            io.println("");
        }
    }

    private void showMenu(ConsoleIO io) {
        io.println("""
      1) Load program XML
      2) Show program
      3) Run
      4) History
      5) Exit
      """);
    }

    private void doLoad(ConsoleIO io) {
        String pathStr = io.ask("Enter full XML path: ");
        Path path = Paths.get(pathStr.trim());

        try {
            XmlProgramReader reader = new XmlProgramReader();
            ProgramXml pxml = reader.read(path);

            new XmlProgramValidator().validate(pxml);

            Program program = XmlToObjects.toProgram(pxml);

            this.currentProgram = program;
            io.println("Program loaded successfully.");
        } catch (XmlReadException e) {
            io.println("Load failed: " + e.getMessage());
        } catch (Exception e) {
            io.println("Load failed: " + e);
        }
    }

    private void doShowProgram(ConsoleIO io) {
        if (!requireLoaded(io)) return;

        Program p = currentProgram;
        io.println("Program: " + p.getName());

        List<String> inputs = p.getVariables().stream()
                .filter(v -> v.getType() == VariableType.INPUT)
                .sorted(Comparator.comparingInt(Variable::getNumber))
                .map(Variable::getRepresentation)
                .toList();
        io.println("Inputs:  " + String.join(", ", inputs));

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (Instruction instr : p.getInstructions()) {
            Label l = instr.getLabel();
            if (l != null && l != FixedLabel.EMPTY) {
                labels.add(l.getLabelRepresentation());
            }
        }
        labels.add(FixedLabel.EXIT.getLabelRepresentation());
        io.println("Labels:  " + String.join(", ", labels));

        io.println("--- Instructions ---");
        List<Instruction> list = p.getInstructions();
        for (int i = 0; i < list.size(); i++) {
            io.println(formatInstruction(i + 1, list.get(i)));
        }
    }

    private void doRun(ConsoleIO io) {
        if (!requireLoaded(io)) return;

        String csv = io.ask("Enter inputs (comma-separated, e.g. 3,6,2): ");
        List<Long> inputs;
        try {
            inputs = parseCsvLongs(csv);
        } catch (IllegalArgumentException ex) {
            io.println("Invalid input list: " + ex.getMessage());
            return;
        }

        try {
            ProgramExecutor exec = new ProgramExecutorImpl(currentProgram);
            long y = exec.run(inputs.toArray(new Long[0]));
            io.println("Result y = " + y);

            int cycles = currentProgram.calculateCycles();
            io.println("Total cycles: " + cycles);

            String hist = String.format("run#%d | inputs=[%s] | y=%d | cycles=%d",
                    runHistory.size() + 1,
                    inputs.stream().map(Object::toString).collect(Collectors.joining(",")),
                    y, cycles);
            runHistory.add(hist);
        } catch (Exception e) {
            io.println("Run failed: " + e.getMessage());
        }
    }

    private void doHistory(ConsoleIO io) {
        if (runHistory.isEmpty()) {
            io.println("No runs yet.");
            return;
        }
        io.println("#  | inputs                | y    | cycles");
        io.println("----+-----------------------+------+-------");
        for (int i = 0; i < runHistory.size(); i++) {
            io.println(runHistory.get(i));
        }
    }

    private boolean requireLoaded(ConsoleIO io) {
        if (currentProgram == null) {
            io.println("No program loaded. Use 'Load program XML' first.");
            return false;
        }
        return true;
    }

    private String formatInstruction(int index1, Instruction instr) {
        String label = "";
        if (instr.getLabel() != null && instr.getLabel() != FixedLabel.EMPTY) {
            label = instr.getLabel().getLabelRepresentation();
        }
        InstructionData data = InstructionData.valueOf(instr.getName());
        String type = data.isBasic() ? "B" : "S";
        int cycles = data.getCycles();

        String body = new InstructionFormatter().format(instr);
        return String.format("#%-3d (%s) [%-3s] %s (%d)", index1, type, label, body, cycles);
    }

    private List<Long> parseCsvLongs(String csv) {
        if (csv == null || csv.trim().isEmpty()) return List.of();
        String[] parts = csv.split(",");
        List<Long> out = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (s.isEmpty()) continue;
            try {
                out.add(Long.parseLong(s));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number: " + s);
            }
        }
        return out;
    }
}
