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
package org.kie.kogito.addons.quarkus.data.index.persistence.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * Quarkus deployment processor for the SQLite Data Index persistence add-on.
 *
 * This is the SQLite counterpart of {@code PostgreSQLDataIndexPersistenceProcessor}.
 * Behaviour is identical; only the feature name and the inherited
 * {@code AbstractKogitoAddonsQuarkusDataIndexPersistenceProcessor} configuration differ.
 *
 * Note: SQLite is normally not used in native-image production builds, but the
 * native-resources step is still wired in for parity with the other dialects.
 */
public class SQLiteDataIndexPersistenceProcessor extends AbstractKogitoAddonsQuarkusDataIndexPersistenceProcessor {

    private static final String FEATURE = "kogito-addons-quarkus-data-index-persistence-sqlite";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void addNativeResources(BuildProducer<NativeImageResourcePatternsBuildItem> resources) {
        resources.produce(new NativeImageResourcePatternsBuildItem.Builder().includeGlob("sql/*.sql").build());
    }

}
