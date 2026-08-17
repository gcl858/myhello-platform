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

import java.util.Iterator;
import java.util.List;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SqlTypes;

/**
 * Hibernate custom function for JSON containment checks on SQLite.
 *
 * Renders SQL fragments equivalent to PostgreSQL's jsonb @> / ?& / ?| operators
 * using the SQLite JSON1 extension ({@code json_each}, {@code json_array}, ...).
 * <ul>
 * <li>{@code contains(j, val)} → checks if {@code j} (TEXT JSON column) contains
 * an element whose value equals {@code val}.</li>
 * <li>{@code containsAll(j, v1, v2, ...)} → checks if {@code j} contains <b>all</b>
 * of the supplied values (AND of EXISTS checks).</li>
 * <li>{@code containsAny(j, v1, v2, ...)} → checks if {@code j} contains <b>any</b>
 * of the supplied values (single EXISTS + IN join).</li>
 * </ul>
 *
 * Requires SQLite 3.38+ for the JSON1 extension; the {@code sqlite-jdbc} 3.40+ driver
 * bundled with Quarkus always satisfies this.
 */
public class ContainsSQLFunction extends StandardSQLFunction {

    static final String CONTAINS_NAME = "contains";
    static final String CONTAINS_ALL_NAME = "containsAll";
    static final String CONTAINS_ANY_NAME = "containsAny";

    private static final BasicTypeReference<Boolean> RETURN_TYPE =
            new BasicTypeReference<>("boolean", Boolean.class, SqlTypes.BOOLEAN);

    private final boolean all;

    ContainsSQLFunction(String name, boolean all) {
        super(name, RETURN_TYPE);
        this.all = all;
    }

    @Override
    public void render(
            SqlAppender sqlAppender,
            List<? extends SqlAstNode> args,
            ReturnableType<?> returnType,
            SqlAstTranslator<?> translator) {
        int size = args.size();
        if (size < 2) {
            throw new IllegalArgumentException("Function " + getName() + " requires at least two arguments");
        }
        // First arg is the JSON document (TEXT). It is re-emitted inline for each
        // EXISTS check, which is safe in SQLite.
        Iterator<? extends SqlAstNode> iter = args.iterator();
        SqlAstNode jsonDoc = iter.next();

        if (all && size > 2) {
            // containsAll: AND across individual EXISTS checks (one per value).
            sqlAppender.append('(');
            boolean first = true;
            while (iter.hasNext()) {
                if (!first) {
                    sqlAppender.append(" AND ");
                }
                sqlAppender.append("json_valid(");
                jsonDoc.accept(translator);
                sqlAppender.append(") = 1 AND EXISTS (SELECT 1 FROM json_each(");
                jsonDoc.accept(translator);
                sqlAppender.append(") AS doc WHERE CAST(doc.value AS TEXT) = CAST(");
                iter.next().accept(translator);
                sqlAppender.append(" AS TEXT))");
                first = false;
            }
            sqlAppender.append(')');
        } else {
            // contains (single value) or containsAny (multi value): single EXISTS with
            // INNER JOIN against a json_array of the candidate values.
            sqlAppender.append("(json_valid(");
            jsonDoc.accept(translator);
            sqlAppender.append(") = 1 AND EXISTS (SELECT 1 FROM json_each(");
            jsonDoc.accept(translator);
            sqlAppender.append(") AS doc INNER JOIN json_each(json_array(");
            boolean first = true;
            while (iter.hasNext()) {
                if (!first) {
                    sqlAppender.append(", ");
                }
                iter.next().accept(translator);
                first = false;
            }
            sqlAppender.append(")) AS arr ON CAST(doc.value AS TEXT) = CAST(arr.value AS TEXT)))");
        }
    }
}
