package SelectedInstructionHistoryChainTable;

import com.google.gson.Gson;
import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import InstructionsTable.InstructionsTableController;
import InstructionsTable.InstructionRow;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

import java.util.*;
import java.util.function.Function;

public class SelectedInstructionHistoryChainTableController {

    @FXML private BorderPane root;
    @FXML private InstructionsTableController instructionsController;

    private Function<String, String> fnNameResolver = s -> s;
    public void setFunctionNameResolver(Function<String, String> f) {
        this.fnNameResolver = (f != null) ? f : (s -> s);
    }

    @FXML
    private void initialize() {}

    public void setInstructionsController(InstructionsTableController controller) {
        this.instructionsController = controller;
    }

    public void showForSelected(InstructionView selected, ProgramView pvOriginal) {
        if (selected == null) {
            clear();
            return;
        }

        try {
            if (selected != null && selected.createdFromViews() != null) {
                List<InstructionView> fixedList = new ArrayList<>();
                for (Object o : selected.createdFromViews()) {
                    if (o instanceof InstructionView iv) {
                        fixedList.add(iv);
                    } else {
                        String json = new Gson().toJson(o);
                        fixedList.add(new Gson().fromJson(json, InstructionView.class));
                    }
                }
                selected = new Gson().fromJson(
                        new Gson().toJson(selected),
                        InstructionView.class
                );
                selected.createdFromViews().clear();
                selected.createdFromViews().addAll(fixedList);
            }
        } catch (Exception e) {
            System.err.println("Failed to reconstruct InstructionView: " + e.getMessage());
        }

        List<InstructionView> chain = new ArrayList<>();
        collectChainRecursive(selected, chain, new HashSet<>());

        if (!chain.isEmpty() && chain.get(chain.size() - 1).index() == selected.index()) {
            chain.remove(chain.size() - 1);
        }
        if (chain.isEmpty()) {
            clear();
            return;
        }

        Collections.reverse(chain);
        List<InstructionRow> items = toRows(chain);

        Platform.runLater(() -> {
            instructionsController.setItems(items);
            System.out.println("History chain updated with " + items.size() + " items");
        });
    }


    private void collectChainRecursive(InstructionView current, List<InstructionView> out, Set<Integer> visited) {
        if (current == null) return;
        if (!visited.add(current.index())) return;

        List<InstructionView> prevs = current.createdFromViews();
        if (prevs != null) {
            for (InstructionView prev : prevs) {
                collectChainRecursive(prev, out, visited);
            }
        }

        out.add(current);
    }

    public void clear() {
        instructionsController.clear();
    }

    // ---------- helpers ----------
    private List<InstructionRow> toRows(List<InstructionView> chain) {
        List<InstructionRow> out = new ArrayList<>(chain.size());
        for (int d = 0; d < chain.size(); d++) {
            InstructionView iv = chain.get(d);
            String opcode = ns(iv.opcode());
            List<String> args = new ArrayList<>(iv.args());

            if (opcode.equalsIgnoreCase("QUOTE") || opcode.equalsIgnoreCase("JUMP_EQUAL_FUNCTION")) {
                for (int i = 0; i < args.size(); i++) {
                    String a = args.get(i);
                    int eq = a.indexOf('=');
                    if (eq > 0) {
                        String key = a.substring(0, eq).toLowerCase(Locale.ROOT);
                        String val = a.substring(eq + 1).trim();

                        if (key.contains("functionname")) {
                            String resolved = fnNameResolver.apply(val);
                            args.set(i, "functionName=" + resolved);
                        } else if (key.contains("functionarguments")) {
                            String resolved = replaceHeadsWithUserStrings(stripParens(val));
                            args.set(i, "functionArguments=" + resolved);
                        }
                    }
                }
            }

            InstructionView patched = new InstructionView(
                    iv.index(),
                    iv.opcode(),
                    iv.label(),
                    iv.basic(),
                    iv.cycles(),
                    args,
                    iv.createdFromChain(),
                    iv.createdFromViews(),
                    iv.creditCost(),
                    iv.architecture()
            );

            String display = instructionsController.prettyCommand(patched);

            InstructionRow row = new InstructionRow(
                    iv.index(),
                    iv.basic(),
                    ns(iv.label()),
                    iv.cycles(),
                    opcode,
                    args,
                    d,
                    patched
            );
            row.display = display;
            out.add(row);
        }
        return out;
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

    private static String stripParens(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("(") && s.endsWith(")")) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private String replaceHeadsWithUserStrings(String fargs) {
        if (fargs == null || fargs.isBlank()) return fargs;
        StringBuilder out = new StringBuilder();
        int n = fargs.length();
        int i = 0;

        while (i < n) {
            char c = fargs.charAt(i);

            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(fargs.charAt(i)) || fargs.charAt(i) == '_')) i++;
                String head = fargs.substring(start, i);
                String repl = fnNameResolver.apply(head);
                out.append(repl);
                continue;
            }

            if (c == '(') {
                out.append(c);
                int start = i + 1;
                int j = start;
                while (j < n && (Character.isLetterOrDigit(fargs.charAt(j)) || fargs.charAt(j) == '_')) j++;
                if (j > start) {
                    String head = fargs.substring(start, j);
                    String repl = fnNameResolver.apply(head);
                    out.append(repl);
                    i = j - 1;
                }
            } else {
                out.append(c);
            }
            i++;
        }
        return out.toString();
    }

    private static String ns(String s) { return s == null ? "" : s; }
}
