PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS score (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player TEXT NOT NULL CHECK (length(trim(player)) BETWEEN 1 AND 12),
    points INTEGER NOT NULL DEFAULT 0 CHECK (points >= 0),
    difficulty TEXT NOT NULL CHECK (difficulty IN ('Easy', 'Normal', 'Hard')),
    played_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_score_points ON score(points DESC);
CREATE INDEX IF NOT EXISTS idx_score_player ON score(player);
