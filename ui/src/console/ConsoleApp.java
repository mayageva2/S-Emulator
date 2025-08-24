package console;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import emulator.api.dto.RunResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConsoleApp {
    private final EmulatorEngine engine;

    public ConsoleApp(EmulatorEngine engine) {
        this.engine = engine;
    }

    public static void main(String[] args) {
        ConsoleIO io = new ConsoleIO(System.in, System.out);
        EmulatorEngine engine = new EmulatorEngineImpl();
        new ConsoleApp(engine).loop(io);
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
        Path path = Paths.get(io.ask("Path to XML: ").trim());
        var res = engine.loadProgram(path);
        io.println(res.ok() ? "Loaded." : "Failed: " + res.message());
    }

    private void doShowProgram(ConsoleIO io) {
        if (!requireLoaded(io)) return;

        ProgramView pv = engine.programView();

        for (InstructionView iv : pv.instructions()) {
            io.println(String.format(
                    "[%03d] %s %-6s %s%s",
                    iv.index(),
                    iv.basic() ? "B" : "S",
                    iv.label() == null ? "" : iv.label(),
                    iv.opcode(),
                    iv.args().isEmpty() ? "" : " " + String.join(", ", iv.args())
            ));
        }
    }

    private void doRun(ConsoleIO io) {
        if (!engine.hasProgramLoaded()) {
            io.println("No program loaded. Use 'Load program XML' first.");
            return;
        }

        String csv = io.ask("Enter inputs (comma-separated, e.g. 3,6,2): ");
        final List<Long> inputs;
        try {
            inputs = parseCsvLongs(csv);
        } catch (IllegalArgumentException ex) {
            io.println("Invalid input list: " + ex.getMessage());
            return;
        }

        try {
            RunResult res = engine.run(inputs.toArray(new Long[0]));
            io.println("Result y = " + res.y());
            io.println("Total cycles: " + res.cycles());
        } catch (Exception e) {
            io.println("Run failed: " + e.getMessage());
        }
    }

    private void doHistory(ConsoleIO io) {
        if (engine.history().isEmpty()) { io.println("No runs yet."); return; }
        //history print add
    }

    private boolean requireLoaded(ConsoleIO io) {
        if (!engine.hasProgramLoaded()) {
            io.println("No program loaded. Use 'Load program XML' first.");
            return false;
        }
        return true;
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
