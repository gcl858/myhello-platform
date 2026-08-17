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

-- V1.45.1.0__metadata_as_jsonb.sql (SQLite port)
--
-- Original PG: collapses a key/value table (definitions_metadata) into a single
--              `metadata jsonb` column on `definitions`. Aggregation uses
--              `json_object_agg(name, meta_value)`.
-- SQLite port: the `metadata` column becomes TEXT and is populated with a JSON
--              object string. We aggregate using SQLite's built-in `json_group_object`
--              (JSON1 extension, available since SQLite 3.38).
--
--              Result: `{"name1":"value1","name2":"value2",...}`.
--
-- WARNING: this script does basic JSON escaping via json_quote() for the values
--          and inserts the keys as-is (Kogito does not produce keys with quotes
--          in practice). If your data contains special characters in the keys,
--          you should sanitise on the application side.

ALTER TABLE definitions ADD COLUMN metadata TEXT;

-- Aggregate definitions_metadata rows into a single JSON object per (process_id, process_version).
UPDATE definitions
SET metadata = (
    SELECT json_group_object(name, json_quote(meta_value))
    FROM definitions_metadata
    WHERE definitions_metadata.process_id = definitions.id
      AND definitions_metadata.process_version = definitions.version
);

DROP TABLE definitions_metadata;
