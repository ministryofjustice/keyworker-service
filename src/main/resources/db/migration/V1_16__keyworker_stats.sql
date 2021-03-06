DROP TABLE IF EXISTS KEYWORKER_STATS;

CREATE TABLE KEYWORKER_STATS
(
  KEYWORKER_STATS_ID        BIGSERIAL    NOT NULL,
  PRISON_ID                 VARCHAR( 6)  NOT NULL,
  SNAPSHOT_DATE             TIMESTAMP    NOT NULL,
  NUM_PRISONERS_ASSIGNED_KW  INTEGER      NOT NULL,
  TOTAL_NUM_PRISONERS       INTEGER      NOT NULL,
  NUM_KW_SESSIONS           INTEGER,
  NUM_KW_ENTRIES            INTEGER,
  NUM_ACTIVE_KEYWORKERS     INTEGER,
  RECPT_TO_ALLOC_DAYS       INTEGER,
  RECPT_TO_KW_SESSION_DAYS  INTEGER,

  CONSTRAINT KEYWORKER_STATS_PK PRIMARY KEY (KEYWORKER_STATS_ID)
);

COMMENT ON TABLE KEYWORKER_STATS IS 'Records the results of each statistic snapshot';

COMMENT ON COLUMN KEYWORKER_STATS.KEYWORKER_STATS_ID      IS 'Primary key ID';
COMMENT ON COLUMN KEYWORKER_STATS.PRISON_ID               IS 'Prison ID';
COMMENT ON COLUMN KEYWORKER_STATS.SNAPSHOT_DATE           IS 'Timestamp of date for which stats apply';

COMMENT ON COLUMN KEYWORKER_STATS.NUM_PRISONERS_ASSIGNED_KW  IS 'Number of prisoner who have been assigned a Key worker';
COMMENT ON COLUMN KEYWORKER_STATS.TOTAL_NUM_PRISONERS       IS 'Total number of prisoners in the prison on this date';
COMMENT ON COLUMN KEYWORKER_STATS.NUM_KW_SESSIONS           IS 'Number of Key worker case note sessions';
COMMENT ON COLUMN KEYWORKER_STATS.NUM_KW_ENTRIES            IS 'Number of Key worker case note entries';
COMMENT ON COLUMN KEYWORKER_STATS.NUM_ACTIVE_KEYWORKERS     IS 'Number of active keyworkers';
COMMENT ON COLUMN KEYWORKER_STATS.RECPT_TO_ALLOC_DAYS       IS 'Number of days (avg) between reception and allocation of a keyworker';
COMMENT ON COLUMN KEYWORKER_STATS.RECPT_TO_KW_SESSION_DAYS  IS 'Number of days (avg) between reception and first Key worker session';