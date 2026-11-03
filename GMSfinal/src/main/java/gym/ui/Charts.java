package gym.ui;

import gym.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Modern custom-painted Java 2D vector charts for ENCORE Pro.
 * Provides high-DPI anti-aliased rendering matching the Midnight Violet theme.
 */
public final class Charts {
    private Charts() {}

    /**
     * Donut chart for membership plan distribution with center metric and clean legend.
     */
    public static class DonutChartPanel extends JPanel {
        private int monthly;
        private int quarterly;
        private int yearly;

        public DonutChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(280, 200));
        }

        public void setData(int monthly, int quarterly, int yearly) {
            this.monthly = monthly;
            this.quarterly = quarterly;
            this.yearly = yearly;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int total = monthly + quarterly + yearly;
            int width = getWidth();
            int height = getHeight();

            if (total == 0) {
                g2.setColor(UITheme.TEXT_MUTED);
                g2.setFont(UITheme.FONT_REGULAR);
                String msg = "No membership data recorded yet";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2);
                g2.dispose();
                return;
            }

            // Dimensions for the donut
            int chartDiameter = Math.min(width / 2, height - 30);
            if (chartDiameter < 60) chartDiameter = 60;
            int cx = 20 + chartDiameter / 2;
            int cy = height / 2;
            int x = cx - chartDiameter / 2;
            int y = cy - chartDiameter / 2;

            double angleMonthly = 360.0 * monthly / total;
            double angleQuarterly = 360.0 * quarterly / total;
            double angleYearly = 360.0 - (angleMonthly + angleQuarterly);

            double currentAngle = 90.0;

            // Monthly segment (Violet)
            if (monthly > 0) {
                g2.setColor(UITheme.VIOLET_PRIMARY);
                g2.fill(new Arc2D.Double(x, y, chartDiameter, chartDiameter, currentAngle, angleMonthly, Arc2D.PIE));
                currentAngle += angleMonthly;
            }

            // Quarterly segment (Cyan)
            if (quarterly > 0) {
                g2.setColor(UITheme.CYAN_ACCENT);
                g2.fill(new Arc2D.Double(x, y, chartDiameter, chartDiameter, currentAngle, angleQuarterly, Arc2D.PIE));
                currentAngle += angleQuarterly;
            }

            // Yearly segment (Amber/Orange)
            if (yearly > 0) {
                g2.setColor(UITheme.WARNING_COLOR);
                g2.fill(new Arc2D.Double(x, y, chartDiameter, chartDiameter, currentAngle, angleYearly, Arc2D.PIE));
            }

            // Inner hole for Donut effect
            int innerHole = (int) (chartDiameter * 0.62);
            int hx = cx - innerHole / 2;
            int hy = cy - innerHole / 2;
            g2.setColor(UITheme.BG_CARD);
            g2.fillOval(hx, hy, innerHole, innerHole);
            g2.setColor(UITheme.BORDER_COLOR);
            g2.drawOval(hx, hy, innerHole, innerHole);

            // Center text: total members
            g2.setColor(UITheme.TEXT_PRIMARY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            String totalStr = String.valueOf(total);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(totalStr, cx - fm.stringWidth(totalStr) / 2, cy + 2);

            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            fm = g2.getFontMetrics();
            g2.drawString("MEMBERS", cx - fm.stringWidth("MEMBERS") / 2, cy + 14);

            // Draw Legend on the right side
            int legendX = x + chartDiameter + 24;
            int legendY = cy - 35;

            drawLegendItem(g2, legendX, legendY, UITheme.VIOLET_PRIMARY, "Monthly", monthly, total);
            drawLegendItem(g2, legendX, legendY + 28, UITheme.CYAN_ACCENT, "Quarterly", quarterly, total);
            drawLegendItem(g2, legendX, legendY + 56, UITheme.WARNING_COLOR, "Yearly", yearly, total);

            g2.dispose();
        }

        private void drawLegendItem(Graphics2D g2, int lx, int ly, Color dotColor, String label, int count, int total) {
            g2.setColor(dotColor);
            g2.fillRoundRect(lx, ly + 2, 10, 10, 3, 3);

            g2.setColor(UITheme.TEXT_PRIMARY);
            g2.setFont(UITheme.FONT_BOLD);
            g2.drawString(label, lx + 16, ly + 11);

            int pct = total > 0 ? (int) Math.round((count * 100.0) / total) : 0;
            String stat = count + " (" + pct + "%)";
            g2.setColor(UITheme.TEXT_SECONDARY);
            g2.setFont(UITheme.FONT_SMALL);
            g2.drawString(stat, lx + 90, ly + 11);
        }
    }

    /**
     * Vertical Bar Chart for 7-day attendance trend.
     */
    public static class AttendanceTrendBarChart extends JPanel {
        private Map<LocalDate, Integer> trend;
        private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE");

        public AttendanceTrendBarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(320, 190));
        }

        public void setTrend(Map<LocalDate, Integer> trend) {
            this.trend = trend;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (trend == null || trend.isEmpty()) {
                g2.setColor(UITheme.TEXT_MUTED);
                g2.setFont(UITheme.FONT_REGULAR);
                String msg = "No attendance activity recorded for this period";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int padLeft = 32;
            int padRight = 16;
            int padBottom = 30;
            int padTop = 20;

            int plotWidth = w - padLeft - padRight;
            int plotHeight = h - padTop - padBottom;

            int maxCount = 5;
            for (int count : trend.values()) {
                if (count > maxCount) maxCount = count;
            }
            maxCount = ((maxCount + 4) / 5) * 5; // round up to nearest multiple of 5

            // Draw subtle horizontal gridlines
            g2.setColor(new Color(0x30, 0x3B, 0x55, 120));
            g2.setFont(UITheme.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i <= 4; i++) {
                int y = padTop + (plotHeight * i) / 4;
                int val = maxCount - (maxCount * i) / 4;
                g2.drawLine(padLeft, y, w - padRight, y);
                g2.setColor(UITheme.TEXT_MUTED);
                String s = String.valueOf(val);
                g2.drawString(s, padLeft - fm.stringWidth(s) - 6, y + 4);
                g2.setColor(new Color(0x30, 0x3B, 0x55, 120));
            }

            int n = trend.size();
            int colWidth = plotWidth / n;
            int barWidth = Math.max(14, colWidth - 14);

            int idx = 0;
            LocalDate today = LocalDate.now();

            for (Map.Entry<LocalDate, Integer> entry : trend.entrySet()) {
                LocalDate date = entry.getKey();
                int count = entry.getValue();

                int bx = padLeft + idx * colWidth + (colWidth - barWidth) / 2;
                int barHeight = maxCount > 0 ? (int) ((double) count / maxCount * plotHeight) : 0;
                int by = padTop + (plotHeight - barHeight);

                // Bar gradient
                Color c1 = date.equals(today) ? UITheme.CYAN_ACCENT : UITheme.VIOLET_PRIMARY;
                Color c2 = date.equals(today) ? new Color(0x06, 0xB6, 0xD4) : UITheme.VIOLET_SECONDARY;

                GradientPaint gp = new GradientPaint(bx, by, c1, bx, by + barHeight, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(bx, by, barWidth, barHeight, 6, 6);

                // Bar count label
                if (count > 0) {
                    g2.setColor(UITheme.TEXT_PRIMARY);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    String valStr = String.valueOf(count);
                    int strW = g2.getFontMetrics().stringWidth(valStr);
                    g2.drawString(valStr, bx + (barWidth - strW) / 2, by - 4);
                }

                // X-axis label
                g2.setColor(date.equals(today) ? UITheme.CYAN_ACCENT : UITheme.TEXT_SECONDARY);
                g2.setFont(UITheme.FONT_SMALL);
                String dayLabel = date.equals(today) ? "Today" : date.format(DAY_FMT);
                int dayW = g2.getFontMetrics().stringWidth(dayLabel);
                g2.drawString(dayLabel, bx + (barWidth - dayW) / 2, h - 10);

                idx++;
            }

            g2.dispose();
        }
    }

    /**
     * Monthly revenue bar chart.
     */
    public static class RevenueBarChart extends JPanel {
        private Map<String, Double> revenueTrend;

        public RevenueBarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(320, 190));
        }

        public void setRevenueTrend(Map<String, Double> trend) {
            this.revenueTrend = trend;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (revenueTrend == null || revenueTrend.isEmpty()) {
                g2.setColor(UITheme.TEXT_MUTED);
                g2.setFont(UITheme.FONT_REGULAR);
                String msg = "No payment records recorded yet";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int padLeft = 45;
            int padRight = 16;
            int padBottom = 30;
            int padTop = 22;

            int plotWidth = w - padLeft - padRight;
            int plotHeight = h - padTop - padBottom;

            double maxRev = 1000.0;
            for (double val : revenueTrend.values()) {
                if (val > maxRev) maxRev = val;
            }

            // Gridlines
            g2.setColor(new Color(0x30, 0x3B, 0x55, 120));
            g2.setFont(UITheme.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i <= 4; i++) {
                int y = padTop + (plotHeight * i) / 4;
                double val = maxRev - (maxRev * i) / 4;
                g2.drawLine(padLeft, y, w - padRight, y);
                g2.setColor(UITheme.TEXT_MUTED);
                String s = formatCompactCurrency(val);
                g2.drawString(s, padLeft - fm.stringWidth(s) - 6, y + 4);
                g2.setColor(new Color(0x30, 0x3B, 0x55, 120));
            }

            int n = revenueTrend.size();
            int colWidth = plotWidth / n;
            int barWidth = Math.max(16, colWidth - 16);

            int idx = 0;
            for (Map.Entry<String, Double> entry : revenueTrend.entrySet()) {
                String ym = entry.getKey();
                double rev = entry.getValue();

                int bx = padLeft + idx * colWidth + (colWidth - barWidth) / 2;
                int barHeight = maxRev > 0 ? (int) ((rev / maxRev) * plotHeight) : 0;
                int by = padTop + (plotHeight - barHeight);

                GradientPaint gp = new GradientPaint(bx, by, new Color(0x10, 0xB9, 0x81), bx, by + barHeight, new Color(0x05, 0x96, 0x69));
                g2.setPaint(gp);
                g2.fillRoundRect(bx, by, barWidth, barHeight, 6, 6);

                if (rev > 0) {
                    g2.setColor(UITheme.TEXT_PRIMARY);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    String valStr = formatCompactCurrency(rev);
                    int strW = g2.getFontMetrics().stringWidth(valStr);
                    g2.drawString(valStr, bx + (barWidth - strW) / 2, by - 4);
                }

                g2.setColor(UITheme.TEXT_SECONDARY);
                g2.setFont(UITheme.FONT_SMALL);
                String label = formatMonthLabel(ym);
                int labelW = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, bx + (barWidth - labelW) / 2, h - 10);

                idx++;
            }

            g2.dispose();
        }

        private String formatCompactCurrency(double amount) {
            if (amount >= 100000) return String.format("%.0fk", amount / 1000);
            if (amount >= 1000) return String.format("%.1fk", amount / 1000);
            return String.format("%.0f", amount);
        }

        private String formatMonthLabel(String ym) {
            try {
                String[] parts = ym.split("-");
                int m = Integer.parseInt(parts[1]);
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                return months[m - 1];
            } catch (Exception e) {
                return ym;
            }
        }
    }
}

