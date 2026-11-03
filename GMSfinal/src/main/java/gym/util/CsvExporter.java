package gym.util;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public final class CsvExporter {
    private CsvExporter() {}

    public static boolean exportTableToCsv(Component parent, TableModel model, String suggestedFileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export to CSV");
        String defaultName = suggestedFileName + "_" + LocalDate.now() + ".csv";
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            int colCount = model.getColumnCount();

            // Write header
            for (int col = 0; col < colCount; col++) {
                writer.write(escapeCsv(model.getColumnName(col)));
                if (col < colCount - 1) writer.write(",");
            }
            writer.newLine();

            // Write rows
            int rowCount = model.getRowCount();
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    Object val = model.getValueAt(row, col);
                    writer.write(escapeCsv(val != null ? val.toString() : ""));
                    if (col < colCount - 1) writer.write(",");
                }
                writer.newLine();
            }

            JOptionPane.showMessageDialog(
                parent,
                "CSV exported successfully to:\n" + file.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE
            );
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                parent,
                "Failed to export CSV: " + e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    public static boolean exportRowsToCsv(Component parent, String[] headers, List<String[]> rows, String suggestedFileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export to CSV");
        String defaultName = suggestedFileName + "_" + LocalDate.now() + ".csv";
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            for (int i = 0; i < headers.length; i++) {
                writer.write(escapeCsv(headers[i]));
                if (i < headers.length - 1) writer.write(",");
            }
            writer.newLine();

            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    writer.write(escapeCsv(row[i] != null ? row[i] : ""));
                    if (i < row.length - 1) writer.write(",");
                }
                writer.newLine();
            }

            JOptionPane.showMessageDialog(
                parent,
                "CSV exported successfully to:\n" + file.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE
            );
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                parent,
                "Failed to export CSV: " + e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "\"\"";
        boolean containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (containsSpecial) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

