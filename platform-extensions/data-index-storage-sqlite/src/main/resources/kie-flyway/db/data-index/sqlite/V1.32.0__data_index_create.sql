/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

-- V1.32.0__data_index_create.sql (SQLite port)
--
-- This is much simpler than the PostgreSQL/MS-SQL original:
--   * SQLite supports `CREATE TABLE IF NOT EXISTS` natively, so the
--     drop-then-add-foreign-key pattern is unnecessary.
--   * Foreign keys are declared inline; the runtime must enable them with
--     `PRAGMA foreign_keys = ON` on every connection (Quarkus config).
--   * No reserved-word brackets needed: `key`, `value`, etc. are not reserved in SQLite.
--   * Types: bytea -> BLOB, jsonb -> TEXT, varchar(n) -> TEXT, int4/int8 -> INTEGER,
--             timestamp -> TEXT, boolean -> INTEGER (0/1).

CREATE TABLE IF NOT EXISTS attachments (
    id          TEXT NOT NULL,
    content     TEXT,
    name        TEXT,
    updated_at  TEXT,
    updated_by  TEXT,
    task_id     TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comments (
    id          TEXT NOT NULL,
    content     TEXT,
    name        TEXT,
    updated_at  TEXT,
    updated_by  TEXT,
    task_id     TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS jobs (
    id                        TEXT NOT NULL,
    callback_endpoint         TEXT,
    endpoint                  TEXT,
    execution_counter         INTEGER,
    expiration_time           TEXT,
    last_update               TEXT,
    node_instance_id          TEXT,
    priority                  INTEGER,
    process_id                TEXT,
    process_instance_id       TEXT,
    repeat_interval           INTEGER,
    repeat_limit              INTEGER,
    retries                   INTEGER,
    root_process_id           TEXT,
    root_process_instance_id  TEXT,
    scheduled_id              TEXT,
    status                    TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS milestones (
    id                   TEXT NOT NULL,
    process_instance_id  TEXT NOT NULL,
    name                 TEXT,
    status               TEXT,
    PRIMARY KEY (id, process_instance_id),
    FOREIGN KEY (process_instance_id) REFERENCES processes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS nodes (
    id                   TEXT NOT NULL,
    definition_id        TEXT,
    enter                TEXT,
    exit                 TEXT,
    name                 TEXT,
    node_id              TEXT,
    type                 TEXT,
    process_instance_id  TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (process_instance_id) REFERENCES processes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS processes (
    id                          TEXT NOT NULL,
    business_key                TEXT,
    end_time                    TEXT,
    endpoint                    TEXT,
    message                     TEXT,
    node_definition_id          TEXT,
    last_update_time            TEXT,
    parent_process_instance_id  TEXT,
    process_id                  TEXT,
    process_name                TEXT,
    root_process_id             TEXT,
    root_process_instance_id    TEXT,
    start_time                  TEXT,
    state                       INTEGER,
    variables                   TEXT, -- was jsonb
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS processes_addons (
    process_id  TEXT NOT NULL,
    addon       TEXT NOT NULL,
    PRIMARY KEY (process_id, addon),
    FOREIGN KEY (process_id) REFERENCES processes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS processes_roles (
    process_id  TEXT NOT NULL,
    role        TEXT NOT NULL,
    PRIMARY KEY (process_id, role),
    FOREIGN KEY (process_id) REFERENCES processes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tasks (
    id                        TEXT NOT NULL,
    actual_owner              TEXT,
    completed                 TEXT,
    description               TEXT,
    endpoint                  TEXT,
    inputs                    TEXT, -- was jsonb
    last_update               TEXT,
    name                      TEXT,
    outputs                   TEXT, -- was jsonb
    priority                  TEXT,
    process_id                TEXT,
    process_instance_id       TEXT,
    reference_name            TEXT,
    root_process_id           TEXT,
    root_process_instance_id  TEXT,
    started                   TEXT,
    state                     TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS tasks_admin_groups (
    task_id   TEXT NOT NULL,
    group_id  TEXT NOT NULL,
    PRIMARY KEY (task_id, group_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tasks_admin_users (
    task_id  TEXT NOT NULL,
    user_id  TEXT NOT NULL,
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tasks_excluded_users (
    task_id  TEXT NOT NULL,
    user_id  TEXT NOT NULL,
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tasks_potential_groups (
    task_id   TEXT NOT NULL,
    group_id  TEXT NOT NULL,
    PRIMARY KEY (task_id, group_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tasks_potential_users (
    task_id  TEXT NOT NULL,
    user_id  TEXT NOT NULL,
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);
