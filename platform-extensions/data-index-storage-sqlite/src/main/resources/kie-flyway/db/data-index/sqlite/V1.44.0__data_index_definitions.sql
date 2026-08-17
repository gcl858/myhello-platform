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

-- V1.44.0__data_index_definitions.sql (SQLite port)
-- bytea -> BLOB, varchar -> TEXT.

CREATE TABLE IF NOT EXISTS definitions (
    id        TEXT NOT NULL,
    version   TEXT NOT NULL,
    name      TEXT,
    type      TEXT,
    source    BLOB, -- was bytea
    endpoint  TEXT,
    PRIMARY KEY (id, version)
);

CREATE TABLE IF NOT EXISTS definitions_addons (
    process_id       TEXT NOT NULL,
    process_version  TEXT NOT NULL,
    addon            TEXT NOT NULL,
    PRIMARY KEY (process_id, process_version, addon),
    FOREIGN KEY (process_id, process_version) REFERENCES definitions(id, version) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS definitions_roles (
    process_id       TEXT NOT NULL,
    process_version  TEXT NOT NULL,
    role             TEXT NOT NULL,
    PRIMARY KEY (process_id, process_version, role),
    FOREIGN KEY (process_id, process_version) REFERENCES definitions(id, version) ON DELETE CASCADE
);

-- Add version column to processes
ALTER TABLE processes ADD COLUMN version TEXT;
