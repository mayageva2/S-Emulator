package InstructionsTable;

import com.google.gson.Gson;
import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableViewSkin;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class InstructionsTableController {

    @FXML private TableView<InstructionRow> table;
    @FXML private TableColumn<InstructionRow, Number> indexCol;
    @FXML private TableColumn<InstructionRow, String> typeCol;
    @FXML private TableColumn<InstructionRow, String> labelCol;
    @FXML private TableColumn<InstructionRow, String> cyclesCol;
    @FXML private TableColumn<InstructionRow, String> instructionCol;

    private Consumer<InstructionView> onRowSelected;
    private String highlightTerm = null;
    private int highlightedIndex = -1;
    private Function<String,String> fnNameResolver = s -> s; // identity default
    public void setFunctionNameResolver(java.util.function.Function<String,String> f) {
        this.fnNameResolver = (f != null) ? f : (s -> s);
    }

    @FXML
    private void initialize() {
        indexCol.setCellValueFactory(cd -> new ReadOnlyIntegerWrapper(cd.getValue().index + 1));
        typeCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().basic ? "B" : "S"));
        labelCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(ns(cd.getValue().label)));
        cyclesCol.setCellValueFactory(cd -> {
            InstructionRow row = cd.getValue();
            String op = row.opcode.toUpperCase(Locale.ROOT);
            String display;

            if (op.equals("QUOTE")) {
                display = row.cycles + "+";
            } else if (op.equals("JUMP_EQUAL_FUNCTION")) {
                display = row.cycles + "+";
            } else {
                display = String.valueOf(row.cycles);
            }

            return new ReadOnlyStringWrapper(display);
        });
        instructionCol.setCellValueFactory(cd -> {
            var r = cd.getValue();
            String text = (r.display != null && !r.display.isBlank())
                    ? r.display
                    : r.opcode + " " + String.join(", ", r.args);
            return new ReadOnlyStringWrapper(text);
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> new TableRow<InstructionRow>() {
            @Override protected void updateItem(InstructionRow row, boolean empty) {
                super.updateItem(row, empty);
                setStyle("");

                if (empty || row == null) return;
                if (getIndex() == highlightedIndex) {
                    setStyle("-fx-background-color: #fff3cd;");
                    return;
                }
                if (!isBlank(highlightTerm) && matches(row, highlightTerm)) {
                    setStyle("-fx-background-color: #fff3cd;");
                }
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (onRowSelected != null && row != null && row.sourceIv != null) {
                onRowSelected.accept(row.sourceIv);
            }
        });

        var css = getClass().getResource("/InstructionsTable/InstructionTable.css");
        if (css != null) {
            table.getStylesheets().add(css.toExternalForm());
            table.getStyleClass().add("instructions");
        }
    }

    public void setHighlightTerm(String term) {
        this.highlightTerm = isNone(term) ? null : term;
        table.refresh();
    }

    private static boolean isNone(String s) {
        return s == null || s.isBlank() || "— None —".equals(s);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static boolean matches(InstructionRow r, String term) {
        String t = term.toLowerCase();

        // Exact match for labels
        if (r.label != null && r.label.equalsIgnoreCase(t))
            return true;

        // For args and opcode, only match whole word boundaries
        if (r.opcode != null && r.opcode.toLowerCase().matches("(?i).*\\b" + java.util.regex.Pattern.quote(t) + "\\b.*"))
            return true;

        if (r.args != null) {
            for (String a : r.args) {
                if (a != null && a.toLowerCase().matches("(?i).*\\b" + java.util.regex.Pattern.quote(t) + "\\b.*"))
                    return true;
            }
        }
        return false;
    }


    public void setItems(List<InstructionRow> items) {
        table.getItems().setAll(items);
    }

    public void clear() {
        table.getItems().clear();
    }

    public void scrollToEnd() {
        var n = table.getItems().size();
        if (n > 0) table.scrollTo(n - 1);
    }

    public TableView<InstructionRow> getTableView() { return table; }

    public void update(ProgramView pv) {
        Objects.requireNonNull(pv, "pv");
        List<InstructionRow> rows = new ArrayList<>(pv.instructions().size());
        for (InstructionView iv : pv.instructions()) {
            rows.add(new InstructionRow(
                    iv.index(),
                    iv.basic(),
                    ns(iv.label()),
                    iv.cycles(),
                    ns(iv.opcode()),
                    iv.args(),
                    0,            // depth 0 in the main table
                    iv            // keep the source for selection callback
            ));
        }
        table.getItems().clear();
        table.getItems().setAll(rows);
        table.refresh();
    }

    public void forceReload(ProgramView pv) {
        if (pv == null) return;
        table.getItems().clear();
        update(pv);
        table.skinProperty().set(null);
        table.setSkin(new TableViewSkin<>(table));
    }

    public void setOnRowSelected(Consumer<InstructionView> handler) {
        this.onRowSelected = handler;
    }
    private static String ns(String s) { return s == null ? "" : s; }
    private static String pretty(String op, List<String> args) {
        op = ns(op);
        return (args == null || args.isEmpty()) ? op : op + " " + String.join(", ", args);
    }

    //This func generates a readable string representation of an instruction
    public String prettyCommand(InstructionView iv) {
        if (iv == null) return "(no source)";
        var args = iv.args();
        String op = iv.opcode().toUpperCase(Locale.ROOT);
        switch (op) {
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
                if (tgt.isEmpty()) tgt = nz(iv.label());
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
            case "ASSIGNMENT": return args.get(0) + "<-" + getArg(args,"assignedVariable");
            case "CONSTANT_ASSIGNMENT": return args.get(0) + "<-" + getArg(args,"constantValue");
            case "QUOTE": {
                String dest = (!args.isEmpty() && !args.get(0).contains("=")) ? args.get(0) : "y";
                String fn = nz(getArgFlex(args, "userString", "user-string", "functionUserString", "function-user-string", "functionName"));
                fn = fnNameResolver.apply(fn);
                String fargs = replaceHeadsWithUserStrings(nz(getArgFlex(args, "functionArguments", "function-arguments")));
                String inside = fargs.isEmpty() ? fn : fn + ", " + fargs;
                return dest + " <- (" + inside + ")";
            }
            case "JUMP_EQUAL_FUNCTION": {
                String V = args.get(0);
                String fn = nz(getArgFlex(args, "userString", "user-string", "functionUserString", "function-user-string", "functionName"));
                fn = fnNameResolver.apply(fn);
                String fargs = replaceHeadsWithUserStrings(nz(getArgFlex(args, "functionArguments", "function-arguments")));
                String inside = fargs.isBlank() ? fn : fn + ", " + fargs;
                String label = findLabel(args);
                return "IF " + V + " = (" + inside + ") GOTO " + label;
            }
            default: return iv.opcode() + " " + String.join(", ", args);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

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

    //This func returns the value for the given key
    private String getArg(List<String> args, String key) {
        for (String a : args) {
            int eq = a.indexOf('=');
            if (eq > 0) {
                String left = a.substring(0, eq);
                if (left.equalsIgnoreCase(key)) {
                    return a.substring(eq + 1);
                }
            }
        }
        return "";
    }

    public void highlightRow(int rowIndex0Based) {
        highlightedIndex = Math.max(-1, rowIndex0Based);
        table.getSelectionModel().clearSelection();
        if (highlightedIndex >= 0 && highlightedIndex < table.getItems().size()) {
            table.scrollTo(highlightedIndex);
            table.getSelectionModel().select(highlightedIndex);
        }
        table.refresh();
    }

    public void clearHighlight() {
        highlightedIndex = -1;
        table.getSelectionModel().clearSelection();
        table.refresh();
    }

    public void setProgramView(ProgramView pv) {
        update(pv);
        try {
            if (table != null) {
                table.getSelectionModel().clearSelection();
                table.scrollTo(0);
            }
            clearHighlight();
        } catch (Throwable ignore) {}
    }

    public void clearSelection() {
        try {
            if (table != null) {
                table.getSelectionModel().clearSelection();
            }
            highlightedIndex = -1;
            table.refresh();
        } catch (Throwable ignore) {}
    }

    private String replaceHeadsWithUserStrings(String fargs) {
        if (fargs == null || fargs.isBlank()) return fargs;
        StringBuilder out = new StringBuilder();
        int n = fargs.length();
        int i = 0;
        while (i < n) {
            char c = fargs.charAt(i);
            out.append(c);
            if (c == '(') {
                int start = i + 1;
                int j = start;
                while (j < n && fargs.charAt(j) != ',' && fargs.charAt(j) != ')') j++;
                String head = fargs.substring(start, j).trim();
                String repl = (head.isEmpty() ? head : fnNameResolver.apply(head));
                out.append(repl);
                i = j - 1;
            }
            i++;
        }
        return out.toString();
    }

    private String getArgFlex(List<String> args, String... keys) {
        if (args == null || keys == null || keys.length == 0) return "";
        for (String a : args) {
            if (a == null) continue;
            int eq = a.indexOf('=');
            if (eq > 0) {
                String leftNorm = a.substring(0, eq).replaceAll("[-_]", "").toLowerCase(Locale.ROOT);
                for (String k : keys) {
                    String kNorm = k.replaceAll("[-_]", "").toLowerCase(Locale.ROOT);
                    if (leftNorm.equals(kNorm)) {
                        return a.substring(eq + 1);
                    }
                }
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public void renderFromJson(List<Map<String, Object>> instructionsList) {
        if (instructionsList == null || instructionsList.isEmpty()) {
            clear();
            return;
        }

        List<InstructionRow> rows = new ArrayList<>();
        for (Map<String, Object> map : instructionsList) {
            int index = ((Number) map.getOrDefault("index", 0)).intValue();
            boolean basic = Boolean.TRUE.equals(map.get("basic"));
            String label = Objects.toString(map.get("label"), "");
            int cycles = ((Number) map.getOrDefault("cycles", 0)).intValue();
            String opcode = Objects.toString(map.get("opcode"), "");
            List<String> args = (List<String>) map.getOrDefault("args", List.of());

            List<Map<String, Object>> subViewsList =
                    (List<Map<String, Object>>) map.get("createdFromViews");
            List<InstructionView> createdFromViews = new ArrayList<>();
            if (subViewsList != null) {
                for (Map<String, Object> subMap : subViewsList) {
                    int sIndex = ((Number) subMap.getOrDefault("index", -1)).intValue();
                    String sOpcode = Objects.toString(subMap.get("opcode"), "");
                    String sLabel = Objects.toString(subMap.get("label"), "");
                    int sCycles = ((Number) subMap.getOrDefault("cycles", 0)).intValue();
                    boolean sBasic = Boolean.TRUE.equals(subMap.get("basic"));
                    List<String> sArgs = (List<String>) subMap.getOrDefault("args", List.of());

                    createdFromViews.add(new InstructionView(
                            sIndex,
                            sOpcode,
                            sLabel,
                            sBasic,
                            sCycles,
                            sArgs,
                            List.of(),
                            List.of()
                    ));
                }
            }

            List<Integer> createdFromChain =
                    (List<Integer>) map.getOrDefault("createdFromChain", List.of());

            InstructionView iv = new InstructionView(
                    index,
                    opcode,
                    label,
                    basic,
                    cycles,
                    args,
                    createdFromChain,
                    createdFromViews
            );

            String display = prettyCommand(iv);
            InstructionRow row = new InstructionRow(index, basic, label, cycles, opcode, args, 0, iv);
            row.display = display;
            rows.add(row);
        }
        setItems(rows);
    }

    private static String extractArg(List<String> args, String... keys) {
        for (String key : keys) {
            for (String a : args) {
                if (a.toLowerCase(Locale.ROOT).startsWith(key.toLowerCase(Locale.ROOT) + "=")) {
                    return a.substring(key.length() + 1);
                }
            }
        }
        return "";
    }

    public void refreshFromServer(int degree) {
        new Thread(() -> {
            try {
                String urlStr = "http://localhost:8080/semulator/view?degree=" + degree;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() != 200) {
                    System.err.println("Server returned status " + conn.getResponseCode());
                    return;
                }

                String json;
                try (InputStream in = conn.getInputStream()) {
                    json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }

                Gson gson = new Gson();
                Map<String, Object> map = gson.fromJson(json, Map.class);
                List<Map<String, Object>> instructionsList =
                        (List<Map<String, Object>>) map.get("instructions");

                Platform.runLater(() -> renderFromJson(instructionsList));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "table-refresh").start();
    }
}
