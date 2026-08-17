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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Custom Hibernate {@link JavaType} for {@link ZonedDateTime} that:
 * <ul>
 * <li>Reads numeric strings (epoch ms) from SQLite TEXT columns —
 * default ZonedDateTime JavaType fails with
 * {@code "Error parsing time stamp"}.</li>
 * <li>Writes epoch-ms via the {@code Long} channel.</li>
 * </ul>
 *
 * <p>
 * Used by
 * {@link SqliteZonedDateTimeIntegrationListener} via the
 * {@link org.hibernate.type.spi.TypeConfiguration} JavaTypeRegistry.
 */
public class SqliteZonedDateTimeJavaType implements JavaType<ZonedDateTime> {

    public static final SqliteZonedDateTimeJavaType INSTANCE = new SqliteZonedDateTimeJavaType();

    @Override
    public Class<ZonedDateTime> getJavaTypeClass() {
        return ZonedDateTime.class;
    }

    @Override
    public Comparator<ZonedDateTime> getComparator() {
        return Comparator.naturalOrder();
    }

    @Override
    public MutabilityPlan<ZonedDateTime> getMutabilityPlan() {
        return ImmutableMutabilityPlan.instance();
    }

    @Override
    public ZonedDateTime fromString(CharSequence s) {
        return parseIso(s.toString());
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
        return SqliteZonedDateTimeJdbcType.INSTANCE;
    }

    @Override
    public <X> X unwrap(ZonedDateTime value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (type == Instant.class) {
            return type.cast(value.toInstant());
        }
        if (type == java.time.OffsetDateTime.class) {
            return type.cast(value.toOffsetDateTime());
        }
        if (type == java.time.LocalDateTime.class) {
            return type.cast(value.toLocalDateTime());
        }
        if (type == Long.class || type == long.class) {
            return type.cast(value.toInstant().toEpochMilli());
        }
        if (type == Timestamp.class) {
            return type.cast(Timestamp.from(value.toInstant()));
        }
        if (type == java.util.Date.class) {
            return type.cast(java.util.Date.from(value.toInstant()));
        }
        if (type == String.class) {
            return type.cast(value.toInstant().toEpochMilli() + "");
        }
        throw new UnsupportedOperationException("Cannot unwrap ZonedDateTime to " + type);
    }

    @Override
    public <X> ZonedDateTime wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (value instanceof ZonedDateTime) {
            return (ZonedDateTime) value;
        }
        if (value instanceof Instant) {
            return ((Instant) value).atZone(ZoneId.systemDefault());
        }
        if (value instanceof java.time.OffsetDateTime) {
            return ((java.time.OffsetDateTime) value).toZonedDateTime();
        }
        if (value instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) value).atZone(ZoneId.systemDefault());
        }
        if (value instanceof Long) {
            return Instant.ofEpochMilli((Long) value).atZone(ZoneId.systemDefault());
        }
        if (value instanceof Number) {
            return Instant.ofEpochMilli(((Number) value).longValue()).atZone(ZoneId.systemDefault());
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toInstant().atZone(ZoneId.systemDefault());
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant().atZone(ZoneId.systemDefault());
        }
        if (value instanceof String) {
            String s = (String) value;
            // The kogito SQLite data-index stores epoch ms as a numeric
            // string like "1786185258763" in a TEXT column. Try parsing
            // it as a Long first.
            try {
                long epochMs = Long.parseLong(s);
                return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault());
            } catch (NumberFormatException nfe) {
                // Fall through to default behavior for ISO-8601 strings
            }
            // Last-ditch effort: delegate to the default ZonedDateTime JavaType
            return parseIso(s);
        }
        throw new UnsupportedOperationException("Cannot wrap " + value.getClass() + " to ZonedDateTime");
    }

    private static ZonedDateTime parseIso(String s) {
        try {
            // Try ISO_OFFSET_DATE_TIME (e.g. "2024-01-15T10:30:00+00:00")
            return ZonedDateTime.parse(s);
        } catch (Exception e1) {
            try {
                // Try ISO_LOCAL_DATE_TIME (e.g. "2024-01-15T10:30:00")
                return ZonedDateTime.parse(s);
            } catch (Exception e2) {
                throw new RuntimeException("Could not parse ZonedDateTime from: " + s, e2);
            }
        }
    }
}
