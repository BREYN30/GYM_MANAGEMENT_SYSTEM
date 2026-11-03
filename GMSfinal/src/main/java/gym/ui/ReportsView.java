package gym.ui;

import gym.Database;
import gym.Member;
import gym.UITheme;
import gym.model.AttendanceRecord;
import gym.model.PaymentRecord;
import gym.model.RenewalRecord;
import gym.util.CsvExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportsView extends JPanel {
    private final JComboBox<String> reportTypeCombo = new JComboBox<>(new String[]{
        "Membership Roster & Status Report",
        "Upcoming Expirations Audit (Next 30 Days)",
        "Financial Revenue & Ledger Report",
        "Attendance Logs & Activity Report",
        "Membership Renewal History Report"
    });

    private final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private final JLabel stat1 = new JLabel("—");
    private final JLabel stat2 = new JLabel("—");
    private final JLabel stat3 = new JLabel("—");

    private final JLabel stat1Title = new JLabel("Metric 1");
    private final JLabel stat2Title = new JLabel("Metric 2");
    private final JLabel stat3Title = new JLabel("Metric 3");

    public ReportsView() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
    }

    private void initUI() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        // Header Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Reports & Business Intelligence");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Comprehensive business auditing, expiration forecasts, attendance analytics, and CSV exports");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_SECONDARY);

        JPanel tp = new JPanel();
        tp.setOpaque(false);
        tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
        tp.add(title);
        tp.add(Box.createVerticalStrut(4));
        tp.add(sub);
        titlePanel.add(tp, BorderLayout.WEST);

        top.add(titlePanel);
        top.add(Box.createVerticalStrut(16));

        // Selector Bar
        JPanel selectBar = UITheme.createCardPanel();
        selectBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));

        UITheme.styleComboBox(reportTypeCombo);
        reportTypeCombo.setPreferredSize(new Dimension(320, 34));
        reportTypeCombo.addActionListener(e -> generateReport());

        JButton genBtn = UITheme.createPrimaryButton("Generate Report");
        genBtn.addActionListener(e -> generateReport());

        JButton exportBtn = UITheme.createSecondaryButton("Export CSV Report");
        exportBtn.addActionListener(e -> CsvExporter.exportTableToCsv(this, tableModel, "encore_report"));

        selectBar.add(new JLabel("Select Audit Report:"));
        selectBar.add(reportTypeCombo);
        selectBar.add(genBtn);
        selectBar.add(exportBtn);

        top.add(selectBar);
        top.add(Box.createVerticalStrut(14));

        // 3 Summary KPI Cards
        JPanel kpis = new JPanel(new GridLayout(1, 3, 14, 0));
        kpis.setOpaque(false);
        kpis.add(UITheme.createStatCard(stat1Title.getText(), stat1, "Calculated from database", UITheme.CYAN_ACCENT));
        kpis.add(UITheme.createStatCard(stat2Title.getText(), stat2, "Live data summary", UITheme.VIOLET_PRIMARY));
        kpis.add(UITheme.createStatCard(stat3Title.getText(), stat3, "Report telemetry", UITheme.SUCCESS_COLOR));

        top.add(kpis);
        add(top, BorderLayout.NORTH);

        // Table
        UITheme.styleTable(table, 32);
        add(UITheme.createScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshData() {
        generateReport();
    }

    private void generateReport() {
        int idx = reportTypeCombo.getSelectedIndex();
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        try {
            switch (idx) {
                case 0 -> generateMembershipRoster();
                case 1 -> generateUpcomingExpirations();
                case 2 -> generateFinancialLedger();
                case 3 -> generateAttendanceReport();
                case 4 -> generateRenewalsReport();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to compile report: " + e.getMessage(), "Report Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateMembershipRoster() throws SQLException {
        tableModel.setColumnIdentifiers(new String[]{"Member ID", "Name", "Phone", "Email", "Plan", "Join Date", "Expiry Date", "Status"});
        List<Member> list = Database.getAllMembers();
        int active = 0, expired = 0;

        for (Member m : list) {
            if ("Active".equalsIgnoreCase(m.getStatus())) active++;
            if ("Expired".equalsIgnoreCase(m.getStatus())) expired++;

            tableModel.addRow(new Object[]{
                m.getFormattedId(),
                m.getName(),
                m.getPhone(),
                m.getEmail().isEmpty() ? "—" : m.getEmail(),
                m.getPlan(),
                m.getJoinDate(),
                m.getExpiryDate(),
                m.getStatus()
            });
        }

        stat1.setText(String.valueOf(list.size()));
        stat2.setText(String.valueOf(active));
        stat3.setText(String.valueOf(expired));
    }

    private void generateUpcomingExpirations() throws SQLException {
        tableModel.setColumnIdentifiers(new String[]{"Member ID", "Name", "Phone", "Plan", "Expiry Date", "Days Remaining", "Status"});
        List<Member> list = Database.getExpiringMembers(30);

        int urgent7Days = 0;
        for (Member m : list) {
            long days = m.getDaysRemaining();
            if (days <= 7) urgent7Days++;

            tableModel.addRow(new Object[]{
                m.getFormattedId(),
                m.getName(),
                m.getPhone(),
                m.getPlan(),
                m.getExpiryDate(),
                days + " days",
                m.getStatus()
            });
        }

        stat1.setText(String.valueOf(list.size()));
        stat2.setText(String.valueOf(urgent7Days));
        stat3.setText(String.valueOf(list.size() - urgent7Days));
    }

    private void generateFinancialLedger() throws SQLException {
        tableModel.setColumnIdentifiers(new String[]{"Txn Ref", "Member ID", "Member Name", "Plan", "Amount", "Method", "Date", "Notes"});
        List<PaymentRecord> list = Database.getPaymentsFiltered("", null, null);

        double total = 0.0;
        double monthTotal = 0.0;
        LocalDate now = LocalDate.now();

        for (PaymentRecord p : list) {
            total += p.amount();
            if (p.paymentDate().getMonth() == now.getMonth() && p.paymentDate().getYear() == now.getYear()) {
                monthTotal += p.amount();
            }

            tableModel.addRow(new Object[]{
                p.txnRef(),
                String.format("ENC-%04d", p.memberId()),
                p.memberName(),
                p.plan(),
                String.format("₹ %.2f", p.amount()),
                p.paymentMethod(),
                p.paymentDate(),
                p.notes()
            });
        }

        stat1.setText(String.format("₹ %.0f", total));
        stat2.setText(String.format("₹ %.0f", monthTotal));
        stat3.setText(String.valueOf(list.size()));
    }

    private void generateAttendanceReport() throws SQLException {
        tableModel.setColumnIdentifiers(new String[]{"Log ID", "Member ID", "Member Name", "Phone", "Date", "Check-in Time", "Workout Note"});
        List<AttendanceRecord> list = Database.getAttendanceFiltered("", null, null);
        int today = Database.getTodayAttendanceCount();

        for (AttendanceRecord a : list) {
            tableModel.addRow(new Object[]{
                "#" + a.id(),
                String.format("ENC-%04d", a.memberId()),
                a.memberName(),
                a.memberPhone(),
                a.checkInDate(),
                a.checkInTime(),
                a.notes()
            });
        }

        stat1.setText(String.valueOf(list.size()));
        stat2.setText(String.valueOf(today));
        stat3.setText(String.format("%.1f", list.size() > 0 ? (double) list.size() / 30.0 : 0.0));
    }

    private void generateRenewalsReport() throws SQLException {
        tableModel.setColumnIdentifiers(new String[]{"Renewal ID", "Member ID", "Member Name", "Plan", "Previous Expiry", "New Expiry", "Renewal Date", "Amount"});
        List<RenewalRecord> list = Database.getAllRenewals();
        double totalRenewalAmt = 0.0;

        for (RenewalRecord r : list) {
            totalRenewalAmt += r.amount();
            tableModel.addRow(new Object[]{
                "#" + r.id(),
                String.format("ENC-%04d", r.memberId()),
                r.memberName(),
                r.plan(),
                r.previousExpiry(),
                r.newExpiry(),
                r.renewalDate(),
                String.format("₹ %.2f", r.amount())
            });
        }

        stat1.setText(String.valueOf(list.size()));
        stat2.setText(String.format("₹ %.0f", totalRenewalAmt));
        stat3.setText(list.isEmpty() ? "—" : list.get(0).renewalDate().toString());
    }
}

