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
    private Path lastXmlPath;

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
            String choice = io.ask("Choose action [1-6]: ").trim();
            switch (choice) {
                case "1" -> doLoad(io);
                case "2" -> doShowProgram(io);
                case "3" -> doRun(io);
                case "4" -> doHistory(io);
                case "5" -> doSaveVersion(io);
                case "6" -> { io.println("Bye!"); return; }
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
      5) Save version
      6) Exit
      """);
    }

    private void doLoad(ConsoleIO io) {
        Path path = Paths.get(io.ask("Path to XML: ").trim());
        var res = engine.loadProgram(path);
        io.println(res.ok() ? "Loaded." : "Failed: " + res.message());
        if (res.ok()) {
            lastXmlPath = path;
        }
    }

    private void doShowProgram(ConsoleIO io) {
        if (!requireLoaded(io)) return;

        var pv = engine.programView();

        for (var iv : pv.instructions()) {
            String line = formatInstruction(iv);
            io.println(line);
        }
    }

    private String formatInstruction(InstructionView iv) {

        String index = "#" + iv.index();
        String type = iv.basic() ? "(B)" : "(S)";
        String label = iv.label() == null ? "" : iv.label();
        String labelField = String.format("[%-5s]", label);
        String command = prettyCommand(iv);
        String cycles = "(" + iv.cycles() + ")";

        return String.format("%-4s %-4s %-8s %-20s %s",
                index, type, labelField, command, cycles);
    }

    private String prettyCommand(InstructionView iv) {
        var args = iv.args();
        switch (iv.opcode()) {
            case "INCREASE": return args.get(0) + "<-" + args.get(0) + " + 1";
            case "DECREASE": return args.get(0) + "<-" + args.get(0) + " - 1";
            case "NEUTRAL": return args.get(0) + "<-" + args.get(0);
            case "ZERO_VARIABLE": return args.get(0) + "<-0";
            case "JUMP_NOT_ZERO": return "IF " + args.get(0) + " != 0 GOTO " + getArg(args,"JNZLabel");
            case "GOTO_LABEL": return "GOTO " + getArg(args,"gotoLabel");
            case "ASSIGNMENT": return args.get(0) + "<-" + getArg(args,"assignedVariable");
            case "CONSTANT_ASSIGNMENT": return args.get(0) + "<-" + getArg(args,"constantValue");
            case "JUMP_ZERO": return "IF " + args.get(0) + " = 0 GOTO " + getArg(args,"JZLabel");
            case "JUMP_EQUAL_CONSTANT": return "IF " + args.get(0) + " = " + getArg(args,"constantValue")
                    + " GOTO " + getArg(args,"JEConstantLabel");
            case "JUMP_EQUAL_VARIABLE": return "IF " + args.get(0) + " = " + getArg(args,"variableName")
                    + " GOTO " + getArg(args,"JEVariableLabel");
            default: return iv.opcode() + " " + String.join(", ", args);
        }
    }

    private String getArg(List<String> args, String key) {
        for (String a : args) {
            int eq = a.indexOf('=');
            if (eq > 0 && a.substring(0,eq).equals(key)) {
                return a.substring(eq+1);
            }
        }
        return "";
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

        int degree = 0;
        try {
            String d = io.ask("Choose expansion degree (0 = none): ").trim();
            degree = d.isEmpty() ? 0 : Integer.parseInt(d);
            if (degree < 0) degree = 0;
        } catch (NumberFormatException ex) {
            io.println("Invalid degree. Using 0.");
            degree = 0;
        }

        try {
            RunResult res = engine.run(degree, inputs.toArray(new Long[0]));
            io.println("Result y = " + res.y());
            io.println("Total cycles: " + res.cycles());
        } catch (Exception e) {
            io.println("Run failed: " + e.getMessage());
        }
    }

    private void doHistory(ConsoleIO io) {
        var history = engine.history();
        if (history.isEmpty()) {
            io.println("No runs yet.");
            return;
        }

        io.println("Run# | Degree | Inputs         | y   | Cycles");
        io.println("-----+--------+----------------+-----+-------");

        for (var r : history) {
            io.println(String.format("%4d | %6d | %-14s | %3d | %5d",
                    r.runNumber(),
                    r.degree(),
                    r.inputsCsv(),
                    r.y(),
                    r.cycles()));
        }
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

    private void doSaveVersion(ConsoleIO io) {
        if (!engine.hasProgramLoaded()) {
            io.println("No program loaded. Load a program first.");
            return;
        }

        Path xmlPath = this.lastXmlPath;
        if (xmlPath == null) {
            String p = io.ask("Path to XML to save as version: ").trim();
            if (p.isEmpty()) {
                io.println("No path provided.");
                return;
            }
            xmlPath = Paths.get(p);
        }

        String vStr = io.ask("Version number (e.g. 1): ").trim();
        int version;
        try {
            version = Integer.parseInt(vStr);
            if (version < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            io.println("Invalid version number: " + vStr);
            return;
        }

        try {
            Path saved = engine.saveOrReplaceVersion(xmlPath, version);
            io.println("[INFO] Saved version " + version + " to " + saved);
        } catch (Exception e) { // IOException או אחרות
            io.println("[WARN] Could not save version: " + e.getMessage());
        }
    }
}
