package com.mycompany.snakegame.ui;

import com.mycompany.snakegame.engine.GameEngine;
import com.mycompany.snakegame.model.Difficulty;
import com.mycompany.snakegame.model.Direction;
import com.mycompany.snakegame.model.Food;
import com.mycompany.snakegame.model.GameObject;
import com.mycompany.snakegame.model.GoldenFood;
import com.mycompany.snakegame.util.GameConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GamePanel extends JPanel implements ActionListener {

    private final GameEngine engine = new GameEngine();
    private final Timer timer;
    private Difficulty difficulty = Difficulty.NORMAL;
    private GameListener listener = new GameListener() { };      

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        timer = new Timer(difficulty.getDelayMs(), this);
        setupKeyBindings();
    }

    // input

    private void setupKeyBindings() {
        bind("LEFT", () -> steer(Direction.LEFT));
        bind("RIGHT", () -> steer(Direction.RIGHT));
        bind("UP", () -> steer(Direction.UP));
        bind("DOWN", () -> steer(Direction.DOWN));
        bind("A", () -> steer(Direction.LEFT));
        bind("D", () -> steer(Direction.RIGHT));
        bind("W", () -> steer(Direction.UP));
        bind("S", () -> steer(Direction.DOWN));
        bind("P", this::togglePause);
        bind("SPACE", this::togglePause);
        bind("ENTER", this::startOrRestart);
    }

    private void bind(String keyName, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyName), keyName);
        getActionMap().put(keyName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void steer(Direction d) {
        if (engine.getState() == GameEngine.State.READY) {
            beginRun();
        }
        engine.turn(d);
    }

    // control

    public void startOrRestart() {
        switch (engine.getState()) {
            case READY -> beginRun();
            case GAME_OVER, WON -> newGame();
            case PAUSED -> togglePause();
            case RUNNING -> { }
        }
    }

   
    public void newGame() {
        prepare();
        beginRun();
    }

    
    public void prepare() {
        timer.stop();
        engine.reset();
        listener.onNewGame();
        listener.onScoreChanged(0);
        listener.onStateChanged(engine.getState());
        repaint();
    }

    private void beginRun() {
        engine.start();
        timer.start();
        listener.onStateChanged(engine.getState());
        repaint();
    }

    public void togglePause() {
        GameEngine.State before = engine.getState();
        if (before != GameEngine.State.RUNNING && before != GameEngine.State.PAUSED) {
            return;
        }
        engine.togglePause();
        if (engine.getState() == GameEngine.State.RUNNING) {
            timer.start();
        } else {
            timer.stop();
        }
        listener.onStateChanged(engine.getState());
        repaint();
    }

    public void pauseIfRunning() {
        if (engine.getState() == GameEngine.State.RUNNING) {
            togglePause();
        }
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        timer.setDelay(difficulty.getDelayMs());
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public GameEngine getEngine() {
        return engine;
    }

    public void setGameListener(GameListener listener) {
        this.listener = listener;
    }

    

    @Override
    public void actionPerformed(ActionEvent e) {
        int before = engine.getScore();
        engine.tick();

        Food eaten = engine.getLastEaten();
        if (eaten instanceof GoldenFood gold) {             
            System.out.printf("Golden apple eaten: +%d points%n", gold.getPoints());
        }
        if (engine.getScore() != before) {
            listener.onScoreChanged(engine.getScore());
        }

        GameEngine.State state = engine.getState();
        if (state == GameEngine.State.GAME_OVER || state == GameEngine.State.WON) {
            timer.stop();
            listener.onStateChanged(state);
            SwingUtilities.invokeLater(() -> listener.onGameEnded(engine.getScore()));
        }
        repaint();
    }

    // drawing

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2);

        GameObject[] objects = (engine.getFood() == null)
                ? new GameObject[]{engine.getSnake()}
                : new GameObject[]{engine.getFood(), engine.getSnake()};
        for (GameObject obj : objects) {
            obj.draw(g2, GameConfig.CELL);                  
        }

        drawScoreBox(g2);
        switch (engine.getState()) {
            case READY -> drawOverlay(g2, "Snake", "Press ENTER or an arrow key to start", null);
            case PAUSED -> drawOverlay(g2, "Paused", "Press P or SPACE to resume", null);
            case GAME_OVER -> drawOverlay(g2, "Game Over", "Score: " + engine.getScore(),
                    "Press ENTER to play again");
            case WON -> drawOverlay(g2, "You Win!", "Score: " + engine.getScore(),
                    "Press ENTER to play again");
            case RUNNING -> { }
        }
        g2.dispose();
    }

    private void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(105, 190, 82),
                0, GameConfig.SCREEN_HEIGHT, new Color(35, 125, 55)));
        g.fillRect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);

        g.setStroke(new BasicStroke(1.2f));                  // small grass pattern
        g.setColor(new Color(255, 255, 255, 25));
        for (int x = 0; x < GameConfig.SCREEN_WIDTH; x += 50) {
            for (int y = 45; y < GameConfig.SCREEN_HEIGHT; y += 50) {
                g.drawLine(x, y, x + 3, y - 6);
                g.drawLine(x + 3, y - 6, x + 7, y);
            }
        }
        g.setColor(new Color(20, 85, 35));                   // border
        g.setStroke(new BasicStroke(6));
        g.drawRect(3, 3, GameConfig.SCREEN_WIDTH - 6, GameConfig.SCREEN_HEIGHT - 6);
    }

    private void drawScoreBox(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(10, 10, 125, 38, 15, 15);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 23));
        g.drawString("Score: " + engine.getScore(), 22, 37);
    }

    private void drawOverlay(Graphics2D g, String title, String line1, String line2) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
        g.setColor(title.equals("Game Over") ? Color.RED : Color.WHITE);
        drawCentered(g, title, new Font("Arial", Font.BOLD, 50), GameConfig.SCREEN_HEIGHT / 2 - 30);
        g.setColor(Color.WHITE);
        drawCentered(g, line1, new Font("Arial", Font.BOLD, 22), GameConfig.SCREEN_HEIGHT / 2 + 20);
        if (line2 != null) {
            drawCentered(g, line2, new Font("Arial", Font.PLAIN, 18), GameConfig.SCREEN_HEIGHT / 2 + 60);
        }
    }

    private void drawCentered(Graphics2D g, String text, Font font, int y) {
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (GameConfig.SCREEN_WIDTH - metrics.stringWidth(text)) / 2, y);
    }
}
