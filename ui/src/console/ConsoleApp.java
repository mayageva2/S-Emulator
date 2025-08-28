package console;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleApp {
    private final EmulatorEngine engine;
    private Path lastXmlPath;
    private int lastMaxDegree = 0;
    private String lastProgramName;

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
            lastProgramName = res.programName();
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
        io.println("Program: " + (lastProgramName == null ? "(unknown)" : lastProgramName));

        List<String> inputsInUseOrder = extractInputsInUseOrder(pv);
        io.println("Inputs: " + (inputsInUseOrder.isEmpty() ? "(none)" : String.join(", ", inputsInUseOrder)));

        List<String> labelsInUseOrder = extractLabelsInUseOrder(pv);
        io.println("Labels: " + (labelsInUseOrder.isEmpty() ? "(none)" : String.join(", ", labelsInUseOrder)));
        printInstructions(io, pv);
    }

    private void doExpand(ConsoleIO io) {
        if (!requireLoaded(io)) return;


        if (lastMaxDegree == 0) {
            ProgramView pv = engine.programView(0);
            printInstructions(io, pv);
        } else {
            String dg = io.ask("Choose expansion degree (0-" + lastMaxDegree + "): ").trim();
            int degree;
            try {
                degree = Integer.parseInt(dg);
            } catch (NumberFormatException e) {
                degree = 0;
            }
            if (degree < 0) degree = 0;
            if (degree > lastMaxDegree) degree = lastMaxDegree;

            ProgramView pv = engine.programView(degree);
            printInstructionsWithProvenance(io, pv);
        }
    }

    private void doRun(ConsoleIO io) {
        if (!engine.hasProgramLoaded()) {
            io.println("No program loaded. Use 'Load program XML' first.");
            return;
        }

        int degree;
        while (true) {
            String dg = io.ask("Choose expansion degree (0-" + lastMaxDegree + "): ").trim();
            if (dg.isEmpty()) {
                degree = 0;
                break;
            }
            try {
                degree = Integer.parseInt(dg);
            } catch (NumberFormatException e) {
                io.println("Invalid number. Please enter an integer between 0 and " + lastMaxDegree + ".");
                continue;
            }
            if (degree < 0) {
                io.println("Degree cannot be negative. Try again.");
                continue;
            }
            if (degree > lastMaxDegree) {
                io.println("Degree " + degree + " exceeds max (" + lastMaxDegree + "). Try again.");
                continue;
            }
            break;
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
            printAllVariables(io, result, inputs);
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

        List<String> inputsPretty = new ArrayList<>(history.size());
        int inputsColWidth = "Inputs".length();

        for (var r : history) {
            String s = formatInputsByPosition(r.inputsCsv());
            inputsPretty.add(s);
            if (s.length() > inputsColWidth) inputsColWidth = s.length();
        }

        io.println(String.format("Run# | Degree | %-" + inputsColWidth + "s | y   | Cycles", "Inputs"));
        io.println(
                repeat('-', 5) + "+" +
                repeat('-', 8) + "+" +
                repeat('-', inputsColWidth + 2) + "+" +
                repeat('-', 5) + "+" +
                repeat('-', 7)
        );

        String rowFmt = "%4d | %6d | %-" + inputsColWidth + "s | %3d | %5d";
        for (int i = 0; i < history.size(); i++) {
            var r = history.get(i);
            io.println(String.format(
                    rowFmt,
                    r.runNumber(),
                    r.degree(),
                    inputsPretty.get(i),
                    r.y(),
                    r.cycles()
            ));
        }
    }

    private String formatInputsByPosition(String csv) {
        if (csv == null || csv.isBlank()) return "";
        String[] parts = csv.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append('x').append(i + 1).append('=').append(s);
        }
        return sb.toString();
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(Math.max(0, n));
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
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

    private void printInstructionsWithProvenance(ConsoleIO io, ProgramView pvExpanded) {
        ProgramView pvOriginal = engine.programView(0);
        Map<Integer, InstructionView> originalByIndex = new HashMap<>();
        for (InstructionView iv0 : pvOriginal.instructions()) {
            originalByIndex.put(iv0.index(), iv0);
        }

        for (InstructionView iv : pvExpanded.instructions()) {
            if (isVirtual(iv)) continue;
            String base = formatInstruction(iv);

            String chain;
            if (iv.createdFromViews() != null && !iv.createdFromViews().isEmpty()) {
                chain = formatProvenanceChainFromViews(iv.createdFromViews());
            } else {
                chain = formatProvenanceChain(iv, originalByIndex);
            }

            io.println(chain == null || chain.isEmpty() ? base : (base + "  <<<   " + chain));
        }
    }

    private String formatProvenanceChainFromViews(List<InstructionView> chainViews) {
        List<String> parts = new ArrayList<>();
        for (InstructionView v : chainViews) {
            parts.add(formatInstruction(v));
        }
        return String.join("  <<<   ", parts);
    }

    private String formatProvenanceChain(InstructionView iv, Map<Integer, InstructionView> originalByIndex) {
        List<Integer> chain = iv.createdFromChain();
        if (chain == null || chain.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (Integer idx : chain) {
            if (idx == null) continue;
            InstructionView origin = originalByIndex.get(idx);
            if (origin != null) parts.add(formatInstruction(origin));
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

    private void printAllVariables(ConsoleIO io, RunResult result, Long[] inputs) {
        List<VariableView> vars = (result == null || result.vars() == null) ? List.of() : result.vars();

        VariableView yVar = null;
        Map<Integer, Long> xValues = new TreeMap<>();
        Map<Integer, Long> zValues = new TreeMap<>();

        for (VariableView v : vars) {
            if (v == null) continue;
            String name = v.name() == null ? "" : v.name().toLowerCase(Locale.ROOT);
            if (v.type() == VarType.RESULT || "y".equals(name)) {
                yVar = v;
            } else if (v.type() == VarType.INPUT || name.startsWith("x")) {
                xValues.put(v.number(), v.value());
            } else if (v.type() == VarType.WORK || name.startsWith("z")) {
                zValues.put(v.number(), v.value());
            }
        }

        if (inputs != null) {
            for (int i = 0; i < inputs.length; i++) {
                int idx = i + 1;
                xValues.putIfAbsent(idx, inputs[i]);
            }
        }

        io.println("Variables:");
        if (yVar != null) {
            io.println(yVar.name() + " = " + yVar.value());
        } else {
            io.println("y = " + (result == null ? 0 : result.y()));
        }

        for (Map.Entry<Integer, Long> e : xValues.entrySet()) {
            io.println("x" + e.getKey() + " = " + e.getValue());
        }

        for (Map.Entry<Integer, Long> e : zValues.entrySet()) {
            io.println("z" + e.getKey() + " = " + e.getValue());
        }
    }

    private List<String> extractInputsInUseOrder(ProgramView pv) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        Pattern PX = Pattern.compile("\\bx(\\d+)\\b");

        for (var iv : pv.instructions()) {
            for (String a : iv.args()) {
                scanForInputs(PX, tokenRaw(a), seen);
                int eq = a.indexOf('=');
                if (eq > 0) scanForInputs(PX, a.substring(eq + 1), seen);
            }
        }
        return new ArrayList<>(seen);
    }

    private static void scanForInputs(Pattern PX, String text, LinkedHashSet<String> out) {
        if (text == null || text.isBlank()) return;
        Matcher m = PX.matcher(text);
        while (m.find()) out.add("x" + m.group(1));
    }

    private List<String> extractLabelsInUseOrder(ProgramView pv) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        for (var iv : pv.instructions()) {
            String lbl = iv.label();
            if (isLabelToken(lbl)) seen.add(lbl);

            for (String a : iv.args()) {
                int eq = a.indexOf('=');
                if (eq > 0) {
                    String val = a.substring(eq + 1).trim();
                    if (isLabelToken(val)) seen.add(val);
                }
            }
        }

        ArrayList<String> ordered = new ArrayList<>(seen);
        int exitIdx = indexOfIgnoreCase(ordered, "EXIT");
        if (exitIdx >= 0) {
            String exit = ordered.remove(exitIdx);
            ordered.add(exit);
        }
        return ordered;
    }

    private static boolean isLabelToken(String s) {
        if (s == null || s.isBlank()) return false;
        if (s.equalsIgnoreCase("EXIT")) return true;
        return s.matches("[Ll]\\d+");
    }

    private static int indexOfIgnoreCase(List<String> list, String target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(target)) return i;
        }
        return -1;
    }

    private static String tokenRaw(String arg) {
        if (arg == null) return "";
        int eq = arg.indexOf('=');
        return (eq > 0) ? arg.substring(0, eq).trim() : arg.trim();
    }

}
