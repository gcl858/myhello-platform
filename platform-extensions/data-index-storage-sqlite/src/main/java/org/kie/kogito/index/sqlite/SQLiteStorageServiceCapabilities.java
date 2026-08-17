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
package org.kie.kogito.index.sqlite;

import java.util.Set;

import org.kie.kogito.persistence.api.StorageServiceCapability;
import org.kie.kogito.persistence.api.StorageServiceCapabilityProvider;

/**
 * Capability provider for the SQLite-backed Data Index storage.
 *
 * SQLite supports JSON path queries via the built-in JSON1 extension
 * ({@code json_extract}, {@code json_each}, ...), so we declare
 * {@link StorageServiceCapability#JSON_QUERY} the same way the PostgreSQL provider does.
 */
public class SQLiteStorageServiceCapabilities implements StorageServiceCapabilityProvider {

    @Override
    public Set<StorageServiceCapability> capabilities() {
        return Set.of(StorageServiceCapability.JSON_QUERY);
    }
}
