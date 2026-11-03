package gym.ui;

import gym.Database;
import gym.UITheme;
import gym.model.PaymentRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PaymentReceiptDialog extends JDialog {
    public PaymentReceiptDialog(Window owner, PaymentRecord record) {
        super(owner, "Payment Receipt — " + record.txnRef(), ModalityType.APPLICATION_MODAL);
        setSize(480, 560);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UITheme.BG_PRIMARY);
        setLayout(new BorderLayout());

        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(UITheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(20, 20, 20, 20),
            new LineBorder(UITheme.BORDER_COLOR, 1, true)
        ));

        // Header
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel gymTitle = new JLabel(Database.getSetting("gym_name", "ENCORE FITNESS CLUB"));
        gymTitle.setFont(UITheme.FONT_TITLE);
        gymTitle.setForeground(UITheme.CYAN_ACCENT);
        gymTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel receiptTitle = new JLabel("OFFICIAL PAYMENT RECEIPT");
        receiptTitle.setFont(UITheme.FONT_SMALL);
        receiptTitle.setForeground(UITheme.TEXT_SECONDARY);
        receiptTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(gymTitle);
        header.add(Box.createVerticalStrut(4));
        header.add(receiptTitle);
        header.add(Box.createVerticalStrut(12));

        // Body with key-value rows
        JPanel body = new JPanel(new GridLayout(7, 1, 0, 8));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 14, 10, 14));

        body.add(row("Transaction Reference:", record.txnRef()));
        body.add(row("Member ID:", String.format("ENC-%04d", record.memberId())));
        body.add(row("Member Name:", record.memberName()));
        body.add(row("Membership Plan:", record.plan()));
        body.add(row("Payment Method:", record.paymentMethod()));
        body.add(row("Transaction Date:", record.paymentDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))));
        body.add(row("Notes / Reference:", record.notes().isEmpty() ? "—" : record.notes()));

        // Amount Box
        JPanel amountBox = new JPanel(new BorderLayout());
        amountBox.setBackground(UITheme.BG_CARD_ELEVATED);
        amountBox.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel amtLabel = new JLabel("TOTAL AMOUNT PAID");
        amtLabel.setFont(UITheme.FONT_SMALL);
        amtLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel amtValue = new JLabel(String.format("₹ %.2f", record.amount()));
        amtValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        amtValue.setForeground(UITheme.SUCCESS_COLOR);

        amountBox.add(amtLabel, BorderLayout.WEST);
        amountBox.add(amtValue, BorderLayout.EAST);

        // Footer buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        JButton close = UITheme.createPrimaryButton("Done");
        close.addActionListener(e -> dispose());
        footer.add(close);

        card.add(header, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0, 12));
        mid.setOpaque(false);
        mid.add(body, BorderLayout.CENTER);
        mid.add(amountBox, BorderLayout.SOUTH);
        card.add(mid, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    private JPanel row(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_SECONDARY);

        JLabel v = new JLabel(value);
        v.setFont(UITheme.FONT_BOLD);
        v.setForeground(UITheme.TEXT_PRIMARY);

        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }
}

