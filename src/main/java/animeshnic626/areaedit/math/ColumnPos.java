package animeshnic626.areaedit.math;

import java.util.Objects;

public class ColumnPos {
    public final int x, z, minY, maxY;

    public ColumnPos(int x, int z, int minY, int maxY) {
        this.x = x;
        this.z = z;
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnPos col)) return false;
        return x == col.x && z == col.z && minY == col.minY && maxY == col.maxY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, minY, maxY);
    }
}