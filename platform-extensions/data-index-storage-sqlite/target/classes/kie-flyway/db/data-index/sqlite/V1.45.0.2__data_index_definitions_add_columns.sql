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

-- V1.45.0.2__data_index_definitions_add_columns.sql (SQLite port)
-- No reserved-word brackets needed in SQLite.

CREATE TABLE IF NOT EXISTS definitions_annotations (
    value            TEXT NOT NULL,
    process_id       TEXT NOT NULL,
    process_version  TEXT NOT NULL,
    PRIMARY KEY (value, process_id, process_version),
    FOREIGN KEY (process_id, process_version) REFERENCES definitions(id, version) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS definitions_metadata (
    process_id       TEXT NOT NULL,
    process_version  TEXT NOT NULL,
    value            TEXT,
    key              TEXT NOT NULL,
    PRIMARY KEY (process_id, process_version, key),
    FOREIGN KEY (process_id, process_version) REFERENCES definitions(id, version) ON DELETE CASCADE
);

ALTER TABLE definitions ADD COLUMN description TEXT;
