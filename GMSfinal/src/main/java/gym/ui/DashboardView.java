package gym.ui;

import gym.Database;
import gym.Member;
import gym.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DashboardView extends JPanel {
    private final Runnable navigateToMembers;
    private final Runnable navigateToRenewals;
    private final Runnable navigateToAttendance;
    private final Runnable navigateToReports;
    private final Runnable onQuickRegister;

    private JLabel totalVal, activeVal, soonVal, expiredVal, attendanceVal, monthlyRevVal;
    private Charts.DonutChartPanel donutChart;
    private Charts.AttendanceTrendBarChart trendChart;
    private Charts.RevenueBarChart revenueChart;

    private DefaultTableModel recentModel;
    private JTable recentTable;

    private DefaultTableModel expiringModel;
    private JTable expiringTable;

    public DashboardView(Runnable navMembers, Runnable navRenewals, Runnable navAttendance,
                         Runnable navReports, Runnable quickRegister) {
        this.navigateToMembers = navMembers;
        this.navigateToRenewals = navRenewals;
        this.navigateToAttendance = navAttendance;
        this.navigateToReports = navReports;
        this.onQuickRegister = quickRegister;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
    }

    private void initUI() {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(UITheme.BG_PRIMARY);

        // Header Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Gym Performance Dashboard");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel sub = new JLabel("Real-time telemetry, membership analytics, and attendance trends");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_SECONDARY);

        JPanel tp = new JPanel();
        tp.setOpaque(false);
        tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
        tp.add(title);
        tp.add(Box.createVerticalStrut(4));
        tp.add(sub);
        titlePanel.add(tp, BorderLayout.WEST);

        // Quick Action Buttons
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        quickActions.setOpaque(false);

        JButton regBtn = UITheme.createPrimaryButton("+ Register Member");
        regBtn.addActionListener(e -> onQuickRegister.run());

        JButton checkinBtn = UITheme.createSecondaryButton("Check-in");
        checkinBtn.addActionListener(e -> navigateToAttendance.run());

        JButton renewBtn = UITheme.createSecondaryButton("Renew Plan");
        renewBtn.addActionListener(e -> navigateToRenewals.run());

        quickActions.add(regBtn);
        quickActions.add(checkinBtn);
        quickActions.add(renewBtn);
        titlePanel.add(quickActions, BorderLayout.EAST);

        scrollContent.add(titlePanel);
        scrollContent.add(Box.createVerticalStrut(18));

        // 6 Top Metrics Cards
        JPanel metricsPanel = new JPanel(new GridLayout(1, 6, 12, 0));
        metricsPanel.setOpaque(false);

        totalVal = new JLabel("0");
        activeVal = new JLabel("0");
        soonVal = new JLabel("0");
        expiredVal = new JLabel("0");
        attendanceVal = new JLabel("0");
        monthlyRevVal = new JLabel("₹ 0");

        metricsPanel.add(UITheme.createStatCard("Total Members", totalVal, "Registered accounts", UITheme.VIOLET_PRIMARY));
        metricsPanel.add(UITheme.createStatCard("Active Members", activeVal, "Current gym passes", UITheme.SUCCESS_COLOR));
        metricsPanel.add(UITheme.createStatCard("Expiring Soon", soonVal, "Within 7 days", UITheme.WARNING_COLOR));
        metricsPanel.add(UITheme.createStatCard("Expired", expiredVal, "Pending renewal", UITheme.ERROR_COLOR));
        metricsPanel.add(UITheme.createStatCard("Today's Attendance", attendanceVal, "Logged check-ins", UITheme.CYAN_ACCENT));
        metricsPanel.add(UITheme.createStatCard("Monthly Revenue", monthlyRevVal, "Recorded this month", new Color(0x34, 0xD3, 0x99)));

        scrollContent.add(metricsPanel);
        scrollContent.add(Box.createVerticalStrut(18));

        // Charts Section (3 cards side by side)
        JPanel chartsPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        chartsPanel.setOpaque(false);

        // Chart 1: Donut Chart
        JPanel donutCard = UITheme.createCardPanel();
        donutCard.setLayout(new BorderLayout(0, 10));
        JLabel dcTitle = new JLabel("Plan Distribution");
        dcTitle.setFont(UITheme.FONT_HEADING);
        dcTitle.setForeground(UITheme.TEXT_PRIMARY);
        donutCard.add(dcTitle, BorderLayout.NORTH);
        donutChart = new Charts.DonutChartPanel();
        donutCard.add(donutChart, BorderLayout.CENTER);

        // Chart 2: Attendance Trend
        JPanel trendCard = UITheme.createCardPanel();
        trendCard.setLayout(new BorderLayout(0, 10));
        JLabel tcTitle = new JLabel("7-Day Attendance Trend");
        tcTitle.setFont(UITheme.FONT_HEADING);
        tcTitle.setForeground(UITheme.TEXT_PRIMARY);
        trendCard.add(tcTitle, BorderLayout.NORTH);
        trendChart = new Charts.AttendanceTrendBarChart();
        trendCard.add(trendChart, BorderLayout.CENTER);

        // Chart 3: Monthly Revenue
        JPanel revCard = UITheme.createCardPanel();
        revCard.setLayout(new BorderLayout(0, 10));
        JLabel rcTitle = new JLabel("Monthly Revenue Trend");
        rcTitle.setFont(UITheme.FONT_HEADING);
        rcTitle.setForeground(UITheme.TEXT_PRIMARY);
        revCard.add(rcTitle, BorderLayout.NORTH);
        revenueChart = new Charts.RevenueBarChart();
        revCard.add(revenueChart, BorderLayout.CENTER);

        chartsPanel.add(donutCard);
        chartsPanel.add(trendCard);
        chartsPanel.add(revCard);

        scrollContent.add(chartsPanel);
        scrollContent.add(Box.createVerticalStrut(18));

        // Lower Tables Section: Expiring Memberships & Recent Registrations
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 14, 0));
        tablesPanel.setOpaque(false);

        // Expiring Memberships
        JPanel expiringCard = UITheme.createCardPanel();
        expiringCard.setLayout(new BorderLayout(0, 10));
        JPanel eh = new JPanel(new BorderLayout());
        eh.setOpaque(false);
        JLabel ehTitle = new JLabel("Memberships Expiring Soon (Next 7 Days)");
        ehTitle.setFont(UITheme.FONT_HEADING);
        ehTitle.setForeground(UITheme.WARNING_COLOR);
        JButton renewAllBtn = UITheme.createSecondaryButton("Renewals Hub");
        renewAllBtn.addActionListener(e -> navigateToRenewals.run());
        eh.add(ehTitle, BorderLayout.WEST);
        eh.add(renewAllBtn, BorderLayout.EAST);
        expiringCard.add(eh, BorderLayout.NORTH);

        expiringModel = new DefaultTableModel(new String[]{"ID", "Member Name", "Phone", "Plan", "Expiry Date"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        expiringTable = new JTable(expiringModel);
        UITheme.styleTable(expiringTable, 30);
        JScrollPane spExpiring = UITheme.createScrollPane(expiringTable);
        spExpiring.setPreferredSize(new Dimension(0, 160));
        expiringCard.add(spExpiring, BorderLayout.CENTER);

        // Recent Registrations
        JPanel recentCard = UITheme.createCardPanel();
        recentCard.setLayout(new BorderLayout(0, 10));
        JPanel rh = new JPanel(new BorderLayout());
        rh.setOpaque(false);
        JLabel rhTitle = new JLabel("Recent Member Registrations");
        rhTitle.setFont(UITheme.FONT_HEADING);
        rhTitle.setForeground(UITheme.TEXT_PRIMARY);
        JButton allMembersBtn = UITheme.createSecondaryButton("View All");
        allMembersBtn.addActionListener(e -> navigateToMembers.run());
        rh.add(rhTitle, BorderLayout.WEST);
        rh.add(allMembersBtn, BorderLayout.EAST);
        recentCard.add(rh, BorderLayout.NORTH);

        recentModel = new DefaultTableModel(new String[]{"ID", "Name", "Phone", "Plan", "Join Date"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        recentTable = new JTable(recentModel);
        UITheme.styleTable(recentTable, 30);
        JScrollPane spRecent = UITheme.createScrollPane(recentTable);
        spRecent.setPreferredSize(new Dimension(0, 160));
        recentCard.add(spRecent, BorderLayout.CENTER);

        tablesPanel.add(expiringCard);
        tablesPanel.add(recentCard);

        scrollContent.add(tablesPanel);

        JScrollPane mainScroll = UITheme.createScrollPane(scrollContent);
        mainScroll.setBorder(null);
        add(mainScroll, BorderLayout.CENTER);
    }

    public void refreshData() {
        try {
            Database.GymStats stats = Database.calculateStats();
            totalVal.setText(String.valueOf(stats.totalMembers()));
            activeVal.setText(String.valueOf(stats.activeMembers()));
            soonVal.setText(String.valueOf(stats.expiringSoonMembers()));
            expiredVal.setText(String.valueOf(stats.expiredMembers()));
            attendanceVal.setText(String.valueOf(stats.todayAttendance()));
            monthlyRevVal.setText(String.format("₹ %.0f", stats.monthlyRevenue()));

            // Update Donut
            donutChart.setData(stats.monthlyCount(), stats.quarterlyCount(), stats.yearlyCount());

            // Update Attendance Trend
            Map<LocalDate, Integer> trend = Database.getAttendanceTrend(7);
            trendChart.setTrend(trend);

            // Update Revenue Trend
            Map<String, Double> revTrend = Database.getMonthlyRevenueTrend(6);
            revenueChart.setRevenueTrend(revTrend);

            // Update Expiring Table
            expiringModel.setRowCount(0);
            List<Member> expiringList = Database.getExpiringMembers(7);
            if (expiringList.isEmpty()) {
                // Table is empty, nothing to add
            } else {
                for (Member m : expiringList) {
                    expiringModel.addRow(new Object[]{
                        m.getFormattedId(),
                        m.getName(),
                        m.getPhone(),
                        m.getPlan(),
                        m.getExpiryDate()
                    });
                }
            }

            // Update Recent Registrations Table
            recentModel.setRowCount(0);
            List<Member> all = Database.getAllMembers();
            int limit = Math.min(6, all.size());
            for (int i = 0; i < limit; i++) {
                Member m = all.get(i);
                recentModel.addRow(new Object[]{
                    m.getFormattedId(),
                    m.getName(),
                    m.getPhone(),
                    m.getPlan(),
                    m.getJoinDate()
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load dashboard metrics: " + e.getMessage(), "Telemetry Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

