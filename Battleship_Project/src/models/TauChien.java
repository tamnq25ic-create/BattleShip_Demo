package models;

public class TauChien {
    private String ten;
    private int chieuDai;

    public TauChien(String ten, int chieuDai) {
        this.ten = ten;
        this.chieuDai = chieuDai;
    }

    public String getTen() { return ten; }
    public int getChieuDai() { return chieuDai; }
}