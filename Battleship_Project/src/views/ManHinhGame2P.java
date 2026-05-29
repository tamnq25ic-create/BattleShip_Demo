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

public class ManHinhGame2P extends JFrame {

    private BanCo p1Data, p2Data;
    private JButton[][] btnTrai = new JButton[10][10];
    private JButton[][] btnPhai = new JButton[10][10];

    // Trạng thái game
    private boolean isSetup = true; 
    private boolean isP1Turn = true; 
    private boolean isAnimating = false;

    // Thông số P1
    private boolean isNgangP1 = true;
    private int tauHienTaiP1 = 0;
    private boolean anTauP1 = false;
    
    // Thông số P2
    private boolean isNgangP2 = true;
    private int tauHienTaiP2 = 0;
    private boolean anTauP2 = false;

    private TauChien[] hamDoi = {
        new TauChien("Tàu Sân Bay", 5),
        new TauChien("Tàu Chiến", 4),
        new TauChien("Tàu Khu Trục 1", 3),
        new TauChien("Tàu Khu Trục 2", 3),
        new TauChien("Tàu Tuần Tra", 2)
    };

    private Image imgHit, imgMiss, imgTau5, imgTau4, imgTau3, imgTau2;
    private Image bgBattle = null; // Ảnh nền chính

    // UI Components
    private JLabel lblTrangThaiMain;
    private JButton btnVaoTran, btnAnHienP1, btnAnHienP2;
    private JPanel pnlSetupP1, pnlSetupP2;

    // Màu radar dạ quang khi rê chuột qua ô cờ
    private static final Color MAU_RADAR_HOVER = new Color(52, 152, 219, 100);

    public ManHinhGame2P() {
        this.setTitle("Battleship Pro - Đấu 2 Người (Bản Tối Ưu Mượt Mà)");
        this.setSize(1280, 750); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        khoiTaoImages();
        p1Data = new BanCo();
        p2Data = new BanCo();

        // --- GẮN BACKGROUND CHO KHU VỰC CHÍNH ---
        JPanel pnlMain = new JPanel(new GridLayout(1, 2, 50, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (bgBattle != null) {
                    g2d.drawImage(bgBattle, 0, 0, getWidth(), getHeight(), this);
                    // Phủ một lớp đen mờ mỏng để làm dịu mắt, nổi bật bàn cờ
                    g2d.setColor(new Color(10, 15, 25, 40)); 
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g2d.setColor(new Color(34, 49, 63));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        pnlMain.setBorder(new EmptyBorder(30, 40, 20, 40));

        pnlMain.add(taoKhuVucP1());
        pnlMain.add(taoKhuVucP2());
        this.add(pnlMain, BorderLayout.CENTER);

        // --- THANH ĐIỀU KHIỂN CHÍNH (BOTTOM) TRONG SUỐT ---
        JPanel pnlControlMain = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setPaint(new GradientPaint(0, 0, new Color(15, 25, 35, 200), 0, getHeight(), new Color(5, 10, 15, 240)));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        
        lblTrangThaiMain = new JLabel("XẾP TÀU VÀ ẤN 'ẨN TÀU' TRƯỚC KHI BẮT ĐẦU");
        lblTrangThaiMain.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTrangThaiMain.setForeground(Color.YELLOW);

        btnVaoTran = taoNutGiaoDien("VÀO TRẬN !!!", new Color(46, 204, 113));
        btnVaoTran.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnVaoTran.setEnabled(false);
        btnVaoTran.addActionListener(e -> batDauTranChien());

        JButton btnReset = taoNutGiaoDien("Reset Game", new Color(243, 156, 18));
        btnReset.addActionListener(e -> {
            int chon = JOptionPane.showConfirmDialog(this, "Chơi lại từ đầu?", "Reset Game", JOptionPane.YES_NO_OPTION);
            if(chon == JOptionPane.YES_OPTION) {
                new ManHinhGame2P().setVisible(true);
                this.dispose();
            }
        });

        JButton btnThoat = taoNutGiaoDien("Về Menu", new Color(231, 76, 60));
        btnThoat.addActionListener(e -> {
            int chon = JOptionPane.showConfirmDialog(this, "Thoát ra Menu chính?", "Thoát?", JOptionPane.YES_NO_OPTION);
            if(chon == JOptionPane.YES_OPTION) {
                new ManHinhChinh().setVisible(true);
                this.dispose();
            }
        });

        pnlControlMain.add(lblTrangThaiMain);
        pnlControlMain.add(Box.createHorizontalStrut(10));
        pnlControlMain.add(btnVaoTran);
        pnlControlMain.add(btnReset);
        pnlControlMain.add(btnThoat);
        this.add(pnlControlMain, BorderLayout.SOUTH);
    }

    private void khoiTaoImages() {
        try {
            imgHit = new ImageIcon(getClass().getResource("/images/hit.png")).getImage();
            imgMiss = new ImageIcon(getClass().getResource("/images/miss.png")).getImage();
            imgTau5 = new ImageIcon(getClass().getResource("/images/tau_5.png")).getImage();
            imgTau4 = new ImageIcon(getClass().getResource("/images/tau_4.png")).getImage();
            imgTau3 = new ImageIcon(getClass().getResource("/images/tau_3.png")).getImage();
            imgTau2 = new ImageIcon(getClass().getResource("/images/tau_2.png")).getImage();
            
            // Load ảnh game.png
            URL bgUrl = getClass().getResource("/images/game.png");
            if (bgUrl != null) {
                bgBattle = new ImageIcon(bgUrl).getImage();
            }
        } catch (Exception e) {
            System.err.println("Lỗi load ảnh.");
        }
    }

    // Nút bấm phong cách mới, hiện đại hơn
    private JButton taoNutGiaoDien(String text, Color mauChuDao) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                if (!isEnabled()) {
                    g2d.setColor(new Color(80, 80, 80, 60));
                    g2d.fillRoundRect(0, 0, w, h, 6, 6);
                } else if (getModel().isPressed()) {
                    g2d.setColor(new Color(10, 15, 20, 180));
                    g2d.fillRoundRect(0, 0, w, h, 6, 6);
                } else if (getModel().isRollover()) {
                    g2d.setPaint(new GradientPaint(0, 0, new Color(mauChuDao.getRed(), mauChuDao.getGreen(), mauChuDao.getBlue(), 90), 0, h, new Color(20, 30, 40, 120)));
                    g2d.fillRoundRect(0, 0, w, h, 6, 6);
                    g2d.setStroke(new BasicStroke(1.8f));
                    g2d.setColor(mauChuDao);
                    g2d.drawRoundRect(1, 1, w - 3, h - 3, 6, 6);
                } else {
                    g2d.setPaint(new GradientPaint(0, 0, new Color(30, 45, 60, 120), 0, h, new Color(15, 20, 30, 160)));
                    g2d.fillRoundRect(0, 0, w, h, 6, 6);
                    g2d.setStroke(new BasicStroke(1f));
                    g2d.setColor(new Color(mauChuDao.getRed(), mauChuDao.getGreen(), mauChuDao.getBlue(), 120));
                    g2d.drawRoundRect(1, 1, w - 3, h - 3, 6, 6);
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
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
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

        public void clearShipInfo() {
            this.shipImg = null; 
            this.isShowShip = false;
        }

        public void resetFull() {
            clearShipInfo();
            this.isMiss = false;
            this.isHitAnimation = false;
            this.isPermanentlyOnFire = false;
            this.setEnabled(true);
            this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Hover chuột sáng lưới radar
            if (getModel().isRollover() && isEnabled()) {
                g2.setColor(MAU_RADAR_HOVER);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            if (isShowShip) {
                if (shipImg != null) {
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
                } else { 
                    g2.setColor(Color.GRAY); g2.fillRect(0, 0, getWidth(), getHeight());
                }
            }
            g2.dispose();
            g2 = (Graphics2D) g.create();

            if ((isHitAnimation || isPermanentlyOnFire) && imgHit != null) {
                g2.drawImage(imgHit, 0, 0, getWidth(), getHeight(), null);
            } else if (isMiss && imgMiss != null) {
                g2.drawImage(imgMiss, 0, 0, getWidth(), getHeight(), null);
            }
            g2.dispose();
        }
    }

    private JPanel taoKhuVucP1() {
        // Hộp kính mờ Glassmorphism
        JPanel pnlTrai = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(12, 18, 28, 150)); 
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        pnlTrai.setOpaque(false);
        pnlTrai.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel lblTieuDe = new JLabel("LÃNH ĐỊA PLAYER 1", SwingConstants.CENTER);
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTieuDe.setForeground(new Color(52, 152, 219)); 
        pnlTrai.add(lblTieuDe, BorderLayout.NORTH);

        JPanel pnlLuoi = new JPanel(new GridLayout(10, 10, 1, 1));
        pnlLuoi.setPreferredSize(new Dimension(400, 400));
        pnlLuoi.setOpaque(false);

        for (int i = 0; i < 100; i++) {
            int r = i / 10, c = i % 10;
            OBanCo btn = new OBanCo();
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 1)); // Viền trắng mờ
            btn.addActionListener(e -> {
                if (isSetup) xuLyDatTau(r, c, 1);
                else if (!isP1Turn && !isAnimating) playerAttack(r, c, 2); 
            });
            btnTrai[r][c] = btn;
            pnlLuoi.add(btn);
        }
        pnlTrai.add(pnlLuoi, BorderLayout.CENTER);

        pnlSetupP1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        pnlSetupP1.setOpaque(false);
        
        JButton btnXoayP1 = taoNutGiaoDien("Xoay: NGANG", new Color(52, 152, 219));
        btnXoayP1.addActionListener(e -> { isNgangP1 = !isNgangP1; btnXoayP1.setText(isNgangP1 ? "Xoay: NGANG" : "Xoay: DỌC"); });
        
        JButton btnRandomP1 = taoNutGiaoDien("Xếp Nhanh", new Color(26, 188, 156));
        btnRandomP1.addActionListener(e -> {
            autoXepTau(1);
            anTauP1 = false; 
            btnAnHienP1.setText("Ẩn Tàu");
        });

        btnAnHienP1 = taoNutGiaoDien("Ẩn Tàu", new Color(155, 89, 182));
        btnAnHienP1.addActionListener(e -> {
            anTauP1 = !anTauP1;
            btnAnHienP1.setText(anTauP1 ? "Hiện Tàu" : "Ẩn Tàu");
            anHienTauNhanh(1, anTauP1);
        });

        pnlSetupP1.add(btnXoayP1);
        pnlSetupP1.add(btnRandomP1);
        pnlSetupP1.add(btnAnHienP1);
        pnlTrai.add(pnlSetupP1, BorderLayout.SOUTH);

        return pnlTrai;
    }

    private JPanel taoKhuVucP2() {
        // Hộp kính mờ Glassmorphism
        JPanel pnlPhai = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(12, 18, 28, 150)); 
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        pnlPhai.setOpaque(false);
        pnlPhai.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel lblTieuDe = new JLabel("LÃNH ĐỊA PLAYER 2", SwingConstants.CENTER);
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTieuDe.setForeground(new Color(231, 76, 60)); 
        pnlPhai.add(lblTieuDe, BorderLayout.NORTH);

        JPanel pnlLuoi = new JPanel(new GridLayout(10, 10, 1, 1));
        pnlLuoi.setPreferredSize(new Dimension(400, 400));
        pnlLuoi.setOpaque(false);

        for (int i = 0; i < 100; i++) {
            int r = i / 10, c = i % 10;
            OBanCo btn = new OBanCo();
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 1)); // Viền trắng mờ
            btn.addActionListener(e -> {
                if (isSetup) xuLyDatTau(r, c, 2);
                else if (isP1Turn && !isAnimating) playerAttack(r, c, 1); 
            });
            btnPhai[r][c] = btn;
            pnlLuoi.add(btn);
        }
        pnlPhai.add(pnlLuoi, BorderLayout.CENTER);

        pnlSetupP2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        pnlSetupP2.setOpaque(false);
        
        JButton btnXoayP2 = taoNutGiaoDien("Xoay: NGANG", new Color(52, 152, 219));
        btnXoayP2.addActionListener(e -> { isNgangP2 = !isNgangP2; btnXoayP2.setText(isNgangP2 ? "Xoay: NGANG" : "Xoay: DỌC"); });
        
        JButton btnRandomP2 = taoNutGiaoDien("Xếp Nhanh", new Color(26, 188, 156));
        btnRandomP2.addActionListener(e -> {
            autoXepTau(2);
            anTauP2 = false; 
            btnAnHienP2.setText("Ẩn Tàu");
        });

        btnAnHienP2 = taoNutGiaoDien("Ẩn Tàu", new Color(155, 89, 182));
        btnAnHienP2.addActionListener(e -> {
            anTauP2 = !anTauP2;
            btnAnHienP2.setText(anTauP2 ? "Hiện Tàu" : "Ẩn Tàu");
            anHienTauNhanh(2, anTauP2);
        });

        pnlSetupP2.add(btnXoayP2);
        pnlSetupP2.add(btnRandomP2);
        pnlSetupP2.add(btnAnHienP2);
        pnlPhai.add(pnlSetupP2, BorderLayout.SOUTH);

        return pnlPhai;
    }

    private void autoXepTau(int player) {
        java.util.Random rand = new java.util.Random();
        if (player == 1) {
            p1Data = new BanCo();
            tauHienTaiP1 = 0;
            for(int r=0; r<10; r++) for(int c=0; c<10; c++) ((OBanCo)btnTrai[r][c]).resetFull();
            while (tauHienTaiP1 < hamDoi.length) {
                isNgangP1 = rand.nextBoolean();
                xuLyDatTau(rand.nextInt(10), rand.nextInt(10), 1);
            }
        } else {
            p2Data = new BanCo();
            tauHienTaiP2 = 0;
            for(int r=0; r<10; r++) for(int c=0; c<10; c++) ((OBanCo)btnPhai[r][c]).resetFull();
            while (tauHienTaiP2 < hamDoi.length) {
                isNgangP2 = rand.nextBoolean();
                xuLyDatTau(rand.nextInt(10), rand.nextInt(10), 2);
            }
        }
    }

    private void xuLyDatTau(int r, int c, int player) {
        BanCo data = (player == 1) ? p1Data : p2Data;
        int tauHienTai = (player == 1) ? tauHienTaiP1 : tauHienTaiP2;
        boolean isNgang = (player == 1) ? isNgangP1 : isNgangP2;

        if (tauHienTai >= hamDoi.length) return;

        TauChien tau = hamDoi[tauHienTai];
        if (data.datTau(tau, r, c, isNgang)) {
            Image img = (tau.getChieuDai() == 5) ? imgTau5 : (tau.getChieuDai() == 4) ? imgTau4 : (tau.getChieuDai() == 3) ? imgTau3 : imgTau2;
            
            for (int i = 0; i < tau.getChieuDai(); i++) {
                int tr = r + (isNgang ? 0 : i);
                int tc = c + (isNgang ? i : 0);
                OBanCo btn = (player == 1) ? (OBanCo)btnTrai[tr][tc] : (OBanCo)btnPhai[tr][tc];
                btn.setShipInfo(img, tau.getChieuDai(), i, isNgang);
                btn.isShowShip = (player == 1) ? !anTauP1 : !anTauP2;
                btn.repaint();
            }
            
            if (player == 1) tauHienTaiP1++; else tauHienTaiP2++;
            kiemTraHoanThanhSetup();
        }
    }

    private void anHienTauNhanh(int player, boolean anTau) {
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                OBanCo btn = (player == 1) ? (OBanCo)btnTrai[r][c] : (OBanCo)btnPhai[r][c];
                if (btn.shipImg != null) {
                    btn.isShowShip = !anTau;
                    btn.repaint();
                }
            }
        }
    }

    private void kiemTraHoanThanhSetup() {
        if (tauHienTaiP1 >= hamDoi.length && tauHienTaiP2 >= hamDoi.length) {
            btnVaoTran.setEnabled(true);
            lblTrangThaiMain.setText("OK! HAI BÊN ĐÃ XONG, BẤM VÀO TRẬN THÔI!");
        }
    }

    private void batDauTranChien() {
        isSetup = false;
        pnlSetupP1.setVisible(false);
        pnlSetupP2.setVisible(false);
        btnVaoTran.setVisible(false);
        
        anHienTauNhanh(1, true);
        anHienTauNhanh(2, true);
        
        isP1Turn = true;
        capNhatTrangThaiChinh();
    }

    private void playerAttack(int r, int c, int attacker) {
        BanCo banCoDich = (attacker == 1) ? p2Data : p1Data;
        if (banCoDich.getOVuong(r, c).isDaBan()) return;

        boolean trung = banCoDich.biBan(r, c);
        
        OBanCo targetBtn = (attacker == 1) ? (OBanCo)btnPhai[r][c] : (OBanCo)btnTrai[r][c];

        if (trung) {
            phatAmThanh("boom.wav");
            isAnimating = true; 
            lblTrangThaiMain.setText("BÙM! ĐANG BỐC CHÁY...");
            lblTrangThaiMain.setForeground(Color.ORANGE);
            
            targetBtn.isShowShip = true;
            targetBtn.hieuUngNoVaChay();
            
            Timer t = new Timer(600, e -> {
                isAnimating = false;
                if (banCoDich.isThuaCuoc()) {
                    String winner = (attacker == 1) ? "PLAYER 1" : "PLAYER 2";
                    lblTrangThaiMain.setText(winner + " ĐÃ CHIẾN THẮNG!");
                    lblTrangThaiMain.setForeground(Color.GREEN);
                    
                    int luaChon = JOptionPane.showConfirmDialog(this, winner + " ĐÃ CHIẾN THẮNG!\nChơi ván mới không?", "KẾT THÚC", JOptionPane.YES_NO_OPTION);
                    if (luaChon == JOptionPane.YES_OPTION) {
                        new ManHinhGame2P().setVisible(true);
                        this.dispose();
                    }
                } else {
                    lblTrangThaiMain.setText("P" + attacker + " BẮN TRÚNG! ĐƯỢC BẮN TIẾP.");
                    lblTrangThaiMain.setForeground((attacker == 1) ? new Color(52, 152, 219) : new Color(231, 76, 60));
                }
            });
            t.setRepeats(false);
            t.start();
            
        } else {
            phatAmThanh("nuoc.wav");
            targetBtn.isMiss = true;
            targetBtn.repaint(); 
            
            isP1Turn = !isP1Turn;
            capNhatTrangThaiChinh();
        }
    }

    private void capNhatTrangThaiChinh() {
        if (isP1Turn) {
            lblTrangThaiMain.setText("<<< LƯỢT PLAYER 1 >>> ");
            lblTrangThaiMain.setForeground(new Color(52, 152, 219));
        } else {
            lblTrangThaiMain.setText("LƯỢT PLAYER 2  >>>");
            lblTrangThaiMain.setForeground(new Color(231, 76, 60));
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
