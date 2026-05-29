package views;

import java.awt.*;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import models.BanCo;
import models.TauChien;

public class ManHinhGame extends JFrame {

    private BanCo dataBanCoMinh, dataBanCoDich;
    private JButton[][] btnMinh = new JButton[10][10];
    private JButton[][] btnDich = new JButton[10][10];
    
    private boolean isDangXepTau = true; 
    private boolean isPlayerTurn = true; 
    private boolean isNgang = true;      
    private boolean isAnimating = false; 
    private int tauHienTai = 0;          
    private int doKhoBot = 0;

    private TauChien[] hạmĐội = {
        new TauChien("Tàu Sân Bay", 5),
        new TauChien("Tàu Chiến", 4),
        new TauChien("Tàu Khu Trục 1", 3),
        new TauChien("Tàu Khu Trục 2", 3),
        new TauChien("Tàu Tuần Tra", 2)
    };

    private Image imgHit, imgMiss;
    private Image imgTau5, imgTau4, imgTau3, imgTau2;
    private Image bgBattle = null; 

    private JLabel lblTrangThai;
    private JButton btnXoayTau, btnVaoTran, btnXepNgauNhien, btnXoaHet, btnThoat;
    private JComboBox<String> cbDoKho;

    // Tăng độ đậm màu khi di chuột để nhìn cực rõ ô đang chọn
    private static final Color MAU_RADAR_HOVER = new Color(52, 152, 219, 100); 

    public ManHinhGame() {
        this.setTitle("Battleship Pro - Hạm Đội Thức Giấc");
        this.setSize(1250, 750); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        khoiTaoImages();

        // --- PANEL CHÍNH: GIỮ NỀN SÁNG ĐẸP TỰ NHIÊN ---
        JPanel pnlMain = new JPanel(new GridLayout(1, 2, 50, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (bgBattle != null) {
                    g2d.drawImage(bgBattle, 0, 0, getWidth(), getHeight(), this);
                    // Lớp phủ siêu mỏng (Alpha = 40) để giữ nguyên độ rực rỡ của ảnh gốc
                    g2d.setColor(new Color(10, 15, 25, 40)); 
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g2d.setColor(new Color(34, 49, 63));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        pnlMain.setBorder(new EmptyBorder(30, 50, 20, 50));

        pnlMain.add(taoMotBanCo("BẢN ĐỒ CỦA TÔI", false, btnMinh));
        pnlMain.add(taoMotBanCo("RA ĐA ĐỊCH (Tàng Hình)", true, btnDich));
        
        this.add(pnlMain, BorderLayout.CENTER);

        // --- THANH ĐIỀU KHIỂN (BOTTOM BAR) ---
        JPanel pnlControl = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setPaint(new GradientPaint(0, 0, new Color(15, 25, 35, 200), 0, getHeight(), new Color(5, 10, 15, 240)));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        pnlControl.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        lblTrangThai = new JLabel("");
        lblTrangThai.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTrangThai.setForeground(Color.YELLOW);
        
        JLabel lblAI = new JLabel("Độ khó AI: ");
        lblAI.setForeground(Color.WHITE);
        lblAI.setFont(new Font("Segoe UI", Font.BOLD, 13));

        String[] mangDoKho = {"Tân Binh", "Sĩ Quan", "Đô Đốc", "Trí Tuệ Nhân Tạo"};
        cbDoKho = new JComboBox<>(mangDoKho);
        cbDoKho.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbDoKho.addActionListener(e -> doKhoBot = cbDoKho.getSelectedIndex());

        btnXoayTau = taoNutDieuKhien("Xoay: NGANG", new Color(52, 152, 219));
        btnXoayTau.addActionListener(e -> {
            isNgang = !isNgang;
            btnXoayTau.setText(isNgang ? "Xoay: NGANG" : "Xoay: DỌC");
        });

        btnXepNgauNhien = taoNutDieuKhien("Xếp ngẫu nhiên", new Color(26, 188, 156));
        btnXepNgauNhien.addActionListener(e -> {
            xoaHetXepLai();
            java.util.Random rand = new java.util.Random();
            while (tauHienTai < hạmĐội.length) {
                int r = rand.nextInt(10);
                int c = rand.nextInt(10);
                isNgang = rand.nextBoolean();
                xuLyDatTau(r, c, false); 
            }
        });

        btnXoaHet = taoNutDieuKhien("Xóa hết xếp lại", new Color(230, 126, 34));
        btnXoaHet.addActionListener(e -> xoaHetXepLai());

        btnVaoTran = taoNutDieuKhien("VÀO TRẬN!", new Color(46, 204, 113));
        btnVaoTran.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnVaoTran.setEnabled(false);
        btnVaoTran.addActionListener(e -> batDauGame());
        
        btnThoat = taoNutDieuKhien("Về Menu", new Color(231, 76, 60)); 
        btnThoat.addActionListener(e -> {
            int chon = JOptionPane.showConfirmDialog(this, "Chắc kèo muốn thoát chưa bro?", "Thoát?", JOptionPane.YES_NO_OPTION);
            if (chon == JOptionPane.YES_OPTION) {
                new ManHinhChinh().setVisible(true); 
                this.dispose(); 
            }
        });

        pnlControl.add(lblTrangThai);
        pnlControl.add(Box.createHorizontalStrut(10));
        pnlControl.add(lblAI);
        pnlControl.add(cbDoKho); 
        pnlControl.add(Box.createHorizontalStrut(5));
        pnlControl.add(btnXoayTau);
        pnlControl.add(btnXepNgauNhien);
        pnlControl.add(btnXoaHet);
        pnlControl.add(btnVaoTran);
        pnlControl.add(btnThoat); 

        this.add(pnlControl, BorderLayout.SOUTH);
        
        xoaHetXepLai(); 
    }

    private void khoiTaoImages() {
        try {
            imgHit = new ImageIcon(getClass().getResource("/images/hit.png")).getImage();
            imgMiss = new ImageIcon(getClass().getResource("/images/miss.png")).getImage();
            imgTau5 = new ImageIcon(getClass().getResource("/images/tau_5.png")).getImage();
            imgTau4 = new ImageIcon(getClass().getResource("/images/tau_4.png")).getImage();
            imgTau3 = new ImageIcon(getClass().getResource("/images/tau_3.png")).getImage();
            imgTau2 = new ImageIcon(getClass().getResource("/images/tau_2.png")).getImage();
            
            java.net.URL bgUrl = getClass().getResource("/images/game.png");
            if (bgUrl != null) {
                bgBattle = new ImageIcon(bgUrl).getImage();
            }
        } catch (Exception e) {
            System.err.println("LỖI: Thiếu file hình ảnh!");
        }
    }

    private JButton taoNutDieuKhien(String text, Color mauVien) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                if (!isEnabled()) {
                    g2d.setColor(new Color(80, 80, 80, 50));
                    g2d.fillRoundRect(0, 0, w, h, 8, 8);
                    g2d.setColor(new Color(120, 120, 120, 100));
                    g2d.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
                } else if (getModel().isPressed()) {
                    g2d.setPaint(new GradientPaint(0, 0, new Color(10, 15, 20, 200), 0, h, new Color(5, 5, 5, 200)));
                    g2d.fillRoundRect(0, 0, w, h, 8, 8);
                } else if (getModel().isRollover()) {
                    g2d.setPaint(new GradientPaint(0, 0, new Color(mauVien.getRed(), mauVien.getGreen(), mauVien.getBlue(), 80), 0, h, new Color(20, 30, 40, 100)));
                    g2d.fillRoundRect(0, 0, w, h, 8, 8);
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.setColor(mauVien);
                    g2d.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
                } else {
                    g2d.setPaint(new GradientPaint(0, 0, new Color(30, 45, 60, 100), 0, h, new Color(15, 20, 30, 140)));
                    g2d.fillRoundRect(0, 0, w, h, 8, 8);
                    g2d.setStroke(new BasicStroke(1.2f));
                    g2d.setColor(new Color(mauVien.getRed(), mauVien.getGreen(), mauVien.getBlue(), 120));
                    g2d.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
                }
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void xoaHetXepLai() {
        dataBanCoMinh = new BanCo(); 
        dataBanCoDich = new BanCo(); 
        dataBanCoDich.xepTauNgauNhien();
        
        isDangXepTau = true;
        tauHienTai = 0;
        isPlayerTurn = true;
        isAnimating = false;

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                ((OBanCo)btnMinh[r][c]).resetFull();
                ((OBanCo)btnDich[r][c]).resetFull();
            }
        }

        btnXoayTau.setEnabled(true);
        btnXepNgauNhien.setEnabled(true);
        btnXoaHet.setEnabled(true);
        btnVaoTran.setVisible(true); 
        btnVaoTran.setEnabled(false);
        cbDoKho.setEnabled(true);
        
        lblTrangThai.setText("Hãy xếp: " + hạmĐội[0].getTen());
        lblTrangThai.setForeground(Color.YELLOW);
    }

    class OBanCo extends JButton {
        Image shipImg = null;
        int shipLen = 1, shipSeg = 0;
        boolean isShipNgang = true;
        boolean isShowShip = false, isMiss = false;
        boolean isHitAnimation = false; 
        boolean isPermanentlyOnFire = false;

        public void setShipInfo(Image img, int len, int seg, boolean ngang) {
            this.shipImg = img; this.shipLen = len; this.shipSeg = seg; 
            this.isShipNgang = ngang; this.isShowShip = true;
        }

        public void hieuUngNoVaChay() {
            this.isHitAnimation = true;
            this.repaint();
            
            Timer timer = new Timer(600, e -> { 
                this.isHitAnimation = false;
                this.isPermanentlyOnFire = true; 
                this.setEnabled(false); 
                this.repaint();
            });
            timer.setRepeats(false);
            timer.start();
        }

        public void resetFull() {
            this.shipImg = null; this.isShowShip = false;
            this.isMiss = false; this.isHitAnimation = false;
            this.isPermanentlyOnFire = false;
            this.setEnabled(true);
            this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Nếu chuột đang rê vào ô: Sáng bừng xanh dạ quang để dễ canh tọa độ
            if (getModel().isRollover() && isEnabled()) {
                g2.setColor(MAU_RADAR_HOVER);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            // Vẽ ảnh tàu chiến lên trên lớp kính mờ
            if (isShowShip && shipImg != null) {
                int W = shipImg.getWidth(null);
                int H = shipImg.getHeight(null);
                int sw = W / shipLen; 
                int sx1 = shipSeg * sw;
                int sx2 = sx1 + sw;

                if (!isShipNgang) { 
                    g2.translate(getWidth() / 2.0, getHeight() / 2.0);
                    g2.rotate(Math.PI / 2);
                    g2.translate(-getHeight() / 2.0, -getWidth() / 2.0);
                    g2.drawImage(shipImg, 0, 0, getHeight(), getWidth(), sx1, 0, sx2, H, null);
                } else { 
                    g2.drawImage(shipImg, 0, 0, getWidth(), getHeight(), sx1, 0, sx2, H, null);
                }
            }
            g2.dispose();

            // Vẽ hiệu ứng đạn trúng / hụt
            g2 = (Graphics2D) g.create();
            if ((isHitAnimation || isPermanentlyOnFire) && imgHit != null) {
                g2.drawImage(imgHit, 0, 0, getWidth(), getHeight(), null);
            } else if (isMiss && imgMiss != null) {
                g2.drawImage(imgMiss, 0, 0, getWidth(), getHeight(), null);
            }
            g2.dispose();
        }
    }

    private JPanel taoMotBanCo(String tieuDe, boolean isEnemy, JButton[][] mangNut) {
        // --- ĐÂY RỒI: TẠO HỘP KÍNH MỜ CHỐNG CHÓI CHO KHU VỰC BÀN CỜ ---
        JPanel pnlKhuVuc = new JPanel(new BorderLayout(0, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Đổ một lớp nền đen mờ (Alpha = 150) bao bọc riêng khu vực bàn cờ này
                // Giúp lưới màu trắng nổi bần bật lên, triệt tiêu sự chói mắt từ background
                g2d.setColor(new Color(12, 18, 28, 150)); 
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        pnlKhuVuc.setOpaque(false);
        // Thêm khoảng đệm lót viền cho đẹp mắt
        pnlKhuVuc.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel lbl = new JLabel(tieuDe, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(isEnemy ? new Color(255, 94, 87) : new Color(52, 152, 219)); 
        pnlKhuVuc.add(lbl, BorderLayout.NORTH);

        JPanel pnlLuoi = new JPanel(new GridLayout(10, 10, 1, 1)); 
        pnlLuoi.setPreferredSize(new Dimension(400, 400));
        pnlLuoi.setOpaque(false); 
        
        for (int i = 0; i < 100; i++) {
            int r = i / 10, c = i % 10;
            OBanCo btn = new OBanCo();
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            
            // ĐƯỜNG LƯỚI CYBERPUNK CHUẨN: Màu trắng đục rõ ràng trên nền kính đen mờ
            btn.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 1)); 
            
            btn.addActionListener(e -> {
                if (isDangXepTau && !isEnemy) xuLyDatTau(r, c, true);
                else if (!isDangXepTau && isEnemy) playerAttack(r, c); 
            });
            mangNut[r][c] = btn;
            pnlLuoi.add(btn);
        }
        pnlKhuVuc.add(pnlLuoi, BorderLayout.CENTER);
        return pnlKhuVuc;
    }

    private void xuLyDatTau(int r, int c, boolean hienThongBao) {
        if (tauHienTai >= hạmĐội.length) return;
        TauChien tau = hạmĐội[tauHienTai];
        
        if (dataBanCoMinh.datTau(tau, r, c, isNgang)) {
            Image img = (tau.getChieuDai() == 5) ? imgTau5 : (tau.getChieuDai() == 4) ? imgTau4 : (tau.getChieuDai() == 3) ? imgTau3 : imgTau2;
            
            for (int i = 0; i < tau.getChieuDai(); i++) {
                int tr = r + (isNgang ? 0 : i);
                int tc = c + (isNgang ? i : 0);
                OBanCo btn = (OBanCo)btnMinh[tr][tc];
                btn.setShipInfo(img, tau.getChieuDai(), i, isNgang);
                btn.repaint();
            }

            tauHienTai++;
            if (tauHienTai < hạmĐội.length) lblTrangThai.setText("Hãy xếp: " + hạmĐội[tauHienTai].getTen());
            else {
                lblTrangThai.setText("HẠM ĐỘI ĐÃ SẴN SÀNG!");
                lblTrangThai.setForeground(Color.GREEN);
                btnVaoTran.setEnabled(true);
            }
        } else if (isDangXepTau && hienThongBao) {
            JOptionPane.showMessageDialog(this, "Chật chội quá, không đặt tàu ở đây được!");
        }
    }

    private void batDauGame() {
        isDangXepTau = false;
        btnVaoTran.setVisible(false);
        btnXoayTau.setEnabled(false);
        btnXepNgauNhien.setEnabled(false);
        btnXoaHet.setEnabled(false);
        cbDoKho.setEnabled(false);

        lblTrangThai.setText("LƯỢT CỦA BẠN. TẤN CÔNG ĐI!");
        lblTrangThai.setForeground(Color.CYAN);
    }

    private void playerAttack(int r, int c) {
        if (!isPlayerTurn || isAnimating || dataBanCoDich.getOVuong(r, c).isDaBan()) return;
        
        boolean trúng = dataBanCoDich.biBan(r, c);
        OBanCo targetBtn = (OBanCo)btnDich[r][c];

        if (trúng) {
            phatAmThanh("boom.wav");
            isAnimating = true;
            targetBtn.isShowShip = true; 
            targetBtn.hieuUngNoVaChay();

            Timer t = new Timer(600, e -> {
                isAnimating = false;
                if (dataBanCoDich.isThuaCuoc()) {
                    JOptionPane.showMessageDialog(this, "ĐỈNH QUÁ! BẠN ĐÃ ĐÁNH CHÌM TOÀN BỘ HẠM ĐỘI ĐỊCH!");
                    xoaHetXepLai();
                } else {
                    lblTrangThai.setText("TRÚNG RỒI! BẠN ĐƯỢC BẮN TIẾP.");
                    lblTrangThai.setForeground(Color.GREEN);
                }
            });
            t.setRepeats(false); t.start();
            
        } else {
            phatAmThanh("nuoc.wav");
            targetBtn.isMiss = true;
            targetBtn.repaint();
            
            isPlayerTurn = false;
            lblTrangThai.setText("ĐỊCH ĐANG KHAI HỎA...");
            lblTrangThai.setForeground(Color.ORANGE);
            Timer t = new Timer(600, e -> botAttack());
            t.setRepeats(false); t.start();
        }
    }

    private void botAttack() {
        java.util.Random rand = new java.util.Random();
        int r = rand.nextInt(10);
        int c = rand.nextInt(10);
        while (dataBanCoMinh.getOVuong(r, c).isDaBan()) {
            r = rand.nextInt(10); c = rand.nextInt(10);
        }

        boolean trúng = dataBanCoMinh.biBan(r, c);
        OBanCo targetBtn = (OBanCo)btnMinh[r][c];

        if (trúng) {
            phatAmThanh("boom.wav");
            targetBtn.hieuUngNoVaChay();

            Timer t = new Timer(700, e -> {
                if (dataBanCoMinh.isThuaCuoc()) {
                    JOptionPane.showMessageDialog(this, "THUA RỒI! HẠM ĐỘI CỦA BẠN ĐÃ BỊ TIÊU DIỆT.");
                    xoaHetXepLai();
                } else {
                    lblTrangThai.setText("BOT BẮN TRÚNG! NÓ ĐƯỢC BẮN TIẾP...");
                    botAttack(); 
                }
            });
            t.setRepeats(false); t.start();
            
        } else {
            phatAmThanh("nuoc.wav");
            targetBtn.isMiss = true;
            targetBtn.repaint();
            
            isPlayerTurn = true;
            lblTrangThai.setText("LƯỢT CỦA BẠN. TẤN CÔNG ĐI!");
            lblTrangThai.setForeground(Color.CYAN);
        }
    }

    private void phatAmThanh(String tenFile) {
        new Thread(() -> {
            try {
                URL url = getClass().getResource("/sounds/" + tenFile);
                if (url != null) {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(url);
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais);
                    clip.start();
                } 
            } catch (Exception e) {}
        }).start();
    }
}
