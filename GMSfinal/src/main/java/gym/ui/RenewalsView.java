package gym.ui;

import gym.Database;
import gym.Member;
import gym.UITheme;
import gym.model.RenewalRecord;
import gym.util.CsvExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class RenewalsView extends JPanel {
    private final Runnable onDataChanged;

    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JComboBox<String> planCombo = new JComboBox<>(new String[]{"Monthly", "Quarterly", "Yearly"});
    private final JComboBox<String> paymentMethodCombo = new JComboBox<>(new String[]{"UPI", "Cash", "Card", "Bank Transfer"});

    private final JLabel currentExpiryLabel = new JLabel("—");
    private final JLabel newExpiryLabel = new JLabel("—");
    private final JLabel renewalPriceLabel = new JLabel("—");
    private final JLabel memberStatusLabel = new JLabel("—");

    private final DefaultTableModel historyModel = new DefaultTableModel(
        new String[]{"Member", "Plan", "Previous Expiry", "New Expiry", "Renewal Date", "Amount Paid"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable historyTable = new JTable(historyModel);

    // Plan pricing display labels
    private JLabel monthlyPriceLabel;
    private JLabel quarterlyPriceLabel;
    private JLabel yearlyPriceLabel;

    public RenewalsView(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;

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
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Memberships & Renewals Hub");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Extend active memberships, reactivate expired plans, and audit renewal transactions");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(sub);

        scrollContent.add(titlePanel);
        scrollContent.add(Box.createVerticalStrut(16));

        // 3 Plan Cards at top
        JPanel planCards = new JPanel(new GridLayout(1, 3, 14, 0));
        planCards.setOpaque(false);

        monthlyPriceLabel = new JLabel("₹ 1,500");
        quarterlyPriceLabel = new JLabel("₹ 4,000");
        yearlyPriceLabel = new JLabel("₹ 12,000");

        planCards.add(createPlanCard("Monthly Plan", "1 Month Access", monthlyPriceLabel, "Standard monthly membership, ideal for casual gym goers."));
        planCards.add(createPlanCard("Quarterly Plan", "3 Months Access", quarterlyPriceLabel, "Save with a 90-day plan, including free fitness assessment."));
        planCards.add(createPlanCard("Yearly Pro Plan", "12 Months Access", yearlyPriceLabel, "Best value annual pass with complimentary personal training."));

        scrollContent.add(planCards);
        scrollContent.add(Box.createVerticalStrut(18));

        // Middle Section: Form on Left, History Table on Right
        JPanel midPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        midPanel.setOpaque(false);

        // Renewal Form Card
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new BorderLayout(0, 14));

        JLabel formTitle = new JLabel("Process Membership Renewal");
        formTitle.setFont(UITheme.FONT_HEADING);
        formTitle.setForeground(UITheme.TEXT_PRIMARY);
        formCard.add(formTitle, BorderLayout.NORTH);

        UITheme.styleComboBox(memberCombo);
        UITheme.styleComboBox(planCombo);
        UITheme.styleComboBox(paymentMethodCombo);

        memberCombo.addActionListener(e -> updatePreview());
        planCombo.addActionListener(e -> updatePreview());

        currentExpiryLabel.setFont(UITheme.FONT_BOLD);
        currentExpiryLabel.setForeground(UITheme.TEXT_PRIMARY);

        newExpiryLabel.setFont(UITheme.FONT_BOLD);
        newExpiryLabel.setForeground(UITheme.CYAN_ACCENT);

        renewalPriceLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        renewalPriceLabel.setForeground(UITheme.SUCCESS_COLOR);

        memberStatusLabel.setFont(UITheme.FONT_BOLD);
        memberStatusLabel.setForeground(UITheme.WARNING_COLOR);

        JPanel fields = new JPanel(new GridLayout(6, 2, 8, 12));
        fields.setOpaque(false);

        fields.add(createFieldLabel("Select Member:"));
        fields.add(memberCombo);

        fields.add(createFieldLabel("Member Status:"));
        fields.add(memberStatusLabel);

        fields.add(createFieldLabel("Renewal Plan:"));
        fields.add(planCombo);

        fields.add(createFieldLabel("Current Expiry Date:"));
        fields.add(currentExpiryLabel);

        fields.add(createFieldLabel("New Calculated Expiry:"));
        fields.add(newExpiryLabel);

        fields.add(createFieldLabel("Amount & Payment:"));
        JPanel payRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        payRow.setOpaque(false);
        payRow.add(renewalPriceLabel);
        payRow.add(paymentMethodCombo);
        fields.add(payRow);

        formCard.add(fields, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        JButton confirmBtn = UITheme.createPrimaryButton("Confirm & Process Renewal");
        confirmBtn.addActionListener(e -> processRenewal());
        btnPanel.add(confirmBtn);

        formCard.add(btnPanel, BorderLayout.SOUTH);
        midPanel.add(formCard);

        // Renewal History Card
        JPanel historyCard = UITheme.createCardPanel();
        historyCard.setLayout(new BorderLayout(0, 10));

        JPanel hh = new JPanel(new BorderLayout());
        hh.setOpaque(false);
        JLabel hhTitle = new JLabel("Recent Renewal Logs");
        hhTitle.setFont(UITheme.FONT_HEADING);
        hhTitle.setForeground(UITheme.TEXT_PRIMARY);

        JButton exportBtn = UITheme.createSecondaryButton("Export CSV");
        exportBtn.addActionListener(e -> CsvExporter.exportTableToCsv(this, historyModel, "encore_renewals"));

        hh.add(hhTitle, BorderLayout.WEST);
        hh.add(exportBtn, BorderLayout.EAST);
        historyCard.add(hh, BorderLayout.NORTH);

        UITheme.styleTable(historyTable, 30);
        JScrollPane spHistory = UITheme.createScrollPane(historyTable);
        spHistory.setPreferredSize(new Dimension(0, 240));
        historyCard.add(spHistory, BorderLayout.CENTER);

        midPanel.add(historyCard);

        scrollContent.add(midPanel);

        JScrollPane mainScroll = UITheme.createScrollPane(scrollContent);
        mainScroll.setBorder(null);
        add(mainScroll, BorderLayout.CENTER);
    }

    private JPanel createPlanCard(String title, String duration, JLabel priceLabel, String desc) {
        JPanel p = UITheme.createCardPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(UITheme.FONT_HEADING);
        t.setForeground(UITheme.VIOLET_SECONDARY);

        JLabel dur = new JLabel(duration.toUpperCase());
        dur.setFont(UITheme.FONT_SMALL);
        dur.setForeground(UITheme.CYAN_ACCENT);

        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        priceLabel.setForeground(UITheme.SUCCESS_COLOR);

        JLabel d = new JLabel("<html>" + desc + "</html>");
        d.setFont(UITheme.FONT_SMALL);
        d.setForeground(UITheme.TEXT_SECONDARY);

        p.add(t);
        p.add(Box.createVerticalStrut(2));
        p.add(dur);
        p.add(Box.createVerticalStrut(8));
        p.add(priceLabel);
        p.add(Box.createVerticalStrut(6));
        p.add(d);

        return p;
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    public void refreshData() {
        // Update plan pricing
        monthlyPriceLabel.setText(String.format("₹ %.0f", Database.getPlanPrice("Monthly")));
        quarterlyPriceLabel.setText(String.format("₹ %.0f", Database.getPlanPrice("Quarterly")));
        yearlyPriceLabel.setText(String.format("₹ %.0f", Database.getPlanPrice("Yearly")));

        // Reload members combo
        int selId = -1;
        Member prev = (Member) memberCombo.getSelectedItem();
        if (prev != null) selId = prev.getId();

        memberCombo.removeAllItems();
        try {
            List<Member> all = Database.getAllMembers();
            for (Member m : all) {
                memberCombo.addItem(m);
            }

            for (int i = 0; i < memberCombo.getItemCount(); i++) {
                if (memberCombo.getItemAt(i).getId() == selId) {
                    memberCombo.setSelectedIndex(i);
                    break;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load members: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        updatePreview();
        loadHistory();
    }

    private void updatePreview() {
        Member m = (Member) memberCombo.getSelectedItem();
        String selectedPlan = (String) planCombo.getSelectedItem();

        if (m == null || selectedPlan == null) {
            currentExpiryLabel.setText("—");
            newExpiryLabel.setText("—");
            renewalPriceLabel.setText("—");
            memberStatusLabel.setText("—");
            return;
        }

        currentExpiryLabel.setText(m.getExpiryDate().toString());
        LocalDate newExpiry = Member.calculateRenewalDate(m.getExpiryDate(), selectedPlan);
        newExpiryLabel.setText(newExpiry.toString());

        double price = Database.getPlanPrice(selectedPlan);
        renewalPriceLabel.setText(String.format("₹ %.2f", price));

        memberStatusLabel.setText(m.getStatus());
        if ("Active".equalsIgnoreCase(m.getStatus())) {
            memberStatusLabel.setForeground(UITheme.SUCCESS_COLOR);
        } else if ("Expiring Soon".equalsIgnoreCase(m.getStatus())) {
            memberStatusLabel.setForeground(UITheme.WARNING_COLOR);
        } else {
            memberStatusLabel.setForeground(UITheme.ERROR_COLOR);
        }
    }

    private void loadHistory() {
        historyModel.setRowCount(0);
        try {
            List<RenewalRecord> list = Database.getAllRenewals();
            for (RenewalRecord r : list) {
                historyModel.addRow(new Object[]{
                    r.memberName() + " (ENC-" + String.format("%04d", r.memberId()) + ")",
                    r.plan(),
                    r.previousExpiry(),
                    r.newExpiry(),
                    r.renewalDate(),
                    String.format("₹ %.2f", r.amount())
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load renewal history: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processRenewal() {
        Member m = (Member) memberCombo.getSelectedItem();
        if (m == null) {
            JOptionPane.showMessageDialog(this, "Please select a member to renew.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String plan = (String) planCombo.getSelectedItem();
        LocalDate newExp = Member.calculateRenewalDate(m.getExpiryDate(), plan);
        double amount = Database.getPlanPrice(plan);
        String method = (String) paymentMethodCombo.getSelectedItem();

        String prompt = String.format(
            "Confirm Renewal for %s (%s)?\n\n" +
            "• Current Expiry: %s\n" +
            "• New Expiry: %s\n" +
            "• Renewal Plan: %s\n" +
            "• Payment Amount: ₹ %.2f via %s",
            m.getName(), m.getFormattedId(), m.getExpiryDate(), newExp, plan, amount, method
        );

        int confirm = JOptionPane.showConfirmDialog(this, prompt, "Confirm Membership Renewal",
                                                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Database.renewMemberWithRecord(m.getId(), plan, newExp.toString(), amount, method);
            JOptionPane.showMessageDialog(this,
                "Membership renewed successfully until " + newExp + "!\nTransaction recorded in payments.",
                "Success", JOptionPane.INFORMATION_MESSAGE);

            refreshData();
            onDataChanged.run();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to process renewal: " + e.getMessage(), "Renewal Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

