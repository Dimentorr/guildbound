package dev.guildbound.combat;

public final class RadialLayout {
    private RadialLayout() {}
    public static double angle(int index, int count) {
        if (count < 1 || index < 0 || index >= count) throw new IllegalArgumentException("Invalid radial slot");
        return (count == 2 ? 0 : -Math.PI / 2) + index * Math.PI * 2 / count;
    }
    public static int pick(double dx, double dy, int count) {
        if (count < 1 || dx * dx + dy * dy < 24 * 24) return -1;
        double best = Double.POSITIVE_INFINITY;
        int result = -1;
        for (int i = 0; i < count; i++) {
            double distance = Math.abs(Math.atan2(Math.sin(Math.atan2(dy, dx) - angle(i, count)),
                    Math.cos(Math.atan2(dy, dx) - angle(i, count))));
            if (distance < best) { best = distance; result = i; }
        }
        return result;
    }
}

