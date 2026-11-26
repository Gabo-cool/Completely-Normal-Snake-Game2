package com.mycompany.snakegame.ui;

import com.mycompany.snakegame.exception.DatabaseException;
import com.mycompany.snakegame.exception.InvalidPlayerNameException;
import com.mycompany.snakegame.model.Score;
import com.mycompany.snakegame.service.ScoreService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class LeaderboardDialog extends JDialog {

    private static final String[] COLUMNS = {"ID", "Player", "Score", "Difficulty", "Played at"};

    private final ScoreService service;
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(12);
    private final JLabel summaryLabel = new JLabel(" ");

    public LeaderboardDialog(Frame owner, ScoreService service) {
        super(owner, "Leaderboard", true);
        this.service = service;
        table.setAutoCreateRowSorter(true);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchBtn = new JButton("Search");
        JButton allBtn = new JButton("Show all");
        top.add(new JLabel("Player:"));
        top.add(searchField);
        top.add(searchBtn);
        top.add(allBtn);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton renameBtn = new JButton("Rename player");
        JButton deleteBtn = new JButton("Delete selected");
        JButton refreshBtn = new JButton("Refresh");
        JButton closeBtn = new JButton("Close");
        buttons.add(renameBtn);
        buttons.add(deleteBtn);
        buttons.add(refreshBtn);
        buttons.add(closeBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(summaryLabel, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);

        setLayout(new BorderLayout(6, 6));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> search());
        searchField.addActionListener(e -> search());
        allBtn.addActionListener(e -> {
            searchField.setText("");
            refresh();
        });
        refreshBtn.addActionListener(e -> refresh());
        renameBtn.addActionListener(e -> renameSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        closeBtn.addActionListener(e -> dispose());

        setSize(640, 420);
        setLocationRelativeTo(owner);
        refresh();
    }

    private void refresh() {
        try {
            fill(service.topScores(100));
            summaryLabel.setText("  " + service.summary());
        } catch (DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void search() {
        try {
            fill(service.search(searchField.getText()));
        } catch (DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void fill(List<Score> scores) {
        model.setRowCount(0);
        for (Score s : scores) {
            model.addRow(new Object[]{s.id(), s.player(), s.points(), s.difficulty(), s.playedAt()});
        }
    }

    
    private int selectedId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Nothing selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return -1;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return (Integer) model.getValueAt(modelRow, 0);          
    }

    private void renameSelected() {
        int id = selectedId();
        if (id < 0) {
            return;
        }
        String input = JOptionPane.showInputDialog(this, "New player name:", "Rename player",
                JOptionPane.PLAIN_MESSAGE);
        if (input == null) {
            return;
        }
        try {
            boolean ok = service.rename(id, input);
            summaryLabel.setText(ok ? "  Player renamed." : "  That score no longer exists.");
            refresh();
        } catch (InvalidPlayerNameException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid name",
                    JOptionPane.WARNING_MESSAGE);
        } catch (DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteSelected() {
        int id = selectedId();
        if (id < 0) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete this score permanently?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.delete(id);
            refresh();
        } catch (DatabaseException ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Database problem", JOptionPane.ERROR_MESSAGE);
    }
}
