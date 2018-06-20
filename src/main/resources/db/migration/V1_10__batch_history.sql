DROP TABLE IF EXISTS BATCH_HISTORY;

CREATE TABLE BATCH_HISTORY
(
  BATCH_ID     BIGSERIAL    NOT NULL,
  NAME         VARCHAR(40)  NOT NULL,
  LAST_RUN     TIMESTAMP    NOT NULL,
  CONSTRAINT BATCH_HISTORY_PK PRIMARY KEY (BATCH_ID)
);

COMMENT ON TABLE BATCH_HISTORY IS 'Records the results of batch runs';

COMMENT ON COLUMN BATCH_HISTORY.BATCH_ID           IS 'Primary key ID';
COMMENT ON COLUMN BATCH_HISTORY.NAME               IS 'Name of batch process';
COMMENT ON COLUMN BATCH_HISTORY.LAST_RUN           IS 'Timestamp of when last run successfully';