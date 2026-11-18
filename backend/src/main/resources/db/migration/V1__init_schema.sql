CREATE SEQUENCE input_dataset_seq INCREMENT BY 1;
CREATE SEQUENCE lane_seq INCREMENT BY 50;
CREATE SEQUENCE transporter_seq INCREMENT BY 50;
CREATE SEQUENCE lane_quote_seq INCREMENT BY 50;

-- One row per submission; submissions never overwrite each other.
CREATE TABLE input_dataset (
    id         BIGINT PRIMARY KEY DEFAULT nextval('input_dataset_seq'),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE lane (
    id          BIGINT PRIMARY KEY,
    dataset_id  BIGINT       NOT NULL REFERENCES input_dataset (id) ON DELETE CASCADE,
    lane_ref    BIGINT       NOT NULL,
    origin      VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    CONSTRAINT uk_lane_dataset_ref UNIQUE (dataset_id, lane_ref)
);

CREATE TABLE transporter (
    id              BIGINT PRIMARY KEY,
    dataset_id      BIGINT       NOT NULL REFERENCES input_dataset (id) ON DELETE CASCADE,
    transporter_ref BIGINT       NOT NULL,
    name            VARCHAR(150) NOT NULL,
    CONSTRAINT uk_transporter_dataset_ref UNIQUE (dataset_id, transporter_ref)
);

CREATE TABLE lane_quote (
    id             BIGINT PRIMARY KEY,
    lane_id        BIGINT         NOT NULL REFERENCES lane (id) ON DELETE CASCADE,
    transporter_id BIGINT         NOT NULL REFERENCES transporter (id) ON DELETE CASCADE,
    quote          NUMERIC(14, 2) NOT NULL CHECK (quote > 0),
    CONSTRAINT uk_lane_quote_transporter_lane UNIQUE (transporter_id, lane_id)
);

CREATE INDEX idx_lane_quote_lane ON lane_quote (lane_id);
