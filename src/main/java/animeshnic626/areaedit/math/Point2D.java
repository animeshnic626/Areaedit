package animeshnic626.areaedit.math;

import java.util.Objects;

public class Point2D {
    public final int x, z;

    public Point2D(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point2D p)) return false;
        return x == p.x && z == p.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}