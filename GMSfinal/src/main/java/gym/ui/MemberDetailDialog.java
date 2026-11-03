package gym.ui;

import gym.Database;
import gym.Member;
import gym.UITheme;
import gym.model.AttendanceRecord;
import gym.model.PaymentRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Detailed profile modal for a gym member displaying personal info, status,
 * attendance activity, and payment history.
 */
public class MemberDetailDialog extends JDialog {
    private final Member member;
    private final Runnable onDataChanged;

    public MemberDetailDialog(Window owner, Member member, Runnable onDataChanged) {
        super(owner, "Member Profile — " + member.getName(), ModalityType.APPLICATION_MODAL);
        this.member = member;
        this.onDataChanged = onDataChanged;

        setSize(780, 620);
        setMinimumSize(new Dimension(680, 520));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UITheme.BG_PRIMARY);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_SIDEBAR);
        p.setBorder(new EmptyBorder(18, 24, 18, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel idLabel = new JLabel(member.getFormattedId());
        idLabel.setFont(UITheme.FONT_SMALL);
        idLabel.setForeground(UITheme.CYAN_ACCENT);

        JLabel nameLabel = new JLabel(member.getName());
        nameLabel.setFont(UITheme.FONT_TITLE);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);

        left.add(idLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(nameLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        right.setOpaque(false);
        right.add(UITheme.createStatusBadge(member.getStatus()));

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildContent() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BOLD);
        tabs.setForeground(UITheme.TEXT_PRIMARY);
        tabs.setBackground(UITheme.BG_SIDEBAR);

        tabs.addTab("Profile & Membership", buildOverviewTab());
        tabs.addTab("Attendance History", buildAttendanceTab());
        tabs.addTab("Payment History", buildPaymentsTab());

        p.add(tabs, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildOverviewTab() {
        JPanel p = new JPanel(new GridLayout(1, 2, 16, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 4, 12, 4));

        // Personal Information Card
        JPanel personal = UITheme.createCardPanel();
        personal.setLayout(new BoxLayout(personal, BoxLayout.Y_AXIS));
        JLabel pt = new JLabel("Personal Information");
        pt.setFont(UITheme.FONT_HEADING);
        pt.setForeground(UITheme.CYAN_ACCENT);
        personal.add(pt);
        personal.add(Box.createVerticalStrut(14));

        addInfoRow(personal, "Phone Number", member.getPhone());
        addInfoRow(personal, "Email Address", member.getEmail().isEmpty() ? "Not provided" : member.getEmail());
        addInfoRow(personal, "Age", member.getAgeDisplay());
        addInfoRow(personal, "Gender", member.getGender());
        addInfoRow(personal, "Residential Address", member.getAddress().isEmpty() ? "Not provided" : member.getAddress());
        addInfoRow(personal, "Emergency Contact", member.getEmergencyContact().isEmpty() ? "Not provided" : member.getEmergencyContact());

        // Membership Plan Card
        JPanel membership = UITheme.createCardPanel();
        membership.setLayout(new BoxLayout(membership, BoxLayout.Y_AXIS));
        JLabel mt = new JLabel("Membership Details");
        mt.setFont(UITheme.FONT_HEADING);
        mt.setForeground(UITheme.VIOLET_SECONDARY);
        membership.add(mt);
        membership.add(Box.createVerticalStrut(14));

        addInfoRow(membership, "Current Plan", member.getPlan());
        addInfoRow(membership, "Join Date", member.getJoinDate().toString());
        addInfoRow(membership, "Expiry Date", member.getExpiryDate().toString());

        long days = member.getDaysRemaining();
        String daysStr = days < 0 ? Math.abs(days) + " days ago (Expired)" : days + " days remaining";
        addInfoRow(membership, "Validity", daysStr);
        addInfoRow(membership, "Status", member.getStatus());

        p.add(personal);
        p.add(membership);
        return p;
    }

    private JPanel buildAttendanceTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 4, 12, 4));

        DefaultTableModel m = new DefaultTableModel(new String[]{"Check-in Date", "Check-in Time", "Workout / Notes"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(m);
        UITheme.styleTable(table, 32);

        try {
            List<AttendanceRecord> records = Database.getAttendanceHistory(member.getId());
            for (AttendanceRecord a : records) {
                m.addRow(new Object[]{a.checkInDate(), a.checkInTime(), a.notes()});
            }
        } catch (SQLException e) {
            m.addRow(new Object[]{"Error loading", e.getMessage(), ""});
        }

        p.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildPaymentsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 4, 12, 4));

        DefaultTableModel m = new DefaultTableModel(new String[]{"Txn Ref", "Plan", "Amount", "Method", "Date", "Notes"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(m);
        UITheme.styleTable(table, 32);

        try {
            List<PaymentRecord> records = Database.getPaymentsForMember(member.getId());
            for (PaymentRecord pay : records) {
                m.addRow(new Object[]{
                    pay.txnRef(),
                    pay.plan(),
                    String.format("₹%.2f", pay.amount()),
                    pay.paymentMethod(),
                    pay.paymentDate(),
                    pay.notes()
                });
            }
        } catch (SQLException e) {
            m.addRow(new Object[]{"Error loading", e.getMessage(), "", "", "", ""});
        }

        p.add(UITheme.createScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void addInfoRow(JPanel p, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(4, 0, 4, 0));

        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_SECONDARY);

        JLabel v = new JLabel(value);
        v.setFont(UITheme.FONT_BOLD);
        v.setForeground(UITheme.TEXT_PRIMARY);

        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        p.add(row);
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        p.setBackground(UITheme.BG_SIDEBAR);

        JButton checkinBtn = UITheme.createSecondaryButton("Check-in Today");
        checkinBtn.addActionListener(e -> {
            try {
                if (Database.isAlreadyCheckedInToday(member.getId())) {
                    JOptionPane.showMessageDialog(this, member.getName() + " has already checked in today.", "Attendance Check-In", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    Database.recordAttendance(member.getId(), "Manual Check-In");
                    JOptionPane.showMessageDialog(this, "Attendance recorded for " + member.getName() + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    if (onDataChanged != null) onDataChanged.run();
                    dispose();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Check-in failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton closeBtn = UITheme.createPrimaryButton("Close");
        closeBtn.addActionListener(e -> dispose());

        p.add(checkinBtn);
        p.add(closeBtn);
        return p;
    }
}

