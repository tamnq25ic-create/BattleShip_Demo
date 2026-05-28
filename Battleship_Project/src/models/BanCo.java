package models;

import java.util.Random;

public class BanCo {
    public static final int KICH_THUOC = 10;
    private OVuong[][] luoi;
    private int soMauConLai; // Tổng số ô tàu chưa bị bắn trúng

    public BanCo() {
        this.luoi = new OVuong[KICH_THUOC][KICH_THUOC];
        this.soMauConLai = 17; // 5 + 4 + 3 + 3 + 2 = 17 ô tàu chuẩn
        khoiTaoBanCo();
    }

    // Khởi tạo lưới 10x10 ô trống
    private void khoiTaoBanCo() {
        for (int i = 0; i < KICH_THUOC; i++) {
            for (int j = 0; j < KICH_THUOC; j++) {
                luoi[i][j] = new OVuong(i, j);
            }
        }
    }

    /**
     * Kiểm tra xem việc đặt tàu tại (x,y) với hướng cụ thể có hợp lệ không
     */
    public boolean kiemTraHopLe(int x, int y, int chieuDai, boolean isNgang) {
        // 1. Kiểm tra xem có lọt ra ngoài biên không
        if (isNgang) {
            if (y + chieuDai > KICH_THUOC) return false;
        } else {
            if (x + chieuDai > KICH_THUOC) return false;
        }

        // 2. Kiểm tra xem có đè lên tàu khác không
        for (int i = 0; i < chieuDai; i++) {
            int currentX = isNgang ? x : x + i;
            int currentY = isNgang ? y + i : y;
            if (luoi[currentX][currentY].isCoTau()) return false;
        }

        return true;
    }

    /**
     * Thực hiện đặt tàu vào bàn cờ
     */
    public boolean datTau(TauChien tau, int x, int y, boolean isNgang) {
        if (!kiemTraHopLe(x, y, tau.getChieuDai(), isNgang)) return false;

        for (int i = 0; i < tau.getChieuDai(); i++) {
            int currentX = isNgang ? x : x + i;
            int currentY = isNgang ? y + i : y;
            luoi[currentX][currentY].setCoTau(true);
        }
        return true;
    }

    /**
     * Xử lý khi ô (x,y) bị bắn
     * @return true nếu trúng tàu, false nếu trượt hoặc đã bắn rồi
     */
    public boolean biBan(int x, int y) {
        OVuong o = luoi[x][y];
        if (o.isDaBan()) return false; // Đã bắn rồi thì không tính nữa

        o.setDaBan(true); // Đánh dấu đã bắn
        if (o.isCoTau()) {
            soMauConLai--;
            return true; // Trúng tàu
        }
        return false; // Trượt
    }

    /**
     * Tự động dàn trận 5 tàu ngẫu nhiên
     */
    public void xepTauNgauNhien() {
        TauChien[] hạmĐội = {
            new TauChien("Tàu Sân Bay", 5),
            new TauChien("Tàu Chiến", 4),
            new TauChien("Tàu Khu Trục 1", 3),
            new TauChien("Tàu Khu Trục 2", 3),
            new TauChien("Tàu Tuần Tra", 2)
        };

        Random rand = new Random();
        for (TauChien tau : hạmĐội) {
            boolean thanhCong = false;
            while (!thanhCong) {
                int x = rand.nextInt(KICH_THUOC);
                int y = rand.nextInt(KICH_THUOC);
                boolean isNgang = rand.nextBoolean();
                thanhCong = datTau(tau, x, y, isNgang);
            }
        }
    }

    public boolean isThuaCuoc() {
        return soMauConLai <= 0;
    }

    public OVuong getOVuong(int x, int y) {
        return luoi[x][y];
    }
}