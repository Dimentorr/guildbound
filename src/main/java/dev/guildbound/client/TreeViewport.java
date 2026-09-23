package dev.guildbound.client;

/** Screen-independent camera math, including cursor-anchored zoom. */
public final class TreeViewport {
    public static final double INITIAL_ZOOM = 2;
    private double zoom = INITIAL_ZOOM, panX, panY;
    public double zoom() { return zoom; }
    public double panX() { return panX; }
    public double panY() { return panY; }
    public double worldX(double x) { return (x - panX) / zoom; }
    public double worldY(double y) { return (y - panY) / zoom; }
    public void pan(double x, double y) { panX += x; panY += y; }
    public void reset() { zoom = INITIAL_ZOOM; panX = 0; panY = 0; }
    public void fitInitial(double width, double height) {
        reset();
        zoom = Math.clamp(Math.min(INITIAL_ZOOM, Math.min(width / 376, height / 60)), .5, INITIAL_ZOOM);
    }
    public void fitBounds(double width, double height, double worldWidth, double worldHeight) {
        panX = 0; panY = 0; zoom = Math.clamp(Math.min(width/worldWidth, height/worldHeight), .15, 2);
    }
    public void zoomAt(double x, double y, double steps) {
        double wx = worldX(x), wy = worldY(y);
        zoom = Math.clamp(zoom * Math.pow(1.15, steps), .15, 2.5);
        panX = x - wx * zoom;
        panY = y - wy * zoom;
    }
}
