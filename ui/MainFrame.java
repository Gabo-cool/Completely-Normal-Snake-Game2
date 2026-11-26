package com.mycompany.snakegame.ui;

import com.mycompany.snakegame.engine.GameEngine;
import com.mycompany.snakegame.exception.InvalidPlayerNameException;
import com.mycompany.snakegame.model.Difficulty;
import com.mycompany.snakegame.model.Food;
import com.mycompany.snakegame.service.ScoreService;
import com.mycompany.snakegame.util.ClockThread;
import com.mycompany.snakegame.util.GameConfig;
import com.mycompany.snakegame.util.ScoreSaver;
import com.mycompany.snakegame.util.SessionStats;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainFrame extends JFrame implements GameListener {

    private final GamePanel panel = new GamePanel();
    private final ScoreService service;
    private final SessionStats stats = new SessionStats();
    private final ClockThread clock;

    private final JLabel scoreLabel = new JLabel("Score: 0", JLabel.CENTER);
    private final JLabel timeLabel = new JLabel("Time: 00:00", JLabel.CENTER);
    private final JLabel lengthLabel = new JLabel("Length: 4", JLabel.CENTER);
    private final JLabel bestLabel = new JLabel("Best: 0", JLabel.CENTER);
    private final JLabel statusLabel = new JLabel(" ", JLabel.CENTER);

    private Thread lastSaver;
    private String lastPlayer = "";

    public MainFrame(ScoreService service) {
        super("Snake Game");
        this.service = service;

        
        JPanel top = new JPanel(new GridLayout(1, 4, 8, 0));
        top.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        top.add(scoreLabel);
        top.add(lengthLabel);
        top.add(timeLabel);
        top.add(bestLabel);

        
        JButton newBtn = new JButton("New Game");
        JButton pauseBtn = new JButton("Pause");
        JButton boardBtn = new JButton("Leaderboard");
        JButton aboutBtn = new JButton("About");
        JComboBox<Difficulty> levelBox = new JComboBox<>(Difficulty.values());
        levelBox.setSelectedItem(Difficulty.NORMAL);

        
        for (var control : new JComponent[]{newBtn, pauseBtn, boardBtn, aboutBtn, levelBox}) {
            control.setFocusable(false);
        }

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        controls.add(newBtn);
        controls.add(pauseBtn);
        controls.add(new JLabel("Level:"));
        controls.add(levelBox);
        controls.add(boardBtn);
        controls.add(aboutBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(controls, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        
        newBtn.addActionListener(e -> {
            panel.newGame();
            panel.requestFocusInWindow();
        });
        pauseBtn.addActionListener(e -> panel.togglePause());
        levelBox.addActionListener(e -> panel.setDifficulty((Difficulty) levelBox.getSelectedItem()));
        boardBtn.addActionListener(e -> showLeaderboard());
        aboutBtn.addActionListener(e -> showAbout());

        panel.setGameListener(this);

        
        clock = new ClockThread(seconds -> SwingUtilities.invokeLater(
                () -> timeLabel.setText(String.format("Time: %02d:%02d", seconds / 60, seconds % 60))));
        clock.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        panel.prepare();
        SwingUtilities.invokeLater(panel::requestFocusInWindow);   
    }

    

    @Override
    public void onNewGame() {
        clock.resetSeconds();
        timeLabel.setText("Time: 00:00");
    }

    @Override
    public void onScoreChanged(int score) {
        scoreLabel.setText("Score: " + score);
        int length = panel.getEngine().getSnake().length();
        
        double fill = (double) length / (GameConfig.COLUMNS * GameConfig.ROWS) * 100;
        lengthLabel.setText(String.format("Length: %d (%.1f%%)", length, fill));
    }

    @Override
    public void onStateChanged(GameEngine.State state) {
        clock.setCounting(state == GameEngine.State.RUNNING);
        String text = switch (state) {
            case READY -> "Press ENTER or an arrow key to start  |  P = pause";
            case RUNNING -> "Eat apples, avoid walls and your own tail.  Golden apple = +"
                    + GameConfig.get("goldenPoints");
            case PAUSED -> "Paused";
            case GAME_OVER -> "Game over";
            case WON -> "You filled the whole board - you win!";
        };
        statusLabel.setText(text);
    }

    @Override
    public void onGameEnded(int score) {
        stats.recordGame(score);
        bestLabel.setText("Best: " + stats.getBestScore());
        promptAndSave(score);
    }

    
    private void promptAndSave(int score) {
        String name = null;
        boolean done = false;
        do {
            Object input = JOptionPane.showInputDialog(this,
                    "Game over! Score: " + score + "\nEnter your name to save it (Cancel to skip):",
                    "Save score", JOptionPane.PLAIN_MESSAGE, null, null, lastPlayer);
            if (input == null) {
                statusLabel.setText("Score not saved.");
                done = true;
            } else {
                try {
                    name = service.validateName(input.toString());    
                    done = true;
                } catch (InvalidPlayerNameException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid name",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        } while (!done);

        if (name != null) {
            lastPlayer = name;
            statusLabel.setText("Saving score...");
            var saver = new Thread(new ScoreSaver(service, name, score, panel.getDifficulty(), stats,
                    message -> SwingUtilities.invokeLater(() -> statusLabel.setText(message))),
                    "score-saver");
            lastSaver = saver;
            saver.start();                                           
        }
        panel.requestFocusInWindow();
    }

  

    private void showLeaderboard() {
        panel.pauseIfRunning();
        new LeaderboardDialog(this, service).setVisible(true);
        panel.requestFocusInWindow();
    }

    
    private void showAbout() {
        panel.pauseIfRunning();
        String info = """
                Snake Game - CS0070 Java Programming final project

                Java version : %s
                JVM          : %s
                Vendor       : %s
                OS           : %s

                Food objects created this run: %d
                %s
                """.formatted(
                System.getProperty("java.version"),
                System.getProperty("java.vm.name"),
                System.getProperty("java.vendor"),
                System.getProperty("os.name"),
                Food.getSpawned(),
                stats.summary());
        JOptionPane.showMessageDialog(this, info, "About", JOptionPane.INFORMATION_MESSAGE);
        panel.requestFocusInWindow();
    }

    private void shutdown() {
        clock.interrupt();
        try {
            clock.join(500);
            if (lastSaver != null) {
                lastSaver.join(2000);          
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        System.out.println(stats.summary());
    }
}
