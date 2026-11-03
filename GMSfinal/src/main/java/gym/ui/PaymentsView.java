package gym.ui;

import gym.Database;
import gym.Member;
import gym.UITheme;
import gym.model.PaymentRecord;
import gym.util.CsvExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PaymentsView extends JPanel {
    private final Runnable onDataChanged;

    private final JLabel totalRevenueVal = new JLabel("₹ 0");
    private final JLabel monthRevenueVal = new JLabel("₹ 0");
    private final JLabel txnVolumeVal = new JLabel("0");

    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> dateRangeCombo = new JComboBox<>(new String[]{"All Time", "This Month", "Last 30 Days", "Today"});

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[]{"Txn Ref", "Member ID", "Member Name", "Plan", "Amount Paid", "Method", "Date", "Notes"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private JButton viewReceiptBtn;

    public PaymentsView(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;

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
        JLabel title = new JLabel("Payment Transactions & Billing");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Financial ledger, payment audits, automated receipt generation, and revenue telemetry");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_SECONDARY);

        JPanel tp = new JPanel();
        tp.setOpaque(false);
        tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
        tp.add(title);
        tp.add(Box.createVerticalStrut(4));
        tp.add(sub);
        titlePanel.add(tp, BorderLayout.WEST);

        JButton recordBtn = UITheme.createPrimaryButton("+ Record Offline Payment");
        recordBtn.addActionListener(e -> showRecordPaymentDialog());
        titlePanel.add(recordBtn, BorderLayout.EAST);

        top.add(titlePanel);
        top.add(Box.createVerticalStrut(16));

        // 3 Revenue Metric Cards
        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));
        metrics.setOpaque(false);
        metrics.add(UITheme.createStatCard("Total Revenue Collected", totalRevenueVal, "All-time verified payments", UITheme.SUCCESS_COLOR));
        metrics.add(UITheme.createStatCard("Current Month Revenue", monthRevenueVal, "Calendar month billing", UITheme.CYAN_ACCENT));
        metrics.add(UITheme.createStatCard("Total Transactions", txnVolumeVal, "Completed receipts logged", UITheme.VIOLET_PRIMARY));

        top.add(metrics);
        top.add(Box.createVerticalStrut(16));

        // Filter and Action Bar
        JPanel filterBar = new JPanel(new BorderLayout());
        filterBar.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftFilters.setOpaque(false);

        UITheme.styleTextField(searchField);
        UITheme.styleComboBox(dateRangeCombo);

        searchField.setPreferredSize(new Dimension(170, 34));
        searchField.setToolTipText("Search by Txn Ref, Member Name, or ID");

        JButton searchBtn = UITheme.createSecondaryButton("Filter");
        searchBtn.addActionListener(e -> loadPayments());
        searchField.addActionListener(e -> loadPayments());
        dateRangeCombo.addActionListener(e -> loadPayments());

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            dateRangeCombo.setSelectedIndex(0);
            loadPayments();
        });

        leftFilters.add(new JLabel("Search:"));
        leftFilters.add(searchField);
        leftFilters.add(new JLabel("Period:"));
        leftFilters.add(dateRangeCombo);
        leftFilters.add(searchBtn);
        leftFilters.add(resetBtn);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);

        viewReceiptBtn = UITheme.createSecondaryButton("View Receipt");
        viewReceiptBtn.setEnabled(false);
        viewReceiptBtn.addActionListener(e -> viewSelectedReceipt());

        JButton exportBtn = UITheme.createSecondaryButton("Export CSV");
        exportBtn.addActionListener(e -> CsvExporter.exportTableToCsv(this, tableModel, "encore_payments"));

        rightBtns.add(viewReceiptBtn);
        rightBtns.add(exportBtn);

        filterBar.add(leftFilters, BorderLayout.WEST);
        filterBar.add(rightBtns, BorderLayout.EAST);
        top.add(filterBar);

        add(top, BorderLayout.NORTH);

        // Table
        UITheme.styleTable(table, 32);
        table.getSelectionModel().addListSelectionListener(e -> {
            viewReceiptBtn.setEnabled(table.getSelectedRow() >= 0);
        });

        add(UITheme.createScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshData() {
        try {
            double total = Database.getTotalRevenue();
            double month = Database.getCurrentMonthRevenue();
            totalRevenueVal.setText(String.format("₹ %.2f", total));
            monthRevenueVal.setText(String.format("₹ %.2f", month));
        } catch (SQLException ignored) {}

        loadPayments();
    }

    private void loadPayments() {
        tableModel.setRowCount(0);
        String q = searchField.getText();
        String period = (String) dateRangeCombo.getSelectedItem();

        LocalDate today = LocalDate.now();
        LocalDate startDate = null;
        LocalDate endDate = null;

        if ("Today".equalsIgnoreCase(period)) {
            startDate = today;
            endDate = today;
        } else if ("This Month".equalsIgnoreCase(period)) {
            startDate = LocalDate.of(today.getYear(), today.getMonth(), 1);
            endDate = today;
        } else if ("Last 30 Days".equalsIgnoreCase(period)) {
            startDate = today.minusDays(30);
            endDate = today;
        }

        try {
            List<PaymentRecord> list = Database.getPaymentsFiltered(q, startDate, endDate);
            txnVolumeVal.setText(String.valueOf(list.size()));

            for (PaymentRecord p : list) {
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
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load payment records: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRecordPaymentDialog() {
        JComboBox<Member> memberCombo = new JComboBox<>();
        JComboBox<String> planCombo = new JComboBox<>(new String[]{"Monthly", "Quarterly", "Yearly"});
        JTextField amountField = new JTextField(12);
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"UPI", "Cash", "Card", "Bank Transfer"});
        JTextField notesField = new JTextField("Gym membership payment", 18);

        UITheme.styleComboBox(memberCombo);
        UITheme.styleComboBox(planCombo);
        UITheme.styleTextField(amountField);
        UITheme.styleComboBox(methodCombo);
        UITheme.styleTextField(notesField);

        try {
            for (Member m : Database.getAllMembers()) {
                memberCombo.addItem(m);
            }
        } catch (SQLException ignored) {}

        Runnable updateAmount = () -> {
            String p = (String) planCombo.getSelectedItem();
            double price = Database.getPlanPrice(p);
            amountField.setText(String.format("%.2f", price));
        };
        planCombo.addActionListener(e -> updateAmount.run());
        updateAmount.run();

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 10));
        form.setOpaque(false);
        form.add(new JLabel("Select Member:"));
        form.add(memberCombo);
        form.add(new JLabel("Membership Plan:"));
        form.add(planCombo);
        form.add(new JLabel("Amount (₹):"));
        form.add(amountField);
        form.add(new JLabel("Payment Method:"));
        form.add(methodCombo);
        form.add(new JLabel("Transaction Notes:"));
        form.add(notesField);

        int res = JOptionPane.showConfirmDialog(this, form, "Record Offline Membership Payment",
                                                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        Member m = (Member) memberCombo.getSelectedItem();
        if (m == null) {
            JOptionPane.showMessageDialog(this, "Please select a member.", "Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid positive payment amount.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String plan = (String) planCombo.getSelectedItem();
        String method = (String) methodCombo.getSelectedItem();
        String notes = notesField.getText().trim();

        try {
            String txnRef = Database.recordPayment(m.getId(), plan, amount, method, notes);
            PaymentRecord rec = new PaymentRecord(0, txnRef, m.getId(), m.getName(), plan, amount, method, LocalDate.now(), notes);

            refreshData();
            onDataChanged.run();

            // Display official receipt dialog immediately
            PaymentReceiptDialog receipt = new PaymentReceiptDialog(SwingUtilities.getWindowAncestor(this), rec);
            receipt.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to record payment: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewSelectedReceipt() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        int mr = table.convertRowIndexToModel(r);

        String txnRef = String.valueOf(tableModel.getValueAt(mr, 0));
        String idStr = String.valueOf(tableModel.getValueAt(mr, 1)).replace("ENC-", "");
        int memberId = Integer.parseInt(idStr);
        String name = String.valueOf(tableModel.getValueAt(mr, 2));
        String plan = String.valueOf(tableModel.getValueAt(mr, 3));
        String amtStr = String.valueOf(tableModel.getValueAt(mr, 4)).replace("₹", "").replace(",", "").trim();
        double amount = Double.parseDouble(amtStr);
        String method = String.valueOf(tableModel.getValueAt(mr, 5));
        LocalDate date = LocalDate.parse(String.valueOf(tableModel.getValueAt(mr, 6)));
        String notes = String.valueOf(tableModel.getValueAt(mr, 7));

        PaymentRecord rec = new PaymentRecord(0, txnRef, memberId, name, plan, amount, method, date, notes);
        PaymentReceiptDialog dlg = new PaymentReceiptDialog(SwingUtilities.getWindowAncestor(this), rec);
        dlg.setVisible(true);
    }
}

