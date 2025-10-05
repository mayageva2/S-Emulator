package SelectedInstructionHistoryChainTable;

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

    public void showForSelected(InstructionView selected, ProgramView pvOriginal) {
        if (selected == null) {
            clear();
            return;
        }
        List<InstructionView> chain = new ArrayList<>(selected.createdFromViews());
        List<InstructionRow> items = toRows(chain);
        Platform.runLater(() -> instructionsController.setItems(items));
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
                    iv.createdFromViews()
            );

            out.add(new InstructionRow(
                    iv.index(),
                    iv.basic(),
                    ns(iv.label()),
                    iv.cycles(),
                    opcode,
                    args,
                    d,
                    patched
            ));
        }
        return out;
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
