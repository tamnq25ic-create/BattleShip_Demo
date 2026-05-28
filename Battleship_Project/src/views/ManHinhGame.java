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
    private boolean isAnimating = false; // Khóa chuột chống spam click khi đang cháy nổ
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

    private JLabel lblTrangThai;
    private JButton btnXoayTau, btnVaoTran, btnXepNgauNhien, btnXoaHet, btnThoat;
    private JComboBox<String> cbDoKho;

    private static final Color MAU_NUOC_BIEN = new Color(52, 152, 219);

    public ManHinhGame() {
        this.setTitle("Battleship Pro - Hạm Đội Thức Giấc");
        this.setSize(1250, 750); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        khoiTaoImages();

        JPanel pnlMain = new JPanel(new GridLayout(1, 2, 50, 0));
        pnlMain.setBorder(new EmptyBorder(20, 20, 20, 20));
        pnlMain.setBackground(new Color(44, 62, 80));

        pnlMain.add(taoMotBanCo("BẢN ĐỒ CỦA TÔI", false, btnMinh));
        pnlMain.add(taoMotBanCo("RA ĐA ĐỊCH (Tàng Hình)", true, btnDich));
        
        this.add(pnlMain, BorderLayout.CENTER);

        JPanel pnlControl = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        pnlControl.setBackground(new Color(52, 73, 94));
        
        lblTrangThai = new JLabel("");
        lblTrangThai.setFont(new Font("Arial", Font.BOLD, 15));
        lblTrangThai.setForeground(Color.YELLOW);
        
        btnXoayTau = new JButton("Xoay: NGANG");
        btnXoayTau.addActionListener(e -> {
            isNgang = !isNgang;
            btnXoayTau.setText(isNgang ? "Xoay: NGANG" : "Xoay: DỌC");
        });

        btnXepNgauNhien = new JButton("Xếp ngẫu nhiên");
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

        btnXoaHet = new JButton("Xóa hết xếp lại");
        btnXoaHet.setBackground(new Color(230, 126, 34));
        btnXoaHet.setForeground(Color.WHITE);
        btnXoaHet.addActionListener(e -> xoaHetXepLai());

        String[] mangDoKho = {"Tân Binh", "Sĩ Quan", "Đô Đốc", "Trí Tuệ Nhân Tạo"};
        cbDoKho = new JComboBox<>(mangDoKho);
        cbDoKho.addActionListener(e -> doKhoBot = cbDoKho.getSelectedIndex());

        btnVaoTran = new JButton("VÀO TRẬN!");
        btnVaoTran.setBackground(new Color(46, 204, 113));
        btnVaoTran.setForeground(Color.WHITE);
        btnVaoTran.setFont(new Font("Arial", Font.BOLD, 14));
        btnVaoTran.setEnabled(false);
        btnVaoTran.addActionListener(e -> batDauGame());
        
        btnThoat = new JButton("Về Menu");
        btnThoat.setBackground(new Color(231, 76, 60)); 
        btnThoat.setForeground(Color.WHITE);
        btnThoat.addActionListener(e -> {
            int chon = JOptionPane.showConfirmDialog(this, "Chắc kèo muốn thoát chưa bro?", "Thoát?", JOptionPane.YES_NO_OPTION);
            if (chon == JOptionPane.YES_OPTION) {
                new ManHinhChinh().setVisible(true); 
                this.dispose(); 
            }
        });

        pnlControl.add(lblTrangThai);
        pnlControl.add(new JLabel("Độ khó AI: "));
        pnlControl.add(cbDoKho); 
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
        } catch (Exception e) {
            System.err.println("LỖI: Thiếu file ảnh!");
        }
    }

    private void xoaHetXepLai() {
        dataBanCoMinh = new BanCo(); 
        dataBanCoDich = new BanCo(); 
        dataBanCoDich.xepTauNgauNhien();
        
        isDangXepTau = true;
        tauHienTai = 0;
        isPlayerTurn = true;
        isAnimating = false;

        // Xóa trắng giao diện mà ko cần quét rác bộ nhớ
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
            super.paintComponent(g); 
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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

    private JPanel taoMotBanCo(String tieuDe, boolean isEnemy, JButton[][] mangNut) {
        JPanel pnlKhuVuc = new JPanel(new BorderLayout(0, 10));
        pnlKhuVuc.setOpaque(false);
        JLabel lbl = new JLabel(tieuDe, SwingConstants.CENTER);
        lbl.setForeground(Color.WHITE);
        pnlKhuVuc.add(lbl, BorderLayout.NORTH);

        JPanel pnlLuoi = new JPanel(new GridLayout(10, 10));
        pnlLuoi.setPreferredSize(new Dimension(400, 400));
        pnlLuoi.setOpaque(false); 
        
        for (int i = 0; i < 100; i++) {
            int r = i / 10, c = i % 10;
            OBanCo btn = new OBanCo();
            btn.setBackground(MAU_NUOC_BIEN);
            btn.setBorder(BorderFactory.createLineBorder(new Color(41, 128, 185), 1));
            btn.setFocusPainted(false);
            btn.setRolloverEnabled(false);
            
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
            
            // TỐI ƯU CỰC MẠNH: Chỉ vẽ lên đúng những ô vừa xếp, không cần load lại toàn bộ bàn cờ!
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
    }

    private void playerAttack(int r, int c) {
        if (!isPlayerTurn || isAnimating || dataBanCoDich.getOVuong(r, c).isDaBan()) return;
        
        boolean trúng = dataBanCoDich.biBan(r, c);
        OBanCo targetBtn = (OBanCo)btnDich[r][c]; // CHỈ MỤC TIÊU NÀY BỊ THAY ĐỔI

        if (trúng) {
            phatAmThanh("boom.wav");
            isAnimating = true;
            targetBtn.isShowShip = true; // Hiện xác tàu ra
            targetBtn.hieuUngNoVaChay();

            Timer t = new Timer(600, e -> {
                isAnimating = false;
                if (dataBanCoDich.isThuaCuoc()) {
                    JOptionPane.showMessageDialog(this, "ĐỈNH QUÁ! BẠN ĐÃ ĐÁNH CHÌM TOÀN BỘ HẠM ĐỘI ĐỊCH!");
                    xoaHetXepLai();
                } else {
                    lblTrangThai.setText("TRÚNG RỒI! BẠN ĐƯỢC BẮN TIẾP.");
                }
            });
            t.setRepeats(false); t.start();
            
        } else {
            phatAmThanh("nuoc.wav");
            targetBtn.isMiss = true;
            targetBtn.repaint();
            
            isPlayerTurn = false;
            lblTrangThai.setText("ĐỊCH ĐANG KHAI HỎA...");
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
        OBanCo targetBtn = (OBanCo)btnMinh[r][c]; // CHỈ MỤC TIÊU NÀY BỊ THAY ĐỔI

        if (trúng) {
            phatAmThanh("boom.wav");
            targetBtn.hieuUngNoVaChay(); // Tàu mình đã hiện sẵn, chỉ cần thêm lửa vào

            Timer t = new Timer(700, e -> {
                if (dataBanCoMinh.isThuaCuoc()) {
                    JOptionPane.showMessageDialog(this, "THUA RỒI! HẠM ĐỘI CỦA BẠN ĐÃ BỊ TIÊU DIỆT.");
                    xoaHetXepLai();
                } else {
                    lblTrangThai.setText("BOT BẮN TRÚNG! NÓ ĐƯỢC BẮN TIẾP...");
                    botAttack(); // Bot bắn tiếp vì trúng
                }
            });
            t.setRepeats(false); t.start();
            
        } else {
            phatAmThanh("nuoc.wav");
            targetBtn.isMiss = true;
            targetBtn.repaint();
            
            isPlayerTurn = true;
            lblTrangThai.setText("LƯỢT CỦA BẠN. TẤN CÔNG ĐI!");
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