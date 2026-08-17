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

-- V1.45.0.8__modify_columns_with_reserved_words.sql (SQLite port)
-- SQLite 3.25+ supports ALTER TABLE ... RENAME COLUMN. The MSSQL port used
-- sp_rename with brackets around [key]; SQLite needs neither (key is not reserved).
-- In SQLite older than 3.25 the workaround is to recreate the table. The Quarkus
-- sqlite-jdbc 3.40+ driver always provides 3.25+.

ALTER TABLE definitions_nodes_metadata RENAME COLUMN key   TO name;
ALTER TABLE definitions_nodes_metadata RENAME COLUMN value TO meta_value;
ALTER TABLE definitions_metadata       RENAME COLUMN key   TO name;
ALTER TABLE definitions_metadata       RENAME COLUMN value TO meta_value;
ALTER TABLE definitions_annotations    RENAME COLUMN value TO annotation;
