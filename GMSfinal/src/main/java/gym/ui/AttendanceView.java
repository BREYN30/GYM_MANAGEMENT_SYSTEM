package gym.ui;

import gym.Database;
import gym.Member;
import gym.UITheme;
import gym.model.AttendanceRecord;
import gym.util.CsvExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class AttendanceView extends JPanel {
    private final Runnable onDataChanged;

    private final JComboBox<Member> checkInMemberCombo = new JComboBox<>();
    private final JComboBox<String> workoutTypeCombo = new JComboBox<>(new String[]{
        "Regular Workout", "Cardio Session", "Strength Training", "Leg Day", "HIIT & Functional", "Personal Training"
    });
    private final JLabel todayCountLabel = new JLabel("0");

    private final JTextField searchField = new JTextField(12);
    private final JComboBox<String> dateFilterCombo = new JComboBox<>(new String[]{"Today", "Yesterday", "Last 7 Days", "All Time"});

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[]{"Log ID", "Member ID", "Member Name", "Phone", "Date", "Check-in Time", "Workout / Notes"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public AttendanceView(Runnable onDataChanged) {
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
        JLabel title = new JLabel("Attendance Management");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Live check-in verification, duplicate prevention, and member visit logs");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_SECONDARY);

        JPanel tp = new JPanel();
        tp.setOpaque(false);
        tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
        tp.add(title);
        tp.add(Box.createVerticalStrut(4));
        tp.add(sub);
        titlePanel.add(tp, BorderLayout.WEST);

        // Today's Check-ins Counter
        JPanel countPill = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        countPill.setBackground(UITheme.BG_CARD_ELEVATED);
        countPill.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1, true));
        JLabel countDesc = new JLabel("Today's Visits:");
        countDesc.setFont(UITheme.FONT_SMALL);
        countDesc.setForeground(UITheme.TEXT_SECONDARY);
        todayCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        todayCountLabel.setForeground(UITheme.CYAN_ACCENT);
        countPill.add(countDesc);
        countPill.add(todayCountLabel);
        titlePanel.add(countPill, BorderLayout.EAST);

        top.add(titlePanel);
        top.add(Box.createVerticalStrut(14));

        // Quick Check-in Card
        JPanel checkInCard = UITheme.createCardPanel();
        checkInCard.setLayout(new BorderLayout(0, 10));

        JLabel ciTitle = new JLabel("Fast Member Check-In");
        ciTitle.setFont(UITheme.FONT_HEADING);
        ciTitle.setForeground(UITheme.CYAN_ACCENT);
        checkInCard.add(ciTitle, BorderLayout.NORTH);

        UITheme.styleComboBox(checkInMemberCombo);
        UITheme.styleComboBox(workoutTypeCombo);
        checkInMemberCombo.setPreferredSize(new Dimension(280, 34));
        workoutTypeCombo.setPreferredSize(new Dimension(180, 34));

        JPanel ciForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        ciForm.setOpaque(false);

        JLabel selLbl = new JLabel("Select Member:");
        selLbl.setFont(UITheme.FONT_SMALL);
        selLbl.setForeground(UITheme.TEXT_SECONDARY);

        JLabel typeLbl = new JLabel("Workout Note:");
        typeLbl.setFont(UITheme.FONT_SMALL);
        typeLbl.setForeground(UITheme.TEXT_SECONDARY);

        JButton checkInBtn = UITheme.createPrimaryButton("✓ Record Check-in");
        checkInBtn.addActionListener(e -> recordCheckIn());

        ciForm.add(selLbl);
        ciForm.add(checkInMemberCombo);
        ciForm.add(typeLbl);
        ciForm.add(workoutTypeCombo);
        ciForm.add(checkInBtn);

        checkInCard.add(ciForm, BorderLayout.CENTER);
        top.add(checkInCard);
        top.add(Box.createVerticalStrut(14));

        // Filter Bar & CSV Export
        JPanel filterRow = new JPanel(new BorderLayout());
        filterRow.setOpaque(false);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);

        UITheme.styleTextField(searchField);
        UITheme.styleComboBox(dateFilterCombo);

        searchField.setPreferredSize(new Dimension(150, 34));
        searchField.setToolTipText("Filter by Member Name, Phone, or ID");

        JButton searchBtn = UITheme.createSecondaryButton("Filter");
        searchBtn.addActionListener(e -> loadAttendanceData());
        searchField.addActionListener(e -> loadAttendanceData());
        dateFilterCombo.addActionListener(e -> loadAttendanceData());

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            dateFilterCombo.setSelectedIndex(0);
            loadAttendanceData();
        });

        filters.add(new JLabel("Search:"));
        filters.add(searchField);
        filters.add(new JLabel("Date Range:"));
        filters.add(dateFilterCombo);
        filters.add(searchBtn);
        filters.add(resetBtn);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);

        JButton deleteBtn = UITheme.createDangerButton("Delete Entry");
        deleteBtn.addActionListener(e -> deleteSelectedAttendance());

        JButton exportBtn = UITheme.createSecondaryButton("Export CSV");
        exportBtn.addActionListener(e -> CsvExporter.exportTableToCsv(this, tableModel, "encore_attendance"));

        rightBtns.add(deleteBtn);
        rightBtns.add(exportBtn);

        filterRow.add(filters, BorderLayout.WEST);
        filterRow.add(rightBtns, BorderLayout.EAST);
        top.add(filterRow);

        add(top, BorderLayout.NORTH);

        // Table
        UITheme.styleTable(table, 32);
        add(UITheme.createScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshData() {
        // Reload member combo
        int selId = -1;
        Member prev = (Member) checkInMemberCombo.getSelectedItem();
        if (prev != null) selId = prev.getId();

        checkInMemberCombo.removeAllItems();
        try {
            List<Member> all = Database.getAllMembers();
            for (Member m : all) {
                checkInMemberCombo.addItem(m);
            }
            for (int i = 0; i < checkInMemberCombo.getItemCount(); i++) {
                if (checkInMemberCombo.getItemAt(i).getId() == selId) {
                    checkInMemberCombo.setSelectedIndex(i);
                    break;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load members: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        loadAttendanceData();
    }

    private void loadAttendanceData() {
        tableModel.setRowCount(0);
        String q = searchField.getText();
        String dateOption = (String) dateFilterCombo.getSelectedItem();

        LocalDate today = LocalDate.now();
        LocalDate startDate = null;
        LocalDate endDate = null;

        if ("Today".equalsIgnoreCase(dateOption)) {
            startDate = today;
            endDate = today;
        } else if ("Yesterday".equalsIgnoreCase(dateOption)) {
            startDate = today.minusDays(1);
            endDate = today.minusDays(1);
        } else if ("Last 7 Days".equalsIgnoreCase(dateOption)) {
            startDate = today.minusDays(6);
            endDate = today;
        }

        try {
            List<AttendanceRecord> records = Database.getAttendanceFiltered(q, startDate, endDate);
            for (AttendanceRecord a : records) {
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

            int countToday = Database.getTodayAttendanceCount();
            todayCountLabel.setText(String.valueOf(countToday));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load attendance logs: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recordCheckIn() {
        Member m = (Member) checkInMemberCombo.getSelectedItem();
        if (m == null) {
            JOptionPane.showMessageDialog(this, "Please select a member to check in.", "Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Warn if membership is expired
        if ("Expired".equalsIgnoreCase(m.getStatus())) {
            int cont = JOptionPane.showConfirmDialog(
                this,
                "Warning: " + m.getName() + "'s membership expired on " + m.getExpiryDate() + ".\n" +
                "Proceed with check-in anyway?",
                "Expired Membership Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (cont != JOptionPane.YES_OPTION) return;
        }

        String note = (String) workoutTypeCombo.getSelectedItem();

        try {
            if (Database.isAlreadyCheckedInToday(m.getId())) {
                JOptionPane.showMessageDialog(
                    this,
                    m.getName() + " has ALREADY checked in for today.\nDuplicate check-in prevented.",
                    "Duplicate Attendance Prevented",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int id = Database.recordAttendance(m.getId(), note);
            JOptionPane.showMessageDialog(
                this,
                "✓ Check-in recorded for " + m.getName() + " (" + m.getFormattedId() + ")!",
                "Check-in Success",
                JOptionPane.INFORMATION_MESSAGE
            );

            loadAttendanceData();
            onDataChanged.run();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Attendance error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedAttendance() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select an attendance entry to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int mr = table.convertRowIndexToModel(r);
        String idStr = String.valueOf(tableModel.getValueAt(mr, 0)).replace("#", "");
        int id = Integer.parseInt(idStr);
        String name = String.valueOf(tableModel.getValueAt(mr, 2));

        int res = JOptionPane.showConfirmDialog(
            this,
            "Delete attendance log #" + id + " for " + name + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (res == JOptionPane.YES_OPTION) {
            try {
                Database.deleteAttendance(id);
                loadAttendanceData();
                onDataChanged.run();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

