CREATE TABLE IF NOT EXISTS organizers (
    admin_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS players (
    player_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    player_name VARCHAR(120) NOT NULL,
    current_score DECIMAL(5,2) NOT NULL DEFAULT 0.0,
    initial_rank INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_players_admin FOREIGN KEY (admin_id)
        REFERENCES organizers(admin_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_players_admin_name UNIQUE (admin_id, player_name)
);

CREATE TABLE IF NOT EXISTS tournaments (
    tournament_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    tournament_name VARCHAR(120) NOT NULL,
    current_round INT NOT NULL DEFAULT 0,
    status VARCHAR(24) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tournaments_admin FOREIGN KEY (admin_id)
        REFERENCES organizers(admin_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tournament_players (
    tournament_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    PRIMARY KEY (tournament_id, player_id),
    CONSTRAINT fk_tournament_players_tournament FOREIGN KEY (tournament_id)
        REFERENCES tournaments(tournament_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tournament_players_player FOREIGN KEY (player_id)
        REFERENCES players(player_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS matches (
    match_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tournament_id BIGINT NOT NULL,
    round_number INT NOT NULL,
    table_number INT NOT NULL,
    player1_id BIGINT NOT NULL,
    player2_id BIGINT NULL,
    match_result VARCHAR(16) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_matches_tournament FOREIGN KEY (tournament_id)
        REFERENCES tournaments(tournament_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_matches_player1 FOREIGN KEY (player1_id)
        REFERENCES players(player_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_matches_player2 FOREIGN KEY (player2_id)
        REFERENCES players(player_id)
        ON DELETE SET NULL,
    CONSTRAINT uq_matches_round_table UNIQUE (tournament_id, round_number, table_number)
);
