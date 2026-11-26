package com.mycompany.snakegame.service;

import com.mycompany.snakegame.db.ScoreDao;
import com.mycompany.snakegame.exception.DatabaseException;
import com.mycompany.snakegame.exception.InvalidPlayerNameException;
import com.mycompany.snakegame.model.Difficulty;
import com.mycompany.snakegame.model.Score;
import com.mycompany.snakegame.util.GameConfig;
import com.mycompany.snakegame.util.Lists;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ScoreService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Comparator<Score> BY_POINTS_DESC =
            Comparator.comparingInt(Score::points).reversed().thenComparing(Score::playedAt);

    private final ScoreDao dao;

    public ScoreService(ScoreDao dao) {
        this.dao = dao;
    }

    public String validateName(String raw) throws InvalidPlayerNameException {
        String name = (raw == null) ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new InvalidPlayerNameException("The name cannot be empty.");
        }
        if (!name.matches(GameConfig.PLAYER_NAME_PATTERN)) {
            throw new InvalidPlayerNameException(
                    "Use 1-12 letters, digits, spaces or underscores.");
        }
        return name;
    }

    public Score save(String player, int points, Difficulty difficulty)
            throws InvalidPlayerNameException, DatabaseException {
        String name = validateName(player);
        if (points < 0) {
            throw new IllegalArgumentException("Score cannot be negative.");   
        }
        Score score = new Score(0, name, points, difficulty.toString(),
                LocalDateTime.now().format(STAMP));
        dao.insert(score);
        return score;
    }


    public List<Score> topScores(int n) throws DatabaseException {
        List<Score> all = new ArrayList<>(dao.findAll());
        Collections.sort(all, BY_POINTS_DESC);
        return Lists.topN(all, n);
    }

    public List<Score> search(String text) throws DatabaseException {
        String query = (text == null) ? "" : text.trim();
        return query.isEmpty() ? topScores(100) : dao.findByPlayer(query);
    }

    public boolean rename(int id, String newName)
            throws InvalidPlayerNameException, DatabaseException {
        return dao.updatePlayer(id, validateName(newName));
    }

    public boolean delete(int id) throws DatabaseException {
        return dao.deleteById(id);                    
    }

    public int bestScore() throws DatabaseException {
        List<Score> all = dao.findAll();
        if (all.isEmpty()) {
            return 0;
        }
        return Collections.max(all, Comparator.comparingInt(Score::points)).points();
    }


    public int rankOf(int points) throws DatabaseException {
        List<Integer> sorted = new ArrayList<>();
        for (Score s : dao.findAll()) {
            sorted.add(s.points());                   
        }
        sorted.sort(Collections.reverseOrder());
        int index = Collections.binarySearch(sorted, points, Collections.reverseOrder());
        int insertion = (index >= 0) ? index : -index - 1;
        return insertion + 1;
    }

    public String summary() throws DatabaseException {
        List<Score> all = dao.findAll();
        Set<String> players = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Integer> bestByPlayer = new HashMap<>();
        for (Score s : all) {
            players.add(s.player());
            bestByPlayer.merge(s.player(), s.points(), Math::max);
        }
        String champion = "-";
        int championPoints = -1;
        for (Map.Entry<String, Integer> e : bestByPlayer.entrySet()) {
            if (e.getValue() > championPoints) {
                champion = e.getKey();
                championPoints = e.getValue();
            }
        }
        return String.format("%d score(s) | %d player(s) | top player: %s (%d)",
                all.size(), players.size(), champion, Math.max(championPoints, 0));
    }
}
