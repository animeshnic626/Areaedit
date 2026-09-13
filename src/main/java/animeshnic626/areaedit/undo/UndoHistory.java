package animeshnic626.areaedit.undo;

import animeshnic626.areaedit.selection.SelectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class UndoHistory {

    public record UndoEntry(
            List<BlockSnapshot> blocks,
            SelectionManager.SelectionSnapshot selection
    ) {}

    private static final Stack<UndoEntry> UNDO_STACK = new Stack<>();
    private static final Stack<UndoEntry> REDO_STACK = new Stack<>();

    public static void pushUndo(List<BlockSnapshot> blocks, SelectionManager.SelectionSnapshot selection) {
        UNDO_STACK.push(new UndoEntry(blocks, selection));
        REDO_STACK.clear();
    }

    public static UndoEntry popUndo() {
        if (UNDO_STACK.isEmpty()) return null;
        UndoEntry entry = UNDO_STACK.pop();
        REDO_STACK.push(entry);
        return entry;
    }

    public static UndoEntry popRedo() {
        if (REDO_STACK.isEmpty()) return null;
        UndoEntry entry = REDO_STACK.pop();
        UNDO_STACK.push(entry);
        return entry;
    }

    public static Stack<UndoEntry> getUndoStack() { return UNDO_STACK; }
    public static Stack<UndoEntry> getRedoStack() { return REDO_STACK; }
}
