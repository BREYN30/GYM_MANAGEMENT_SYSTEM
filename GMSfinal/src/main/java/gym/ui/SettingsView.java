package gym.ui;

import gym.Database;
import gym.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SettingsView extends JPanel {
    private final Runnable onSettingsChanged;

    private final JTextField monthlyPriceField = new JTextField(10);
    private final JTextField quarterlyPriceField = new JTextField(10);
    private final JTextField yearlyPriceField = new JTextField(10);

    private final JTextField gymNameField = new JTextField(20);
    private final JTextField gymSubtitleField = new JTextField(20);
    private final JTextField currencyField = new JTextField(6);

    private final JLabel dbPathLabel = new JLabel();
    private final JLabel dbSizeLabel = new JLabel();
    private final JLabel dbStatusLabel = new JLabel("● Database Active & Verified");

    public SettingsView(Runnable onSettingsChanged) {
        this.onSettingsChanged = onSettingsChanged;

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
        JLabel title = new JLabel("System Settings & Database Maintenance");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Configure membership plan pricing, business branding, and execute SQLite backups");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(sub);

        scrollContent.add(titlePanel);
        scrollContent.add(Box.createVerticalStrut(18));

        // Two Cards Side by Side: Pricing & Gym Profile
        JPanel topCards = new JPanel(new GridLayout(1, 2, 16, 0));
        topCards.setOpaque(false);

        // 1. Membership Pricing Card
        JPanel priceCard = UITheme.createCardPanel();
        priceCard.setLayout(new BorderLayout(0, 12));

        JLabel pcTitle = new JLabel("Membership Plan Pricing (₹)");
        pcTitle.setFont(UITheme.FONT_HEADING);
        pcTitle.setForeground(UITheme.CYAN_ACCENT);
        priceCard.add(pcTitle, BorderLayout.NORTH);

        UITheme.styleTextField(monthlyPriceField);
        UITheme.styleTextField(quarterlyPriceField);
        UITheme.styleTextField(yearlyPriceField);

        JPanel pForm = new JPanel(new GridLayout(3, 2, 8, 12));
        pForm.setOpaque(false);
        pForm.add(createLabel("Monthly Plan Fee:"));
        pForm.add(monthlyPriceField);
        pForm.add(createLabel("Quarterly Plan Fee:"));
        pForm.add(quarterlyPriceField);
        pForm.add(createLabel("Yearly Plan Fee:"));
        pForm.add(yearlyPriceField);
        priceCard.add(pForm, BorderLayout.CENTER);

        JButton savePricesBtn = UITheme.createPrimaryButton("Save Plan Prices");
        savePricesBtn.addActionListener(e -> savePrices());
        JPanel priceBtnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        priceBtnRow.setOpaque(false);
        priceBtnRow.add(savePricesBtn);
        priceCard.add(priceBtnRow, BorderLayout.SOUTH);

        // 2. Gym Profile Card
        JPanel profileCard = UITheme.createCardPanel();
        profileCard.setLayout(new BorderLayout(0, 12));

        JLabel prTitle = new JLabel("Gym Brand Identity & Currency");
        prTitle.setFont(UITheme.FONT_HEADING);
        prTitle.setForeground(UITheme.VIOLET_SECONDARY);
        profileCard.add(prTitle, BorderLayout.NORTH);

        UITheme.styleTextField(gymNameField);
        UITheme.styleTextField(gymSubtitleField);
        UITheme.styleTextField(currencyField);

        JPanel prForm = new JPanel(new GridLayout(3, 2, 8, 12));
        prForm.setOpaque(false);
        prForm.add(createLabel("Gym Brand Name:"));
        prForm.add(gymNameField);
        prForm.add(createLabel("Subtitle / Tagline:"));
        prForm.add(gymSubtitleField);
        prForm.add(createLabel("Currency Symbol:"));
        prForm.add(currencyField);
        profileCard.add(prForm, BorderLayout.CENTER);

        JButton saveProfileBtn = UITheme.createPrimaryButton("Save Brand Info");
        saveProfileBtn.addActionListener(e -> saveProfile());
        JPanel profileBtnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        profileBtnRow.setOpaque(false);
        profileBtnRow.add(saveProfileBtn);
        profileCard.add(profileBtnRow, BorderLayout.SOUTH);

        topCards.add(priceCard);
        topCards.add(profileCard);
        scrollContent.add(topCards);
        scrollContent.add(Box.createVerticalStrut(18));

        // 3. Database Tools & Storage Maintenance Card
        JPanel dbCard = UITheme.createCardPanel();
        dbCard.setLayout(new BorderLayout(0, 14));

        JLabel dbTitle = new JLabel("SQLite Database Management & Safety");
        dbTitle.setFont(UITheme.FONT_HEADING);
        dbTitle.setForeground(UITheme.TEXT_PRIMARY);
        dbCard.add(dbTitle, BorderLayout.NORTH);

        dbStatusLabel.setFont(UITheme.FONT_BOLD);
        dbStatusLabel.setForeground(UITheme.SUCCESS_COLOR);

        dbPathLabel.setFont(UITheme.FONT_REGULAR);
        dbPathLabel.setForeground(UITheme.TEXT_SECONDARY);

        dbSizeLabel.setFont(UITheme.FONT_REGULAR);
        dbSizeLabel.setForeground(UITheme.TEXT_PRIMARY);

        JPanel dbInfo = new JPanel(new GridLayout(3, 2, 8, 8));
        dbInfo.setOpaque(false);
        dbInfo.add(createLabel("Database Engine Status:"));
        dbInfo.add(dbStatusLabel);
        dbInfo.add(createLabel("Storage File Location:"));
        dbInfo.add(dbPathLabel);
        dbInfo.add(createLabel("Database File Size:"));
        dbInfo.add(dbSizeLabel);
        dbCard.add(dbInfo, BorderLayout.CENTER);

        JPanel dbActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        dbActions.setOpaque(false);

        JButton backupBtn = UITheme.createPrimaryButton("Create Database Backup (.bak)");
        backupBtn.addActionListener(e -> performBackup());

        JButton verifyBtn = UITheme.createSecondaryButton("Run Integrity Check (PRAGMA)");
        verifyBtn.addActionListener(e -> verifyDatabase());

        dbActions.add(backupBtn);
        dbActions.add(verifyBtn);
        dbCard.add(dbActions, BorderLayout.SOUTH);

        scrollContent.add(dbCard);

        JScrollPane mainScroll = UITheme.createScrollPane(scrollContent);
        mainScroll.setBorder(null);
        add(mainScroll, BorderLayout.CENTER);
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    public void refreshData() {
        monthlyPriceField.setText(String.format("%.0f", Database.getPlanPrice("Monthly")));
        quarterlyPriceField.setText(String.format("%.0f", Database.getPlanPrice("Quarterly")));
        yearlyPriceField.setText(String.format("%.0f", Database.getPlanPrice("Yearly")));

        gymNameField.setText(Database.getSetting("gym_name", "ENCORE FITNESS CLUB"));
        gymSubtitleField.setText(Database.getSetting("gym_subtitle", "PREMIUM HEALTH & WELLNESS"));
        currencyField.setText(Database.getSetting("currency_symbol", "₹"));

        Path p = Database.getDatabasePath();
        dbPathLabel.setText(p.toString());

        File f = p.toFile();
        if (f.exists()) {
            dbSizeLabel.setText(String.format("%.2f KB (%d bytes)", f.length() / 1024.0, f.length()));
        } else {
            dbSizeLabel.setText("File not found on disk");
        }
    }

    private void savePrices() {
        try {
            double mo = Double.parseDouble(monthlyPriceField.getText().trim());
            double qu = Double.parseDouble(quarterlyPriceField.getText().trim());
            double ye = Double.parseDouble(yearlyPriceField.getText().trim());

            if (mo <= 0 || qu <= 0 || ye <= 0) throw new NumberFormatException();

            Database.setSetting("plan_monthly_price", String.valueOf(mo));
            Database.setSetting("plan_quarterly_price", String.valueOf(qu));
            Database.setSetting("plan_yearly_price", String.valueOf(ye));

            JOptionPane.showMessageDialog(this, "Plan prices updated successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
            if (onSettingsChanged != null) onSettingsChanged.run();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric plan prices.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveProfile() {
        try {
            String name = gymNameField.getText().trim();
            String sub = gymSubtitleField.getText().trim();
            String curr = currencyField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Gym name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Database.setSetting("gym_name", name);
            Database.setSetting("gym_subtitle", sub);
            Database.setSetting("currency_symbol", curr.isEmpty() ? "₹" : curr);

            JOptionPane.showMessageDialog(this, "Brand settings updated.", "Saved", JOptionPane.INFORMATION_MESSAGE);
            if (onSettingsChanged != null) onSettingsChanged.run();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to save settings: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performBackup() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path dest = Path.of("data", "gym_backup_" + ts + ".db");
        boolean ok = Database.backupDatabase(dest);
        if (ok) {
            JOptionPane.showMessageDialog(
                this,
                "Database backup created successfully:\n" + dest.toAbsolutePath(),
                "Backup Successful",
                JOptionPane.INFORMATION_MESSAGE
            );
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Backup operation failed.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verifyDatabase() {
        try (Connection c = Database.connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA integrity_check;")) {
            if (r.next()) {
                String result = r.getString(1);
                if ("ok".equalsIgnoreCase(result)) {
                    JOptionPane.showMessageDialog(this, "SQLite Integrity Check Result: OK\nAll database tables and B-Trees are healthy.", "Integrity Check Passed", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Integrity Check Warning: " + result, "Integrity Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to run integrity check: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

