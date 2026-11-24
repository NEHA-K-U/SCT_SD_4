import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProductScraperGUI extends JFrame {

    private JTextField urlField;
    private JTextField itemField;
    private JTextField nameField;
    private JTextField priceField;
    private JTextField ratingField;

    private JTable table;
    private DefaultTableModel model;

    private JButton scrapeButton;
    private JButton saveButton;

    public ProductScraperGUI() {

        setTitle("Product Scraper - SkillCraft Technology");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Input fields
        urlField = new JTextField("https://example.com/products");
        itemField = new JTextField(".product-card");
        nameField = new JTextField(".product-name");
        priceField = new JTextField(".product-price");
        ratingField = new JTextField(".product-rating");

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(new JLabel("Website URL:"));
        panel.add(urlField);

        panel.add(new JLabel("Product Container:"));
        panel.add(itemField);

        panel.add(new JLabel("Product Name Selector:"));
        panel.add(nameField);

        panel.add(new JLabel("Product Price Selector:"));
        panel.add(priceField);

        panel.add(new JLabel("Product Rating Selector:"));
        panel.add(ratingField);

        // Buttons
        scrapeButton = new JButton("Scrape Products");
        saveButton = new JButton("Save CSV");

        saveButton.setEnabled(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(scrapeButton);
        buttonPanel.add(saveButton);

        // Table
        model = new DefaultTableModel(
                new String[]{"Product Name", "Price", "Rating"}, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);

        JScrollPane scrollPane = new JScrollPane(table);

        JLabel status = new JLabel("Ready to scrape products.");
        status.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.add(panel, BorderLayout.CENTER);
        top.add(buttonPanel, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        // Scrape button
        scrapeButton.addActionListener(e -> {

            String url = urlField.getText().trim();
            String item = itemField.getText().trim();
            String name = nameField.getText().trim();
            String price = priceField.getText().trim();
            String rating = ratingField.getText().trim();

            if (url.isEmpty() || item.isEmpty()
                    || name.isEmpty() || price.isEmpty()
                    || rating.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this, "Please fill all fields.");
                return;
            }

            try {
                java.net.URI uri = java.net.URI.create(url);

                if (uri.getHost() == null
                        || !("http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme()))) {
                    throw new IllegalArgumentException();
                }

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                        this, "Please enter a valid website URL.");
                return;
            }

            scrapeButton.setEnabled(false);
            saveButton.setEnabled(false);
            model.setRowCount(0);
            status.setText("Scraping products...");

            SwingWorker<List<String[]>, Void> worker =
                    new SwingWorker<List<String[]>, Void>() {

                @Override
                protected List<String[]> doInBackground()
                        throws Exception {

                    return scrapeProducts(
                            url, item, name, price, rating);
                }

                @Override
                protected void done() {

                    try {
                        List<String[]> products = get();

                        for (String[] product : products) {
                            model.addRow(product);
                        }

                        if (products.isEmpty()) {
                            status.setText(
                                    "No products found. Check selectors.");
                        } else {
                            status.setText(
                                    products.size() + " products found.");
                            saveButton.setEnabled(true);
                        }

                    } catch (Exception ex) {
                        status.setText("Scraping failed.");

                        Throwable cause = ex.getCause();

                        JOptionPane.showMessageDialog(
                                ProductScraperGUI.this,
                                cause == null
                                        ? ex.getMessage()
                                        : cause.getMessage(),
                                "Scraping Error",
                                JOptionPane.ERROR_MESSAGE);

                    } finally {
                        scrapeButton.setEnabled(true);
                    }
                }
            };

            worker.execute();
        });

        // Save CSV button
        saveButton.addActionListener(e -> {

            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(
                        this, "No data available.");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("products.csv"));

            if (chooser.showSaveDialog(this)
                    != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File file = chooser.getSelectedFile();

            if (file.exists()) {
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        "File exists. Overwrite?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION);

                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try (FileWriter writer = new FileWriter(file)) {

                writer.write("Product Name,Price,Rating\n");

                for (int i = 0; i < model.getRowCount(); i++) {

                    writer.write(
                            csv(model.getValueAt(i, 0)) + "," +
                            csv(model.getValueAt(i, 1)) + "," +
                            csv(model.getValueAt(i, 2)) + "\n");
                }

                JOptionPane.showMessageDialog(
                        this, "CSV file saved successfully!");

            } catch (IOException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "Error saving CSV: " + ex.getMessage());
            }
        });
    }

    // Product scraping method
    private static List<String[]> scrapeProducts(
            String url,
            String itemSelector,
            String nameSelector,
            String priceSelector,
            String ratingSelector) throws IOException {

        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(20000)
                .get();

        List<String[]> products = new ArrayList<String[]>();

        for (Element item : document.select(itemSelector)) {

            String name = getText(item, nameSelector);
            String price = getText(item, priceSelector);
            String rating = getText(item, ratingSelector);

            products.add(new String[]{name, price, rating});
        }

        return products;
    }

    // Extract product details
    private static String getText(
            Element element, String selector) {

        Element result = element.selectFirst(selector);

        if (result == null || result.text().trim().isEmpty()) {
            return "N/A";
        }

        return result.text().trim();
    }

    // CSV formatting
    private static String csv(Object value) {

        String text = String.valueOf(value);

        if (text.contains(",") || text.contains("\"")
                || text.contains("\n") || text.contains("\r")) {

            return "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
    }

    // Main method
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new ProductScraperGUI().setVisible(true);
        });
    }
}