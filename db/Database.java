package com.mycompany.snakegame.db;

import com.mycompany.snakegame.exception.DatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String URL = "jdbc:sqlite:snake_scores.db";

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS score (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                player      TEXT    NOT NULL,
                points      INTEGER NOT NULL CHECK (points >= 0),
                difficulty  TEXT    NOT NULL,
                played_at   TEXT    NOT NULL
            )
            """;

    private Database() {
    }

    public static Connection connect() throws DatabaseException {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            logTechnical(e);
            throw new DatabaseException("Could not open the score database.", e);
        }
    }
    
    public static void init() throws DatabaseException {
        try (Connection con = connect();
             Statement st = con.createStatement()) {
            st.executeUpdate(CREATE_TABLE);                       
        } catch (SQLException e) {
            logTechnical(e);
            throw new DatabaseException("Could not prepare the score table.", e);
        }
    }

    static void logTechnical(SQLException e) {
        System.err.println("[DB] " + e.getMessage() + " (SQLState=" + e.getSQLState() + ")");
    }
}
