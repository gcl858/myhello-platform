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

-- V1.45.0.4__increase_varchar_length.sql (SQLite port)
--
-- The PG version widens the column to varchar(65535) which is effectively unbounded.
-- SQLite has no column-length enforcement on TEXT — it is always unbounded. Therefore
-- this migration is a no-op for the SQLite port; it is kept (empty) to preserve the
-- migration version sequence across dialects.

-- (no-op)
