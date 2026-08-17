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

-- V1.45.0.0__data_index_node_definitions.sql (SQLite port)

CREATE TABLE IF NOT EXISTS definitions_nodes (
    id               TEXT NOT NULL,
    name             TEXT,
    unique_id        TEXT,
    type             TEXT,
    process_id       TEXT NOT NULL,
    process_version  TEXT NOT NULL,
    PRIMARY KEY (id, process_id, process_version),
    FOREIGN KEY (process_id, process_version) REFERENCES definitions(id, version) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS definitions_nodes_metadata (
    node_id          TEXT NOT NULL,
    process_id       TEXT NOT NULL,
    process_version  TEXT NOT NULL,
    value            TEXT,
    key              TEXT NOT NULL,
    PRIMARY KEY (node_id, process_id, process_version, key),
    FOREIGN KEY (node_id, process_id, process_version) REFERENCES definitions_nodes(id, process_id, process_version) ON DELETE CASCADE
);
