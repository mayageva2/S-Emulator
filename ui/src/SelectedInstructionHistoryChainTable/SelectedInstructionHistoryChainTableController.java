package SelectedInstructionHistoryChainTable;

import emulator.api.dto.InstructionView;
import emulator.api.dto.ProgramView;
import InstructionsTable.InstructionsTableController;
import InstructionsTable.InstructionRow;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

import java.util.*;

public class SelectedInstructionHistoryChainTableController {

    @FXML private BorderPane root;
    @FXML private InstructionsTableController instructionsController;

    @FXML
    private void initialize() {
        // nothing else to do — columns, CSS, etc. live in the reusable table
    }

    /** Call this when a row is clicked in the expanded table. */
    public void showForSelected(InstructionView selected, ProgramView pvOriginal) {
        if (selected == null) { clear(); return; }
        List<InstructionView> chain = toChainRootToLeaf(selected, pvOriginal);
        List<InstructionRow> items = toRows(chain);

        Platform.runLater(() -> {
            instructionsController.setItems(items);
            instructionsController.scrollToEnd();
        });
    }

    public void clear() { instructionsController.clear(); }

    // ---------- helpers (same logic as before) ----------
    private List<InstructionView> toChainRootToLeaf(InstructionView selected, ProgramView pvOriginal) {
        List<InstructionView> fromViews = selected.createdFromViews();
        if (fromViews != null && !fromViews.isEmpty()) {
            List<InstructionView> list = new ArrayList<>(fromViews);
            list.add(selected);
            return list;
        }
        List<Integer> idxChain = selected.createdFromChain();
        if (idxChain != null && !idxChain.isEmpty()) {
            Map<Integer, InstructionView> byIdx = new HashMap<>();
            for (InstructionView iv0 : pvOriginal.instructions()) byIdx.put(iv0.index(), iv0);
            List<InstructionView> list = new ArrayList<>(idxChain.size() + 1);
            for (Integer idx : idxChain) {
                InstructionView origin = byIdx.get(idx);
                if (origin != null) list.add(origin);
            }
            list.add(selected);
            return list;
        }
        return List.of(selected);
    }

    private List<InstructionRow> toRows(List<InstructionView> chain) {
        List<InstructionRow> out = new ArrayList<>(chain.size());
        for (int d = 0; d < chain.size(); d++) {
            InstructionView iv = chain.get(d);
            out.add(new InstructionRow(
                    iv.index(), iv.basic(), ns(iv.label()), iv.cycles(), ns(iv.opcode()), iv.args(), d, iv
            ));
        }
        return out;
    }

    private static String ns(String s) { return s == null ? "" : s; }
}
