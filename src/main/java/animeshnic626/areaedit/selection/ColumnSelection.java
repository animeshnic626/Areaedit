package animeshnic626.areaedit.selection;

public class ColumnSelection {
    private final ColumnPos pos;
    private final int yMin;
    private final int yMax;

    public ColumnSelection(ColumnPos pos, int yMin, int yMax) {
        this.pos = pos;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public ColumnPos getPos() { return pos; }
    public int getYMin() { return yMin; }
    public int getYMax() { return yMax; }
}
