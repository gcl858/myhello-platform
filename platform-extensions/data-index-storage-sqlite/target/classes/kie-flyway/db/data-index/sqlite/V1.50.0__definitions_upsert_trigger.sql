-- V1.50.0__definitions_upsert_trigger.sql
-- Prevent SQLITE_CONSTRAINT_PRIMARYKEY (definitions.id, definitions.version) when
-- Kogito re-registers workflow definitions on startup.
-- Deleting pre-existing row BEFORE insert ensures INSERT succeeds with affected_rows=1.

CREATE TRIGGER IF NOT EXISTS definitions_upsert_trigger
BEFORE INSERT ON definitions
FOR EACH ROW
WHEN EXISTS (SELECT 1 FROM definitions WHERE id = NEW.id AND version = NEW.version)
BEGIN
    DELETE FROM definitions WHERE id = NEW.id AND version = NEW.version;
END;
