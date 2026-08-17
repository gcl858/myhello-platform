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
package org.kie.kogito.addons.quarkus.data.index.runtime;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;

/**
 * Runtime integration that fixes the kogito SQLite data-index
 * {@code ZonedDateTime} round-trip.
 *
 * <p>
 * Strategy:
 * <ol>
 * <li>Register a custom {@link JavaType} for {@code ZonedDateTime} in
 * the {@link TypeConfiguration}'s JavaTypeRegistry. The custom
 * JavaType handles {@code wrap(X, WrapperOptions)} for the
 * {@code Long} case (epoch ms) and the {@code String} case
 * (numeric string like {@code "1786185258763"}).</li>
 * <li>Use reflection to clear the cached {@code type} field of every
 * {@code SimpleValue} for the target kogito data-index entities
 * so the next call to {@code SimpleValue.getType()} re-resolves
 * and picks up our new JavaType.</li>
 * </ol>
 *
 * <p>
 * Why this works: the JdbcType's {@code extract} method calls
 * {@code javaType.wrap(rs.getString(col), options)} (via the
 * {@link org.hibernate.type.descriptor.jdbc.BasicExtractor#doExtract}
 * path) — by providing a {@code wrap} implementation that accepts the
 * numeric-string form, the read succeeds.
 */
public class SqliteZonedDateTimeIntegrationListener implements HibernateOrmIntegrationStaticInitListener {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(SqliteZonedDateTimeIntegrationListener.class);

    /** FQNs of entities whose ZonedDateTime fields need patching. */
    private static final Set<String> TARGET_ENTITIES = new HashSet<>();
    static {
        TARGET_ENTITIES.add("org.kie.kogito.index.jpa.model.ProcessInstanceEntity");
        TARGET_ENTITIES.add("org.kie.kogito.index.jpa.model.UserTaskEntity");
        TARGET_ENTITIES.add("org.kie.kogito.index.jpa.model.JobEntity");
    }

    @Override
    public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
        // No boot properties needed.
    }

    @Override
    public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
            BiConsumer<String, Object> propertyCollector) {
        LOG.infof("SqliteZonedDateTimeIntegrationListener: onMetadataInitialized called");

        // Register the custom ZonedDateTime JavaType
        TypeConfiguration typeConfig = bootstrapContext.getTypeConfiguration();
        JavaType<java.time.ZonedDateTime> customType = new SqliteZonedDateTimeJavaType();
        typeConfig.getJavaTypeRegistry().addDescriptor(customType);
        LOG.infof("SqliteZonedDateTimeIntegrationListener: registered custom ZonedDateTime JavaType");

        // Clear the cached Type on every target SimpleValue so the new
        // JavaType is picked up on next access.
        int cleared = 0;
        for (PersistentClass pc : metadata.getEntityBindings()) {
            if (!TARGET_ENTITIES.contains(pc.getEntityName())) {
                continue;
            }
            for (Property prop : pc.getProperties()) {
                if (java.time.ZonedDateTime.class.equals(prop.getType().getReturnedClass())) {
                    Value value = prop.getValue();
                    if (value instanceof SimpleValue) {
                        SimpleValue sv = (SimpleValue) value;
                        if (clearCachedType(sv)) {
                            cleared++;
                            LOG.infof("  - cleared cached type on " + pc.getEntityName() + "." + prop.getName());
                        }
                    }
                }
            }
        }
        LOG.infof("SqliteZonedDateTimeIntegrationListener: cleared cached Type on " + cleared + " SimpleValues");
    }

    private static boolean clearCachedType(SimpleValue sv) {
        try {
            Field f = SimpleValue.class.getDeclaredField("type");
            f.setAccessible(true);
            Object current = f.get(sv);
            if (current != null) {
                f.set(sv, null);
                return true;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOG.warnf("Could not clear SimpleValue.type: " + e.getMessage());
        }
        return false;
    }
}
