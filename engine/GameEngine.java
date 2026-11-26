package com.mycompany.snakegame.engine;

import com.mycompany.snakegame.model.Cell;
import com.mycompany.snakegame.model.Direction;
import com.mycompany.snakegame.model.Food;
import com.mycompany.snakegame.model.GoldenFood;
import com.mycompany.snakegame.model.NormalFood;
import com.mycompany.snakegame.model.Resettable;
import com.mycompany.snakegame.model.Snake;
import com.mycompany.snakegame.util.GameConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


public class GameEngine implements Resettable {

    public enum State { READY, RUNNING, PAUSED, GAME_OVER, WON }

    private final Snake snake;
    private final Deque<Direction> pendingTurns = new ArrayDeque<>();   
    private final Random random = new Random();
    private Food food;
    private Food lastEaten;
    private int score;
    private State state = State.READY;

    public GameEngine() {
        snake = new Snake(new Cell(GameConfig.COLUMNS / 3, GameConfig.ROWS / 2),
                GameConfig.START_LENGTH);
        reset();
    }

    @Override
    public final void reset() {
        snake.reset();
        pendingTurns.clear();
        score = 0;
        lastEaten = null;
        state = State.READY;
        food = spawnFood();
    }

    public void start() {
        if (state == State.READY) {
            state = State.RUNNING;
        }
    }

    public void togglePause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
        } else if (state == State.PAUSED) {
            state = State.RUNNING;
        }
    }

  
    public void turn(Direction wanted) {
        if (state != State.RUNNING) {
            return;
        }
        Direction last = pendingTurns.isEmpty() ? snake.getDirection() : pendingTurns.peekLast();
        if (wanted != last && wanted != last.opposite() && pendingTurns.size() < 2) {
            pendingTurns.addLast(wanted);
        }
    }

     public void tick() {
        if (state != State.RUNNING) {
            return;
        }
        lastEaten = null;
        Direction next = pendingTurns.pollFirst();
        if (next != null) {
            snake.setDirection(next);
        }

        Cell head = snake.peekNextHead();
        if (!inBounds(head)) {                         
            state = State.GAME_OVER;
            return;
        }

        boolean eating = head.equals(food.getPosition());
        snake.advance(eating);
        if (snake.hitsSelf()) {
            state = State.GAME_OVER;
            return;
        }

        if (eating) {
            lastEaten = food;
            score += food.getPoints();                
            food = spawnFood();
            if (food == null) {
                state = State.WON;                     
            }
        }
    }

    private boolean inBounds(Cell c) {
        return c.col() >= 0 && c.col() < GameConfig.COLUMNS
                && c.row() >= 0 && c.row() < GameConfig.ROWS;
    }
    
    private Food spawnFood() {
        Set<Cell> occupied = new HashSet<>(snake.getCells());
        List<Cell> free = new ArrayList<>();
        for (int c = 0; c < GameConfig.COLUMNS; c++) {
            for (int r = 0; r < GameConfig.ROWS; r++) {          
                Cell cell = new Cell(c, r);
                if (!occupied.contains(cell)) {
                    free.add(cell);
                }
            }
        }
        if (free.isEmpty()) {
            return null;
        }
        Cell spot = free.get(random.nextInt(free.size()));
        boolean golden = random.nextInt(100) < GameConfig.get("goldenChance");
        return golden ? new GoldenFood(spot) : new NormalFood(spot);   
    }

    public Snake getSnake() { return snake; }

    public Food getFood() { return food; }

    public Food getLastEaten() { return lastEaten; }

    public int getScore() { return score; }

    public State getState() { return state; }
}
