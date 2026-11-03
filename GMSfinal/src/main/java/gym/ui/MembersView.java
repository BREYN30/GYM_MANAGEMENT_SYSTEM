package gym.ui;

import gym.Database;
import gym.Member;
import gym.UITheme;
import gym.util.CsvExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class MembersView extends JPanel {
    private final Runnable onDataChanged;
    private final Runnable navigateToRenewals;

    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Statuses", "Active", "Expiring Soon", "Expired"});
    private final JComboBox<String> planFilter = new JComboBox<>(new String[]{"All Plans", "Monthly", "Quarterly", "Yearly"});
    private final JComboBox<String> sortSelector = new JComboBox<>(new String[]{
        "ID (Newest First)", "ID (Oldest First)", "Name (A-Z)", "Name (Z-A)", "Expiry Date", "Join Date"
    });

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[]{"ID", "Full Name", "Phone", "Email", "Plan", "Join Date", "Expiry Date", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private JButton viewBtn;
    private JButton editBtn;
    private JButton renewBtn;
    private JButton deleteBtn;

    public MembersView(Runnable onDataChanged, Runnable navigateToRenewals) {
        this.onDataChanged = onDataChanged;
        this.navigateToRenewals = navigateToRenewals;

        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BG_PRIMARY);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initUI();
    }

    private void initUI() {
        // Top Toolbar
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);

        // Heading
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Member Directory");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Search, filter, edit profiles, track validity, and export member records");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(sub);
        top.add(titlePanel, BorderLayout.NORTH);

        // Filter Bar Card
        JPanel filterCard = UITheme.createCardPanel();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));

        UITheme.styleTextField(searchField);
        UITheme.styleComboBox(statusFilter);
        UITheme.styleComboBox(planFilter);
        UITheme.styleComboBox(sortSelector);

        searchField.setPreferredSize(new Dimension(160, 34));
        searchField.setToolTipText("Search by Name, Phone, Email, or Member ID");

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UITheme.FONT_SMALL);
        searchLbl.setForeground(UITheme.TEXT_SECONDARY);

        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(UITheme.FONT_SMALL);
        statusLbl.setForeground(UITheme.TEXT_SECONDARY);

        JLabel planLbl = new JLabel("Plan:");
        planLbl.setFont(UITheme.FONT_SMALL);
        planLbl.setForeground(UITheme.TEXT_SECONDARY);

        JLabel sortLbl = new JLabel("Sort:");
        sortLbl.setFont(UITheme.FONT_SMALL);
        sortLbl.setForeground(UITheme.TEXT_SECONDARY);

        JButton searchBtn = UITheme.createSecondaryButton("Search");
        searchBtn.addActionListener(e -> filter());
        searchField.addActionListener(e -> filter());

        statusFilter.addActionListener(e -> filter());
        planFilter.addActionListener(e -> filter());
        sortSelector.addActionListener(e -> filter());

        JButton resetBtn = UITheme.createSecondaryButton("Reset Filters");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            planFilter.setSelectedIndex(0);
            sortSelector.setSelectedIndex(0);
            filter();
        });

        filterCard.add(searchLbl);
        filterCard.add(searchField);
        filterCard.add(searchBtn);
        filterCard.add(Box.createHorizontalStrut(6));
        filterCard.add(statusLbl);
        filterCard.add(statusFilter);
        filterCard.add(planLbl);
        filterCard.add(planFilter);
        filterCard.add(sortLbl);
        filterCard.add(sortSelector);
        filterCard.add(resetBtn);

        top.add(filterCard, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);
        JButton addBtn = UITheme.createPrimaryButton("+ Add New Member");
        addBtn.addActionListener(e -> showAddMemberDialog());
        leftActions.add(addBtn);

        JButton exportBtn = UITheme.createSecondaryButton("Export to CSV");
        exportBtn.addActionListener(e -> CsvExporter.exportTableToCsv(this, tableModel, "encore_members"));
        leftActions.add(exportBtn);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        viewBtn = UITheme.createSecondaryButton("View Profile");
        viewBtn.setEnabled(false);
        viewBtn.addActionListener(e -> showSelectedMemberDetails());

        editBtn = UITheme.createSecondaryButton("Edit Details");
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> showEditMemberDialog());

        renewBtn = UITheme.createSecondaryButton("Renew Plan");
        renewBtn.setEnabled(false);
        renewBtn.addActionListener(e -> showQuickRenewDialog());

        deleteBtn = UITheme.createDangerButton("Delete");
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> deleteSelectedMember());

        rightActions.add(viewBtn);
        rightActions.add(editBtn);
        rightActions.add(renewBtn);
        rightActions.add(deleteBtn);

        actions.add(leftActions, BorderLayout.WEST);
        actions.add(rightActions, BorderLayout.EAST);

        top.add(actions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // Table setup
        UITheme.styleTable(table, 34);
        table.setAutoCreateRowSorter(true);

        // Custom renderer for Status column (column index 7) to render status badge
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                String valStr = value != null ? value.toString() : "";
                JPanel badge = UITheme.createStatusBadge(valStr);
                badge.setBackground(isSelected ? UITheme.SELECTION_BG : (row % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_SIDEBAR));
                return badge;
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSel = table.getSelectedRow() >= 0;
            viewBtn.setEnabled(hasSel);
            editBtn.setEnabled(hasSel);
            renewBtn.setEnabled(hasSel);
            deleteBtn.setEnabled(hasSel);
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    showSelectedMemberDetails();
                }
            }
        });

        add(UITheme.createScrollPane(table), BorderLayout.CENTER);
    }

    public void filter() {
        tableModel.setRowCount(0);
        String q = searchField.getText();
        String st = (String) statusFilter.getSelectedItem();
        String pl = (String) planFilter.getSelectedItem();
        String so = (String) sortSelector.getSelectedItem();

        try {
            List<Member> list = Database.searchAndFilterMembers(q, st, pl, so);
            for (Member m : list) {
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
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load members: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getSelectedMemberId() {
        int vr = table.getSelectedRow();
        if (vr < 0) return -1;
        int mr = table.convertRowIndexToModel(vr);
        String formattedId = String.valueOf(tableModel.getValueAt(mr, 0));
        // Extract numeric ID from "ENC-0001" or parse integer
        try {
            if (formattedId.startsWith("ENC-")) {
                return Integer.parseInt(formattedId.substring(4));
            }
            return Integer.parseInt(formattedId);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void showSelectedMemberDetails() {
        int id = getSelectedMemberId();
        if (id < 0) return;
        try {
            Member m = Database.getMemberById(id);
            if (m != null) {
                MemberDetailDialog dlg = new MemberDetailDialog(SwingUtilities.getWindowAncestor(this), m, () -> {
                    filter();
                    onDataChanged.run();
                });
                dlg.setVisible(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load member details: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showAddMemberDialog() {
        JTextField nameField = new JTextField(18);
        JTextField phoneField = new JTextField(18);
        JTextField emailField = new JTextField(18);
        JTextField ageField = new JTextField(6);
        JComboBox<String> genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other", "Prefer not to say"});
        JComboBox<String> planCombo = new JComboBox<>(new String[]{"Monthly", "Quarterly", "Yearly"});
        JTextField joinDateField = new JTextField(LocalDate.now().toString(), 12);
        JLabel expiryPreview = new JLabel();
        expiryPreview.setForeground(UITheme.CYAN_ACCENT);
        expiryPreview.setFont(UITheme.FONT_BOLD);
        JTextField addressField = new JTextField(18);
        JTextField emergencyField = new JTextField(18);

        JCheckBox recordPaymentCheck = new JCheckBox("Record membership payment immediately", true);
        recordPaymentCheck.setOpaque(false);
        recordPaymentCheck.setForeground(UITheme.TEXT_PRIMARY);
        recordPaymentCheck.setFont(UITheme.FONT_SMALL);

        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"UPI", "Cash", "Card", "Bank Transfer"});
        JLabel amountLabel = new JLabel();
        amountLabel.setFont(UITheme.FONT_BOLD);
        amountLabel.setForeground(UITheme.SUCCESS_COLOR);

        UITheme.styleTextField(nameField);
        UITheme.styleTextField(phoneField);
        UITheme.styleTextField(emailField);
        UITheme.styleTextField(ageField);
        UITheme.styleComboBox(genderCombo);
        UITheme.styleComboBox(planCombo);
        UITheme.styleTextField(joinDateField);
        UITheme.styleTextField(addressField);
        UITheme.styleTextField(emergencyField);
        UITheme.styleComboBox(methodCombo);

        Runnable updatePreviews = () -> {
            try {
                LocalDate jd = LocalDate.parse(joinDateField.getText().trim());
                String p = (String) planCombo.getSelectedItem();
                LocalDate exp = Member.calculateExpiryDate(jd, p);
                expiryPreview.setText(exp.toString());
                double price = Database.getPlanPrice(p);
                amountLabel.setText(String.format("₹ %.2f", price));
            } catch (Exception e) {
                expiryPreview.setText("Invalid date");
                amountLabel.setText("—");
            }
        };

        joinDateField.addActionListener(e -> updatePreviews.run());
        planCombo.addActionListener(e -> updatePreviews.run());
        updatePreviews.run();

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addFormRow(form, gbc, row++, "Full Name *:", nameField);
        addFormRow(form, gbc, row++, "Phone (10 digits) *:", phoneField);
        addFormRow(form, gbc, row++, "Email Address:", emailField);
        addFormRow(form, gbc, row++, "Age:", ageField);
        addFormRow(form, gbc, row++, "Gender:", genderCombo);
        addFormRow(form, gbc, row++, "Membership Plan *:", planCombo);
        addFormRow(form, gbc, row++, "Join Date (YYYY-MM-DD) *:", joinDateField);
        addFormRow(form, gbc, row++, "Calculated Expiry:", expiryPreview);
        addFormRow(form, gbc, row++, "Residential Address:", addressField);
        addFormRow(form, gbc, row++, "Emergency Contact:", emergencyField);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        form.add(Box.createVerticalStrut(8), gbc);
        form.add(recordPaymentCheck, gbc);

        JPanel payRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        payRow.setOpaque(false);
        payRow.add(new JLabel("Amount:"));
        payRow.add(amountLabel);
        payRow.add(new JLabel("Method:"));
        payRow.add(methodCombo);

        gbc.gridy = row++;
        form.add(payRow, gbc);

        int res = JOptionPane.showConfirmDialog(this, form, "Register New Member — ENCORE Pro",
                                                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String gender = (String) genderCombo.getSelectedItem();
        String plan = (String) planCombo.getSelectedItem();
        String address = addressField.getText().trim();
        String emergency = emergencyField.getText().trim();

        Integer age = null;
        if (!ageField.getText().trim().isEmpty()) {
            try {
                age = Integer.parseInt(ageField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Age must be a valid number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        LocalDate joinDate;
        try {
            joinDate = LocalDate.parse(joinDateField.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Join date must be in YYYY-MM-DD format.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> errors = Member.validate(name, phone, email, joinDate, plan);
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, String.join("\n", errors), "Validation Errors", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate expiryDate = Member.calculateExpiryDate(joinDate, plan);

        try {
            int newMemberId = Database.addMember(name, phone, email, age, gender, plan,
                                                 joinDate.toString(), expiryDate.toString(), address, emergency);

            if (recordPaymentCheck.isSelected() && newMemberId > 0) {
                double price = Database.getPlanPrice(plan);
                String method = (String) methodCombo.getSelectedItem();
                String txn = Database.recordPayment(newMemberId, plan, price, method, "Initial membership registration");
                JOptionPane.showMessageDialog(this,
                    "Member registered successfully with ID: " + String.format("ENC-%04d", newMemberId) +
                    "\nPayment recorded: " + txn,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Member registered successfully with ID: " + String.format("ENC-%04d", newMemberId),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            filter();
            onDataChanged.run();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database registration failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showEditMemberDialog() {
        int id = getSelectedMemberId();
        if (id < 0) return;

        try {
            Member m = Database.getMemberById(id);
            if (m == null) return;

            JTextField nameField = new JTextField(m.getName(), 18);
            JTextField phoneField = new JTextField(m.getPhone(), 18);
            JTextField emailField = new JTextField(m.getEmail(), 18);
            JTextField ageField = new JTextField(m.getAge() != null ? String.valueOf(m.getAge()) : "", 6);
            JComboBox<String> genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other", "Prefer not to say"});
            genderCombo.setSelectedItem(m.getGender());
            JComboBox<String> planCombo = new JComboBox<>(new String[]{"Monthly", "Quarterly", "Yearly"});
            planCombo.setSelectedItem(m.getPlan());
            JTextField joinDateField = new JTextField(m.getJoinDate().toString(), 12);
            JTextField expiryDateField = new JTextField(m.getExpiryDate().toString(), 12);
            JTextField addressField = new JTextField(m.getAddress(), 18);
            JTextField emergencyField = new JTextField(m.getEmergencyContact(), 18);

            UITheme.styleTextField(nameField);
            UITheme.styleTextField(phoneField);
            UITheme.styleTextField(emailField);
            UITheme.styleTextField(ageField);
            UITheme.styleComboBox(genderCombo);
            UITheme.styleComboBox(planCombo);
            UITheme.styleTextField(joinDateField);
            UITheme.styleTextField(expiryDateField);
            UITheme.styleTextField(addressField);
            UITheme.styleTextField(emergencyField);

            JPanel form = new JPanel(new GridBagLayout());
            form.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 6, 4, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;
            addFormRow(form, gbc, row++, "Full Name *:", nameField);
            addFormRow(form, gbc, row++, "Phone *:", phoneField);
            addFormRow(form, gbc, row++, "Email:", emailField);
            addFormRow(form, gbc, row++, "Age:", ageField);
            addFormRow(form, gbc, row++, "Gender:", genderCombo);
            addFormRow(form, gbc, row++, "Plan *:", planCombo);
            addFormRow(form, gbc, row++, "Join Date (YYYY-MM-DD):", joinDateField);
            addFormRow(form, gbc, row++, "Expiry Date (YYYY-MM-DD):", expiryDateField);
            addFormRow(form, gbc, row++, "Address:", addressField);
            addFormRow(form, gbc, row++, "Emergency Contact:", emergencyField);

            int res = JOptionPane.showConfirmDialog(this, form, "Edit Member Details — " + m.getFormattedId(),
                                                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;

            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String gender = (String) genderCombo.getSelectedItem();
            String plan = (String) planCombo.getSelectedItem();
            String address = addressField.getText().trim();
            String emergency = emergencyField.getText().trim();

            Integer age = null;
            if (!ageField.getText().trim().isEmpty()) {
                age = Integer.parseInt(ageField.getText().trim());
            }

            LocalDate jd = LocalDate.parse(joinDateField.getText().trim());
            LocalDate ed = LocalDate.parse(expiryDateField.getText().trim());

            List<String> errors = Member.validate(name, phone, email, jd, plan);
            if (!errors.isEmpty()) {
                JOptionPane.showMessageDialog(this, String.join("\n", errors), "Validation Errors", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Database.updateMember(id, name, phone, email, age, gender, plan, jd.toString(), ed.toString(), address, emergency);
            JOptionPane.showMessageDialog(this, "Member updated successfully.", "Updated", JOptionPane.INFORMATION_MESSAGE);
            filter();
            onDataChanged.run();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to update member: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showQuickRenewDialog() {
        int id = getSelectedMemberId();
        if (id < 0) return;
        try {
            Member m = Database.getMemberById(id);
            if (m == null) return;

            JComboBox<String> planCombo = new JComboBox<>(new String[]{"Monthly", "Quarterly", "Yearly"});
            planCombo.setSelectedItem(m.getPlan());
            JLabel oldExpiryLabel = new JLabel(m.getExpiryDate().toString());
            oldExpiryLabel.setFont(UITheme.FONT_BOLD);
            oldExpiryLabel.setForeground(UITheme.TEXT_SECONDARY);

            JLabel newExpiryPreview = new JLabel();
            newExpiryPreview.setFont(UITheme.FONT_BOLD);
            newExpiryPreview.setForeground(UITheme.CYAN_ACCENT);

            JLabel priceLabel = new JLabel();
            priceLabel.setFont(UITheme.FONT_BOLD);
            priceLabel.setForeground(UITheme.SUCCESS_COLOR);

            JComboBox<String> methodCombo = new JComboBox<>(new String[]{"UPI", "Cash", "Card", "Bank Transfer"});

            UITheme.styleComboBox(planCombo);
            UITheme.styleComboBox(methodCombo);

            Runnable update = () -> {
                String p = (String) planCombo.getSelectedItem();
                LocalDate nexp = Member.calculateRenewalDate(m.getExpiryDate(), p);
                newExpiryPreview.setText(nexp.toString());
                double price = Database.getPlanPrice(p);
                priceLabel.setText(String.format("₹ %.2f", price));
            };

            planCombo.addActionListener(e -> update.run());
            update.run();

            JPanel form = new JPanel(new GridLayout(5, 2, 8, 10));
            form.setOpaque(false);
            form.add(new JLabel("Member:"));
            form.add(new JLabel(m.getName() + " (" + m.getFormattedId() + ")"));
            form.add(new JLabel("Renewal Plan:"));
            form.add(planCombo);
            form.add(new JLabel("Current Expiry:"));
            form.add(oldExpiryLabel);
            form.add(new JLabel("New Expiry Date:"));
            form.add(newExpiryPreview);
            form.add(new JLabel("Payment (Amount & Method):"));
            JPanel pay = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            pay.setOpaque(false);
            pay.add(priceLabel);
            pay.add(methodCombo);
            form.add(pay);

            int res = JOptionPane.showConfirmDialog(this, form, "Quick Renewal — " + m.getName(),
                                                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;

            String selectedPlan = (String) planCombo.getSelectedItem();
            LocalDate newExp = Member.calculateRenewalDate(m.getExpiryDate(), selectedPlan);
            double amount = Database.getPlanPrice(selectedPlan);
            String method = (String) methodCombo.getSelectedItem();

            Database.renewMemberWithRecord(m.getId(), selectedPlan, newExp.toString(), amount, method);
            JOptionPane.showMessageDialog(this, "Membership renewed until " + newExp + "!\nPayment of ₹" + amount + " logged.",
                                          "Renewal Success", JOptionPane.INFORMATION_MESSAGE);
            filter();
            onDataChanged.run();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Renewal failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedMember() {
        int id = getSelectedMemberId();
        if (id < 0) return;

        try {
            Member m = Database.getMemberById(id);
            if (m == null) return;

            int res = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete member: " + m.getName() + " (" + m.getFormattedId() + ")?\n" +
                "This will also delete related attendance and payment records permanently.",
                "Confirm Permanent Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (res == JOptionPane.YES_OPTION) {
                Database.deleteMember(id);
                JOptionPane.showMessageDialog(this, "Member deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                filter();
                onDataChanged.run();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to delete member: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(l, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(comp, gbc);
    }
}

