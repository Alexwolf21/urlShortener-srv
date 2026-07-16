CREATE SEQUENCE IF NOT EXISTS url_sequence START WITH 100000 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS analytics_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(100) NOT NULL,
    clicked_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(100),
    user_agent VARCHAR(1000),
    referrer VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_analytics_short_code ON analytics_logs(short_code);
