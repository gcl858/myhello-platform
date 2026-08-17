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

-- V1.45.0.3__add_fk_index.sql (SQLite port)
-- CREATE INDEX IF NOT EXISTS is supported natively in SQLite.

CREATE INDEX IF NOT EXISTS idx_attachments_tid ON attachments(task_id);
CREATE INDEX IF NOT EXISTS idx_comments_tid ON comments(task_id);
CREATE INDEX IF NOT EXISTS idx_definitions_addons_pid_pv ON definitions_addons(process_id, process_version);
CREATE INDEX IF NOT EXISTS idx_definitions_annotations_pid_pv ON definitions_annotations(process_id, process_version);
CREATE INDEX IF NOT EXISTS idx_definitions_metadata_pid_pv ON definitions_metadata(process_id, process_version);
CREATE INDEX IF NOT EXISTS idx_definitions_nodes_pid_pv ON definitions_nodes(process_id, process_version);
CREATE INDEX IF NOT EXISTS idx_definitions_nodes_metadata_pid_pv ON definitions_nodes_metadata(process_id, process_version);
CREATE INDEX IF NOT EXISTS idx_definitions_roles_pid_pv ON definitions_roles(process_id, process_version);
CREATE INDEX IF NOT EXISTS idx_milestones_piid ON milestones(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_nodes_piid ON nodes(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_processes_addons_pid ON processes_addons(process_id);
CREATE INDEX IF NOT EXISTS idx_processes_roles_pid ON processes_roles(process_id);
CREATE INDEX IF NOT EXISTS idx_tasks_admin_groups_tid ON tasks_admin_groups(task_id);
CREATE INDEX IF NOT EXISTS idx_tasks_admin_users_tid ON tasks_admin_users(task_id);
CREATE INDEX IF NOT EXISTS idx_tasks_excluded_users_tid ON tasks_excluded_users(task_id);
CREATE INDEX IF NOT EXISTS idx_tasks_potential_groups_tid ON tasks_potential_groups(task_id);
CREATE INDEX IF NOT EXISTS idx_tasks_potential_users_tid ON tasks_potential_users(task_id);
