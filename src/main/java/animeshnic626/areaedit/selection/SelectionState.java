package animeshnic626.areaedit.selection;

public class SelectionState {
    private static boolean extendedTo626 = false;
    private static Integer customMaxY = null;

    public static boolean isExtendedTo626() {
        return extendedTo626;
    }

    public static void toggleExtendedTo626() {
        extendedTo626 = !extendedTo626;
    }

    public static Integer getCustomMaxY() {
        return customMaxY;
    }

    public static void setCustomMaxY(Integer maxY) {
        customMaxY = maxY;
    }
}
