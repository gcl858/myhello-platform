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

import org.kie.kogito.addons.quarkus.data.index.runtime.SqliteZonedDateTimeIntegrationListener;

import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;

/**
 * Quarkus deployment processor that registers the
 * {@link SqliteZonedDateTimeIntegrationListener} with Hibernate ORM.
 *
 * <p>
 * Quarkus's {@code HibernateOrmIntegrationStaticConfiguredBuildItem} is
 * the canonical way for an extension to participate in the Hibernate
 * {@code Metadata} post-processing pipeline at runtime.
 */
public class SqliteZonedDateTimeIntegrationProcessor {

    // @BuildStep
    // Disabled experimental ZonedDateTime reflection listener to prevent bootstrap / reflection risks.
    public HibernateOrmIntegrationStaticConfiguredBuildItem register() {
        return null;
    }
}
