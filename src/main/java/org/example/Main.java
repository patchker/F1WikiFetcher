package org.example;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class.getName());
    private static final Map<String, String> teamURLs = new HashMap<>();
    private static JScrollPane scrollPane;
    private static JTree infoTree;

    static {
        teamURLs.put("Red Bull Racing", "https://pl.wikipedia.org/wiki/Red_Bull_Racing");
        teamURLs.put("Scuderia Ferrari", "https://pl.wikipedia.org/wiki/Scuderia_Ferrari");
        teamURLs.put("Mercedes", "https://pl.wikipedia.org/wiki/Mercedes_(Formu%C5%82a_1)");
        teamURLs.put("McLaren F1", "https://pl.wikipedia.org/wiki/McLaren_F1");
        teamURLs.put("Alpine F1", "https://pl.wikipedia.org/wiki/Alpine_F1");
        teamURLs.put("Aston Martin F1", "https://pl.wikipedia.org/wiki/Aston_Martin_F1");
        teamURLs.put("Alfa Romeo", "https://pl.wikipedia.org/wiki/Alfa_Romeo_(Formu%C5%82a_1)");
        teamURLs.put("Scuderia AlphaTauri", "https://pl.wikipedia.org/wiki/Scuderia_AlphaTauri");
        teamURLs.put("Haas F1", "https://pl.wikipedia.org/wiki/Haas_F1");
        teamURLs.put("Williams F1", "https://pl.wikipedia.org/wiki/Williams_F1");
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("F1 Team Info");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(300, 300));
        infoTree = new JTree(new DefaultMutableTreeNode()); // initializing JTree with empty root
        scrollPane.setViewportView(infoTree);

        JTextField teamField = new JTextField(15);

        JButton fetchButton = new JButton("Fetch");
        JTextArea infoArea = new JTextArea(30, 40);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setEditable(false);



        fetchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String teamName = teamField.getText().trim();
                boolean containsErrors = false;

                if (teamName.isEmpty()) {
                    containsErrors = true;
                    JOptionPane.showMessageDialog(frame, "Wprowadź nazwę zespołu.", "Błąd", JOptionPane.ERROR_MESSAGE);
                } else {
                    String allowedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ";
                    for (char c : teamName.toCharArray()) {
                        if (allowedCharacters.indexOf(c) == -1) {
                            containsErrors = true;
                            break;
                        }
                    }

                    if (containsErrors) {
                        JOptionPane.showMessageDialog(frame, "Wprowadzona nazwa zespołu zawiera niedozwolone znaki.", "Błąd", JOptionPane.ERROR_MESSAGE);
                    }
                }






                logger.info("Fetching information for team: " + teamField.getText());

                new SwingWorker<DefaultMutableTreeNode, Void>() {
                    @Override
                    protected DefaultMutableTreeNode doInBackground() {
                        return fetchTeamInfo(teamName);
                    }

                    @Override
                    protected void done() {
                        try {
                            DefaultMutableTreeNode result = get();
                            infoTree.setModel(new DefaultTreeModel(result));
                        } catch (Exception e) {
                            logger.error("Failed to fetch data for " + teamName, e);
                            infoTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Failed to fetch data for " + teamName))); // Update the existing JTree with error message
                        }

                        infoTree.addTreeSelectionListener(new TreeSelectionListener() {
                            @Override
                            public void valueChanged(TreeSelectionEvent e) {
                                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) infoTree.getLastSelectedPathComponent();
                                if (selectedNode != null) {
                                    infoArea.setText(selectedNode.toString());
                                }
                            }
                        });

                        scrollPane.setViewportView(infoTree);

                        frame.revalidate();
                        frame.repaint();
                    }

                }.execute();
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(teamField);
        panel.add(fetchButton);
        panel.add(new JScrollPane(infoArea));

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.getContentPane().add(scrollPane, BorderLayout.WEST);
        frame.setVisible(true);
    }

    private static DefaultMutableTreeNode fetchTeamInfo(String teamName) {
        try {
            System.out.print(teamName);

            String url = teamURLs.get(teamName);
            System.out.print(url);

            if (url == null) {
                System.out.print("Team not found");

                return new DefaultMutableTreeNode("Team not found.");
            }

            Document teamDoc = Jsoup.connect(url).get();

            // Fetch infobox data
            Element infoBox = teamDoc.selectFirst(".infobox");
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(teamName);
            DefaultMutableTreeNode infoBoxNode = new DefaultMutableTreeNode("Informacje");
            root.add(infoBoxNode);

            if (infoBox != null) {
                Elements rows = infoBox.select("tr");
                for (Element row : rows) {
                    Elements header = row.select("th");
                    Elements data = row.select("td");
                    if (!header.isEmpty() && !data.isEmpty()) {
                        infoBoxNode.add(new DefaultMutableTreeNode(header.first().text() + ": " + data.first().text()));
                    }
                }
            }
            Element table = teamDoc.selectFirst(".wikitable");

            DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode("Wyniki w Formule 1");
            root.add(tableNode);

            if (table != null) {
                Elements rows = table.select("tr");
                boolean isFirstRow = true;
                for (Element row : rows) {
                    Elements cells = row.select("td, th");
                    StringBuilder rowData = new StringBuilder();
                    int columnIndex = 0;
                    for (Element cell : cells) {
                        if (isFirstRow) {
                            rowData.append(cell.text()).append(": ");
                        } else {
                            String header = rows.first().select("th").get(columnIndex).text();
                            rowData.append(header).append(": ");
                        }
                        rowData.append(cell.text()).append("\n\n");
                        columnIndex++;
                    }
                    isFirstRow = false;
                    tableNode.add(new DefaultMutableTreeNode(rowData.toString().trim()));
                }
            }



            // Fetch main body content
            Elements bodyContent = teamDoc.select("#mw-content-text > div.mw-parser-output > p, h2, h3");

            DefaultMutableTreeNode currentSection = null;

            for (Element el : bodyContent) {
                String text = el.text();
                text = text.replace("[edytuj | edytuj kod]", "").replace("[edytuj kod]", "");

                if (text.equals("Uwagi") || text.equals("Przypisy") || text.equals("Linki zewnętrzne") || text.equals("Wyniki w Formule 1")) {
                    break;
                }
                if (el.tagName().equals("h2")) {
                    currentSection = new DefaultMutableTreeNode(text);
                    root.add(currentSection);
                } else if (el.tagName().equals("h3")) {
                    DefaultMutableTreeNode subSection = new DefaultMutableTreeNode(text);
                    currentSection.add(subSection);
                    currentSection = subSection;
                } else if (el.tagName().equals("p")) {
                    currentSection.add(new DefaultMutableTreeNode(text));
                }
            }

            System.out.print(root);
            return root;

        } catch (IOException e) {
            logger.error("Failed to fetch data for " + teamName, e);
            return new DefaultMutableTreeNode("Failed to fetch data for " + teamName);
        }
    }
}