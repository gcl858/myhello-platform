package org.acme.platform.dataindex;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Custom Hibernate 6 JdbcType for SQLite ZonedDateTime fields.
 * Reads epoch-ms strings (e.g. "1786185258763") or ISO-8601 strings from SQLite TEXT columns
 * without throwing "Error parsing time stamp" (DateTimeParseException).
 */
public class SqliteZonedDateTimeJdbcType implements JdbcType {

    public static final SqliteZonedDateTimeJdbcType INSTANCE = new SqliteZonedDateTimeJdbcType();

    @Override
    public int getJdbcTypeCode() {
        return Types.VARCHAR;
    }

    @Override
    public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
        return new BasicBinder<X>(javaType, this) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                if (value instanceof ZonedDateTime) {
                    st.setString(index, String.valueOf(((ZonedDateTime) value).toInstant().toEpochMilli()));
                } else if (value != null) {
                    st.setString(index, value.toString());
                } else {
                    st.setNull(index, Types.VARCHAR);
                }
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
                if (value instanceof ZonedDateTime) {
                    st.setString(name, String.valueOf(((ZonedDateTime) value).toInstant().toEpochMilli()));
                } else if (value != null) {
                    st.setString(name, value.toString());
                } else {
                    st.setNull(name, Types.VARCHAR);
                }
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
        return new BasicExtractor<X>(javaType, this) {
            @Override
            protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
                String str = rs.getString(paramIndex);
                return parseValue(str, javaType, options);
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                String str = statement.getString(index);
                return parseValue(str, javaType, options);
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                String str = statement.getString(name);
                return parseValue(str, javaType, options);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <X> X parseValue(String str, JavaType<X> javaType, WrapperOptions options) {
        if (str == null) {
            return null;
        }
        ZonedDateTime zdt = null;
        try {
            long epochMs = Long.parseLong(str);
            zdt = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            try {
                zdt = ZonedDateTime.parse(str);
            } catch (Exception ex) {
                // Ignore parse exception
            }
        }
        if (zdt == null) {
            return null;
        }
        if (javaType.getJavaTypeClass().equals(ZonedDateTime.class)) {
            return (X) zdt;
        }
        return javaType.wrap(zdt, options);
    }
}
