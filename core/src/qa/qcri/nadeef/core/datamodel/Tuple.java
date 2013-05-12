/*
 * Copyright (C) Qatar Computing Research Institute, 2013.
 * All rights reserved.
 */

package qa.qcri.nadeef.core.datamodel;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Tuple class.
 * TODO: consider using Trove for better hashmap performance.
 */
public class Tuple {
    //<editor-fold desc="Private Fields">
    private Object[] values;
    private Schema schema;
    private String tableName;
    private int tupleId;
    //</editor-fold>

    //<editor-fold desc="Public Members">
    /**
     * Constructor.
     */
    public Tuple(int tupleId, Schema schema, Object[] values) {
        if (schema == null || values == null || tupleId < 1) {
            throw new IllegalArgumentException("Input attribute/value cannot be null.");
        }
        if (schema.size() != values.length) {
            throw new IllegalArgumentException("Incorrect input with attributes and values");
        }

        this.tableName = schema.getTableName();
        this.tupleId = tupleId;
        this.schema = schema;
        this.values = values;
    }

    /**
     * Gets the value from the tuple.
     * @param key The attribute key
     * @return Output Value
     */
    public Object get(Column key) {
        int index = schema.get(key);
        return values[index];
    }

    /**
     * Gets the value from the tuple.
     * @param columnAttribute The attribute key
     * @return Output Value
     */
    public Object get(String columnAttribute) {
        Column column = new Column(tableName, columnAttribute);
        int index = schema.get(column);
        return values[index];
    }

    /**
     * Gets the value from the tuple.
     * @param key The attribute key
     * @return Output Value
     */
    public String getString(Column key) {
        Object value = get(key);
        return (String)value;
    }

    /**
     * Gets the value from the tuple.
     * @param columnAttribute The attribute key
     * @return Output Value
     */
    public String getString(String columnAttribute) {
        Object value = get(columnAttribute);
        return (String)value;
    }

    /**
     * Gets Tuple Id.
     * @return tuple id.
     */
    public int getTupleId() {
        return this.tupleId;
    }

    /**
     * Gets the Cell given a column key.
     * @param key key.
     * @return Cell.
     */
    public Cell getCell(Column key) {
        return new Cell(key, tupleId, get(key));
    }

    /**
     * Gets the Cell given a column key.
     * @param key key.
     * @return Cell.
     */
    public Cell getCell(String key) {
        return new Cell(new Column(tableName, key), tupleId, get(key));
    }

    /**
     * Gets all the values in the tuple.
     * @return value collections.
     */
    public ImmutableSet<Cell> getCells() {
        List<Column> columns = schema.getColumns().asList();
        List<Cell> cells = Lists.newArrayList();
        for (Column column : columns) {
            if (column.getAttributeName().equals("tid")) {
                continue;
            }
            Cell cell = new Cell(column, tupleId, get(column));
            cells.add(cell);
        }
        return ImmutableSet.copyOf(cells);
    }

    /**
     * Gets all the cells in the tuple.
     * @return Attribute collection
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Gets the table names.
     * @return table names.
     */
    public String getTableName() {
        return tableName;
    }
    //</editor-fold>
}
