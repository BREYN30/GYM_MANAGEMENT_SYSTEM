package gym;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Midnight Violet theme system for ENCORE Pro Gym Management System.
 * High contrast, modern dark elevation, electric purple and cyan accents.
 */
public final class UITheme {
    // Primary Midnight Violet Palette
    public static final Color BG_PRIMARY = new Color(0x0B, 0x10, 0x20);        // #0B1020
    public static final Color BG_SIDEBAR = new Color(0x11, 0x18, 0x2B);        // #11182B
    public static final Color BG_CARD = new Color(0x17, 0x1F, 0x35);           // #171F35
    public static final Color BG_CARD_HOVER = new Color(0x1D, 0x27, 0x44);     // #1D2744
    public static final Color BG_CARD_ELEVATED = new Color(0x1D, 0x27, 0x40);  // #1D2740
    public static final Color BG_INPUT = new Color(0x11, 0x18, 0x2B);          // #11182B

    public static final Color VIOLET_PRIMARY = new Color(0x7C, 0x3A, 0xED);    // #7C3AED
    public static final Color VIOLET_SECONDARY = new Color(0xA8, 0x55, 0xF7);  // #A855F7
    public static final Color VIOLET_GRAD_START = new Color(0x6D, 0x28, 0xD9); // #6D28D9
    public static final Color VIOLET_GRAD_END = new Color(0x93, 0x33, 0xEA);   // #9333EA
    public static final Color CYAN_ACCENT = new Color(0x22, 0xD3, 0xEE);       // #22D3EE

    public static final Color TEXT_PRIMARY = new Color(0xF8, 0xFA, 0xFC);      // #F8FAFC
    public static final Color TEXT_SECONDARY = new Color(0xA8, 0xB3, 0xC7);    // #A8B3C7
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);        // #64748B

    public static final Color BORDER_COLOR = new Color(0x30, 0x3B, 0x55);      // #303B55
    public static final Color BORDER_FOCUS = new Color(0x7C, 0x3A, 0xED);      // #7C3AED
    public static final Color SELECTION_BG = new Color(0x31, 0x2E, 0x81);      // Deep indigo highlight

    public static final Color SUCCESS_COLOR = new Color(0x22, 0xC5, 0x5E);     // #22C55E
    public static final Color WARNING_COLOR = new Color(0xF5, 0x9E, 0x0B);     // #F59E0B
    public static final Color ERROR_COLOR = new Color(0xF8, 0x71, 0x71);       // #F87171

    // Backward compatibility aliases
    public static final Color MAIN_BG = BG_PRIMARY;
    public static final Color SECONDARY_BG = BG_SIDEBAR;
    public static final Color CARD_BG = BG_CARD;
    public static final Color PRIMARY_ACCENT = VIOLET_PRIMARY;

    // Fonts
    public static final Font FONT_BRAND = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_STAT = new Font("Segoe UI", Font.BOLD, 28);

    private UITheme() {}

    /**
     * Primary action button with anti-aliased violet gradient and hover sheen.
     */
    public static JButton createPrimaryButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                Color c1 = VIOLET_GRAD_START;
                Color c2 = VIOLET_GRAD_END;

                if (!isEnabled()) {
                    c1 = new Color(0x30, 0x3B, 0x55);
                    c2 = new Color(0x1F, 0x29, 0x3D);
                } else if (getModel().isPressed()) {
                    c1 = c1.darker();
                    c2 = c2.darker();
                } else if (getModel().isRollover()) {
                    c1 = new Color(0x7C, 0x3A, 0xED);
                    c2 = new Color(0xA8, 0x55, 0xF7);
                }

                GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 10, 10);

                if (isEnabled()) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        b.setFont(FONT_BOLD);
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 16, 9, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /**
     * Secondary button with dark elevated slate background and border.
     */
    public static JButton createSecondaryButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                Color bg = BG_CARD_ELEVATED;
                if (!isEnabled()) {
                    bg = BG_SIDEBAR;
                } else if (getModel().isPressed()) {
                    bg = new Color(0x14, 0x1B, 0x2E);
                } else if (getModel().isRollover()) {
                    bg = BG_CARD_HOVER;
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        b.setFont(FONT_BOLD);
        b.setForeground(TEXT_PRIMARY);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /**
     * Danger button with subtle red hover and border.
     */
    public static JButton createDangerButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                Color bg = new Color(0x2A, 0x15, 0x1E);
                if (getModel().isPressed()) {
                    bg = new Color(0x4C, 0x13, 0x1A);
                } else if (getModel().isRollover()) {
                    bg = new Color(0x3B, 0x15, 0x22);
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                g2.setColor(new Color(0x7F, 0x1D, 0x1D));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        b.setFont(FONT_BOLD);
        b.setForeground(ERROR_COLOR);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /**
     * Sidebar navigation button with violet active indicator and hover styling.
     */
    public static JButton createNavButton(String text, boolean active) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                boolean isActive = Boolean.TRUE.equals(getClientProperty("active"));

                if (isActive) {
                    GradientPaint gp = new GradientPaint(0, 0, new Color(0x4F, 0x46, 0xE5, 180),
                                                         w, 0, new Color(0x7C, 0x3A, 0xED, 180));
                    g2.setPaint(gp);
                    g2.fillRoundRect(4, 2, w - 8, h - 4, 8, 8);

                    // Left pill highlight
                    g2.setColor(CYAN_ACCENT);
                    g2.fillRoundRect(6, 6, 4, h - 12, 3, 3);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 12));
                    g2.fillRoundRect(4, 2, w - 8, h - 4, 8, 8);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(FONT_BOLD);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 20, 10, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateNavButtonState(b, active);
        return b;
    }

    public static void updateNavButtonState(JButton b, boolean active) {
        b.setForeground(active ? Color.WHITE : TEXT_SECONDARY);
        b.putClientProperty("active", active);
        b.repaint();
    }

    /**
     * Standard rounded card panel for widgets, tables, and forms.
     */
    public static JPanel createCardPanel() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 18, 16, 18));
        return p;
    }

    /**
     * Elevated card with subtle gradient highlight for top dashboard metrics.
     */
    public static JPanel createStatCard(String title, JLabel value, String subtitle, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Dark card background
                g2.setColor(BG_CARD_ELEVATED);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                // Subtle top accent line
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
                g2.fillRoundRect(1, 1, w - 2, 4, 3, 3);

                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel t = new JLabel(title.toUpperCase());
        t.setFont(FONT_SMALL);
        t.setForeground(TEXT_SECONDARY);
        p.add(t, BorderLayout.NORTH);

        value.setFont(FONT_STAT);
        value.setForeground(accent != null ? accent : TEXT_PRIMARY);
        p.add(value, BorderLayout.CENTER);

        JLabel s = new JLabel(subtitle);
        s.setFont(FONT_SMALL);
        s.setForeground(TEXT_MUTED);
        p.add(s, BorderLayout.SOUTH);

        return p;
    }

    /**
     * Consistent text field styling.
     */
    public static void styleTextField(JTextField f) {
        f.setFont(FONT_REGULAR);
        f.setForeground(TEXT_PRIMARY);
        f.setBackground(BG_INPUT);
        f.setCaretColor(CYAN_ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
    }

    /**
     * Consistent dropdown styling.
     */
    public static void styleComboBox(JComboBox<?> c) {
        c.setFont(FONT_REGULAR);
        c.setForeground(TEXT_PRIMARY);
        c.setBackground(BG_INPUT);
        c.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(4, 6, 4, 6)
        ));
    }

    /**
     * High-contrast dark table styling with alternating rows and custom header.
     */
    public static void styleTable(JTable t, int rowHeight) {
        t.setFont(FONT_REGULAR);
        t.setForeground(TEXT_PRIMARY);
        t.setBackground(BG_CARD);
        t.setGridColor(BORDER_COLOR);
        t.setSelectionBackground(SELECTION_BG);
        t.setSelectionForeground(TEXT_PRIMARY);
        t.setRowHeight(rowHeight > 0 ? rowHeight : 34);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setFillsViewportHeight(true);

        // Custom cell renderer for alternating row background
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    c.setBackground(SELECTION_BG);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? BG_CARD : BG_SIDEBAR);
                    c.setForeground(TEXT_PRIMARY);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        JTableHeader h = t.getTableHeader();
        h.setFont(FONT_BOLD);
        h.setForeground(TEXT_SECONDARY);
        h.setBackground(BG_SIDEBAR);
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 38));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setBackground(BG_SIDEBAR);
                l.setForeground(TEXT_SECONDARY);
                l.setFont(FONT_BOLD);
                l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    new EmptyBorder(0, 10, 0, 10)
                ));
                return l;
            }
        });
    }

    /**
     * Modern dark scroll pane with custom scrollbars.
     */
    public static JScrollPane createScrollPane(Component comp) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        sp.getViewport().setBackground(BG_CARD);

        styleScrollBar(sp.getVerticalScrollBar());
        styleScrollBar(sp.getHorizontalScrollBar());
        return sp;
    }

    private static void styleScrollBar(JScrollBar sb) {
        sb.setBackground(BG_SIDEBAR);
        sb.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(0x30, 0x3B, 0x55);
                this.trackColor = BG_SIDEBAR;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x40, 0x4D, 0x6E));
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                                  thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
        });
    }

    public static JPanel createStatusBadge(String status) {
        Color bg;
        Color fg;

        switch (status) {
            case "Active" -> {
                bg = new Color(0x13, 0x33, 0x22);
                fg = SUCCESS_COLOR;
            }
            case "Expiring Soon" -> {
                bg = new Color(0x38, 0x25, 0x10);
                fg = WARNING_COLOR;
            }
            case "Expired" -> {
                bg = new Color(0x3D, 0x15, 0x18);
                fg = ERROR_COLOR;
            }
            default -> {
                bg = new Color(0x1F, 0x29, 0x3D);
                fg = TEXT_SECONDARY;
            }
        }

        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(fg);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        JLabel l = new JLabel(status);
        l.setFont(FONT_SMALL);
        l.setForeground(fg);
        pill.add(l);
        return pill;
    }
}