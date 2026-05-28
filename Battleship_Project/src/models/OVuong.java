package models;

public class OVuong {
    private int x;
    private int y;
    private boolean coTau; // Ô này có chứa thân tàu không
    private boolean daBan; // Ô này đã bị đối phương nã đạn vào chưa

    public OVuong(int x, int y) {
        this.x = x;
        this.y = y;
        this.coTau = false;
        this.daBan = false;
    }

    // --- Getters & Setters ---
    public int getX() { return x; }
    public int getY() { return y; }

    public boolean isCoTau() { return coTau; }
    public void setCoTau(boolean coTau) { this.coTau = coTau; }

    public boolean isDaBan() { return daBan; }
    public void setDaBan(boolean daBan) { this.daBan = daBan; }
}