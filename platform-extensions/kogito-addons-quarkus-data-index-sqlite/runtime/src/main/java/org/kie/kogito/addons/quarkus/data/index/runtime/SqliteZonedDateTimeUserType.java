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

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * Custom Hibernate {@link UserType} that maps {@link ZonedDateTime} ↔
 * {@code Long epoch-ms} for SQLite {@code TEXT} columns.
 *
 * <p>
 * Used by
 * {@link SqliteZonedDateTimeIntegrationListener} to globally override the
 * default ZonedDateTime handling in the kogito data-index scope.
 *
 * <p>
 * Why a UserType instead of a JdbcType? Because
 * {@code SimpleValue.setJdbcType(JdbcType)} doesn't exist in Hibernate 7 —
 * the JdbcType is locked at metadata-build time. The UserType is a
 * higher-level abstraction that lets us replace the entire type
 * (Java-side + JDBC-side) at runtime via the type registry.
 */
public class SqliteZonedDateTimeUserType implements UserType<ZonedDateTime> {

    public static final SqliteZonedDateTimeUserType INSTANCE = new SqliteZonedDateTimeUserType();

    @Override
    public int getSqlType() {
        return Types.BIGINT;
    }

    @Override
    public Class<ZonedDateTime> returnedClass() {
        return ZonedDateTime.class;
    }

    @Override
    public boolean equals(ZonedDateTime x, ZonedDateTime y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(ZonedDateTime x) throws HibernateException {
        return Objects.hashCode(x);
    }

    @Override
    public ZonedDateTime nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        long epochMs = rs.getLong(position);
        if (rs.wasNull() || epochMs == 0L) {
            return null;
        }
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault());
    }

    @Override
    public void nullSafeSet(PreparedStatement st, ZonedDateTime value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.BIGINT);
        } else {
            st.setLong(index, value.toInstant().toEpochMilli());
        }
    }

    @Override
    public ZonedDateTime deepCopy(ZonedDateTime value) throws HibernateException {
        return value == null ? null : value.withZoneSameInstant(ZoneId.systemDefault());
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(ZonedDateTime value) throws HibernateException {
        return value == null ? null : value.toInstant().toEpochMilli();
    }

    @Override
    public ZonedDateTime assemble(Serializable cached, Object owner) throws HibernateException {
        if (cached == null) {
            return null;
        }
        return Instant.ofEpochMilli((Long) cached).atZone(ZoneId.systemDefault());
    }
}
