package views;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;          
import java.net.*;         
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.*; 
import javax.swing.*;
import javax.swing.border.*;
import models.BanCo;
import models.TauChien;

public class ManHinhGameOnline extends JFrame {

    // Hệ màu Cyberpunk/Sci-fi contrast cao
    private static final Color BG_DARK_VOID = new Color(2, 4, 10);      // Đen sâu thẳm
    private static final Color COLOR_CYAN = new Color(0, 240, 255);     // Neon Cyan
    private static final Color COLOR_GOLD = new Color(255, 190, 0);     // Vàng hổ phách
    private static final Color COLOR_RED = new Color(255, 30, 80);      // Đỏ Neon
    private static final Color TEXT_MUTED = new Color(130, 150, 175);   
    
    // Màu cho tinh vân (Nebulae) nền
    private static final Color NEBULA_PURPLE = new Color(40, 10, 70, 40); // Tím mờ
    private static final Color NEBULA_BLUE = new Color(10, 40, 80, 30);   // Xanh mờ

    private static final int CELL_SIZE = 45;       
    private static final int GRID_Y = 160;         
    private static final int GRID_MINH_X = 80;     
    private static final int GRID_DICH_X = 640;    

    private BanCo dataBanCoMinh, dataBanCoDich;
    private OBanCo[][] btnMinh = new OBanCo[10][10];
    private OBanCo[][] btnDich = new OBanCo[10][10];
    
    private boolean isDangXepTau = false; 
    private boolean isMyTurn = false; 
    private boolean isNgang = true;      
    private int tauHienTai = 0;          

    private boolean isHost = false;
    private Socket socket;             
    private ServerSocket serverSocket; 
    private PrintWriter out;           
    private BufferedReader in;         
    private boolean daKetNoi = false; 
    private boolean doiPhuongSanSang = false;
    private boolean minhSanSang = false;
    private boolean dangThoat = false; 
    

    private String maPhongHienTai = "";
    private JButton btnCopyMa;
    private JTextArea txtChat;
    private JTextArea txtHeThong; 
    private JTextField txtNhapChat;
    private JButton btnGuiChat;

    private TauChien[] hamDoi = {
        new TauChien("Siêu Hạm Chỉ Huy", 5),
        new TauChien("Tuần Dương Hạm Lớp Alpha", 4),
        new TauChien("Khinh Hạm Tấn Công I", 3),
        new TauChien("Khinh Hạm Tấn Công II", 3),
        new TauChien("Tàu Ngầm Trinh Sát", 2)
    };

    private Image imgHit, imgMiss, imgTau5, imgTau4, imgTau3, imgTau2;
    private JLabel lblTrangThai;
    private JButton btnXoayTau, btnXepNgauNhien, btnSanSang, btnThoatGame;
    
    private BattlefieldPanel pnlBattlefield;
    private CardLayout cardLayout;
    private StarryBackgroundPanel pnlCards; 
    
    // Biến cho hiệu ứng Pulsing Text
    private float statusPulseAlpha = 1.0f;
    private boolean statusPulseUp = false;

    public ManHinhGameOnline() {
        this.setTitle("BATTLESHIP TACTICAL COMMAND center v2.0 - LIVE INTERFACE");
        this.setSize(1450, 850);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        
        khoiTaoImages();
        khoiTaoBanCoBanDau(); 

        cardLayout = new CardLayout();
        pnlCards = new StarryBackgroundPanel(cardLayout);

        pnlCards.add(taoManHinhLobby(), "LOBBY");
        pnlCards.add(taoManHinhGame(), "GAME");

        this.setLayout(new BorderLayout());
        this.add(pnlCards, BorderLayout.CENTER);
        
        cardLayout.show(pnlCards, "LOBBY");
        
        // Timer global cho hiệu ứng nhấp nháy chữ trạng thái
        new Timer(50, e -> {
            if (lblTrangThai != null && lblTrangThai.isShowing()) {
                if (statusPulseUp) {
                    statusPulseAlpha += 0.05f;
                    if (statusPulseAlpha >= 1.0f) { statusPulseAlpha = 1.0f; statusPulseUp = false; }
                } else {
                    statusPulseAlpha -= 0.05f;
                    if (statusPulseAlpha <= 0.4f) { statusPulseAlpha = 0.4f; statusPulseUp = true; }
                }
                Color baseC = lblTrangThai.getForeground();
                lblTrangThai.setForeground(new Color(baseC.getRed(), baseC.getGreen(), baseC.getBlue(), (int)(statusPulseAlpha * 255)));
            }
        }).start();
    }

    private void phatAmThanh(String tenFile) {
        new Thread(() -> {
            try {
                URL url = getClass().getResource("/sounds/" + tenFile);
                if (url != null) {
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.start();
                }
            } catch (Exception e) { }
        }).start();
    }

    /**
     * 1. NÂNG CẤP CAO CẤP: NỀN VŨ TRỤ ĐỘNG CÓ TINH VÂN TRÔI (nebulae drift) VÀ SAO ĐA DẠNG
     */
    class StarryBackgroundPanel extends JPanel {
        private class Star {
            int x, y, size; float alpha, speed; boolean dimming;
            Star(int x, int y, int size, float speed) {
                this.x = x; this.y = y; this.size = size; this.speed = speed;
                this.alpha = (float) Math.random();
                this.dimming = Math.random() > 0.5;
            }
            void update() {
                if (dimming) {
                    alpha -= speed;
                    if (alpha <= 0.05f) { alpha = 0.05f; dimming = false; }
                } else {
                    alpha += speed;
                    if (alpha >= 1.0f) { alpha = 1.0f; dimming = true; }
                }
            }
        }
        
        private List<Star> stars = new ArrayList<>();
        // Các biến cho tinh vân trôi
        private float nebula1X, nebula1Y, nebula2X, nebula2Y;

        public StarryBackgroundPanel(LayoutManager layout) {
            super(layout);
            Random r = new Random();
            // Tạo sao nhiều lớp: sao nhỏ mờ (xa), sao lớn sáng (gần)
            for (int i = 0; i < 250; i++) { 
                int size = (r.nextInt(10) > 7) ? 2 : 1; // 70% sao 1px, 30% sao 2px
                float speed = (size == 1) ? 0.005f + r.nextFloat()*0.005f : 0.015f + r.nextFloat()*0.01f;
                stars.add(new Star(r.nextInt(1920), r.nextInt(1080), size, speed));
            }
            nebula1X = 0; nebula1Y = 0; nebula2X = 1000; nebula2Y = 500;

            // Timer chính cập nhật nền (25 FPS cho mượt)
            new Timer(40, e -> {
                for (Star s : stars) s.update();
                // Nebulae di chuyển chậm chạp chéo nhau
                nebula1X += 0.3f; nebula1Y += 0.15f;
                nebula2X -= 0.2f; nebula2Y -= 0.25f;
                // Reset vị trí nebulae khi trôi quá xa
                if (nebula1X > 2000) nebula1X = -500; if (nebula1Y > 1200) nebula1Y = -500;
                if (nebula2X < -800) nebula2X = 2000; if (nebula2Y < -800) nebula2Y = 1200;
                repaint();
            }).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Nền đen sâu
            g2.setColor(BG_DARK_VOID);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Vẽ tinh vân chìm (Layer nền)
            veNebula(g2, nebula1X, nebula1Y, 1200, NEBULA_PURPLE);
            veNebula(g2, nebula2X, nebula2Y, 1000, NEBULA_BLUE);

            // Vẽ Radial Gradient tạo chiều sâu trung tâm (Layer giữa)
            Point2D center = new Point2D.Float(getWidth() / 2.0f, getHeight() / 2.0f);
            RadialGradientPaint rgp = new RadialGradientPaint(center, Math.max(getWidth(), getHeight()), 
                new float[]{0.0f, 0.8f, 1.0f},
                new Color[]{new Color(15, 25, 50, 60), new Color(5, 8, 15, 20), new Color(0, 0, 0, 0)});
            g2.setPaint(rgp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            // Vẽ sao lấp lánh (Layer trên cùng nền)
            for (Star s : stars) {
                g2.setColor(new Color(1f, 1f, 1f, s.alpha));
                g2.fillOval(s.x, s.y, s.size, s.size);
            }
            g2.dispose();
        }

        private void veNebula(Graphics2D g2, float x, float y, int size, Color color) {
            RadialGradientPaint rgp = new RadialGradientPaint(
                new Point2D.Float(x + size/2f, y + size/2f), size/2f,
                new float[]{0.0f, 1.0f}, new Color[]{color, new Color(0,0,0,0)});
            g2.setPaint(rgp);
            g2.fillOval((int)x, (int)y, size, size);
        }
    }

    class OBanCo {
        int r, c;
        boolean isEnemy;
        Image shipImg = null; 
        int shipLen = 1, shipSeg = 0; 
        boolean isShipNgang = true; 
        boolean isShowShip = false; 
        boolean isMiss = false; 
        boolean isHit = false;
        boolean isHovered = false;

        public OBanCo(int r, int c, boolean isEnemy) { this.r = r; this.c = c; this.isEnemy = isEnemy; }
        public void setShipInfo(Image img, int len, int seg, boolean ngang) {
            this.shipImg = img; this.shipLen = len; this.shipSeg = seg; this.isShipNgang = ngang; this.isShowShip = true;
        }
        public void resetFull() { this.shipImg = null; this.isShowShip = false; this.isMiss = false; this.isHit = false; this.isHovered = false; }
    }

    /**
     * 2. NÂNG CẤP CAO CẤP: BÀN CỜ HOLOGRAPHIC LẤP LÁNH, CÓ RADAR BEAM QUÉT XOAY
     */
    class BattlefieldPanel extends JPanel {
        private float radarAngle = 0f; // Góc xoay của tia radar

        public BattlefieldPanel() {
            setOpaque(false);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int x = e.getX(); int y = e.getY();
                    for (int r = 0; r < 10; r++) { for (int c = 0; c < 10; c++) { btnMinh[r][c].isHovered = false; btnDich[r][c].isHovered = false; } }
                    if (isInside(x, y, GRID_MINH_X)) { updateHover(x, y, GRID_MINH_X, btnMinh); }
                    if (isInside(x, y, GRID_DICH_X)) { updateHover(x, y, GRID_DICH_X, btnDich); }
                    repaint();
                }
                private boolean isInside(int x, int y, int startX) { return x >= startX && x < startX + 10 * CELL_SIZE && y >= GRID_Y && y < GRID_Y + 10 * CELL_SIZE; }
                private void updateHover(int x, int y, int startX, OBanCo[][] grid) {
                    int c = (x - startX) / CELL_SIZE; int r = (y - GRID_Y) / CELL_SIZE;
                    if (r >= 0 && r < 10 && c >= 0 && c < 10) grid[r][c].isHovered = true;
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int x = e.getX(); int y = e.getY();
                    if (isInside(x, y, GRID_MINH_X)) { handleClick(x, y, GRID_MINH_X, true); }
                    if (isInside(x, y, GRID_DICH_X)) { handleClick(x, y, GRID_DICH_X, false); }
                }
                private boolean isInside(int x, int y, int startX) { return x >= startX && x < startX + 10 * CELL_SIZE && y >= GRID_Y && y < GRID_Y + 10 * CELL_SIZE; }
                private void handleClick(int x, int y, int startX, boolean isMinh) {
                    int c = (x - startX) / CELL_SIZE; int r = (y - GRID_Y) / CELL_SIZE;
                    if (r < 0 || r >= 10 || c < 0 || c >= 10) return;
                    if (isMinh) { if (isDangXepTau && daKetNoi) xuLyDatTau(r, c, true); }
                    else { if (!isDangXepTau && !btnDich[r][c].isHit && !btnDich[r][c].isMiss) guiLenhBan(r, c); }
                }
            });

            // Timer xoay tia Radar bên lưới địch
            new Timer(30, e -> { radarAngle += 2.5f; if(radarAngle>=360) radarAngle=0; repaint(GRID_DICH_X, GRID_Y, 10*CELL_SIZE, 10*CELL_SIZE); }).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g; 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            veLưới2D(g2, GRID_MINH_X, "📡 HẠM ĐỘI QUÂN TA (PHÒNG THỦ)", COLOR_CYAN, btnMinh, false);
            veLưới2D(g2, GRID_DICH_X, "🎯 TRẬN ĐỊA ĐỐI ĐỊCH (DÒ TÌM)", COLOR_GOLD, btnDich, true);
        }

        private void veLưới2D(Graphics2D g2, int startX, String tieuDe, Color mauChuDao, OBanCo[][] maTranO, boolean veRadar) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15)); g2.setColor(mauChuDao);
            g2.drawString(tieuDe, startX, GRID_Y - 40);

            // Nền lưới tối hơi xanh, bóng kính mờ
            g2.setColor(new Color(5, 10, 20, 190));
            g2.fillRect(startX, GRID_Y, 10 * CELL_SIZE, 10 * CELL_SIZE);

            // Hiệu ứng Hover lấp lánh (Bloom/Glow)
            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    int ox = startX + c * CELL_SIZE;
                    int oy = GRID_Y + r * CELL_SIZE;
                    OBanCo o = maTranO[r][c];
                    if (o.isHovered) { veHoverGlow(g2, ox, oy, mauChuDao); }
                    else if (o.isShowShip) { g2.setColor(new Color(mauChuDao.getRed(), mauChuDao.getGreen(), mauChuDao.getBlue(), 30)); g2.fillRect(ox, oy, CELL_SIZE, CELL_SIZE); }
                }
            }

            // Vẽ Tia Radar quét xoay (Chỉ lưới địch - Tạo vẻ sống động dò tìm)
            if (veRadar) { veRadarSweep(g2, startX); }

            // Vẽ Tàu chiến
            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    OBanCo o = maTranO[r][c];
                    if (o.isShowShip && o.shipImg != null && o.shipSeg == 0) { veTau(g2, startX, r, c, o); }
                }
            }
            
            // Vẽ điểm TRÚNG rực rỡ / HỤT lấp lánh
            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    int ox = startX + c * CELL_SIZE;
                    int oy = GRID_Y + r * CELL_SIZE;
                    OBanCo o = maTranO[r][c];
                    if (o.isHit) { veHit(g2, ox, oy); }
                    if (o.isMiss) { veMiss(g2, ox, oy); }
                }
            }

            // Vẽ Lưới Hologram lấp lánh (Glowing Grid)
            veGlowingGrid(g2, startX, mauChuDao);

            // Tọa độ viền ký tự
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13)); g2.setColor(TEXT_MUTED);
            String[] chuCai = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
            for (int c = 0; c < 10; c++) { g2.drawString(chuCai[c], startX + c * CELL_SIZE + CELL_SIZE/2 - 4, GRID_Y - 10); }
            for (int r = 0; r < 10; r++) { g2.drawString(String.format("%02d", r + 1), startX - 25, GRID_Y + r * CELL_SIZE + CELL_SIZE/2 + 5); }
        }

        // --- Các hàm bổ trợ vẽ chi tiết sinh động ---
        private void veHoverGlow(Graphics2D g2, int ox, int oy, Color c) {
            g2.setPaint(new LinearGradientPaint(ox, oy, ox, oy + CELL_SIZE,
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), 0),
                            new Color(c.getRed(), c.getGreen(), c.getBlue(), 120),
                            new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
            g2.fillRect(ox, oy, CELL_SIZE, CELL_SIZE);
        }
        private void veRadarSweep(Graphics2D g2, int startX) {
            int cx = startX + 5 * CELL_SIZE; int cy = GRID_Y + 5 * CELL_SIZE;
            int r = 5 * CELL_SIZE;
            g2.setClip(new Rectangle(startX, GRID_Y, 10*CELL_SIZE, 10*CELL_SIZE));
            // Tia quét chính
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), r, new float[]{0.8f, 1f},
                new Color[]{new Color(COLOR_GOLD.getRed(), COLOR_GOLD.getGreen(), COLOR_GOLD.getBlue(), 0),
                            new Color(COLOR_GOLD.getRed(), COLOR_GOLD.getGreen(), COLOR_GOLD.getBlue(), 100)}));
            g2.fillArc(cx-r, cy-r, r*2, r*2, (int)radarAngle, 15);
            g2.setClip(null);
        }
        private void veTau(Graphics2D g2, int startX, int r, int c, OBanCo o) {
            int sx = startX + c * CELL_SIZE; int sy = GRID_Y + r * CELL_SIZE;
            Graphics2D gTau = (Graphics2D) g2.create();
            gTau.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (!o.isShipNgang) {
                double cx = sx + CELL_SIZE / 2.0; double cy = sy + (o.shipLen * CELL_SIZE) / 2.0;
                gTau.rotate(Math.toRadians(90), cx, cy);
                gTau.drawImage(o.shipImg, (int)(cx - (o.shipLen * CELL_SIZE)/2.0), (int)(cy - CELL_SIZE/2.0), o.shipLen * CELL_SIZE, CELL_SIZE, null);
            } else { gTau.drawImage(o.shipImg, sx, sy, o.shipLen * CELL_SIZE, CELL_SIZE, null); }
            gTau.dispose();
        }
        private void veHit(Graphics2D g2, int ox, int oy) {
            // Nền đỏ phát quang
            RadialGradientPaint rgp = new RadialGradientPaint(new Point2D.Float(ox+CELL_SIZE/2f, oy+CELL_SIZE/2f), CELL_SIZE/2f,
                new float[]{0f, 1f}, new Color[]{COLOR_RED, new Color(COLOR_RED.getRed(),0,0,0)});
            g2.setPaint(rgp); g2.fillRect(ox, oy, CELL_SIZE, CELL_SIZE);
            if (imgHit != null) { g2.drawImage(imgHit, ox, oy, CELL_SIZE, CELL_SIZE, null); }
            else { g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(3f)); g2.drawLine(ox+10, oy+10, ox+CELL_SIZE-10, oy+CELL_SIZE-10); g2.drawLine(ox+CELL_SIZE-10, oy+10, ox+10, oy+CELL_SIZE-10); }
        }
        private void veMiss(Graphics2D g2, int ox, int oy) {
            // Điểm xanh lấp lánh như nước văng
            g2.setColor(new Color(0, 240, 255, 200));
            g2.fillOval(ox + CELL_SIZE/2 - 6, oy + CELL_SIZE/2 - 6, 12, 12);
            g2.setColor(Color.WHITE); g2.fillOval(ox + CELL_SIZE/2 - 3, oy + CELL_SIZE/2 - 3, 6, 6);
        }
        private void veGlowingGrid(Graphics2D g2, int startX, Color c) {
            g2.setStroke(new BasicStroke(1.2f));
            // Viền ngoài rực rỡ
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
            g2.drawRect(startX, GRID_Y, 10 * CELL_SIZE, 10 * CELL_SIZE);
            // Lưới trong mờ hơn, lấp lánh (Glow effect dùng Alpha chéo)
            for (int i = 1; i < 10; i++) {
                int l = startX + i * CELL_SIZE; int t = GRID_Y + i * CELL_SIZE;
                veGlowingLine(g2, l, GRID_Y, l, GRID_Y + 10 * CELL_SIZE, c);
                veGlowingLine(g2, startX, t, startX + 10 * CELL_SIZE, t, c);
            }
        }
        private void veGlowingLine(Graphics2D g2, int x1, int y1, int x2, int y2, Color c) {
            // Vẽ 2 lớp: lớp dưới nhạt rộng (glow), lớp trên đậm gốc
            g2.setStroke(new BasicStroke(2.5f)); g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 25)); g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(new BasicStroke(1.0f)); g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70)); g2.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 3. NÚT BẤM NEON GLOSSY ĐỘNG - TỰ PHẢN CHIẾU VÀ HOVER PHÁT QUANG
     */
    private JButton taoNutBam(String text, Color mauNeon) {
        JButton btn = new JButton(text.toUpperCase()) {
            private float glowAlpha = 0f; private Timer timer;
            {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { startGlow(true); }
                    @Override public void mouseExited(MouseEvent e) { startGlow(false); }
                    private void startGlow(boolean in) {
                        if (timer != null && timer.isRunning()) timer.stop();
                        timer = new Timer(15, act -> { glowAlpha += in ? 0.2f : -0.2f; if(glowAlpha>=1f){glowAlpha=1f; timer.stop();} else if(glowAlpha<=0f){glowAlpha=0f; timer.stop();} repaint(); });
                        timer.start();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Nền đậm, viền Neon
                g2.setColor(new Color(10, 15, 30, 230)); g2.fillRoundRect(0, 0, w, h, 14, 14);
                // Glow khi hover
                if (glowAlpha > 0) { veGlow(g2, w, h, glowAlpha); }
                // Viền Neon bóng
                veBorder(g2, w, h);
                // Text viền chìm
                veText(g2, w, h);
                g2.dispose();
            }
            private void veGlow(Graphics2D g2, int w, int h, float alpha) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.7f));
                LinearGradientPaint p = new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 1f}, new Color[]{mauNeon, new Color(0,0,0,0)});
                g2.setPaint(p); g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setComposite(AlphaComposite.SrcOver);
            }
            private void veBorder(Graphics2D g2, int w, int h) {
                g2.setStroke(new BasicStroke(1.5f));
                LinearGradientPaint p = new LinearGradientPaint(0, 0, w, h, new float[]{0f, 0.5f, 1f}, new Color[]{Color.WHITE, mauNeon, new Color(0,0,0,0)});
                g2.setPaint(p); g2.drawRoundRect(1, 1, w-3, h-3, 14, 14);
                // Lớp gloss kính
                g2.setPaint(new LinearGradientPaint(0,0,0,h/2f, new float[]{0f,1f}, new Color[]{new Color(255,255,255,50), new Color(255,255,255,0)}));
                g2.fillRoundRect(2, 2, w-4, h/2, 12, 12);
            }
            private void veText(Graphics2D g2, int w, int h) {
                g2.setFont(getFont()); FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2; int ty = (h + fm.getAscent()) / 2 - 2;
                g2.setColor(new Color(0,0,0,150)); g2.drawString(getText(), tx+1, ty+1); // Bóng text
                g2.setColor(Color.WHITE); g2.drawString(getText(), tx, ty);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13)); btn.setContentAreaFilled(false); btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25)); return btn;
    }

    /**
     * 4. NÂNG CẤP CAO CẤP: KHUNG KÍNH MỜ GLASSMORPHISM "THỞ" (PULSING GLOW BORDER) VÀ SCANLINES
     */
    private JPanel taoKhungTrongSuot(String tieuDe, JComponent tPhan) {
        JPanel pnl = new JPanel(new BorderLayout()) {
            private float borderAlpha = 0.5f; private boolean alphaUp = true;
            {   // Timer làm viền phát quang nhấp nháy mượt mà (Tạo cảm giác "thở")
                new Timer(60, e -> {
                    if (alphaUp) { borderAlpha += 0.03f; if(borderAlpha>=0.9f){borderAlpha=0.9f; alphaUp=false;} }
                    else { borderAlpha -= 0.03f; if(borderAlpha<=0.4f){borderAlpha=0.4f; alphaUp=true;} }
                    repaint();
                }).start();
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Thân kính mờ, đổ bóng tầng nền
                g2.setColor(new Color(0, 0, 0, 100)); g2.fillRoundRect(3, 5, w-6, h-8, 18, 18);
                g2.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 15), new Color(255, 255, 255, 5)}));
                g2.fillRoundRect(0, 0, w, h, 18, 18);
                // Lớp Scanlines Hologram tĩnh (Kẻ ngang mờ)
                veScanlines(g2, w, h);
                // Viền phát quang động (Pulsing Glow Border)
                g2.setStroke(new BasicStroke(1.3f));
                veGlowingBorder(g2, w, h, borderAlpha);
                g2.dispose();
            }
            private void veScanlines(Graphics2D g2, int w, int h) {
                g2.setColor(new Color(255, 255, 255, 8));
                g2.setStroke(new BasicStroke(0.5f));
                for(int y=0; y<h; y+=3){ g2.drawLine(5, y, w-5, y); }
            }
            private void veGlowingBorder(Graphics2D g2, int w, int h, float alpha) {
                // Viền bóng
                g2.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 0.5f, 1f}, 
                    new Color[]{new Color(255, 255, 255, (int)(alpha*200)), new Color(0, 240, 255, (int)(alpha*80)), new Color(0, 0, 0, 0)}));
                g2.drawRoundRect(0, 0, w-1, h-1, 18, 18);
                // Lớp kính bóng bề mặt
                g2.setPaint(new LinearGradientPaint(0,0,0,h/2f, new float[]{0f,1f}, new Color[]{new Color(255,255,255,30), new Color(255,255,255,0)}));
                g2.fillRoundRect(2, 2, w-4, h/2, 16, 16);
            }
        };
        pnl.setOpaque(false); pnl.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        JLabel lbl = new JLabel("📡 " + tieuDe.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(COLOR_CYAN);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        pnl.add(lbl, BorderLayout.NORTH); pnl.add(tPhan, BorderLayout.CENTER);
        return pnl;
    }

    private JPanel taoManHinhLobby() {
        JPanel lobby = new JPanel(new GridBagLayout());
        lobby.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("BATTLESHIP TACTICAL FLAT", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 46)); title.setForeground(Color.WHITE);
        lobby.add(title, gbc);

        gbc.gridy = 1;
        JLabel sub = new JLabel("HỆ THỐNG ĐIỀU HÀNH CHIẾN TRƯỜNG SONG SONG 2D", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14)); sub.setForeground(COLOR_CYAN);
        lobby.add(sub, gbc);

        gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(40, 10, 10, 10);
        JButton host = taoNutBam("Khởi Tạo Phòng Chỉ Huy (HOST)", COLOR_CYAN);
        host.setPreferredSize(new Dimension(380, 50)); host.addActionListener(e -> xuLyTaoPhong());
        lobby.add(host, gbc);

        gbc.gridy = 3; gbc.insets = new Insets(10, 10, 10, 10);
        JButton join = taoNutBam("Liên Kết Tọa Độ Phòng (JOIN)", new Color(40, 167, 69));
        join.setPreferredSize(new Dimension(380, 50)); join.addActionListener(e -> xuLyVaoPhong());
        lobby.add(join, gbc);

        gbc.gridy = 4; gbc.insets = new Insets(20, 10, 10, 10);
        JButton back = taoNutBam("Quay lại màn hình chính", COLOR_RED);
        back.addActionListener(e -> { 
            dangThoat = true; 
            ngatKetNoi(); 
            this.dispose(); 
            SwingUtilities.invokeLater(() -> { new ManHinhChinh().setVisible(true); });
        });
        lobby.add(back, gbc);
        return lobby;
    }

    private JPanel taoManHinhGame() {
        JPanel game = new JPanel(new BorderLayout());
        game.setOpaque(false);
        game.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        pnlBattlefield = new BattlefieldPanel();
        game.add(pnlBattlefield, BorderLayout.CENTER);

        JPanel pnlPhai = new JPanel(new GridLayout(2, 1, 0, 12));
        pnlPhai.setOpaque(false); pnlPhai.setPreferredSize(new Dimension(330, 0));
        pnlPhai.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 0));

        txtHeThong = new JTextArea(); txtHeThong.setEditable(false); txtHeThong.setLineWrap(true); txtHeThong.setWrapStyleWord(true);
        txtHeThong.setFont(new Font("Consolas", Font.PLAIN, 12)); // Font code cho radar
        txtHeThong.setBackground(new Color(5, 8, 15, 230)); 
        txtHeThong.setForeground(COLOR_GOLD); 
        JScrollPane scrollHeThong = new JScrollPane(txtHeThong); scrollHeThong.setBorder(BorderFactory.createLineBorder(new Color(40, 52, 74)));
        scrollHeThong.getViewport().setOpaque(false);
        JPanel pnlHeThongCore = new JPanel(new BorderLayout()); pnlHeThongCore.setOpaque(false);
        pnlHeThongCore.add(scrollHeThong, BorderLayout.CENTER);

        txtChat = new JTextArea(); txtChat.setEditable(false); txtChat.setLineWrap(true); txtChat.setWrapStyleWord(true);
        txtChat.setFont(new Font("Segoe UI", Font.PLAIN, 13)); txtChat.setBackground(new Color(5, 8, 15, 230)); 
        txtChat.setForeground(Color.WHITE); 
        JScrollPane scrollChat = new JScrollPane(txtChat); scrollChat.setBorder(BorderFactory.createLineBorder(new Color(40, 52, 74)));
        scrollChat.getViewport().setOpaque(false);

        JPanel pnlNhap = new JPanel(new BorderLayout(5, 0)); pnlNhap.setOpaque(false); pnlNhap.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        txtNhapChat = new JTextField(); txtNhapChat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNhapChat.setBackground(new Color(5, 8, 15, 230)); txtNhapChat.setForeground(Color.WHITE); txtNhapChat.setCaretColor(Color.WHITE);
        txtNhapChat.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(40, 52, 74)), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        txtNhapChat.addActionListener(e -> guiTinNhanChat());
        
        btnGuiChat = taoNutBam("Gửi", COLOR_CYAN);
        pnlNhap.add(txtNhapChat, BorderLayout.CENTER); pnlNhap.add(btnGuiChat, BorderLayout.EAST);

        JPanel pnlChatCore = new JPanel(new BorderLayout()); pnlChatCore.setOpaque(false);
        pnlChatCore.add(scrollChat, BorderLayout.CENTER); pnlChatCore.add(pnlNhap, BorderLayout.SOUTH);

        pnlPhai.add(taoKhungTrongSuot("Nhật ký radar chiến trường", pnlHeThongCore));
        pnlPhai.add(taoKhungTrongSuot("Kênh liên lạc mật", pnlChatCore));
        game.add(pnlPhai, BorderLayout.EAST);

        // Khung dưới Glassmorphism tự vẽ
        JPanel pnlDuoi = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(), h=getHeight();
                g2.setColor(new Color(0,0,0,100)); g2.fillRoundRect(3, 4, w-6, h-6, 16, 16); // Shadow
                g2.setPaint(new LinearGradientPaint(0,0,0,h, new float[]{0f,1f}, new Color[]{new Color(255,255,255,12), new Color(255,255,255,4)}));
                g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.setStroke(new BasicStroke(1.2f));
                g2.setPaint(new LinearGradientPaint(0,0,w,0, new float[]{0f,0.5f,1f}, new Color[]{COLOR_CYAN, new Color(0,0,0,0), COLOR_CYAN}));
                g2.drawRoundRect(0, 0, w-1, h-1, 16, 16);
                g2.dispose();
            }
        };
        pnlDuoi.setOpaque(false); pnlDuoi.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

     // Tích hợp cụm Trạng Thái & Nút Copy sang góc trái của thanh trạng thái dưới
        JPanel pnlTrangThaiCum = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlTrangThaiCum.setOpaque(false);

        lblTrangThai = new JLabel("HỆ THỐNG TRẠNG THÁI: ĐANG CHỜ KẾT NỐI ĐỒNG BỘ...");
        lblTrangThai.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblTrangThai.setForeground(Color.WHITE);
        
        // Tạo nút Copy nhanh cực gọn
        btnCopyMa = taoNutBam("Copy Mã", COLOR_CYAN);
        btnCopyMa.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnCopyMa.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        btnCopyMa.setVisible(false); // Ban đầu ẩn, chỉ hiện khi lấy được mã phòng
        btnCopyMa.addActionListener(e -> {
            if (!maPhongHienTai.isEmpty()) {
                try {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(maPhongHienTai);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                    txtHeThong.append("> Hệ thống: Đã sao chép mã phòng [" + maPhongHienTai + "] vào bộ nhớ máy!\n");
                } catch (Exception ex) {}
            }
        });

        pnlTrangThaiCum.add(lblTrangThai);
        pnlTrangThaiCum.add(btnCopyMa);
        pnlDuoi.add(pnlTrangThaiCum, BorderLayout.CENTER);

        JPanel pnlNutBam = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); pnlNutBam.setOpaque(false);
        btnXoayTau = taoNutBam("Xoay: Ngang", COLOR_CYAN);
        btnXoayTau.addActionListener(e -> { isNgang = !isNgang; btnXoayTau.setText(isNgang ? "Xoay: Ngang" : "Xoay: Dọc"); });
        btnXepNgauNhien = taoNutBam("Xếp Tự Động", COLOR_CYAN);
        btnXepNgauNhien.addActionListener(e -> {
            if (!daKetNoi) return; khoiTaoBanCoBanDau(); daKetNoi = true; isDangXepTau = true; 
            Random rand = new Random();
            while (tauHienTai < hamDoi.length) { isNgang = rand.nextBoolean(); xuLyDatTau(rand.nextInt(10), rand.nextInt(10), false); }
        });
        btnSanSang = taoNutBam("Xác Nhận Đội Hình", COLOR_GOLD); btnSanSang.addActionListener(e -> xacNhanSanSang());
        btnThoatGame = taoNutBam("Ngắt Kết Nối", COLOR_RED);
        btnThoatGame.addActionListener(e -> { 
            dangThoat = true; ngatKetNoi(); khoiTaoBanCoBanDau(); 
            txtChat.setText(""); txtHeThong.setText(""); 
            lblTrangThai.setText("HỆ THỐNG TRẠNG THÁI: ĐANG CHỜ KẾT NỐI ĐỒNG BỘ...");
            lblTrangThai.setForeground(Color.WHITE); // Reset color
            cardLayout.show(pnlCards, "LOBBY"); 
        });

        pnlNutBam.add(btnXoayTau); pnlNutBam.add(btnXepNgauNhien); pnlNutBam.add(btnSanSang); pnlNutBam.add(btnThoatGame);
        pnlDuoi.add(pnlNutBam, BorderLayout.EAST);
        
        JPanel bottomWrap = new JPanel(new BorderLayout()); bottomWrap.setOpaque(false);
        bottomWrap.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bottomWrap.add(pnlDuoi, BorderLayout.CENTER); game.add(bottomWrap, BorderLayout.SOUTH);
        return game;
    }

    private void khoiTaoBanCoBanDau() {
        dataBanCoMinh = new BanCo(); dataBanCoDich = new BanCo(); isDangXepTau = false; daKetNoi = false; minhSanSang = false; doiPhuongSanSang = false; tauHienTai = 0; maPhongHienTai = "";
        for (int r = 0; r < 10; r++) { for (int c = 0; c < 10; c++) { btnMinh[r][c] = new OBanCo(r, c, false); btnDich[r][c] = new OBanCo(r, c, true); } }
        if (btnXoayTau != null) { btnXoayTau.setEnabled(false); btnXepNgauNhien.setEnabled(false); btnSanSang.setEnabled(false); btnCopyMa.setVisible(false); }
        if (pnlBattlefield != null) pnlBattlefield.repaint();
    }

    private String layIPMay() { try { return InetAddress.getLocalHost().getHostAddress(); } catch (Exception e) { return "127.0.0.1"; } }
    private String maHoaCode(String ip, int port) { try { String[] parts = ip.split("\\."); StringBuilder sb = new StringBuilder(); for (String p : parts) { String h = Integer.toHexString(Integer.parseInt(p)).toUpperCase(); sb.append(h.length() == 1 ? "0" + h : h); } String pHex = Integer.toHexString(port).toUpperCase(); while (pHex.length() < 4) pHex = "0" + pHex; sb.append(pHex); return sb.toString(); } catch (Exception e) { return "ERROR"; } }
    private String[] giaiMaCode(String c) { try { if (c.length() != 12) return null; StringBuilder ip = new StringBuilder(); for (int i = 0; i < 8; i += 2) { ip.append(Integer.parseInt(c.substring(i, i + 2), 16)); if (i < 6) ip.append("."); } int p = Integer.parseInt(c.substring(8, 12), 16); return new String[]{ip.toString(), String.valueOf(p)}; } catch (Exception e) { return null; } }

    private void xuLyTaoPhong() {
        cardLayout.show(pnlCards, "GAME"); isHost = true; isMyTurn = true; dangThoat = false; txtChat.setText(""); txtHeThong.setText(""); 
        lblTrangThai.setText("Đang mở cổng kết nối trạm chủ...");
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(0); int port = serverSocket.getLocalPort(); String roomCode = maHoaCode(layIPMay(), port);
                
                // [THÊM] Xử lý lưu mã phòng và tự động nạp vào bộ nhớ máy (Clipboard)
                maPhongHienTai = roomCode;
                try {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(roomCode);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                } catch (Exception ex) {}

                // [THÊM] Hiển thị mã phòng và kích hoạt nút Copy lên giao diện
                SwingUtilities.invokeLater(() -> { 
                    lblTrangThai.setText("MÃ PHÒNG CHIẾN TRƯỜNG: " + roomCode ); 
                    btnCopyMa.setVisible(true);
                    txtHeThong.append("> Hệ thống: Đã tự động sao chép mã phòng vào clipboard máy tính.\n");
                });

                socket = serverSocket.accept(); thietLapLuotDocGhi();
                SwingUtilities.invokeLater(() -> { daKetNoi = true; isDangXepTau = true; btnXoayTau.setEnabled(true); btnXepNgauNhien.setEnabled(true); lblTrangThai.setText("Đã kết nối! Vui lòng đặt: " + hamDoi[0].getTen()); txtHeThong.append("[HỆ THỐNG] Đối thủ chiến lược đã vào phòng.\n"); pnlBattlefield.repaint(); });
            } catch (Exception e) { if (!dangThoat) { SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "Khởi động phòng thất bại!"); cardLayout.show(pnlCards, "LOBBY"); }); } }
        }).start();
    }

    private void xuLyVaoPhong() {
        String code = JOptionPane.showInputDialog(this, "Nhập mã phòng chiến trường (12 ký tự):"); if (code == null || code.trim().isEmpty()) return;
        String[] info = giaiMaCode(code.trim().toUpperCase()); if (info == null) { JOptionPane.showMessageDialog(this, "Định dạng mã phòng lỗi!"); return; }
        cardLayout.show(pnlCards, "GAME"); isHost = false; isMyTurn = false; dangThoat = false; txtChat.setText(""); txtHeThong.setText("");
        lblTrangThai.setText("Đang đồng bộ tín hiệu đến phòng chủ...");
        new Thread(() -> {
            try {
                socket = new Socket(info[0], Integer.parseInt(info[1])); thietLapLuotDocGhi();
                SwingUtilities.invokeLater(() -> { daKetNoi = true; isDangXepTau = true; btnXoayTau.setEnabled(true); btnXepNgauNhien.setEnabled(true); lblTrangThai.setText("Đã liên kết thành công! Vui lòng đặt: " + hamDoi[0].getTen()); txtHeThong.append("[HỆ THỐNG] Đã liên kết đường truyền mạng thành công.\n"); pnlBattlefield.repaint(); });
            } catch (Exception e) { if (!dangThoat) { SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "Kết nối phòng thất bại hoặc phòng không tồn tại!"); cardLayout.show(pnlCards, "LOBBY"); }); } }
        }).start();
    }

    private void thietLapLuotDocGhi() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(() -> {
                try { String line; while ((line = in.readLine()) != null) { final String cmd = line; SwingUtilities.invokeLater(() -> xuLyLenhMang(cmd)); } }
                catch (Exception e) { if (!dangThoat) { SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "Đường truyền mạng bị gián đoạn!"); btnThoatGame.doClick(); }); } }
            }).start();
        } catch (Exception e) {}
    }

    private void xuLyLenhMang(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return;
        if (cmd.startsWith("CHAT:")) { txtChat.append("[ĐỐI THỦ] " + cmd.substring(5) + "\n"); } 
        else if (cmd.equals("READY")) { txtHeThong.append("[HỆ THỐNG] Đối phương đã xếp trận địa hoàn tất.\n"); if (minhSanSang) batDauTranDau(); } 
        else if (cmd.startsWith("FIRE:")) {
            String[] parts = cmd.split(":"); int r = Integer.parseInt(parts[1]); int c = Integer.parseInt(parts[2]);
            if (btnMinh[r][c].isShowShip) {
                btnMinh[r][c].isHit = true; phatAmThanh("hit.wav"); if (out != null) out.println("HIT:" + r + ":" + c);
                txtHeThong.append("[CẢNH BÁO] Hạm đội bị đánh trúng tại [" + chuCaiSo(c) + "," + (r+1) + "]!\n");
                isMyTurn = false; lblTrangThai.setText("ĐỐI PHƯƠNG ĐANG OANH TẠC... HÃY PHÒNG THỦ."); lblTrangThai.setForeground(COLOR_RED);
            } else {
                btnMinh[r][c].isMiss = true; phatAmThanh("miss.wav"); if (out != null) out.println("MISS:" + r + ":" + c);
                txtHeThong.append("[RA ĐA] Đối phương bắn hụt tại ô [" + chuCaiSo(c) + "," + (r+1) + "].\n");
                isMyTurn = true; lblTrangThai.setText("LƯỢT CỦA BẠN! HÃY KHAI HỎA."); lblTrangThai.setForeground(COLOR_CYAN);
            }
            kiemTraThuaCuoc(); pnlBattlefield.repaint();
        } 
        else if (cmd.startsWith("HIT:")) {
            phatAmThanh("hit.wav"); String[] parts = cmd.split(":"); int r = Integer.parseInt(parts[1]); int c = Integer.parseInt(parts[2]);
            btnDich[r][c].isHit = true; txtHeThong.append("[HỆ THỐNG] TRÚNG MỤC TIÊU tại [" + chuCaiSo(c) + "," + (r+1) + "]!\n");
            isMyTurn = true; lblTrangThai.setText("BẮN TRÚNG MỤC TIÊU! TIẾP TỤC KHAI HỎA."); lblTrangThai.setForeground(COLOR_CYAN);
            kiemTraThangCuoc(); pnlBattlefield.repaint();
        } 
        else if (cmd.startsWith("MISS:")) {
            phatAmThanh("miss.wav"); String[] parts = cmd.split(":"); int r = Integer.parseInt(parts[1]); int c = Integer.parseInt(parts[2]);
            btnDich[r][c].isMiss = true; txtHeThong.append("[HỆ THỐNG] Đạn lạc rơi xuống biển tại [" + chuCaiSo(c) + "," + (r+1) + "].\n");
            isMyTurn = false; lblTrangThai.setText("ĐỐI PHƯƠNG ĐANG OANH TẠC... HÃY PHÒNG THỦ."); lblTrangThai.setForeground(COLOR_RED);
            pnlBattlefield.repaint();
        }
    }

    private String chuCaiSo(int c) { String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"}; return (c >= 0 && c < 10) ? letters[c] : ""; }

    private void xuLyDatTau(int r, int c, boolean clickTay) {
        if (tauHienTai >= hamDoi.length) return;
        int size = (tauHienTai == 0) ? 5 : (tauHienTai == 1) ? 4 : (tauHienTai == 2 || tauHienTai == 3) ? 3 : 2;
        if (isNgang) {
            if (c + size > 10) { if (clickTay) lblTrangThai.setText("❌ KHÔNG ĐỦ KHÔNG GIAN HÀNG NGANG!"); return; }
            for (int i = 0; i < size; i++) { if (btnMinh[r][c + i].isShowShip) { if (clickTay) lblTrangThai.setText("❌ VỊ TRÍ ĐÃ CÓ TÀU AN TOÀN!"); return; } }
            Image img = (size == 5) ? imgTau5 : (size == 4) ? imgTau4 : (size == 3) ? imgTau3 : imgTau2;
            for (int i = 0; i < size; i++) { btnMinh[r][c + i].setShipInfo(img, size, i, true); }
        } else {
            if (r + size > 10) { if (clickTay) lblTrangThai.setText("❌ KHÔNG ĐỦ KHÔNG GIAN HÀNG DỌC!"); return; }
            for (int i = 0; i < size; i++) { if (btnMinh[r + i][c].isShowShip) { if (clickTay) lblTrangThai.setText("❌ VỊ TRÍ ĐÃ CÓ TÀU AN TOÀN!"); return; } }
            Image img = (size == 5) ? imgTau5 : (size == 4) ? imgTau4 : (size == 3) ? imgTau3 : imgTau2;
            for (int i = 0; i < size; i++) { btnMinh[r + i][c].setShipInfo(img, size, i, false); }
        }
        tauHienTai++;
        if (tauHienTai < hamDoi.length) { lblTrangThai.setText("Vui lòng đặt: " + hamDoi[tauHienTai].getTen()); } 
        else {
            lblTrangThai.setText("⚡ HẠM ĐỘI HOÀT TẤT. ẤN [XÁC NHẬN ĐỘI HÌNH] ĐỂ SẴN SÀNG TRANH ĐẤU.");
            btnSanSang.setEnabled(true); btnXoayTau.setEnabled(false); btnXepNgauNhien.setEnabled(false); isDangXepTau = false;
        }
        pnlBattlefield.repaint();
    }

    private void guiLenhBan(int r, int c) {
        if (!isMyTurn || isDangXepTau || !daKetNoi) return;
        if (out != null) { out.println("FIRE:" + r + ":" + c); isMyTurn = false; lblTrangThai.setText("TÊN LỬA ĐANG PHÓNG... ĐANG CHỜ PHẢN HỒI KẾT QUẢ RADAR..."); }
    }

    private void xacNhanSanSang() {
        minhSanSang = true; btnSanSang.setEnabled(false); 
        lblTrangThai.setText("ĐÃ KHÓA TRẬN ĐỊA. ĐANG ĐỢI PHÍA ĐỐI THỦ KHỔI ĐỘNG XONG...");
        if (out != null) out.println("READY"); if (doiPhuongSanSang) batDauTranDau();
    }

    private void batDauTranDau() {
        isDangXepTau = false; txtHeThong.append("[HỆ THỐNG] Kết nối thành công! Trận đấu chính thức bắt đầu.\n");
        if (isHost) { isMyTurn = true; lblTrangThai.setText("LƯỢT CỦA BẠN! CHỌN TỌA ĐỘ TRÊN LƯỚI ĐỊCH ĐỂ KHAI HỎA."); lblTrangThai.setForeground(COLOR_CYAN); } 
        else { isMyTurn = false; lblTrangThai.setText("ĐỐI PHƯƠNG ĐANG OANH TẠC... HÃY PHÒNG THỦ."); lblTrangThai.setForeground(COLOR_RED); }
    }

    private void guiTinNhanChat() {
        String msg = txtNhapChat.getText().trim(); if (msg.isEmpty()) return;
        if (out != null) out.println("CHAT:" + msg); txtChat.append("[BẠN] " + msg + "\n"); txtNhapChat.setText("");
    }

    private void ngatKetNoi() {
        try { if (out != null) out.close(); if (in != null) in.close(); if (socket != null && !socket.isClosed()) socket.close(); if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); } catch (Exception e) {}
        daKetNoi = false;
    }

    private void kiemTraThuaCuoc() {
        boolean conTau = false;
        for (int r = 0; r < 10; r++) { for (int c = 0; c < 10; c++) { if (btnMinh[r][c].isShowShip && !btnMinh[r][c].isHit) { conTau = true; break; } } }
        if (!conTau) { JOptionPane.showMessageDialog(this, "Toàn bộ hạm đội của bạn đã bị phá hủy hoàn toàn! Bạn thua cuộc.", "TRẬN ĐẤU KẾT THÚC", JOptionPane.ERROR_MESSAGE); btnThoatGame.doClick(); }
    }

    private void kiemTraThangCuoc() {
        int tongSoONo = 0;
        for (int r = 0; r < 10; r++) { for (int c = 0; c < 10; c++) { if (btnDich[r][c].isHit) tongSoONo++; } }
        if (tongSoONo == 17) { JOptionPane.showMessageDialog(this, "Chiến thắng vang dội! Bạn đã hạ gục toàn bộ căn cứ của đối phương!", "CHIẾN THẮNG QUANG VINH", JOptionPane.INFORMATION_MESSAGE); btnThoatGame.doClick(); }
    }

    private void khoiTaoImages() {
        try {
            imgHit = new ImageIcon(getClass().getResource("/images/hit.png")).getImage(); 
            imgMiss = new ImageIcon(getClass().getResource("/images/miss.png")).getImage();
            imgTau5 = new ImageIcon(getClass().getResource("/images/tau_5.png")).getImage(); 
            imgTau4 = new ImageIcon(getClass().getResource("/images/tau_4.png")).getImage();
            imgTau3 = new ImageIcon(getClass().getResource("/images/tau_3.png")).getImage(); 
            imgTau2 = new ImageIcon(getClass().getResource("/images/tau_2.png")).getImage();
        } catch (Exception e) { }
    }
}