package views;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ManHinhChinh extends JFrame {
    private Image bgImage = null;

    public ManHinhChinh() {
        this.setTitle("Battleship");
        this.setSize(850, 580); // Kích thước chuẩn để lộ ảnh nền biển cực đẹp
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null); 
        this.setLayout(new BorderLayout());

        // --- LOAD ẢNH NỀN HẢI CHIẾN ---
        try {
            java.net.URL imgUrl = getClass().getResource("/images/home.png");
            if (imgUrl != null) {
                bgImage = new ImageIcon(imgUrl).getImage();
            } else {
                System.out.println("❌ Vẫn không tìm thấy ảnh! Kiểm tra lại tên file 'home.png' trong folder images.");
            }
        } catch (Exception e) {
            System.out.println("Lỗi load ảnh nền: " + e.getMessage());
        }

        // --- PANEL CHÍNH GHI ĐÈ ĐỂ VẼ NỀN ---
        JPanel pnlMain = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (bgImage != null) {
                    g2d.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                    g2d.setColor(new Color(10, 20, 30, 145)); 
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g2d.setColor(new Color(20, 30, 45));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        pnlMain.setLayout(new BoxLayout(pnlMain, BoxLayout.Y_AXIS));

        // --- TIÊU ĐỀ GAME (STYLE HIỆN ĐẠI) ---
        pnlMain.add(Box.createVerticalStrut(60)); 
        
        JLabel lblTitle = new JLabel("BATTLESHIP");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 48));
        lblTitle.setForeground(new Color(241, 196, 15)); // Màu vàng hoàng gia rực rỡ
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblSubTitle = new JLabel("HỆ THỐNG ĐIỀU HÀNH CHIẾN TRƯỜNG 2D");
        lblSubTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSubTitle.setForeground(new Color(52, 152, 219)); // Màu xanh Neon công nghệ
        lblSubTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlMain.add(lblTitle);
        pnlMain.add(Box.createVerticalStrut(8));
        pnlMain.add(lblSubTitle);
        pnlMain.add(Box.createVerticalStrut(50)); 

        // --- KHỞI TẠO CÁC NÚT BẤM NÂNG CẤP SIÊU ĐẸP ---
        JButton btnOffline1P = taoNutSieuDep("CHƠI ĐƠN (Đấu với Máy)", new Color(46, 204, 113));
        btnOffline1P.addActionListener(e -> {
            new ManHinhGame().setVisible(true);
            this.dispose(); 
        });

        JButton btnOffline2P = taoNutSieuDep("CHƠI 2 NGƯỜI (Chung 1 máy)", new Color(155, 89, 182));
        btnOffline2P.addActionListener(e -> {
            new ManHinhGame2P().setVisible(true);
            this.dispose(); 
        });

        JButton btnOnline = taoNutSieuDep("CHƠI ONLINE (PvP qua Mạng)", new Color(52, 152, 219));
        btnOnline.addActionListener(e -> {
            new ManHinhGameOnline().setVisible(true);
            this.dispose(); 
        });

        // Add cụm nút vào panel chính
        pnlMain.add(btnOffline1P);
        pnlMain.add(Box.createVerticalStrut(18));
        pnlMain.add(btnOffline2P);
        pnlMain.add(Box.createVerticalStrut(18));
        pnlMain.add(btnOnline);

        this.add(pnlMain, BorderLayout.CENTER);
    }

    // --- HÀM DESIGN NÚT BẤM PHONG CÁCH TACTICAL NEON GLOW CHUYÊN NGHIỆP ---
    private JButton taoNutSieuDep(String text, Color mauVien) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                int boGoc = 12; // Bo góc 12px hiện đại, mềm mại hơn

                if (getModel().isPressed()) {
                    // 1. Trạng thái ĐANG CLICK: Đổ bóng chìm, nền tối lại tạo cảm giác nút bị ấn xuống thật
                    g2d.setPaint(new GradientPaint(0, 0, new Color(10, 15, 20, 240), 0, h, new Color(5, 5, 10, 240)));
                    g2d.fillRoundRect(0, 0, w, h, boGoc, boGoc);
                    
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.setColor(mauVien.darker());
                    g2d.drawRoundRect(1, 1, w - 3, h - 3, boGoc, boGoc);

                } else if (getModel().isRollover()) {
                    // 2. Trạng thái HOVER (Di chuột vào): Tỏa sáng Neon cực mạnh
                    // Vẽ lớp tỏa sáng mờ phía sau nút trước (Glow effect)
                    g2d.setColor(new Color(mauVien.getRed(), mauVien.getGreen(), mauVien.getBlue(), 25));
                    g2d.fillRoundRect(2, 2, w - 4, h - 4, boGoc, boGoc);
                    
                    // Đổ màu nền Gradient công nghệ nửa trong suốt
                    g2d.setPaint(new GradientPaint(0, 0, new Color(mauVien.getRed(), mauVien.getGreen(), mauVien.getBlue(), 55), 
                                                   0, h, new Color(15, 25, 40, 180)));
                    g2d.fillRoundRect(0, 0, w, h, boGoc, boGoc);
                    
                    // Vẽ viền Neon phát sáng dày hơn bình thường
                    g2d.setStroke(new BasicStroke(2.5f));
                    g2d.setColor(mauVien);
                    g2d.drawRoundRect(1, 1, w - 3, h - 3, boGoc, boGoc);

                } else {
                    // 3. Trạng thái BÌNH THƯỜNG: Thiết kế Glassmorphism (Kính mờ) sang trọng
                    g2d.setPaint(new GradientPaint(0, 0, new Color(25, 40, 65, 120), 0, h, new Color(10, 15, 25, 190)));
                    g2d.fillRoundRect(0, 0, w, h, boGoc, boGoc);
                    
                    // Viền mảnh tinh tế nửa trong suốt
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.setColor(new Color(mauVien.getRed(), mauVien.getGreen(), mauVien.getBlue(), 160));
                    g2d.drawRoundRect(1, 1, w - 3, h - 3, boGoc, boGoc);
                }
                
                g2d.dispose();
                super.paintComponent(g); // Gọi lớp cha để tự động vẽ chữ lên trên cùng mà không sợ nhòe chữ
            }
        };
        
        // --- CẤU HÌNH THUỘC TÍNH NÚT BẤM ---
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        
        // Vô hiệu hóa triệt để các nét vẽ vuông thô kệch mặc định của Windows/Java
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Kích thước nút chuẩn UI game chỉ huy quân sự (Dày dặn và dài hơn tí nhìn rất oai)
        btn.setMaximumSize(new Dimension(380, 50));
        btn.setPreferredSize(new Dimension(380, 50));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ManHinhChinh().setVisible(true);
        });
    }
}