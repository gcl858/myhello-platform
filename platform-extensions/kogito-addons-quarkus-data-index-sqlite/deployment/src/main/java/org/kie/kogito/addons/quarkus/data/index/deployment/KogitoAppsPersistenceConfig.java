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
package org.kie.kogito.addons.quarkus.data.index.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Quarkus config root that registers {@code kogito.apps.persistence.type} as a
 * known configuration key — quieting the
 * {@code "Unrecognized configuration key kogito.apps.persistence.type"}
 * warning emitted by {@code io.quarkus.config} on every boot.
 *
 * <p>
 * The sibling runtime jar
 * {@code kogito-addons-quarkus-data-index-sqlite-10.2.0.jar} ships
 * {@code META-INF/resources/application.properties} with
 * {@code kogito.apps.persistence.type=sqlite} (read at runtime via
 * {@link org.kie.kogito.persistence.api.factory.Constants#PERSISTENCE_TYPE_PROPERTY}).
 * Quarkus has no {@code @ConfigMapping} for that key, so it warns every boot.
 *
 * <p>
 * By defining a top-level {@code @ConfigRoot(prefix = "kogito")} config
 * root with an {@code apps.persistence.type} sub-mapping, the build-time
 * config discovery picks this up and the key is recorded as "known".
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "kogito.apps.persistence")
public interface KogitoAppsPersistenceConfig {

    /**
     * Kogito storage backend selection. Read at runtime by
     * {@code org.kie.kogito.persistence.api.factory.KogitoPersistenceEventListener}
     * (and friends) via {@code org.eclipse.microprofile.config.Config}.
     *
     * <p>
     * Valid values: {@code inmemory}, {@code postgresql}, {@code mongodb},
     * {@code sqlite}. Default is {@code sqlite} (set by the bundled
     * {@code META-INF/resources/application.properties}).
     */
    @WithName("type")
    @WithDefault("sqlite")
    String type();
}
