/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.wire.h2db.store;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.internal.wire.db.store.DbDataTypeMapper;
import org.eclipse.kura.internal.wire.db.store.DbDataTypeMapper.JdbcType;
import org.eclipse.kura.internal.wire.db.store.DbWireRecordStoreOptions;
import org.eclipse.kura.internal.wire.h2db.common.H2DbServiceHelper;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class H2DbWireRecordStore is a wire component which is responsible to store
 * the received {@link WireRecord}.
 * 
 * @deprecated this class is deprecated since 2.2. Use {@link org.eclipse.kura.internal.wire.db.store.DbWireRecordStore}
 */
@Deprecated
public class H2DbWireRecordStore implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final String COLUMN_NAME = "COLUMN_NAME";

    private static final String DATA_TYPE = "DATA_TYPE";

    private static final Logger logger = LogManager.getLogger(H2DbWireRecordStore.class);

    private static final String SQL_ADD_COLUMN = "ALTER TABLE {0} ADD COLUMN {1} {2};";

    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS {0} (ID BIGINT GENERATED BY DEFAULT "
            + "AS IDENTITY(START WITH 1 INCREMENT BY 1) PRIMARY KEY, TIMESTAMP BIGINT);";

    private static final String SQL_CREATE_TABLE_INDEX = "CREATE INDEX IF NOT EXISTS {0} ON {1} {2};";

    private static final String SQL_ROW_COUNT_TABLE = "SELECT COUNT(*) FROM {0};";

    private static final String SQL_DELETE_RANGE_TABLE = "DELETE FROM {0} WHERE ID IN (SELECT ID FROM {0} ORDER BY ID ASC LIMIT {1});";

    private static final String SQL_DROP_COLUMN = "ALTER TABLE {0} DROP COLUMN {1};";

    private static final String SQL_INSERT_RECORD = "INSERT INTO {0} ({1}) VALUES ({2});";

    private static final String SQL_TRUNCATE_TABLE = "TRUNCATE TABLE {0};";

    private static final String[] TABLE_TYPE = new String[] { "TABLE" };

    private static final String NULL_TABLE_NAME_ERROR_MSG = "Table name cannot be null";
    private static final String NULL_WIRE_RECORD_ERROR_MSG = "WireRecord cannot be null";

    private H2DbServiceHelper dbHelper;

    private H2DbService dbService;

    private DbWireRecordStoreOptions wireRecordStoreOptions;

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    public synchronized void bindDbService(H2DbService dbService) {
        this.dbService = dbService;
        this.dbHelper = H2DbServiceHelper.of(dbService);
        if (nonNull(this.dbService) && nonNull(this.wireRecordStoreOptions)) {
            reconcileDB(this.wireRecordStoreOptions.getTableName());
        }
    }

    public synchronized void unbindDbService(H2DbService dbService) {
        if (this.dbService == dbService) {
            this.dbHelper = null;
            this.dbService = null;
            this.wireRecordStoreOptions = null;
        }
    }

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi Service Component callback for activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug("Activating DB Wire Record Store...");
        this.wireRecordStoreOptions = new DbWireRecordStoreOptions(properties);

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        if (nonNull(this.dbService)) {
            reconcileDB(this.wireRecordStoreOptions.getTableName());
        }

        logger.debug("Activating DB Wire Record Store... Done");
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated service component properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating DB Wire Record Store...");

        this.wireRecordStoreOptions = new DbWireRecordStoreOptions(properties);

        reconcileDB(this.wireRecordStoreOptions.getTableName());

        logger.debug("Updating DB Wire Record Store... Done");
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("Deactivating DB Wire Record Store...");
        this.dbHelper = null;
        this.dbService = null;
        this.wireRecordStoreOptions = null;
        logger.debug("Deactivating DB Wire Record Store... Done");
    }

    /**
     * Truncates tables containing {@link WireRecord}s
     */
    private void truncate() {
        final int noOfRecordsToKeep = this.wireRecordStoreOptions.getNoOfRecordsToKeep();

        truncate(noOfRecordsToKeep);
    }

    /**
     * Truncates the records in the table
     *
     * @param noOfRecordsToKeep
     *            the no of records to keep in the table
     */
    private void truncate(final int noOfRecordsToKeep) {
        final String tableName = this.wireRecordStoreOptions.getTableName();
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        final int maxTableSize = this.wireRecordStoreOptions.getMaximumTableSize();

        try {

            int entriesToDeleteCount = getTableSize() + 1; // +1 to make room for the new record
            if (maxTableSize < noOfRecordsToKeep) {
                logger.info("{} > {}, using {} = {}.", DbWireRecordStoreOptions.CLEANUP_RECORDS_KEEP,
                        DbWireRecordStoreOptions.MAXIMUM_TABLE_SIZE, DbWireRecordStoreOptions.CLEANUP_RECORDS_KEEP,
                        DbWireRecordStoreOptions.MAXIMUM_TABLE_SIZE);
                entriesToDeleteCount -= maxTableSize;
            } else {
                entriesToDeleteCount -= noOfRecordsToKeep;
            }

            final String limit = Integer.toString(entriesToDeleteCount);

            this.dbHelper.withConnection(c -> {
                final String catalog = c.getCatalog();
                final DatabaseMetaData dbMetaData = c.getMetaData();
                try (final ResultSet rsTbls = dbMetaData.getTables(catalog, null, tableName, TABLE_TYPE)) {
                    if (rsTbls.next()) {
                        // table does exist, truncate it
                        if (noOfRecordsToKeep == 0) {
                            logger.info("Truncating table {}...", sqlTableName);
                            this.dbHelper.execute(c, MessageFormat.format(SQL_TRUNCATE_TABLE, sqlTableName));
                        } else {
                            logger.info("Partially emptying table {}", sqlTableName);
                            this.dbHelper.execute(c, MessageFormat.format(SQL_DELETE_RANGE_TABLE, sqlTableName, limit));
                        }
                    }
                }
                return null;
            });
        } catch (final SQLException sqlException) {
            logger.error("Error in truncating the table {}...", sqlTableName, sqlException);
        }
    }

    private int getTableSize() throws SQLException {
        final String tableName = this.wireRecordStoreOptions.getTableName();
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);

        return this.dbHelper.withConnection(c -> {
            try (final Statement stmt = c.createStatement();
                    final ResultSet rset = stmt.executeQuery(MessageFormat.format(SQL_ROW_COUNT_TABLE, sqlTableName))) {
                rset.next();
                return rset.getInt(1);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEvelope) {
        requireNonNull(wireEvelope, "Wire Envelope cannot be null");
        final List<WireRecord> records = wireEvelope.getRecords();

        if (this.dbHelper == null) {
            logger.warn("H2DbService instance not attached");
            return;
        }

        try {
            if (getTableSize() >= this.wireRecordStoreOptions.getMaximumTableSize()) {
                truncate();
            }
        } catch (SQLException e) {
            logger.warn("Exception while trying to clean db");
        }

        for (WireRecord wireRecord : records) {
            store(wireRecord);
        }

        // emit the list of Wire Records to the downstream components
        this.wireSupport.emit(records);
    }

    /**
     * Stores the provided {@link WireRecord} in the database
     *
     * @param wireRecord
     *            the {@link WireRecord} to be stored
     * @throws NullPointerException
     *             if the provided argument is null
     */
    private void store(final WireRecord wireRecord) {
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);
        int retryCount = 0;
        final String tableName = this.wireRecordStoreOptions.getTableName();
        do {
            try {
                insertDataRecord(tableName, wireRecord);
                break;
            } catch (final SQLException e) {
                logger.error("Insertion failed. Reconciling Table and Columns...", e);
                reconcileDB(wireRecord, tableName);
                retryCount++;
            }
        } while (retryCount < 2);
    }

    /**
     * Tries to reconcile the database.
     *
     * @param wireRecord
     *            against which the database columns have to be reconciled.
     * @param tableName
     *            the table name in the database that needs to be reconciled.
     */
    private void reconcileDB(final WireRecord wireRecord, final String tableName) {
        try {
            if (nonNull(tableName) && !tableName.isEmpty()) {
                reconcileTable(tableName);
                reconcileColumns(tableName, wireRecord);
            }
        } catch (final SQLException ee) {
            logger.error("Error while storing Wire Records...", ee);
        }
    }

    /**
     * Tries to reconcile the database.
     *
     * @param tableName
     *            the table name in the database that needs to be reconciled.
     */
    private synchronized void reconcileDB(final String tableName) {
        try {
            if (nonNull(this.dbHelper) && nonNull(tableName) && !tableName.isEmpty()) {
                reconcileTable(tableName);
            }
        } catch (final SQLException ee) {
            logger.error("Error while storing Wire Records...", ee);
        }
    }

    /**
     * Reconcile table.
     *
     * @param tableName
     *            the table name
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if the provided argument is null
     */
    private void reconcileTable(final String tableName) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);

        this.dbHelper.withConnection(c -> {
            // check for the table that would collect the data of this emitter
            final String catalog = c.getCatalog();
            final DatabaseMetaData dbMetaData = c.getMetaData();
            try (final ResultSet rsTbls = dbMetaData.getTables(catalog, null,
                    this.wireRecordStoreOptions.getTableName(), TABLE_TYPE)) {
                if (!rsTbls.next()) {
                    // table does not exist, create it
                    logger.info("Creating table {}...", sqlTableName);
                    this.dbHelper.execute(c, MessageFormat.format(SQL_CREATE_TABLE, sqlTableName));
                    createIndex(this.dbHelper.sanitizeSqlTableAndColumnName(tableName + "_TIMESTAMP"), sqlTableName,
                            "(TIMESTAMP DESC)");
                }
            }
            return null;
        });
    }

    private void createIndex(String indexname, String table, String order) throws SQLException {
        this.dbHelper.withConnection(c -> {
            this.dbHelper.execute(c, MessageFormat.format(SQL_CREATE_TABLE_INDEX, indexname, table, order));
            return null;
        });
        logger.info("Index {} created, order is {}", indexname, order);
    }

    /**
     * Reconcile columns.
     *
     * @param tableName
     *            the table name
     * @param wireRecord
     *            the data record
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private void reconcileColumns(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);

        final Map<String, Integer> columns = CollectionUtil.newHashMap();

        this.dbHelper.withConnection(c -> {
            final String catalog = c.getCatalog();
            final DatabaseMetaData dbMetaData = c.getMetaData();
            try (final ResultSet rsColumns = dbMetaData.getColumns(catalog, null, tableName, null)) {
                // map the columns
                while (rsColumns.next()) {
                    final String colName = rsColumns.getString(COLUMN_NAME);
                    final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(colName);
                    final int colType = rsColumns.getInt(DATA_TYPE);
                    columns.put(sqlColName, colType);
                }
            }

            for (Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {
                final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(entry.getKey());
                final Integer sqlColType = columns.get(sqlColName);
                final JdbcType jdbcType = DbDataTypeMapper.getJdbcType(entry.getValue().getType());
                final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
                if (isNull(sqlColType)) {
                    // add column
                    this.dbHelper.execute(c,
                            MessageFormat.format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString()));
                } else if (sqlColType != jdbcType.getType()) {
                    // drop old column and add new one
                    this.dbHelper.execute(c, MessageFormat.format(SQL_DROP_COLUMN, sqlTableName, sqlColName));
                    this.dbHelper.execute(c,
                            MessageFormat.format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString()));
                }
            }

            return null;
        });
    }

    /**
     * Insert the provided {@link WireRecord} to the specified table
     *
     * @param tableName
     *            the table name
     * @param wireRecord
     *            the {@link WireRecord}
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private void insertDataRecord(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);

        final Map<String, TypedValue<?>> wireRecordProperties = wireRecord.getProperties();

        this.dbHelper.withConnection(c -> {
            try (final PreparedStatement stmt = prepareStatement(c, tableName, wireRecordProperties,
                    new Date().getTime())) {
                stmt.execute();
                c.commit();
                return null;
            }

        });

        logger.debug("Stored typed value");
    }

    private PreparedStatement prepareStatement(Connection connection, String tableName,
            final Map<String, TypedValue<?>> properties, long timestamp) throws SQLException {

        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        final StringBuilder sbCols = new StringBuilder();
        final StringBuilder sbVals = new StringBuilder();

        // add the timestamp
        sbCols.append("TIMESTAMP");
        sbVals.append("?");

        int i = 2;
        for (Entry<String, TypedValue<?>> entry : properties.entrySet()) {
            final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(entry.getKey());
            sbCols.append(", ").append(sqlColName);
            sbVals.append(", ?");
        }

        logger.debug("Storing data into table {}...", sqlTableName);
        final String sqlInsert = MessageFormat.format(SQL_INSERT_RECORD, sqlTableName, sbCols.toString(),
                sbVals.toString());
        final PreparedStatement stmt = connection.prepareStatement(sqlInsert);
        stmt.setLong(1, timestamp);

        for (Entry<String, TypedValue<?>> entry : properties.entrySet()) {
            final DataType dataType = entry.getValue().getType();
            final Object value = entry.getValue();
            switch (dataType) {
            case BOOLEAN:
                stmt.setBoolean(i, ((BooleanValue) value).getValue());
                break;
            case FLOAT:
                stmt.setFloat(i, ((FloatValue) value).getValue());
                break;
            case DOUBLE:
                stmt.setDouble(i, ((DoubleValue) value).getValue());
                break;
            case INTEGER:
                stmt.setInt(i, ((IntegerValue) value).getValue());
                break;
            case LONG:
                stmt.setLong(i, ((LongValue) value).getValue());
                break;
            case BYTE_ARRAY:
                byte[] byteArrayValue = ((ByteArrayValue) value).getValue();
                InputStream is = new ByteArrayInputStream(byteArrayValue);
                stmt.setBlob(i, is, byteArrayValue.length);
                break;
            case STRING:
                stmt.setString(i, ((StringValue) value).getValue());
                break;
            default:
                break;
            }
            i++;
        }
        return stmt;

    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }
}
