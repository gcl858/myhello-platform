Ensure migration scripts are developed to support several executions over the same database without any error.
This feature will make sure this migration execution would be compatible with other needed flyway migrations without broking the chain.

IMPORTANT: Due to the strong dependency between this module and persistence-commons-sqlite please be sure that any new
Flyway migration added here uses a consistent version that doesn't collide with persistence-commons-sqlite

This folder contains the SQLite port of the Data Index Flyway migrations. They are
equivalent in semantics to the postgresql/ folder; only the SQL dialect differs.
SQLite-specific notes:
  * `bytea` -> BLOB, `jsonb` -> TEXT, `varchar(n)` -> TEXT, `int4/int8` -> INTEGER,
    `timestamp` -> TEXT, `boolean` -> INTEGER (0/1).
  * SQLite supports `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS`
    natively, so migrations are simpler than the PG / MSSQL equivalents.
  * No reserved-word brackets needed.
  * `ALTER TABLE ... DROP CONSTRAINT` is not supported; foreign keys are declared
    inline in CREATE TABLE and the runtime must enable them with
    `PRAGMA foreign_keys = ON` per connection.
  * Requires SQLite 3.38+ for the JSON1 extension (`json_extract`, `json_group_object`,
    `json_each`, `json_valid`).
