package com.mycompany.snakegame.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class GameConfig {

    public static final int COLUMNS = 24;
    public static final int ROWS = 24;
    public static final int CELL = 25;
    public static final int SCREEN_WIDTH = COLUMNS * CELL;    
    public static final int SCREEN_HEIGHT = ROWS * CELL;      
    public static final int START_LENGTH = 4;
    public static final String PLAYER_NAME_PATTERN = "[A-Za-z0-9_ ]{1,12}";

    private static final String DEFAULTS = """
            normalPoints=1
            goldenPoints=3
            goldenChance=20
            """;

    private static final Map<String, Integer> VALUES = load();

    private GameConfig() {

    }

    private static Map<String, Integer> load() {
        Map<String, Integer> map = new HashMap<>();
        try (Scanner sc = new Scanner(DEFAULTS)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("=");              
                map.put(parts[0], Integer.parseInt(parts[1])); 
            }
        }
        return map;
    }

    public static int get(String key) {
        return VALUES.getOrDefault(key, 0);                    
    }
}
