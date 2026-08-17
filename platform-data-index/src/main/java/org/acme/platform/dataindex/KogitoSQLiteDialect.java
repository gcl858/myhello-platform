package org.acme.platform.dataindex;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.service.ServiceRegistry;
import org.kie.kogito.addons.quarkus.data.index.runtime.SqliteZonedDateTimeJavaType;

/**
 * Custom SQLite dialect for Kogito Data Index that registers
 * {@link SqliteZonedDateTimeJdbcType} and {@link SqliteZonedDateTimeJavaType}
 * during Hibernate Metadata building.
 *
 * <p>
 * This allows Hibernate ORM 6+ to seamlessly parse both Unix epoch ms strings
 * (e.g. "1786185258763") and ISO-8601 strings when reading {@code ZonedDateTime}
 * fields from SQLite TEXT columns, resolving the GraphQL ProcessInstances
 * timestamp parsing bug (#9).
 */
public class KogitoSQLiteDialect extends SQLiteDialect {

    public KogitoSQLiteDialect() {
        super();
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        typeContributions.contributeJdbcType(SqliteZonedDateTimeJdbcType.INSTANCE);
        typeContributions.contributeJavaType(SqliteZonedDateTimeJavaType.INSTANCE);

        org.hibernate.type.BasicType<java.time.ZonedDateTime> customBasicType = new org.hibernate.type.internal.BasicTypeImpl<>(
                SqliteZonedDateTimeJavaType.INSTANCE,
                SqliteZonedDateTimeJdbcType.INSTANCE
        );
        typeContributions.contributeType(customBasicType, "zdt", "ZonedDateTime", java.time.ZonedDateTime.class.getName());
    }
}
