package console;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConsoleApp {
    private final EmulatorEngine engine;
    private Path lastXmlPath;
    private int lastMaxDegree = 0;

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
            String choice = io.ask("Choose action [1-7]: ").trim();
            switch (choice) {
                case "1" -> doLoad(io);
                case "2" -> doShowProgram(io);
                case "3" -> doExpand(io);
                case "4" -> doRun(io);
                case "5" -> doHistory(io);
                case "6" -> doSaveVersion(io);
                case "7" -> { io.println("Bye!"); return; }
                default -> io.println("Invalid choice. Try again.");
            }
            io.println("");
        }
    }

    private void showMenu(ConsoleIO io) {
        io.println("""
      1) Load program XML
      2) Show program
      3) Show Expanded program
      4) Run
      5) History
      6) Save version
      7) Exit
      """);
    }

    private void doLoad(ConsoleIO io) {
        String raw = io.ask("Enter full XML path: ").trim();
        if (raw.isEmpty()) {
            io.println("Path cannot be empty.");
            return;
        }

        Path path = Paths.get(raw);
        if (!Files.exists(path)) {
            io.println("File not found: " + path.toAbsolutePath());
            return;
        }
        if (!raw.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            io.println("Please select a .xml file.");
            return;
        }

        try {
            var res = engine.loadProgram(path);
            lastMaxDegree = res.maxDegree();
            lastXmlPath = path;
            io.println("Loaded '" + res.programName() + "' with " + res.instructionCount() + " instructions. Max degree: " + lastMaxDegree);
        } catch (Exception ex) {
            io.println("Load failed: " + (ex.getMessage() == null ? "Unknown error" : ex.getMessage()));
        }
    }

    private void doShowProgram(ConsoleIO io) {
        if (!requireLoaded(io)) return;
        ProgramView pv = engine.programView();
        printInstructions(io, pv);
    }

    private void doExpand(ConsoleIO io) {
        if (!requireLoaded(io)) return;

        String dg = io.ask("Choose expansion degree (0-" + lastMaxDegree + "): ").trim();
        int degree;
        try { degree = Integer.parseInt(dg); } catch (NumberFormatException e) { degree = 0; }
        if (degree < 0) degree = 0;
        if (degree > lastMaxDegree) degree = lastMaxDegree;

        ProgramView pv = engine.programView(degree);
        printInstructionsWithProvenance(io, pv);
    }

    private void doRun(ConsoleIO io) {
        if (!engine.hasProgramLoaded()) {
            io.println("No program loaded. Use 'Load program XML' first.");
            return;
        }

        String dg = io.ask("Choose expansion degree (0-" + lastMaxDegree + "): ").trim();
        int degree;
        try {
            degree = Integer.parseInt(dg);
        } catch (NumberFormatException e) {
            degree = 0;
        }

        if (degree < 0) degree = 0;
        if (degree > lastMaxDegree) {
            io.println("Degree " + degree + " exceeds max; using " + lastMaxDegree + " instead.");
            degree = lastMaxDegree;
        }

        var pv0 = engine.programView();
        List<String> usedInputIdx = engine.extractInputVars(pv0);
        io.println("The inputs of this program are:");
        io.println(String.join(", ", usedInputIdx));

        String csv = io.ask("Enter inputs (comma-separated, e.g. 3,6,2): ").trim();
        Long[] inputs = csv.isEmpty() ? new Long[0]
                : Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).toArray(Long[]::new);

        try {
            var result = engine.run(degree, inputs);

            ProgramView pv = engine.programView();
            if (degree == 0) {
                printInstructions(io, pv);
            } else {
                printInstructionsWithProvenance(io, pv);
            }

            io.println("Result y = " + result.y());
            io.println("Total cycles = " + result.cycles());
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

    private int countRealRows(ProgramView pv) {
        int expect = 1, count = 0;
        for (InstructionView iv : pv.instructions()) {
            if (iv.index() == expect) { count++; expect++; } else break;
        }
        return count;
    }

    private void printInstructions(ConsoleIO io, ProgramView pv) {
        for (InstructionView iv : pv.instructions()) {
            if (isVirtual(iv)) continue;
            io.println(formatInstruction(iv));
        }
    }

    private void printInstructionsWithProvenance(ConsoleIO io, ProgramView pv) {
        Map<Integer, InstructionView> byIndex = new HashMap<>();
        for (InstructionView iv : pv.instructions()) byIndex.put(iv.index(), iv);

        for (InstructionView iv : pv.instructions()) {
            if (isVirtual(iv)) continue;
            String base = formatInstruction(iv);
            String chain = formatProvenanceChain(iv, byIndex);
            io.println(chain.isEmpty() ? base : (base + "  <<<   " + chain));
        }
    }

    private String formatProvenanceChain(InstructionView iv, Map<Integer, InstructionView> byIndex) {
        List<Integer> chain = iv.createdFromChain();
        if (chain == null || chain.isEmpty()) return "";
        int self = iv.index();
        List<String> parts = new ArrayList<>();
        for (Integer idx : chain) {
            if (idx != null && !idx.equals(self)) {
                InstructionView parent = byIndex.get(idx);
                if (parent != null) parts.add(formatInstruction(parent));
            }
        }
        return String.join("  <<<   ", parts);
    }

    private String formatInstruction(InstructionView iv) {
        String index = "#" + iv.index();
        String type = iv.basic() ? "(B)" : "(S)";
        String label = iv.label() == null ? "" : iv.label();
        String labelField = String.format("[ %-3s ]", label); // 5-wide label, as spec
        String command = prettyCommand(iv);
        String cycles = "(" + iv.cycles() + ")";
        return String.format("%-4s %-4s %-8s %-20s %s", index, type, labelField, command, cycles);
    }

    private String prettyCommand(InstructionView iv) {
        var args = iv.args();
        switch (iv.opcode()) {
            case "INCREASE": return args.get(0) + "<-" + args.get(0) + " + 1";
            case "DECREASE": return args.get(0) + "<-" + args.get(0) + " - 1";
            case "NEUTRAL":  return args.get(0) + "<-" + args.get(0);
            case "ZERO_VARIABLE": return args.get(0) + "<-0";

            case "JUMP_NOT_ZERO": {
                String tgt = findLabel(args);
                return "IF " + args.get(0) + " != 0 GOTO " + tgt;
            }
            case "JUMP_ZERO": {
                String tgt = findLabel(args);
                return "IF " + args.get(0) + " = 0 GOTO " + tgt;
            }
            case "GOTO_LABEL": {
                String tgt = findLabel(args);
                return "GOTO " + tgt;
            }
            case "JUMP_EQUAL_CONSTANT": {
                String tgt = findLabel(args);
                return "IF " + args.get(0) + " = " + getArg(args, "constantValue") + " GOTO " + tgt;
            }
            case "JUMP_EQUAL_VARIABLE": {
                String tgt = findLabel(args);
                return "IF " + args.get(0) + " = " + getArg(args, "variableName") + " GOTO " + tgt;
            }

            case "ASSIGNMENT":           return args.get(0) + "<-" + getArg(args,"assignedVariable");
            case "CONSTANT_ASSIGNMENT":  return args.get(0) + "<-" + getArg(args,"constantValue");
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
        } catch (Exception e) {
            io.println("[WARN] Could not save version: " + e.getMessage());
        }
    }

    private boolean isVirtual(InstructionView iv) {
        for (String a : iv.args()) {
            int eq = a.indexOf('=');
            if (eq > 0) {
                String k = a.substring(0, eq);
                String v = a.substring(eq + 1);
                if ("__virtual__".equals(k) && "1".equals(v)) return true;
            }
        }
        return false;
    }

    private String findLabel(List<String> args) {

        String v;
        if (!(v = getArg(args, "JNZLabel")).isEmpty()) return v;
        if (!(v = getArg(args, "gotoLabel")).isEmpty()) return v;
        if (!(v = getArg(args, "JZLabel")).isEmpty()) return v;
        if (!(v = getArg(args, "JEConstantLabel")).isEmpty()) return v;
        if (!(v = getArg(args, "JEVariableLabel")).isEmpty()) return v;

        for (String a : args) {
            int eq = a.indexOf('=');
            if (eq > 0) {
                String k = a.substring(0, eq).toLowerCase(Locale.ROOT);
                if (k.contains("label") || k.contains("goto") || k.contains("target")) {
                    return a.substring(eq + 1);
                }
            }
        }

        for (String a : args) {
            int eq = a.indexOf('=');
            if (eq > 0) {
                String val = a.substring(eq + 1);
                if (val.matches("[A-Za-z]?\\d+") || val.matches("L\\d+")) return val;
            }
        }
        return "";
    }

}
