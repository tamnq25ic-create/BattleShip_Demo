package views;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;          
import java.net.*;         
import java.util.Random;
import javax.sound.sampled.*; 
import javax.swing.*;
import models.BanCo;
import models.TauChien;

public class ManHinhGameOnline extends JFrame {

    // --- HỆ MÀU CYBERPUNK ---
    private static final Color BG_DARK_VOID = new Color(2, 4, 10);
    private static final Color COLOR_CYAN = new Color(0, 240, 255);
    private static final Color COLOR_GOLD = new Color(255, 190, 0);
    private static final Color COLOR_RED = new Color(255, 30, 80);
    private static final Color TEXT_MUTED = new Color(130, 150, 175);
    
    // Tối ưu: Khởi tạo sẵn các Brush và Overlay dùng nhiều lần để tránh tràn RAM
    private static final Color PANEL_OVERLAY = new Color(10, 15, 25, 40);
    private static final Color GRID_BG = new Color(5, 10, 20, 190);
    private static final Color HIT_MISS_GLOW = new Color(0, 240, 255, 200);
    private static final BasicStroke STROKE_1 = new BasicStroke(1.0f);
    private static final BasicStroke STROKE_1_2 = new BasicStroke(1.2f);
    private static final BasicStroke STROKE_1_5 = new BasicStroke(1.5f);
    private static final BasicStroke STROKE_2_5 = new BasicStroke(2.5f);
    private static final BasicStroke STROKE_3 = new BasicStroke(3.0f);

    private static final int CELL_SIZE = 45;       
    private static final int GRID_Y = 160;         
    private static final int GRID_MINH_X = 80;     
    private static final int GRID_DICH_X = 640;    

    private BanCo dataBanCoMinh, dataBanCoDich;
    private OBanCo[][] btnMinh = new OBanCo[10][10];
    private OBanCo[][] btnDich = new OBanCo[10][10];
    
    private boolean isDangXepTau = false, isMyTurn = false, isNgang = true;      
    private int tauHienTai = 0;          

    // --- SOCKET & MẠNG ---
    private boolean isHost = false, daKetNoi = false, doiPhuongSanSang = false, minhSanSang = false, dangThoat = false;
    private Socket socket;             
    private ServerSocket serverSocket; 
    private PrintWriter out;           
    private BufferedReader in;         
    private String maPhongHienTai = "";

    // --- GIAO DIỆN ---
    private JButton btnCopyMa, btnXoayTau, btnXepNgauNhien, btnSanSang, btnThoatGame, btnGuiChat;
    private JTextArea txtChat, txtHeThong; 
    private JTextField txtNhapChat;
    private JLabel lblTrangThai;
    
    private Image imgHit, imgMiss, imgTau5, imgTau4, imgTau3, imgTau2, bgBattle;
    private BattlefieldPanel pnlBattlefield;
    private CardLayout cardLayout;
    private StarryBackgroundPanel pnlCards; 
    
    private TauChien[] hamDoi = {
        new TauChien("Siêu Hạm Chỉ Huy", 5), new TauChien("Tuần Dương Hạm Lớp Alpha", 4),
        new TauChien("Khinh Hạm Tấn Công I", 3), new TauChien("Khinh Hạm Tấn Attack II", 3),
        new TauChien("Tàu Ngầm Trinh Sát", 2)
    };

    private float statusPulseAlpha = 1.0f;
    private boolean statusPulseUp = false;

    public ManHinhGameOnline() {
        setTitle("BATTLESHIP TACTICAL COMMAND center v2.0 - LIVE INTERFACE");
        setSize(1450, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        khoiTaoImages();
        khoiTaoBanCoBanDau(); 

        cardLayout = new CardLayout();
        pnlCards = new StarryBackgroundPanel(cardLayout);
        pnlCards.add(taoManHinhLobby(), "LOBBY");
        pnlCards.add(taoManHinhGame(), "GAME");

        setLayout(new BorderLayout());
        add(pnlCards, BorderLayout.CENTER);
        cardLayout.show(pnlCards, "LOBBY");
        
        // Timer nhấp nháy trạng thái hệ thống
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
                    clip.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP) clip.close(); });
                    clip.open(audioIn);
                    clip.start();
                }
            } catch (Exception e) { }
        }).start();
    }

    private void ghiLogHeThong(String msg) {
        if (txtHeThong != null) {
            txtHeThong.append("[RADAR] " + msg + "\n");
            txtHeThong.setCaretPosition(txtHeThong.getDocument().getLength());
        }
    }

    // --- CÁC CLASS UI TÙY CHỈNH ---

    class StarryBackgroundPanel extends JPanel {
        public StarryBackgroundPanel(LayoutManager layout) { super(layout); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            if (bgBattle != null) {
                g2.drawImage(bgBattle, 0, 0, getWidth(), getHeight(), this);
                g2.setColor(PANEL_OVERLAY); g2.fillRect(0, 0, getWidth(), getHeight()); 
            } else {
                g2.setColor(BG_DARK_VOID); g2.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    class OBanCo {
        int r, c, shipLen = 1, shipSeg = 0;
        boolean isEnemy, isShipNgang = true, isShowShip = false, isMiss = false, isHit = false, isHovered = false;
        Image shipImg = null; 
        public OBanCo(int r, int c, boolean isEnemy) { this.r = r; this.c = c; this.isEnemy = isEnemy; }
        public void setShipInfo(Image img, int len, int seg, boolean ngang) { this.shipImg = img; this.shipLen = len; this.shipSeg = seg; this.isShipNgang = ngang; this.isShowShip = true; }
        public void resetFull() { this.shipImg = null; this.isShowShip = false; this.isMiss = false; this.isHit = false; this.isHovered = false; }
    }

    class BattlefieldPanel extends JPanel {
        private float radarAngle = 0f; 

        public BattlefieldPanel() {
            setOpaque(false);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int x = e.getX(), y = e.getY();
                    resetHover(btnMinh); resetHover(btnDich);
                    if (isInside(x, y, GRID_MINH_X)) updateHover(x, y, GRID_MINH_X, btnMinh);
                    if (isInside(x, y, GRID_DICH_X)) updateHover(x, y, GRID_DICH_X, btnDich);
                }
                private void resetHover(OBanCo[][] grid) { for(int r=0; r<10; r++) for(int c=0; c<10; c++) if(grid[r][c].isHovered) { grid[r][c].isHovered=false; repaint(getBounds(grid, c, r)); } }
                private Rectangle getBounds(OBanCo[][] grid, int c, int r) { int sx = (grid == btnMinh) ? GRID_MINH_X : GRID_DICH_X; return new Rectangle(sx + c*CELL_SIZE, GRID_Y + r*CELL_SIZE, CELL_SIZE, CELL_SIZE); }
                private void updateHover(int x, int y, int startX, OBanCo[][] grid) {
                    int c = (x - startX) / CELL_SIZE, r = (y - GRID_Y) / CELL_SIZE;
                    if (r >= 0 && r < 10 && c >= 0 && c < 10) { grid[r][c].isHovered = true; repaint(getBounds(grid, c, r)); }
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    int x = e.getX(), y = e.getY();
                    if (isInside(x, y, GRID_MINH_X)) handleClick(x, y, GRID_MINH_X, true);
                    if (isInside(x, y, GRID_DICH_X)) handleClick(x, y, GRID_DICH_X, false);
                }
                private void handleClick(int x, int y, int startX, boolean isMinh) {
                    int c = (x - startX) / CELL_SIZE, r = (y - GRID_Y) / CELL_SIZE;
                    if (r < 0 || r >= 10 || c < 0 || c >= 10) return;
                    if (isMinh) { if (isDangXepTau && daKetNoi) xuLyDatTau(r, c, true); }
                    else { if (!isDangXepTau && !btnDich[r][c].isHit && !btnDich[r][c].isMiss) guiLenhBan(r, c); }
                }
            });

            new Timer(30, e -> { radarAngle += 2.5f; if(radarAngle>=360) radarAngle=0; repaint(GRID_DICH_X, GRID_Y, 10*CELL_SIZE, 10*CELL_SIZE); }).start();
        }
        private boolean isInside(int x, int y, int startX) { return x >= startX && x < startX + 10 * CELL_SIZE && y >= GRID_Y && y < GRID_Y + 10 * CELL_SIZE; }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g; 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            veLưới2D(g2, GRID_MINH_X, "📡 HẠM ĐỘI QUÂN TA (PHÒNG THỦ)", COLOR_CYAN, btnMinh, false);
            veLưới2D(g2, GRID_DICH_X, "🎯 TRẬN ĐỊA ĐỐI ĐỊCH (DÒ TÌM)", COLOR_GOLD, btnDich, true);
        }

        private void veLưới2D(Graphics2D g2, int startX, String tieuDe, Color mauChuDao, OBanCo[][] maTranO, boolean veRadar) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15)); g2.setColor(mauChuDao);
            g2.drawString(tieuDe, startX, GRID_Y - 40);

            g2.setColor(GRID_BG); g2.fillRect(startX, GRID_Y, 10 * CELL_SIZE, 10 * CELL_SIZE);

            if (veRadar) veRadarSweep(g2, startX);

            Color shipShadow = new Color(mauChuDao.getRed(), mauChuDao.getGreen(), mauChuDao.getBlue(), 30);
            
            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    int ox = startX + c * CELL_SIZE; int oy = GRID_Y + r * CELL_SIZE;
                    OBanCo o = maTranO[r][c];

                    if (o.isHovered) veHoverGlow(g2, ox, oy, mauChuDao);
                    else if (o.isShowShip && !veRadar) { g2.setColor(shipShadow); g2.fillRect(ox, oy, CELL_SIZE, CELL_SIZE); }

                    if (o.isShowShip && o.shipImg != null && o.shipSeg == 0) veTau(g2, startX, r, c, o);
                    if (o.isHit) veHit(g2, ox, oy);
                    if (o.isMiss) veMiss(g2, ox, oy);
                }
            }

            veGlowingGrid(g2, startX, mauChuDao);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 13)); g2.setColor(TEXT_MUTED);
            String[] chuCai = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
            for (int c = 0; c < 10; c++) g2.drawString(chuCai[c], startX + c * CELL_SIZE + CELL_SIZE/2 - 4, GRID_Y - 10);
            for (int r = 0; r < 10; r++) g2.drawString(String.format("%02d", r + 1), startX - 25, GRID_Y + r * CELL_SIZE + CELL_SIZE/2 + 5);
        }

        private void veHoverGlow(Graphics2D g2, int ox, int oy, Color c) {
            g2.setPaint(new LinearGradientPaint(ox, oy, ox, oy + CELL_SIZE, new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), 0), new Color(c.getRed(), c.getGreen(), c.getBlue(), 120), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
            g2.fillRect(ox, oy, CELL_SIZE, CELL_SIZE);
        }
        private void veRadarSweep(Graphics2D g2, int startX) {
            int cx = startX + 5 * CELL_SIZE, cy = GRID_Y + 5 * CELL_SIZE, r = 5 * CELL_SIZE;
            g2.setClip(new Rectangle(startX, GRID_Y, 10*CELL_SIZE, 10*CELL_SIZE));
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), r, new float[]{0.8f, 1f},
                new Color[]{new Color(COLOR_GOLD.getRed(), COLOR_GOLD.getGreen(), COLOR_GOLD.getBlue(), 0), new Color(COLOR_GOLD.getRed(), COLOR_GOLD.getGreen(), COLOR_GOLD.getBlue(), 100)}));
            g2.fillArc(cx-r, cy-r, r*2, r*2, (int)radarAngle, 15); g2.setClip(null);
        }
        private void veTau(Graphics2D g2, int startX, int r, int c, OBanCo o) {
            int sx = startX + c * CELL_SIZE, sy = GRID_Y + r * CELL_SIZE;
            Graphics2D gTau = (Graphics2D) g2.create();
            gTau.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (!o.isShipNgang) {
                double cx = sx + CELL_SIZE / 2.0, cy = sy + (o.shipLen * CELL_SIZE) / 2.0;
                gTau.rotate(Math.toRadians(90), cx, cy);
                gTau.drawImage(o.shipImg, (int)(cx - (o.shipLen * CELL_SIZE)/2.0), (int)(cy - CELL_SIZE/2.0), o.shipLen * CELL_SIZE, CELL_SIZE, null);
            } else gTau.drawImage(o.shipImg, sx, sy, o.shipLen * CELL_SIZE, CELL_SIZE, null);
            gTau.dispose();
        }
        private void veHit(Graphics2D g2, int ox, int oy) {
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(ox+CELL_SIZE/2f, oy+CELL_SIZE/2f), CELL_SIZE/2f, new float[]{0f, 1f}, new Color[]{COLOR_RED, new Color(COLOR_RED.getRed(),0,0,0)}));
            g2.fillRect(ox, oy, CELL_SIZE, CELL_SIZE);
            if (imgHit != null) g2.drawImage(imgHit, ox, oy, CELL_SIZE, CELL_SIZE, null);
            else { g2.setColor(Color.WHITE); g2.setStroke(STROKE_3); g2.drawLine(ox+10, oy+10, ox+CELL_SIZE-10, oy+CELL_SIZE-10); g2.drawLine(ox+CELL_SIZE-10, oy+10, ox+10, oy+CELL_SIZE-10); }
        }
        private void veMiss(Graphics2D g2, int ox, int oy) {
            g2.setColor(HIT_MISS_GLOW); g2.fillOval(ox + CELL_SIZE/2 - 6, oy + CELL_SIZE/2 - 6, 12, 12);
            g2.setColor(Color.WHITE); g2.fillOval(ox + CELL_SIZE/2 - 3, oy + CELL_SIZE/2 - 3, 6, 6);
        }
        private void veGlowingGrid(Graphics2D g2, int startX, Color c) {
            g2.setStroke(STROKE_1_2); g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150)); g2.drawRect(startX, GRID_Y, 10 * CELL_SIZE, 10 * CELL_SIZE);
            for (int i = 1; i < 10; i++) {
                int l = startX + i * CELL_SIZE, t = GRID_Y + i * CELL_SIZE;
                veGlowingLine(g2, l, GRID_Y, l, GRID_Y + 10 * CELL_SIZE, c);
                veGlowingLine(g2, startX, t, startX + 10 * CELL_SIZE, t, c);
            }
        }
        private void veGlowingLine(Graphics2D g2, int x1, int y1, int x2, int y2, Color c) {
            g2.setStroke(STROKE_2_5); g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 25)); g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(STROKE_1); g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70)); g2.drawLine(x1, y1, x2, y2);
        }
    }

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
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(10, 15, 30, 230)); g2.fillRoundRect(0, 0, w, h, 14, 14);
                if (glowAlpha > 0) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, glowAlpha * 0.7f));
                    g2.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 1f}, new Color[]{mauNeon, new Color(0,0,0,0)})); g2.fillRoundRect(0, 0, w, h, 14, 14);
                    g2.setComposite(AlphaComposite.SrcOver);
                }
                g2.setStroke(STROKE_1_5); g2.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 0.5f, 1f}, new Color[]{Color.WHITE, mauNeon, new Color(0,0,0,0)}));
                g2.drawRoundRect(1, 1, w-3, h-3, 14, 14);
                g2.setPaint(new LinearGradientPaint(0,0,0,h/2f, new float[]{0f,1f}, new Color[]{new Color(255,255,255,50), new Color(255,255,255,0)})); g2.fillRoundRect(2, 2, w-4, h/2, 12, 12);
                
                g2.setFont(getFont()); FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2, ty = (h + fm.getAscent()) / 2 - 2;
                g2.setColor(new Color(0,0,0,150)); g2.drawString(getText(), tx+1, ty+1); g2.setColor(Color.WHITE); g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13)); btn.setContentAreaFilled(false); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25)); return btn;
    }

    private JPanel taoKhungTrongSuot(String tieuDe, JComponent tPhan) {
        JPanel pnl = new JPanel(new BorderLayout()) {
            private float borderAlpha = 0.5f; private boolean alphaUp = true;
            { new Timer(60, e -> { if (alphaUp) { borderAlpha += 0.03f; if(borderAlpha>=0.9f){borderAlpha=0.9f; alphaUp=false;} } else { borderAlpha -= 0.03f; if(borderAlpha<=0.4f){borderAlpha=0.4f; alphaUp=true;} } repaint(); }).start(); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(0, 0, 0, 100)); g2.fillRoundRect(3, 5, w-6, h-8, 18, 18);
                g2.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 15), new Color(255, 255, 255, 5)})); g2.fillRoundRect(0, 0, w, h, 18, 18);
                g2.setColor(new Color(255, 255, 255, 8)); g2.setStroke(new BasicStroke(0.5f)); for(int y=0; y<h; y+=3) g2.drawLine(5, y, w-5, y);
                g2.setStroke(STROKE_1_2); g2.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 0.5f, 1f}, new Color[]{new Color(255, 255, 255, (int)(borderAlpha*200)), new Color(0, 240, 255, (int)(borderAlpha*80)), new Color(0, 0, 0, 0)}));
                g2.drawRoundRect(0, 0, w-1, h-1, 18, 18);
                g2.setPaint(new LinearGradientPaint(0,0,0,h/2f, new float[]{0f,1f}, new Color[]{new Color(255,255,255,30), new Color(255,255,255,0)})); g2.fillRoundRect(2, 2, w-4, h/2, 16, 16); g2.dispose();
            }
        };
        pnl.setOpaque(false); pnl.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        JLabel lbl = new JLabel("📡 " + tieuDe.toUpperCase()); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(COLOR_CYAN); lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        pnl.add(lbl, BorderLayout.NORTH); pnl.add(tPhan, BorderLayout.CENTER); return pnl;
    }

    // --- CÁC MÀN HÌNH CHÍNH ---

    private JPanel taoManHinhLobby() {
        JPanel lobby = new JPanel(new GridBagLayout()); lobby.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10, 10, 10, 10); gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("BATTLESHIP TACTICAL FLAT", SwingConstants.CENTER); title.setFont(new Font("Segoe UI", Font.BOLD, 46)); title.setForeground(Color.WHITE); lobby.add(title, gbc);
        gbc.gridy = 1; JLabel sub = new JLabel("HỆ THỐNG ĐIỀU HÀNH CHIẾN TRƯỜNG SONG SONG 2D", SwingConstants.CENTER); sub.setFont(new Font("Segoe UI", Font.PLAIN, 14)); sub.setForeground(COLOR_CYAN); lobby.add(sub, gbc);
        
        JButton host = taoNutBam("Khởi Tạo Phòng Chỉ Huy (HOST)", COLOR_CYAN); host.setPreferredSize(new Dimension(380, 50)); host.addActionListener(e -> xuLyTaoPhong()); gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(40, 10, 10, 10); lobby.add(host, gbc);
        JButton join = taoNutBam("Liên Kết Tọa Độ Phòng (JOIN)", new Color(40, 167, 69)); join.setPreferredSize(new Dimension(380, 50)); join.addActionListener(e -> xuLyVaoPhong()); gbc.gridy = 3; gbc.insets = new Insets(10, 10, 10, 10); lobby.add(join, gbc);
        JButton back = taoNutBam("Quay lại màn hình chính", COLOR_RED); back.addActionListener(e -> { dangThoat = true; ngatKetNoi(); this.dispose(); SwingUtilities.invokeLater(() -> new ManHinhChinh().setVisible(true)); }); gbc.gridy = 4; gbc.insets = new Insets(20, 10, 10, 10); lobby.add(back, gbc);
        return lobby;
    }

    private JPanel taoManHinhGame() {
        JPanel game = new JPanel(new BorderLayout()); game.setOpaque(false); game.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        pnlBattlefield = new BattlefieldPanel(); game.add(pnlBattlefield, BorderLayout.CENTER);

        JPanel pnlPhai = new JPanel(new GridLayout(2, 1, 0, 12)); pnlPhai.setOpaque(false); pnlPhai.setPreferredSize(new Dimension(330, 0)); pnlPhai.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 0));

        txtHeThong = new JTextArea(); txtHeThong.setEditable(false); txtHeThong.setLineWrap(true); txtHeThong.setWrapStyleWord(true); txtHeThong.setFont(new Font("Consolas", Font.PLAIN, 12)); txtHeThong.setBackground(new Color(5, 8, 15, 230)); txtHeThong.setForeground(COLOR_GOLD); 
        JScrollPane scrollHeThong = new JScrollPane(txtHeThong); scrollHeThong.setBorder(BorderFactory.createLineBorder(new Color(40, 52, 74))); scrollHeThong.getViewport().setOpaque(false);
        JPanel pnlHeThongCore = new JPanel(new BorderLayout()); pnlHeThongCore.setOpaque(false); pnlHeThongCore.add(scrollHeThong, BorderLayout.CENTER);

        txtChat = new JTextArea(); txtChat.setEditable(false); txtChat.setLineWrap(true); txtChat.setWrapStyleWord(true); txtChat.setFont(new Font("Segoe UI", Font.PLAIN, 13)); txtChat.setBackground(new Color(5, 8, 15, 230)); txtChat.setForeground(Color.WHITE); 
        JScrollPane scrollChat = new JScrollPane(txtChat); scrollChat.setBorder(BorderFactory.createLineBorder(new Color(40, 52, 74))); scrollChat.getViewport().setOpaque(false);

        JPanel pnlNhap = new JPanel(new BorderLayout(5, 0)); pnlNhap.setOpaque(false); pnlNhap.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        txtNhapChat = new JTextField(); txtNhapChat.setFont(new Font("Segoe UI", Font.PLAIN, 13)); txtNhapChat.setBackground(new Color(5, 8, 15, 230)); txtNhapChat.setForeground(Color.WHITE); txtNhapChat.setCaretColor(Color.WHITE); txtNhapChat.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(40, 52, 74)), BorderFactory.createEmptyBorder(4, 6, 4, 6))); txtNhapChat.addActionListener(e -> guiTinNhanChat());
        btnGuiChat = taoNutBam("Gửi", COLOR_CYAN); btnGuiChat.addActionListener(e -> guiTinNhanChat()); 
        pnlNhap.add(txtNhapChat, BorderLayout.CENTER); pnlNhap.add(btnGuiChat, BorderLayout.EAST);

        JPanel pnlChatCore = new JPanel(new BorderLayout()); pnlChatCore.setOpaque(false); pnlChatCore.add(scrollChat, BorderLayout.CENTER); pnlChatCore.add(pnlNhap, BorderLayout.SOUTH);
        pnlPhai.add(taoKhungTrongSuot("Nhật ký radar chiến trường", pnlHeThongCore)); pnlPhai.add(taoKhungTrongSuot("Kênh liên lạc mật", pnlChatCore)); game.add(pnlPhai, BorderLayout.EAST);

        JPanel pnlDuoi = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); int w=getWidth(), h=getHeight();
                g2.setColor(new Color(0,0,0,100)); g2.fillRoundRect(3, 4, w-6, h-6, 16, 16); g2.setPaint(new LinearGradientPaint(0,0,0,h, new float[]{0f,1f}, new Color[]{new Color(255,255,255,12), new Color(255,255,255,4)})); g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.setStroke(STROKE_1_2); g2.setPaint(new LinearGradientPaint(0,0,w,0, new float[]{0f,0.5f,1f}, new Color[]{COLOR_CYAN, new Color(0,0,0,0), COLOR_CYAN})); g2.drawRoundRect(0, 0, w-1, h-1, 16, 16); g2.dispose();
            }
        };
        pnlDuoi.setOpaque(false); pnlDuoi.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // --- CỤM SAO CHÉP MÃ PHÒNG ---
        JPanel pnlTrangThaiCum = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); pnlTrangThaiCum.setOpaque(false);
        lblTrangThai = new JLabel("HỆ THỐNG TRẠNG THÁI: ĐANG CHỜ KẾT NỐI ĐỒNG BỘ..."); lblTrangThai.setFont(new Font("Segoe UI", Font.BOLD, 14)); lblTrangThai.setForeground(COLOR_GOLD); 
        
        btnCopyMa = taoNutBam("Copy Mã Phòng", COLOR_CYAN); 
        btnCopyMa.setVisible(false); // Sẽ hiện lên khi làm Host
        btnCopyMa.addActionListener(e -> { 
            if (!maPhongHienTai.isEmpty()) { 
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(maPhongHienTai), null); 
                ghiLogHeThong("Đã sao chép mã phòng vào Clipboard thành công."); 
                JOptionPane.showMessageDialog(this, "Đã lưu mã phòng vào Clipboard!\nHãy gửi mã này cho bạn bè để vào phòng.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } 
        });
        
        pnlTrangThaiCum.add(lblTrangThai); pnlTrangThaiCum.add(btnCopyMa); pnlDuoi.add(pnlTrangThaiCum, BorderLayout.WEST);

        JPanel pnlDieuKhien = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); pnlDieuKhien.setOpaque(false);
        btnXoayTau = taoNutBam("Xoay: Ngang", COLOR_CYAN); btnXoayTau.addActionListener(e -> { isNgang = !isNgang; btnXoayTau.setText(isNgang ? "Xoay: Ngang" : "Xoay: Dọc"); });
        btnXepNgauNhien = taoNutBam("Xếp nhanh", COLOR_CYAN); btnXepNgauNhien.addActionListener(e -> xuLyXepNgauNhien());
        btnSanSang = taoNutBam("Sẵn Sàng", new Color(40, 167, 69)); btnSanSang.addActionListener(e -> xuLySanSang());
        btnThoatGame = taoNutBam("Rút Lui", COLOR_RED); btnThoatGame.addActionListener(e -> { if(JOptionPane.showConfirmDialog(this, "Chắc kèo muốn rút lui không commander?", "Thoát?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { ngatKetNoi(); cardLayout.show(pnlCards, "LOBBY"); } });
        
        pnlDieuKhien.add(btnXoayTau); pnlDieuKhien.add(btnXepNgauNhien); pnlDieuKhien.add(btnSanSang); pnlDieuKhien.add(btnThoatGame);
        pnlDuoi.add(pnlDieuKhien, BorderLayout.EAST); game.add(pnlDuoi, BorderLayout.SOUTH); return game;
    }

    // --- GAME LOGIC & KHỞI TẠO ---

    private void khoiTaoBanCoBanDau() {
        dataBanCoMinh = new BanCo(); dataBanCoDich = new BanCo();
        for (int r = 0; r < 10; r++) for (int c = 0; c < 10; c++) { btnMinh[r][c] = new OBanCo(r, c, false); btnDich[r][c] = new OBanCo(r, c, true); }
    }

    private void khoiTaoImages() {
        try {
            imgHit = new ImageIcon(getClass().getResource("/images/hit.png")).getImage(); imgMiss = new ImageIcon(getClass().getResource("/images/miss.png")).getImage();
            imgTau5 = new ImageIcon(getClass().getResource("/images/tau_5.png")).getImage(); imgTau4 = new ImageIcon(getClass().getResource("/images/tau_4.png")).getImage();
            imgTau3 = new ImageIcon(getClass().getResource("/images/tau_3.png")).getImage(); imgTau2 = new ImageIcon(getClass().getResource("/images/tau_2.png")).getImage();
            URL bgUrl = getClass().getResource("/images/game.png"); if (bgUrl != null) bgBattle = new ImageIcon(bgUrl).getImage();
        } catch (Exception e) { System.err.println("Lỗi load ảnh Cyberpunk."); }
    }

    private void ngatKetNoi() {
        try {
            if(out != null) { out.println("THOAT"); out.close(); }
            if(in != null) in.close();
            if(socket != null) socket.close();
            if(serverSocket != null) serverSocket.close();
        } catch (IOException e) { }
        daKetNoi = false; isHost = false; doiPhuongSanSang = false; minhSanSang = false;
        if(!dangThoat) SwingUtilities.invokeLater(() -> { cardLayout.show(pnlCards, "LOBBY"); resetBanCoVeBanDau(); });
    }

    private void resetBanCoVeBanDau() {
        dataBanCoMinh = new BanCo(); dataBanCoDich = new BanCo();
        for (int r = 0; r < 10; r++) for (int c = 0; c < 10; c++) { btnMinh[r][c].resetFull(); btnDich[r][c].resetFull(); }
        tauHienTai = 0; isDangXepTau = false; isMyTurn = false;
    }

    // --- KẾT NỐI MẠNG (ROOM CODE LOGIC) ---

    private void moServer(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port); 
                ghiLogHeThong("Phòng chỉ huy mở tại Port " + port + ".");
                
                SwingUtilities.invokeLater(() -> {
                    lblTrangThai.setText("ĐANG CHỜ ĐỐI TÁC NHẬP MÃ KẾT NỐI...");
                    btnCopyMa.setVisible(true); 
                });
                
                socket = serverSocket.accept(); 
                
                SwingUtilities.invokeLater(() -> btnCopyMa.setVisible(false)); 
                ghiLogHeThong("Đồng bộ thành công! IP: " + socket.getInetAddress().getHostAddress());
                thietLapLuotDocGhi();
                
            } catch (IOException e) { if(!dangThoat) { ghiLogHeThong("Lỗi khởi tạo phòng."); ngatKetNoi(); } }
        }).start();
    }

    private void ketNoiServer(String ip, int port) {
        new Thread(() -> {
            try {
                ghiLogHeThong("Đang thiết lập liên kết tới " + ip + ":" + port + "...");
                socket = new Socket(ip, port); 
                ghiLogHeThong("Đồng bộ thành công!");
                thietLapLuotDocGhi();
            } catch (IOException e) { 
                if(!dangThoat) { 
                    ghiLogHeThong("Lỗi liên kết. Vui lòng kiểm tra lại mã phòng!"); 
                    JOptionPane.showMessageDialog(this, "Không thể kết nối! Hãy chắc chắn mã phòng chính xác và Host đang chờ.", "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                    ngatKetNoi(); 
                } 
            }
        }).start();
    }

    private void thietLapLuotDocGhi() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        daKetNoi = true; batDauLuongDoc();
        SwingUtilities.invokeLater(() -> { cardLayout.show(pnlCards, "GAME"); prepareSetup(); });
    }

    private void batDauLuongDoc() {
        new Thread(() -> {
            try { String line; while (daKetNoi && (line = in.readLine()) != null) xuLyDuLieuNhanVe(line); } 
            catch (IOException e) { if(!dangThoat) ngatKetNoi(); }
        }).start();
    }

    private void xuLyDuLieuNhanVe(String msg) {
        String[] p = msg.split(":"); String cmd = p[0];
        SwingUtilities.invokeLater(() -> {
            switch(cmd) {
                case "CHAT": txtChat.append("ĐỐI PHƯƠNG: " + msg.substring(5) + "\n"); break;
                case "READY":
                    doiPhuongSanSang = true; ghiLogHeThong("Đối phương đã SẮN SÀNG chiến đấu.");
                    if(minhSanSang) batDauVaoTranThucSu(); break;
                case "BAN":
                    int r = Integer.parseInt(p[1]), c = Integer.parseInt(p[2]); 
                    boolean hit = dataBanCoMinh.biBan(r, c);
                    btnMinh[r][c].isHit = hit; btnMinh[r][c].isMiss = !hit; 
                    out.println("KETQUA:" + r + ":" + c + ":" + hit);
                    
                    boolean conTau = false;
                    for (int i=0; i<10; i++) {
                        for (int j=0; j<10; j++) {
                            if (btnMinh[i][j].isShowShip && !btnMinh[i][j].isHit) { conTau = true; break; }
                        }
                    }
                    if(!conTau) { 
                        out.println("WINNER"); 
                        JOptionPane.showMessageDialog(this, "HẠM ĐỘI CỦA BẠN ĐÃ BỊ HỦY DIỆT! THUA CUỘC.", "KẾT THÚC", JOptionPane.ERROR_MESSAGE); 
                        resetGameOnline(); 
                    } else { 
                        isMyTurn = true; 
                        updateStatusTurn(); 
                    } 
                    pnlBattlefield.repaint(); 
                    break;
                case "KETQUA":
                    int kr = Integer.parseInt(p[1]), kc = Integer.parseInt(p[2]); 
                    boolean khit = Boolean.parseBoolean(p[3]);
                    if(khit) { 
                        btnDich[kr][kc].isHit = true; 
                        phatAmThanh("boom.wav"); 
                        ghiLogHeThong("BẮN TRÚNG! Nổ lớn tại tọa độ địch."); 
                    } else { 
                        btnDich[kr][kc].isMiss = true; 
                        phatAmThanh("nuoc.wav"); 
                        ghiLogHeThong("BẮN TRƯỢT. Đạn rơi xuống biển."); 
                    }
                    
                    int tongSoONo = 0;
                    for (int i=0; i<10; i++) {
                        for (int j=0; j<10; j++) {
                            if (btnDich[i][j].isHit) tongSoONo++;
                        }
                    }
                    if (tongSoONo == 17) { 
                        out.println("WINNER"); 
                        JOptionPane.showMessageDialog(this, "BẠN ĐÃ HẠ GỤC TOÀN BỘ CĂN CỨ ĐỊCH!", "CHIẾN THẮNG", JOptionPane.INFORMATION_MESSAGE); 
                        resetGameOnline(); 
                    } else { 
                        isMyTurn = khit; 
                        updateStatusTurn(); 
                    } 
                    pnlBattlefield.repaint(); 
                    break;
                case "WINNER": 
                    JOptionPane.showMessageDialog(this, "ĐỈNH CAO! BẠN ĐÃ ĐÁNH CHÌM TOÀN BỘ HẠM ĐỘI ĐỊCH!", "CHIẾN THẮNG", JOptionPane.INFORMATION_MESSAGE); 
                    resetGameOnline(); 
                    break;
                case "THOAT": 
                    JOptionPane.showMessageDialog(this, "ĐỐI PHƯƠNG ĐÃ RÚT LUI!"); 
                    ngatKetNoi(); 
                    break;
            }
        });
    }

    // --- LOGIC TRẬN CHIẾN ---

    private void prepareSetup() {
        resetBanCoVeBanDau(); isDangXepTau = true;
        btnXoayTau.setEnabled(true); btnXepNgauNhien.setEnabled(true); btnSanSang.setEnabled(false);
        lblTrangThai.setText("TRẠNG THÁI: HÃY XẾP " + hamDoi[tauHienTai].getTen().toUpperCase());
    }

    private void xuLyDatTau(int r, int c, boolean hienThongBao) {
        if (tauHienTai >= hamDoi.length) return; TauChien tau = hamDoi[tauHienTai];
        if (dataBanCoMinh.datTau(tau, r, c, isNgang)) {
            Image img = (tau.getChieuDai() == 5) ? imgTau5 : (tau.getChieuDai() == 4) ? imgTau4 : (tau.getChieuDai() == 3) ? imgTau3 : imgTau2;
            for (int i = 0; i < tau.getChieuDai(); i++) { int tr = r + (isNgang ? 0 : i), tc = c + (isNgang ? i : 0); btnMinh[tr][tc].setShipInfo(img, tau.getChieuDai(), i, isNgang); }
            pnlBattlefield.repaint(); tauHienTai++; phatAmThanh("xep.wav");
            if (tauHienTai < hamDoi.length) lblTrangThai.setText("TRẠNG THÁI: HÃY XẾP " + hamDoi[tauHienTai].getTen().toUpperCase());
            else { lblTrangThai.setText("TRẠNG THÁI: HẠM ĐỘI SẴN SÀNG. BẤM 'SẴN SÀNG'!"); btnSanSang.setEnabled(true); }
        } else if (hienThongBao) phatAmThanh("loi.wav");
    }

    private void xuLyXepNgauNhien() {
        dataBanCoMinh = new BanCo(); for (int r=0; r<10; r++) for (int c=0; c<10; c++) btnMinh[r][c].resetFull();
        tauHienTai = 0; Random rand = new Random();
        while (tauHienTai < hamDoi.length) { isNgang = rand.nextBoolean(); xuLyDatTau(rand.nextInt(10), rand.nextInt(10), false); }
    }

    private void xuLySanSang() {
        minhSanSang = true; isDangXepTau = false; btnXoayTau.setEnabled(false); btnXepNgauNhien.setEnabled(false); btnSanSang.setEnabled(false);
        out.println("READY"); ghiLogHeThong("Tín hiệu SẴN SÀNG đã gửi. Chờ đối phương..");
        if(doiPhuongSanSang) batDauVaoTranThucSu();
    }

    private void batDauVaoTranThucSu() { phatAmThanh("start.wav"); isMyTurn = true; updateStatusTurn(); }

    private void updateStatusTurn() {
        if(isMyTurn) { lblTrangThai.setText("TRẠNG THÁI: LƯỢT BẠN. KHAI HỎA!"); lblTrangThai.setForeground(COLOR_CYAN); } 
        else { lblTrangThai.setText("TRẠNG THÁI: LƯỢT ĐỐI PHƯƠNG. ĐANG CHỜ.."); lblTrangThai.setForeground(COLOR_GOLD); }
    }

    private void guiLenhBan(int r, int c) {
        if(!daKetNoi || !isMyTurn) return;
        out.println("BAN:" + r + ":" + c); isMyTurn = false; updateStatusTurn();
    }

    private void resetGameOnline() { SwingUtilities.invokeLater(() -> { prepareSetup(); pnlBattlefield.repaint(); }); }

    private void guiTinNhanChat() {
        String txt = txtNhapChat.getText().trim();
        if(!txt.isEmpty() && daKetNoi) { out.println("CHAT:" + txt); txtChat.append("TÔI: " + txt + "\n"); txtNhapChat.setText(""); }
    }

    // --- XỬ LÝ TẠO PHÒNG (MÃ HÓA THÀNH MÃ PHÒNG CHUẨN) ---
    private void xuLyTaoPhong() {
        String pStr = JOptionPane.showInputDialog(this, "Nhập Port mở phòng (Khuyên dùng: 12345):", "12345");
        if(pStr != null) {
            try { 
                int port = Integer.parseInt(pStr);
                String ip = InetAddress.getLocalHost().getHostAddress(); 
                
                String rawCode = ip + ":" + port;
                maPhongHienTai = java.util.Base64.getEncoder().encodeToString(rawCode.getBytes());
                
                isHost = true; 
                cardLayout.show(pnlCards, "GAME"); 
                moServer(port); 
            } catch(Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi tạo phòng: " + e.getMessage());
            }
        }
    }
    
    // --- XỬ LÝ VÀO PHÒNG (GIẢI MÃ TỪ CHUỒI BASE64) ---
    private void xuLyVaoPhong() {
        String maPhong = JOptionPane.showInputDialog(this, "Nhập MÃ PHÒNG do Host cung cấp:", ""); 
        if(maPhong == null || maPhong.trim().isEmpty()) return;
        
        try { 
            String decoded = new String(java.util.Base64.getDecoder().decode(maPhong.trim()));
            String[] parts = decoded.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);

            isHost = false; 
            cardLayout.show(pnlCards, "GAME"); 
            ketNoiServer(ip, port); 
        } catch(Exception e){
            JOptionPane.showMessageDialog(this, "Mã phòng không hợp lệ hoặc sai định dạng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new ManHinhGameOnline().setVisible(true)); }
}
