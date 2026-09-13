package animeshnic626.areaedit.config;

public class AreaEditConfig {
    private static int speedMultiplier = 1; // По умолчанию 1

    public static int getSpeedMultiplier() {
        return Math.max(1, speedMultiplier);
    }

    public static void setSpeedMultiplier(int speed) {
        speedMultiplier = Math.max(1, speed);
    }
}