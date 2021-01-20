/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import java.sql.Types;

import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qbean.MNode;

/**
 * Querydsl query type for M_OBJECT table.
 */
@SuppressWarnings("unused")
public class QNode extends QObject<MNode> {

    private static final long serialVersionUID = 2042159341967925185L;

    public static final String TABLE_NAME = "m_node";

    public static final ColumnMetadata NODE_IDENTIFIER =
            ColumnMetadata.named("nodeIdentifier").ofType(Types.VARCHAR).withSize(255);

    // columns and relations

    public final StringPath nodeIdentifier = createString("nodeIdentifier", NODE_IDENTIFIER);

    public final PrimaryKey<MNode> pk = createPrimaryKey(oid);
    public final ForeignKey<QQName> qNameFk = createForeignKey(createChannelId, QQName.ID.getName());

    public QNode(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QNode(String variable, String schema, String table) {
        super(MNode.class, variable, schema, table);
    }
}
