/* Canto Compiler and Runtime Engine
 * 
 * DatabaseRow.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.util.*;

import canto.lang.*;

import java.sql.*;

/**
 * A DatabaseRow represents one row in a ResultSet returned by a database query.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.13 $
 */

public class DatabaseRow extends AbstractMap {
    private static Type ROWTYPE = new PrimitiveType(DatabaseRow.class); 
    protected Record record = null;
    protected int row = -1;


    public DatabaseRow(ResultSet resultSet, String[] columns) {
        try {
        	row = resultSet.getRow();
        	init(resultSet, columns);
        } catch (Exception e) {
            System.out.println("Exception creating DatabaseRow: " + e);
        }
    }
    
    public DatabaseRow(ResultSet resultSet, String[] columns, int row) {
        this.row = row;
        try {
        	init(resultSet, columns);
        } catch (Exception e) {
            System.out.println("Exception creating DatabaseRow: " + e);
        }
    }
    
    protected void init(ResultSet resultSet, String[] columns) throws SQLException {
	    record = new Record(ROWTYPE, columns, resultSet);
    }

    public Set entrySet() {
        return new ColumnSet();
    }

    public Object get(Object key) {
        try {
            return record.get(key.toString());
        } catch (RuntimeException re) {
            System.out.println("Exception in DatabaseRow.get: " + re);
            throw re;
        } catch (Exception e) {
            System.out.println("Exception in DatabaseRow.get: " + e);
            return null;
        }
    }


    class ColumnSet extends AbstractSet {
        public Iterator iterator() {
            return new ColumnIterator();
        }
        public int size() {
            try {
                return record.getColumnCount();
            } catch (Exception e) {
                return -1;
            }
        }
    }

    class ColumnIterator implements Iterator {
        private int column;
        private int columnCount;
        private String[] columnNames;

        public ColumnIterator() {
            try {
                columnNames = record.getColumnNames();
                columnCount = (columnNames == null ? 0 : columnNames.length);
                column = 1;
            } catch (Exception e) {
                columnCount = -1;
                column = 0;
            }
        }

        public boolean hasNext() {
            return (column <= columnCount);
        }

        public Object next() {
            try {
                Object entry = new ColumnEntry(columnNames[column - 1], record.get(columnNames[column - 1]));
                column++;
                return entry;
            } catch (Exception e) {
                throw new NoSuchElementException("(underlying exception: " + e.toString() + ")");
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Column entries are immutable");
        }
    }

    private static Object NULL_VALUE = new Object();
    class ColumnEntry implements Map.Entry {
        private Object key;
        private Object value;
        public ColumnEntry(Object key, Object value) {
            if (key == null) {
                throw new NullPointerException("ColumnEntry key must be be nonnull");
            }
            if (value == null) {
                value = NULL_VALUE;
            }
            this.key = key;
            this.value = value;
        }

        public boolean equals(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry e = (Map.Entry) o;
                return (key.equals(e.getKey()) && (value.equals(e.getValue()) || (value == NULL_VALUE && e.getValue() == null)));
            }
            return false;
        }

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return (value == NULL_VALUE ? null : value);
        }

        public int hashCode() {
            return (key.hashCode() ^ value.hashCode());
        }

        public Object setValue(Object value) {
            throw new UnsupportedOperationException("Column entries are immutable");
        }
    }
}


