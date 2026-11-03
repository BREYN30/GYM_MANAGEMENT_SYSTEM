package gym;

import gym.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ENCORE Pro — Gym Management System
 * Premium Desktop Application with Midnight Violet identity, real SQLite storage,
 * and integrated modules for Members, Memberships, Attendance, Payments, Reports, and Settings.
 */
public class GymApp extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    // Navigation Buttons
    private JButton navDashboard;
    private JButton navMembers;
    private JButton navRenewals;
    private JButton navAttendance;
    private JButton navPayments;
    private JButton navReports;
    private JButton navSettings;

    // Header Components
    private JLabel brandTitle;
    private JLabel brandSubtitle;
    private JLabel dateLabel;
    private JLabel expiringBadge;
    private JTextField globalSearchField;

    // Child Views
    private DashboardView dashboardView;
    private MembersView membersView;
    private RenewalsView renewalsView;
    private AttendanceView attendanceView;
    private PaymentsView paymentsView;
    private ReportsView reportsView;
    private SettingsView settingsView;

    private String currentView = "DASH";

    public GymApp() {
        setTitle("ENCORE Pro — Gym Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1060, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_PRIMARY);
        setLayout(new BorderLayout());

        initViews();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        refreshAllViews();
    }

    private void initViews() {
        dashboardView = new DashboardView(
            () -> showView("MEMBERS"),
            () -> showView("PLANS"),
            () -> showView("ATTENDANCE"),
            () -> showView("REPORTS"),
            () -> membersView.showAddMemberDialog()
        );

        membersView = new MembersView(
            this::refreshAllViews,
            () -> showView("PLANS")
        );

        renewalsView = new RenewalsView(this::refreshAllViews);
        attendanceView = new AttendanceView(this::refreshAllViews);
        paymentsView = new PaymentsView(this::refreshAllViews);
        reportsView = new ReportsView();
        settingsView = new SettingsView(this::onSettingsUpdated);

        contentPanel.setBackground(UITheme.BG_PRIMARY);
        contentPanel.add(dashboardView, "DASH");
        contentPanel.add(membersView, "MEMBERS");
        contentPanel.add(renewalsView, "PLANS");
        contentPanel.add(attendanceView, "ATTENDANCE");
        contentPanel.add(paymentsView, "PAYMENTS");
        contentPanel.add(reportsView, "REPORTS");
        contentPanel.add(settingsView, "SETTINGS");
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Sleek dark navy gradient for header
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x11, 0x18, 0x27),
                                                     w, 0, new Color(0x1E, 0x1B, 0x4B));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(0, h - 1, w, h - 1);

                g2.dispose();
            }
        };

        header.setPreferredSize(new Dimension(0, 68));
        header.setBorder(new EmptyBorder(12, 22, 12, 22));

        // Left Branding
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        brandPanel.setOpaque(false);

        // Logo Mark (EN)
        JPanel logoMark = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, UITheme.VIOLET_PRIMARY, getWidth(), getHeight(), UITheme.CYAN_ACCENT);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("E", (getWidth() - fm.stringWidth("E")) / 2, 26);
                g2.dispose();
            }
        };
        logoMark.setPreferredSize(new Dimension(38, 38));
        logoMark.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        String savedName = Database.getSetting("gym_name", "ENCORE FITNESS CLUB");
        String savedSub = Database.getSetting("gym_subtitle", "GYM MANAGEMENT SYSTEM");

        brandTitle = new JLabel("ENCORE PRO");
        brandTitle.setFont(UITheme.FONT_BRAND);
        brandTitle.setForeground(UITheme.TEXT_PRIMARY);

        brandSubtitle = new JLabel(savedSub.toUpperCase());
        brandSubtitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        brandSubtitle.setForeground(UITheme.CYAN_ACCENT);

        titles.add(brandTitle);
        titles.add(brandSubtitle);

        brandPanel.add(logoMark);
        brandPanel.add(titles);

        // Center / Global Search
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        centerPanel.setOpaque(false);

        globalSearchField = new JTextField(18);
        UITheme.styleTextField(globalSearchField);
        globalSearchField.setPreferredSize(new Dimension(220, 34));
        globalSearchField.setToolTipText("Search member and press Enter...");
        globalSearchField.addActionListener(e -> {
            String q = globalSearchField.getText().trim();
            if (!q.isEmpty()) {
                showView("MEMBERS");
                membersView.filter();
            }
        });

        JLabel searchIcon = new JLabel("Search:");
        searchIcon.setFont(UITheme.FONT_SMALL);
        searchIcon.setForeground(UITheme.TEXT_SECONDARY);
        centerPanel.add(searchIcon);
        centerPanel.add(globalSearchField);

        // Right Info & Actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 2));
        rightPanel.setOpaque(false);

        // Date Display
        dateLabel = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")));
        dateLabel.setFont(UITheme.FONT_SMALL);
        dateLabel.setForeground(UITheme.TEXT_SECONDARY);

        // Database Connection Indicator
        JLabel dbStatus = new JLabel("● SQLite Online");
        dbStatus.setFont(UITheme.FONT_BOLD);
        dbStatus.setForeground(UITheme.SUCCESS_COLOR);

        // Expiring Soon Notification Pill
        expiringBadge = new JLabel("0 Expiring");
        expiringBadge.setFont(UITheme.FONT_SMALL);
        expiringBadge.setForeground(UITheme.WARNING_COLOR);
        expiringBadge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        expiringBadge.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showView("PLANS");
            }
        });

        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x38, 0x25, 0x10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UITheme.WARNING_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(expiringBadge);

        JButton quickRegBtn = UITheme.createPrimaryButton("+ Register");
        quickRegBtn.addActionListener(e -> membersView.showAddMemberDialog());

        rightPanel.add(dateLabel);
        rightPanel.add(dbStatus);
        rightPanel.add(badgeWrapper);
        rightPanel.add(quickRegBtn);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(centerPanel, BorderLayout.CENTER);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER_COLOR));

        JPanel navList = new JPanel();
        navList.setOpaque(false);
        navList.setBorder(new EmptyBorder(18, 12, 14, 12));
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));

        navDashboard = UITheme.createNavButton("  Dashboard", true);
        navMembers = UITheme.createNavButton("  Members Directory", false);
        navRenewals = UITheme.createNavButton("  Plans & Renewals", false);
        navAttendance = UITheme.createNavButton("  Attendance Logs", false);
        navPayments = UITheme.createNavButton("  Billing & Payments", false);
        navReports = UITheme.createNavButton("  Reports & Analytics", false);
        navSettings = UITheme.createNavButton("  System Settings", false);

        JButton[] buttons = {
            navDashboard, navMembers, navRenewals, navAttendance,
            navPayments, navReports, navSettings
        };

        for (JButton b : buttons) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            navList.add(b);
            navList.add(Box.createVerticalStrut(5));
        }

        navDashboard.addActionListener(e -> showView("DASH"));
        navMembers.addActionListener(e -> showView("MEMBERS"));
        navRenewals.addActionListener(e -> showView("PLANS"));
        navAttendance.addActionListener(e -> showView("ATTENDANCE"));
        navPayments.addActionListener(e -> showView("PAYMENTS"));
        navReports.addActionListener(e -> showView("REPORTS"));
        navSettings.addActionListener(e -> showView("SETTINGS"));

        // Sidebar Footer
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(14, 18, 18, 14));

        JLabel vLabel = new JLabel("ENCORE Pro v2.0");
        vLabel.setFont(UITheme.FONT_BOLD);
        vLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel envLabel = new JLabel("SQLite Enterprise Edition");
        envLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        envLabel.setForeground(UITheme.TEXT_MUTED);

        footer.add(vLabel);
        footer.add(Box.createVerticalStrut(2));
        footer.add(envLabel);

        sidebar.add(navList, BorderLayout.NORTH);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    public void showView(String viewName) {
        currentView = viewName;
        cardLayout.show(contentPanel, viewName);

        UITheme.updateNavButtonState(navDashboard, viewName.equals("DASH"));
        UITheme.updateNavButtonState(navMembers, viewName.equals("MEMBERS"));
        UITheme.updateNavButtonState(navRenewals, viewName.equals("PLANS"));
        UITheme.updateNavButtonState(navAttendance, viewName.equals("ATTENDANCE"));
        UITheme.updateNavButtonState(navPayments, viewName.equals("PAYMENTS"));
        UITheme.updateNavButtonState(navReports, viewName.equals("REPORTS"));
        UITheme.updateNavButtonState(navSettings, viewName.equals("SETTINGS"));

        switch (viewName) {
            case "DASH" -> dashboardView.refreshData();
            case "MEMBERS" -> membersView.filter();
            case "PLANS" -> renewalsView.refreshData();
            case "ATTENDANCE" -> attendanceView.refreshData();
            case "PAYMENTS" -> paymentsView.refreshData();
            case "REPORTS" -> reportsView.refreshData();
            case "SETTINGS" -> settingsView.refreshData();
        }

        updateExpiringCount();
    }

    private void updateExpiringCount() {
        try {
            List<Member> expiring = Database.getExpiringMembers(7);
            int count = expiring.size();
            expiringBadge.setText(count + " Expiring");
            if (count > 0) {
                expiringBadge.setForeground(UITheme.WARNING_COLOR);
            } else {
                expiringBadge.setText("0 Expiring");
                expiringBadge.setForeground(UITheme.SUCCESS_COLOR);
            }
        } catch (SQLException ignored) {}
    }

    public void refreshAllViews() {
        dashboardView.refreshData();
        membersView.filter();
        renewalsView.refreshData();
        attendanceView.refreshData();
        paymentsView.refreshData();
        reportsView.refreshData();
        settingsView.refreshData();
        updateExpiringCount();
    }

    private void onSettingsUpdated() {
        brandSubtitle.setText(Database.getSetting("gym_subtitle", "GYM MANAGEMENT SYSTEM").toUpperCase());
        refreshAllViews();
    }

    public static void main(String[] args) {
        // Enforce dark styling defaults across LookAndFeel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("Panel.background", UITheme.BG_PRIMARY);
            UIManager.put("OptionPane.background", UITheme.BG_CARD);
            UIManager.put("OptionPane.messageForeground", UITheme.TEXT_PRIMARY);
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            try {
                Database.initialize();
                GymApp app = new GymApp();
                app.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                    null,
                    "Failed to launch ENCORE Pro: " + e.getMessage(),
                    "Fatal Startup Error",
                    JOptionPane.ERROR_MESSAGE
                );
                e.printStackTrace();
            }
        });
    }
}
