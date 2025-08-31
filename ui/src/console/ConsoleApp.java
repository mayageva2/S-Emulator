package console;

import emulator.api.EmulatorEngine;
import emulator.api.EmulatorEngineImpl;
import emulator.api.dto.*;
import emulator.exception.*;

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
    private static record InputsCol(List<String> pretty, int width) {}

    public ConsoleApp(EmulatorEngine engine) {
        this.engine = engine;
    }

    public static void main(String[] args) {
        ConsoleIO io = new ConsoleIO(System.in, System.out);
        EmulatorEngine engine = new EmulatorEngineImpl();
        new ConsoleApp(engine).loop(io);
    }

    //This func runs the main console loop of the emulator
    public void loop(ConsoleIO io) {
        io.println("S-Emulator");
        while (true) {
            showMenu(io);
            String choice = io.ask("Choose action [1-8]: ").trim();
            switch (choice) {
                case "1" -> doLoad(io);
                case "2" -> doShowProgram(io);
                case "3" -> doExpand(io);
                case "4" -> doRun(io);
                case "5" -> doHistory(io);
                case "6" -> doSaveState(io);
                case "7" -> doLoadState(io);
                case "8" -> { io.println("Bye!"); return; }
                default -> io.println("Invalid choice. Try again.");
            }
            io.println("");
        }
    }

    //This func print main menu
    private void showMenu(ConsoleIO io) {
        io.println("""
      1) Load program XML
      2) Show program
      3) Show Expanded program
      4) Run
      5) History
      6) Save state
      7) Load state
      8) Exit
      """);
    }

    //This func loads an XML file
    private void doLoad(ConsoleIO io) {
        final String raw = io.ask("Enter full XML path: ").trim();   //Ask user for the XML file path
        if (raw.isEmpty()) { io.println("Load cancelled: empty path."); return; }

        final Path path;
        try { path = Paths.get(raw); }
        catch (RuntimeException e) { io.println("Load failed: invalid path syntax."); return; }

        if (!Files.exists(path)) { io.println("Load failed: file not found."); return; }
        if (!raw.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            io.println("Load failed: file must have .xml extension."); return;
        }

        try {
            var res = engine.loadProgram(path);
            lastProgramName = res.programName();
            lastMaxDegree   = res.maxDegree();
            lastXmlPath     = path;
            io.println("XML loaded: '" + res.programName() + "' (" +
                    res.instructionCount() + " instructions). Max degree: " + lastMaxDegree);
        } catch (XmlWrongExtensionException e) {
            io.println("Load failed: file must have .xml extension.");
        } catch (XmlNotFoundException e) {
            io.println("Load failed: file not found.");
        } catch (XmlReadException e) {
            io.println("Load failed: XML is malformed – " + e.getMessage());
        } catch (XmlInvalidContentException e) {
            io.println("Load failed: invalid XML content – " + e.getMessage());
        } catch (InvalidInstructionException e) {
            io.println("Load failed: invalid instruction – " + e.getMessage());
        } catch (MissingLabelException e) {
            io.println("Load failed: missing label – " + e.getMessage());
        } catch (ProgramException e) {
            io.println("Load failed: program error – " + e.getMessage());
        } catch (Exception e) {
            io.println("Load failed: unexpected error – " + e.getMessage());
        }
    }


    //This func displays the currently loaded program details
    private void doShowProgram(ConsoleIO io) {
        if (!requireLoaded(io)) return;
        try {
            ProgramView pv = engine.programView();
            io.println("Program: " + (lastProgramName == null ? "(unknown)" : lastProgramName));

            List<String> inputsInUseOrder = extractInputsInUseOrder(pv);
            io.println("Inputs: " + (inputsInUseOrder.isEmpty() ? "(none)" : String.join(", ", inputsInUseOrder)));

            List<String> labelsInUseOrder = extractLabelsInUseOrder(pv);
            io.println("Labels: " + (labelsInUseOrder.isEmpty() ? "(none)" : String.join(", ", labelsInUseOrder)));
            printInstructions(io, pv);
        } catch (Exception e) {
            io.println("Show program failed: " + friendlyMsg(e));
        }
    }

    //This func displays the currently loaded program expended instructions details
    private void doExpand(ConsoleIO io) {
        if (!requireLoaded(io)) return;

        try {
            if (lastMaxDegree == 0) {
                ProgramView pv = engine.programView(0);
                printInstructions(io, pv);
            } else {
                int degree = promptExpansionDegree(io, lastMaxDegree);
                ProgramView pv = engine.programView(degree);
                printInstructionsWithProvenance(io, pv);
            }
        } catch (Exception e) {
            io.println("Expand failed: " + friendlyMsg(e));
        }
    }


    //This func runs the program
    private void doRun(ConsoleIO io) {
        if (!engine.hasProgramLoaded()) {
            io.println("No program loaded. Use 'Load program XML' first.");
            return;
        }

        int degree = 0;
        if (lastMaxDegree != 0) {
            degree = promptExpansionDegree(io, lastMaxDegree);

        }

        var pv = engine.programView(degree);
        Long[] inputs = getInputs(io, pv);
        try {
            var result = engine.run(degree, inputs);

            if (degree == 0) {
                printInstructions(io, pv);
            } else {
                printInstructionsWithProvenance(io, pv);
            }

            io.println("Result y = " + result.y());
            printAllVariables(io, result, inputs);
            io.println("Total cycles = " + result.cycles());
        } catch (Exception e) {
            io.println("Run failed: " + friendlyMsg(e));
        }
    }

    //This func returns all inputs
    private Long[] getInputs(ConsoleIO io, ProgramView pv){
        List<String> usedInputIdx = engine.extractInputVars(pv);
        Long[] inputs = null;
        boolean validInputs = false;
        while (!validInputs) {
            try {
                inputs = readInputs(io, usedInputIdx);
                validInputs = true;
            } catch (Exception e) {
                io.println("Invalid input: " + friendlyMsg(e));
                io.println("Please try again");
            }
        }
        return inputs;
    }

    //This func asks the user for expansion degree
    private int promptExpansionDegree(ConsoleIO io, int max) {
        while (true) {
            String dg = io.ask("Choose expansion degree (0-" + max + "): ").trim();
            if (dg.isEmpty()) return 0;
            try {
                int degree = Integer.parseInt(dg);
                if (degree < 0) {
                    io.println("Degree cannot be negative. Try again.");
                } else if (degree > max) {
                    io.println("Degree " + degree + " exceeds max (" + max + "). Try again.");
                } else {
                    return degree;
                }
            } catch (NumberFormatException e) {
                io.println("Invalid number. Please enter an integer between 0 and " + max + ".");
            }
        }
    }

    //This func print required inputs
    private Long[] readInputs(ConsoleIO io, List<String> usedInputIdx) {
        io.println("The inputs of this program are:");
        io.println(usedInputIdx.isEmpty() ? "(none)" : String.join(", ", usedInputIdx));

        String csv = io.ask("Enter inputs (comma-separated, e.g. 3,6,2): ").trim();
        if (csv.isEmpty()) return new Long[0];
        try {
            return Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .peek(n -> {
                        if (n < 0) {
                            throw new IllegalArgumentException("Negative numbers are not allowed: " + n);
                        }
                    })
                    .toArray(Long[]::new);
        } catch (NumberFormatException nfe) {
            throw nfe;
        }
    }

    //This func prints previous runs
    private void doHistory(ConsoleIO io) {
        var history = engine.history();
        if (history.isEmpty()) {
            io.println("No runs yet.");
            return;
        }

        List<String> prettyInputs = prettifyInputs(history);
        ColWidths w = computeWidths(history, prettyInputs);
        printHistoryTable(io, history, prettyInputs, w);
    }

    private static record ColWidths(int run, int degree, int inputs, int y, int cycles) {}

    //This func formats input
    private List<String> prettifyInputs(List<RunRecord> history) {
        List<String> pretty = new ArrayList<>(history.size());
        for (var r : history) {
            pretty.add(formatInputsByPosition(r.inputsCsv()));
        }
        return pretty;
    }

    //This func checks max width for each column
    private ColWidths computeWidths(List<RunRecord> history, List<String> prettyInputs) {
        String hRun = "Run#", hDegree = "Degree", hInputs = "Inputs", hY = "y", hCycles = "Cycles";

        int runW    = hRun.length();
        int degreeW = hDegree.length();
        int inputsW = hInputs.length();
        int yW      = hY.length();
        int cyclesW = hCycles.length();

        for (int i = 0; i < history.size(); i++) {
            var r = history.get(i);
            runW    = Math.max(runW,    String.valueOf(r.runNumber()).length());
            degreeW = Math.max(degreeW, String.valueOf(r.degree()).length());
            inputsW = Math.max(inputsW, prettyInputs.get(i).length());
            yW      = Math.max(yW,      String.valueOf(r.y()).length());
            cyclesW = Math.max(cyclesW, String.valueOf(r.cycles()).length());
        }

        return new ColWidths(runW, degreeW, inputsW, yW, cyclesW);
    }

    //This func prints history
    private void printHistoryTable(ConsoleIO io, List<RunRecord> history, List<String> prettyInputs, ColWidths w) {

        String hRun = "Run#", hDegree = "Degree", hInputs = "Inputs", hY = "y", hCycles = "Cycles";     // headers
        String headerFmt = "%-" + w.run() + "s | %-" + w.degree() + "s | %-" + w.inputs() + "s | %-" + w.y() + "s | %-" + w.cycles() + "s";
        io.println(String.format(headerFmt, hRun, hDegree, hInputs, hY, hCycles));

        io.println(
                repeat('-', w.run())   + "-+-" +
                        repeat('-', w.degree())+ "-+-" +
                        repeat('-', w.inputs())+ "-+-" +
                        repeat('-', w.y())     + "-+-" +
                        repeat('-', w.cycles())
        );

        String rowFmt = "%"+w.run()+"d | %"+w.degree()+"d | %-" + w.inputs() + "s | %"+w.y()+"d | %"+w.cycles()+"d";
        for (int i = 0; i < history.size(); i++) {
            var r = history.get(i);
            io.println(String.format(
                    rowFmt,
                    r.runNumber(),
                    r.degree(),
                    prettyInputs.get(i),
                    r.y(),
                    r.cycles()
            ));
        }
    }



    //This func formats a comma-separated input string
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

    //This func creates and returns a string n times char string
    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(Math.max(0, n));
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    //This func saves current state
    private void doSaveState(ConsoleIO io){
        if (!engine.hasProgramLoaded()) {
            io.println("No program loaded. Use 'Load program XML' first.");
            return;
        }
        String path = io.ask("Enter full path (without extension) to save state: ");
        try { engine.saveState(Paths.get(path)); io.println("Saved!"); }
        catch (Exception e) { io.println("Save failed: " + e.getMessage()); }
    }

    //This func loads saved state from file
    private void doLoadState(ConsoleIO io){
        String path = io.ask("Enter full path (without extension) to load state: ");
        try {
            engine.loadState(Paths.get(path));
            io.println("Loaded!");
        } catch (Exception e) {
            io.println("Load failed: " + e.getMessage());
        }
    }

    //This func checks whether a program is loaded
    private boolean requireLoaded(ConsoleIO io) {
        if (!engine.hasProgramLoaded()) {
            io.println("No program loaded. Use 'Load program XML' first.");
            return false;
        }
        return true;
    }

    //This func prints all non-virtual instructions
    private void printInstructions(ConsoleIO io, ProgramView pv) {
        for (InstructionView iv : pv.instructions()) {
            if (isVirtual(iv)) continue;
            io.println(formatInstruction(iv));
        }
    }

    //This func prints each expanded instruction along with its provenance chain
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

            io.println(chain == null || chain.isEmpty() ? base : (base + "  >>>   " + chain));
        }
    }

    //This func formats a provenance chain
    private String formatProvenanceChainFromViews(List<InstructionView> chainViews) {
        List<String> parts = new ArrayList<>();
        for (InstructionView v : chainViews) {
            parts.add(formatInstruction(v));
        }
        return String.join("  >>>   ", parts);
    }

    //This func formats an instruction’s provenance chain
    private String formatProvenanceChain(InstructionView iv, Map<Integer, InstructionView> originalByIndex) {
        List<Integer> chain = iv.createdFromChain();
        if (chain == null || chain.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (Integer idx : chain) {
            if (idx == null) continue;
            InstructionView origin = originalByIndex.get(idx);
            if (origin != null) parts.add(formatInstruction(origin));
        }
        return String.join("  >>>   ", parts);
    }

    //This func formats an instruction
    private String formatInstruction(InstructionView iv) {
        String index = "#" + iv.index();
        String type = iv.basic() ? "(B)" : "(S)";
        String label = iv.label() == null ? "" : iv.label();
        String labelField = String.format("[ %-3s ]", label); // 5-wide label
        String command = prettyCommand(iv);
        String cycles = "(" + iv.cycles() + ")";
        return String.format("%-4s %-4s %-8s %-20s %s", index, type, labelField, command, cycles);
    }

    //This func generates a readable string representation of an instruction
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

    //This func returns the value for the given key
    private String getArg(List<String> args, String key) {
        for (String a : args) {
            int eq = a.indexOf('=');
            if (eq > 0 && a.substring(0,eq).equals(key)) {
                return a.substring(eq+1);
            }
        }
        return "";
    }

    //This func checks whether an instruction is virtual
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

    //This func extracts and returns the label value
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

    //This func prints all variables
    private void printAllVariables(ConsoleIO io, RunResult result, Long[] inputs) {
        VariableView yVar;
        Map<Integer, Long> xValues = new TreeMap<>();
        Map<Integer, Long> zValues = new TreeMap<>();

        yVar = classifyVariables(result, xValues, zValues);
        fillMissingXFromInputs(xValues, inputs);

        io.println("Variables:");
        if (yVar != null) {
            io.println(yVar.name() + " = " + yVar.value());
        } else {
            io.println("y = " + (result == null ? 0 : result.y()));
        }
        for (var e : xValues.entrySet()) {
            io.println("x" + e.getKey() + " = " + e.getValue());
        }
        for (var e : zValues.entrySet()) {
            io.println("z" + e.getKey() + " = " + e.getValue());
        }
    }

    //This func classifies RESULT variables
    private VariableView classifyVariables(RunResult result,
                                           Map<Integer, Long> xOut,
                                           Map<Integer, Long> zOut) {
        List<VariableView> vars = (result == null || result.vars() == null) ? List.of() : result.vars();
        VariableView yVar = null;

        for (VariableView v : vars) {
            if (v == null) continue;
            String name = v.name() == null ? "" : v.name().toLowerCase(Locale.ROOT);

            if (v.type() == VarType.RESULT || "y".equals(name)) {
                yVar = v;
            } else if (v.type() == VarType.INPUT || name.startsWith("x")) {
                xOut.put(v.number(), v.value());
            } else if (v.type() == VarType.WORK || name.startsWith("z")) {
                zOut.put(v.number(), v.value());
            }
        }
        return yVar;
    }

    //This func fills inputs
    private void fillMissingXFromInputs(Map<Integer, Long> xValues, Long[] inputs) {
        if (inputs == null) return;
        for (int i = 0; i < inputs.length; i++) {
            int idx = i + 1;
            xValues.putIfAbsent(idx, inputs[i]);
        }
    }

    //This func extracts and returns a unique, ordered list of input variable names
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

    //This func scans a text for patterns and adds input variable
    private static void scanForInputs(Pattern PX, String text, LinkedHashSet<String> out) {
        if (text == null || text.isBlank()) return;
        Matcher m = PX.matcher(text);
        while (m.find()) out.add("x" + m.group(1));
    }

    //This func extracts all label names
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

    //This func checks whether a given string is a valid label
    private static boolean isLabelToken(String s) {
        if (s == null || s.isBlank()) return false;
        if (s.equalsIgnoreCase("EXIT")) return true;
        return s.matches("[Ll]\\d+");
    }

    //This func returns the index of the first element
    private static int indexOfIgnoreCase(List<String> list, String target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(target)) return i;
        }
        return -1;
    }

    //This func extracts the raw token from an argument string
    private static String tokenRaw(String arg) {
        if (arg == null) return "";
        int eq = arg.indexOf('=');
        return (eq > 0) ? arg.substring(0, eq).trim() : arg.trim();
    }

    // --- Error helpers --- //
    private static String friendlyMsg(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            // XML/load-time issues
            if (cur instanceof XmlWrongExtensionException)   return "file must have .xml extension.";
            if (cur instanceof XmlNotFoundException)         return "file not found.";
            if (cur instanceof XmlReadException)             return "XML is malformed" + opt(cur.getMessage());
            if (cur instanceof XmlInvalidContentException)   return "invalid XML content" + opt(cur.getMessage());

            // Program/domain issues
            if (cur instanceof ProgramNotLoadedException)    return "no program loaded.";
            if (cur instanceof MissingLabelException)        return "missing label" + opt(cur.getMessage());
            if (cur instanceof InvalidInstructionException)  return "invalid instruction" + opt(cur.getMessage());
            if (cur instanceof ProgramException)             return "program error" + opt(cur.getMessage());
            if (cur instanceof NumberFormatException)        return "should be Integer.";
        }
        String msg = t.getMessage();
        return (msg == null || msg.isBlank()) ? "unexpected error." : msg;
    }

    private static String opt(String msg) {
        return (msg != null && !msg.isBlank()) ? " – " + msg : "";
    }

}
