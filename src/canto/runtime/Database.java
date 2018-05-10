/* Canto Compiler and Runtime Engine
 * 
 * Database.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.*;
import java.util.*;
import java.util.Date;

import canto.lang.*;

import java.sql.*;

/**
 * Supports basic database access by Canto code.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.91 $
 */

public class Database {

    /** True if the database driver was successfully located and initialized. */
    public static boolean driver_initialized = false;

    /** True if this database was successfully initialized. */
    private boolean initialized = false;

    /** The database driver.
     */
    private String driver = null;

    /** The database URL.
     */
    private String url = null;

    /** The username for accessing the database.
     */
    private String user_name = null;

    /** The password for accessing the database.
     */
    private String password = null;

    public Database(String driver, String url, String user_name, String password) throws Redirection {
        if (driver == null || driver.length() == 0) {
            throw new Redirection(Redirection.STANDARD_ERROR, "No database driver specified");
        }
        if (url == null || url.length() == 0) {
            throw new Redirection(Redirection.STANDARD_ERROR, "No database URL specified");
        }
        if (user_name == null || user_name.length() == 0) {
            throw new Redirection(Redirection.STANDARD_ERROR, "No database username specified");
        }
        this.driver = driver;
        this.url = url;
        this.user_name = user_name;
        this.password = password;
    }

    public boolean checkConnection() {
        System.out.println("Checking connection to " + user_name + " at " + url + " on " + (new Date()).toString());
        try {
            Connection connection = DriverManager.getConnection(url, user_name, password);
            if (connection != null) {
                connection.close();
                return true;
            }
        } catch (Exception e) {
            System.out.println("Error connecting to db: " + e);
        }
        return false;
    }

    public Connection getConnection() {
        Connection connection = null;
        try {
            System.out.println("Connecting to " + user_name + " at " + url + " on " + (new Date()).toString());
            connection = DriverManager.getConnection(url, user_name, password);
        } catch (Exception e) {
            System.out.println("Error connecting to db: " + e);
        }
        return connection;
    }

    public boolean init() {
        if (driver_initialized) {
            return true;
        } else {
            return initDriver(driver);
        }
    }

    public static boolean initDriver(String driverName) {
        if (driverName == null || driverName.length() == 0) {
            return false;
        }
        try {
            Class.forName(driverName).newInstance();
            driver_initialized = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean enabled() {
        if (driver_initialized && !initialized) {
            initialized = checkConnection();
        }
        return driver_initialized && initialized;
    }
    
    public RecordCollectionDefinition table(Type recordType, Type keyType) {
        return new RecordCollectionDefinition(recordType, keyType);    
    }
    
    public RecordCollectionDefinition table(String recordName, String keyName) {
        ComplexType recordType = new ComplexType(null, recordName);
        ComplexType keyType = new ComplexType(null, keyName);
        RecordCollectionDefinition recDef = new RecordCollectionDefinition(recordType, keyType);
        recordType.setOwner(recDef);
        keyType.setOwner(recDef);
        return recDef;
    }

    /** Record collections correspond to tables or views in a database. **/
    
    public class RecordCollectionDefinition extends CollectionDefinition {
        private Type recordType = null;
        private Type keyType = null;
        //private Type[] fromTypes = null;
        //private AbstractNode[] constraints = null;
        //private Type orderedBy = null;
        
        public RecordCollectionDefinition(Type recordType, Type keyType) {
            this.recordType = recordType;
            this.keyType = keyType;
            NameNode name = new NameNode(recordType.getName());
            name.setOwner(this);
            setName(name);
            setType(recordType);
            name.setOwner(this);
        }

        public RecordCollectionDefinition(Type recordType, Type keyType, Type[] fromTypes, AbstractNode where, Type by) {
            this.recordType = recordType;
            this.keyType = keyType;
            //this.fromTypes = fromTypes;
            setSuper(new TypeList(Arrays.asList(fromTypes), getOwner()));
            NameNode name = new NameNode(recordType.getName());
            name.setOwner(this);
            setName(name);
            setType(recordType);
        }
        
        public boolean isArray() {
            return false;
        }
        
        public boolean isTable() {
            return true;
        }
        
        public Type getRecordType() {
            return recordType;
        }
      
        public Type getKeyType() {
            return keyType;
        }
      
        public CollectionInstance getCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
            // this could be cached
            if (recordType.getDefinition() == null) {
                recordType = new ComplexType(null, recordType.getName());
                if (context.size() > 1) {
                    try {
                        context.unpush();
                        ((AbstractNode) recordType).setOwner(context.peek().def);
                    } finally {
                        context.repush();
                    }
                }
                recordType.resolve();
            }
            return new ResolvedRecordCollection(this, context, args, indexes);
        }
    }
    
    public class ResolvedRecordCollection extends ResolvedCollection {

        protected RecordCollection recs = null;
        private RecordCollectionDefinition recDef = null;
        private Type recordType;
        private Type keyType;

        public ResolvedRecordCollection(RecordCollectionDefinition recDef, Context context, ArgumentList args, List<Index> indexes) throws Redirection {
            super(recDef, context, args, indexes);

            this.recDef = recDef;
            recordType = recDef.getRecordType();
            if (recordType.getDefinition() == null) {
                recordType = new ComplexType(null, recordType.getName());
                if (context.size() > 1) {
                    try {
                        context.unpush();
                        ((AbstractNode) recordType).setOwner(context.peek().def);
                    } finally {
                        context.repush();
                    }
                }
                recordType.resolve();
            }
            
            keyType = recDef.getKeyType();
            if (keyType.getDefinition() == null) {
                keyType = new ComplexType(null, keyType.getName());
                ((AbstractNode) keyType).setOwner(((AbstractNode) recordType).getOwner());
                keyType.resolve();
            }

            String name = recDef.getName();
            String fullName = recDef.getFullNameInContext(context);
            recs = (RecordCollection) context.getData(recDef, name, args, null);
            // avoid calling getDefinition when unnecessary
            Definition defInKeep = (recs != null ? context.getDefinition(name, fullName, args) : null);
            if (recs == null || !recDef.equals(defInKeep)) {
                recs = createRecordCollection(recDef, context, args);
                context.putData(recDef, args, null, name, recs);
            }
        }

        /** Creates a RecordCollection based on a RecordCollectionDefinition.
         */
        private RecordCollection createRecordCollection(RecordCollectionDefinition recDef, Context context, ArgumentList args) throws Redirection {
            RecordCollection recs = null;
            ParameterList params = recDef.getParamsForArgs(args, context);

            context.push(recDef, params, args, false);
            try {
                if (recordType != null) {
                    recs = new RecordCollection(Database.this, recordType, keyType, context);
                }
                
                //List dims = recDef.getDims();
                //Dim majorDim = (Dim) dims.get(0);
                //int dimType = majorDim.getType();

                Object contents = recDef.getContents();
                if (contents != null) {

                    // table defined with an TableInitExpression
                    if (contents instanceof ArgumentList) {
                        ArgumentList elements = (ArgumentList) contents;
                        addElements(context, elements, recs);

                    // table is aliased or externally defined
//                    } else if (contents instanceof Instantiation) {
//                        table = new TableInstance((Instantiation) contents, context);
//
//                    } else {
//                        // this will throw an exception if the contents aren't a map
//                        table = new HashMap((Map) contents);
                    }
                }
                
            } finally {
                context.pop();
            }
            return recs;
        }


        private void addElements(Context context, List elements, Map table) throws Redirection {
            if (elements != null) {
                Iterator it = elements.iterator();
                while (it.hasNext()) {
                    Object item = it.next();
                    if (item instanceof TableElement) {
                        TableElement element = (TableElement) item;
                        String key = (element.isDynamic() ? element.getDynamicKey(context).getString() : element.getKey().getString());
                        Definition elementDef = getElementDefinition(element);
                        table.put(key, elementDef);
                    } else if (item instanceof ConstructionGenerator) {
                        List constructions = ((ConstructionGenerator) item).generateConstructions(context);
                        addElements(context, constructions, table);
                    } else {
                        throw new Redirection(Redirection.STANDARD_ERROR, recDef.getFullName() + " contains invalid element type: " + item.getClass().getName());
                    }
                }
            }
        }
        /** Returns the RecordCollectionDefinition defining this collection. */
        public CollectionDefinition getCollectionDefinition() {
            return recDef;
        }

        public Object get(Object key) {
            return recs.get(key);
        }

        public Definition getElement(Index index, Context context) {
            Object element = null;
            if (context == null) {
                context = getResolutionContext();
            }
            if (index.isNumericIndex(context)) {
                int i = index.getIndex(context);
                Object[] keys = recs.keySet().toArray();
                Arrays.sort(keys);
                element = recs.get(keys[i]);
            } else {
                String key = index.getKey(context);
                element = recs.get(key);
            }
            return getElementDefinition(element);
        }

        /** If the collection has been resolved, return a ResolvedInstance representing the element. 
         */
        public ResolvedInstance getResolvedElement(Index index, Context context) {
            Object element = null;
            if (context == null) {
                context = getResolutionContext();
            }
            
            if (index.isNumericIndex(context)) {
                int i = index.getIndex(context);
                Object[] keys = recs.keySet().toArray();
                Arrays.sort(keys);
                element = recs.get(keys[i]);
            } else {
                String key = index.getKey(context);
                element = recs.get(key);
            }
            
            if (element instanceof ResolvedInstance) {
                return (ResolvedInstance) element;
            } else {
                return null;
            }
        }

        public int getSize() {
            return recs.size();
        }

        public int count() {
        	return getSize();
        }
        
        public Object getCollectionObject() {
            return recs;
        }


        /** Returns an iterator for the records in the table.
         */
        public Iterator<Definition> iterator() {
            return new RecordIterator();
        }
            
        public class RecordIterator implements Iterator<Definition> {
            Iterator<String> keys = recs.keySet().iterator();
            
            public RecordIterator() {}

            public boolean hasNext() {
                return keys.hasNext();
            }

            public Definition next() {
                String key = (String) keys.next();
                return new ElementDefinition(recDef, recs.get(key));
            }

            public void remove() {
                throw new UnsupportedOperationException("remove not supported in index iterators");
            }
        }

        public Iterator<Index> indexIterator() {
            return new TableIndexIterator();
        }

        public class TableIndexIterator implements Iterator<Index> {
            Iterator<String> keys = recs.keySet().iterator();
            public TableIndexIterator() {}

            public boolean hasNext() {
                return keys.hasNext();
            }

            public Index next() {
                String key = (String) keys.next();
                return new Index(new PrimitiveValue(key));
            }

            public void remove() {
                throw new UnsupportedOperationException("remove not supported in index iterators");
            }
        }

        public void put(int id, Object element) {
            put(new Integer(id), element);
        }

        public void put(long id, Object element) {
            put(new Long(id), element);
        }

        public void put(Object key, Object element) {
            String k = null;
            if (key instanceof Value) {
                k = ((Value) key).getString();
            } else if (key != null) {
                k = key.toString();
            }
            recs.put(k, (Record) element);
        }

        public void putElement(TableElement element) {
            put(element.getKey().getString(), element);
        }

        public void add(Object element) {
            if (element instanceof TableElement) {
                putElement((TableElement) element);
            } else {
                throw new IllegalArgumentException("Only TableElements may be added to a table.");
            }
        }

        @Override
        public Iterator<Construction> constructionIterator() {
            // TODO Auto-generated method stub
            return null;
        }
    }
        
    public int int_query(String sql) throws Redirection {
        int value = 0;
        Connection connection = null;
        try {
            System.out.println("Connecting to " + user_name + " at " + url + " for query on " + (new Date()).toString());
            connection = DriverManager.getConnection(url, user_name, password);
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            Statement s = connection.createStatement();
            sql = sql.trim();
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1);
            }
            System.out.println("    ...int_query: " + sql);
            ResultSet results = s.executeQuery(sql);
            if (results.next()) {
                value = results.getInt(1);
            }
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception querying database (sql = \"" + sql + "\": " + e);
        } finally {
            System.out.println("Query completed on " + (new Date()).toString());
        }
        
        return value;
    }
    
    public String str_query(String sql) throws Redirection {
        String str = null;
        Connection connection = null;
        try {
            System.out.println("Connecting to " + user_name + " at " + url + " for query on " + (new Date()).toString());
            connection = DriverManager.getConnection(url, user_name, password);
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            Statement s = connection.createStatement();
            sql = sql.trim();
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1);
            }
            System.out.println("    ...str_query: " + sql);
            ResultSet results = s.executeQuery(sql);
            if (results.next()) {
                str = results.getString(1);
            }
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception querying database (sql = \"" + sql + "\": " + e);
        } finally {
            System.out.println("Query completed on " + (new Date()).toString());
        }
        
        return str;
    }
    
    public CantoArray query(String sql, Object[] fields) throws Redirection {
        try {
            return new ResultArray(execute_query(sql, fields));
        } catch (Exception e) {
            throw new Redirection(Redirection.STANDARD_ERROR, e.toString());
        }
    }

    
    public CantoArray query(String sql) throws Redirection {
        try {
            return new ResultArray(execute_query(sql, null));
        } catch (Exception e) {
            throw new Redirection(Redirection.STANDARD_ERROR, e.toString());
        }
    }

    public CantoArray query(String sql, String count_sql) throws Redirection {
        try {
            return new ResultArray(execute_query(sql, null), int_query(count_sql));
        } catch (Exception e) {
            throw new Redirection(Redirection.STANDARD_ERROR, e.toString());
        }
    }


    public ResultSet execute_query(String sql, Object[] fields) throws Redirection {
        ResultSet results = null;
        Connection connection = null;
        //ConnectedResultSet connectedResults = null;
        if (sql == null) {
        	throw new Redirection(Redirection.STANDARD_ERROR, "SQL string is empty.");
       
        } else {
        	sql = sql.trim();
        	if (sql.endsWith(";")) {
              	sql = sql.substring(0, sql.length() - 1);
        	}
        }
        try {
            System.out.println("Connecting to " + user_name + " at " + url + " for query on " + (new Date()).toString());
            connection = DriverManager.getConnection(url, user_name, password);
            if (connection != null) {
                PreparedStatement s = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

                if (fields != null) {
                    for (int i = 0; i < fields.length; i++) {
                        s.setObject(i + 1, fields[i]);
                    }
                }
                System.out.println("    ...execute_query: " + sql);
                results = s.executeQuery();
                //connectedResults = new ConnectedResultSet(connection, results);
            }
        } catch (Exception e) {
            throw new Redirection(Redirection.STANDARD_ERROR, e.toString());
        } finally {
            if (connection != null && results == null) { // connectedResults == null) {
                try {
                    connection.close();
                } catch (SQLException sqle) {
                    ;
                }
            }
            System.out.println("Query completed on " + (new Date()).toString());
        }
        return results;
    }

    public int execute_update(String sql, Object[] fields) throws Redirection {
        int results = -1;
        Connection connection = null;
        try {
            System.out.println("Connecting to " + user_name + " at " + url + " for update on " + (new Date()).toString());
            connection = DriverManager.getConnection(url, user_name, password);
            if (connection != null) {
                sql = sql.trim();
                if (sql.endsWith(";")) {
                    sql = sql.substring(0, sql.length() - 1);
                }
                PreparedStatement s = connection.prepareStatement(sql);
                for (int i = 0; i < fields.length; i++) {
                    s.setObject(i + 1, fields[i]);
                }
                results = s.executeUpdate();
            }
        } catch (Exception e) {
            throw new Redirection(Redirection.STANDARD_ERROR, e.toString());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException sqle) {
                    ;
                }
            }
        }
        return results;
    }

    public int execute(String sql) throws Redirection {
        int results = -1;
        Connection connection = null;
        try {
            System.out.println("Connecting to " + user_name + " at " + url + " for execute on " + (new Date()).toString());
            connection = DriverManager.getConnection(url, user_name, password);
            if (connection != null) {
                Statement s = connection.createStatement();
                sql = sql.trim();
                if (sql.endsWith(";")) {
                    sql = sql.substring(0, sql.length() - 1);
                }
                results = s.executeUpdate(sql);
            }
        } catch (Exception e) {
            throw new Redirection(Redirection.STANDARD_ERROR, e.toString());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException sqle) {
                    ;
                }
            }
            System.out.println("Update completed on " + (new Date()).toString());
        }
        return results;
    }

    public Object[] execute_batch(Object[] sqls) throws Redirection {
        
        if (sqls == null) {
            return null;
        }
        int numSqls = sqls.length;
        Object[] retVals = new Object[numSqls];
            
        Connection connection = null;
        try {
            System.out.println("Connecting to " + user_name + " at " + url + " for batch execute on " + (new Date()).toString());
            connection = DriverManager.getConnection(url, user_name, password);
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            Statement s = connection.createStatement();
            for (int i = 0; i < numSqls; i++) {
                String sql = sqls[i].toString();
                sql = sql.trim();
                if (sql.endsWith(";")) {
                    sql = sql.substring(0, sql.length() - 1);
                }
                s.addBatch(sql);
            }
            int[] retCodes = s.executeBatch();
            for (int i = 0; i < numSqls; i++) {
                retVals[i] = new Integer(retCodes[i]);
            }
            
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception executing batch: " + e);
        } finally {
            System.out.println("Query completed on " + (new Date()).toString());
        }
        
        return retVals;
    }

    public void execute_procedure(String procName, Object[] fields) throws Redirection {
        execute_call(procName, fields, null);        
    }
    
    
    public Object execute_call(String procName, Object[] fields, Object retVal) throws Redirection {
        ResultSet results = null;
        Connection connection = null;
        
        if (procName == null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "Procedure name is empty.");
        } else {
            procName = procName.trim();
        }

        // construct the command string
        StringBuffer command = new StringBuffer();
        if (retVal != null) {
            command.append("{? = ");
        } else {
            command.append("{");
        }
        command.append("call ");
        command.append(procName);
        command.append("(");
        for (int i = 0; i < fields.length; i++) {
            command.append("?");
            if (i < fields.length - 1) {
                command.append(",");
            }
        }
        command.append(")}");
        
        try {
            System.out.println("Connecting to " + user_name + " at " + url + " for procedure call on " + (new Date()).toString());
            connection = DriverManager.getConnection(url, user_name, password);
            if (connection != null) {
                CallableStatement s = connection.prepareCall(command.toString());

                if (fields != null) {
                    for (int i = 0; i < fields.length; i++) {
                        s.setObject(i + 1, fields[i]);
                    }
                }
                System.out.println("    ...execute_call: " + command.toString());
                results = s.executeQuery();
            }
        } catch (Exception e) {
            throw new Redirection(Redirection.STANDARD_ERROR, e.toString());
        } finally {
            if (connection != null && results == null) { // connectedResults == null) {
                try {
                    connection.close();
                } catch (SQLException sqle) {
                    ;
                }
            }
            System.out.println("Query completed on " + (new Date()).toString());
        }
        return results;
    }

}


/** A RecordCollection is the instantiation of a database query, as well as a
 *  table instance suitable for iterating over in <code>for</code> statements
 *  or storing memoized instantiations via <code>keep</code> statements. 
 */
class RecordCollection implements Map<String, Record> {
    private Database db;
    private Type tableType;
    private String tableName;
    private String[] columnNames;
    private String columnList;
    private Map columnMap;
    private Class[] columnClasses;
    private String keyName;
    private Class keyClass;
    private Context resolutionContext;
    
    public RecordCollection(Database db, Type tableType, Type keyType, Context context) {
        this(db, tableType, keyType, false, context);
    }
    
    
    public RecordCollection(Database db, Type tableType, Type keyType, boolean useFullName, Context context) {

        resolutionContext = context.clone(false);        
        
        this.db = db;
        this.tableType = tableType;
        this.tableName = (useFullName && tableType.getDefinition() != null) ? tableType.getDefinition().getFullName() : tableType.getName();
        this.keyName = keyType.getName();
        this.keyClass = keyType.getTypeClass(context);
        
        Type[] childTypes = tableType.getPersistableChildTypes();
        int numTypes = childTypes == null ? 0 : childTypes.length;
        if (numTypes < 1) {
            throw new IllegalArgumentException("Table type has no child types; cannot construct RecordCollection");
        }
        columnMap = new HashMap(numTypes);
        columnMap.put(childTypes[0].getName(), childTypes[0]);
        columnNames = new String[numTypes];
        columnNames[0] = childTypes[0].getName();
        columnList = columnNames[0];
        
        columnClasses = new Class[numTypes];
        columnClasses[0] = childTypes[0].getTypeClass(context);
        
        for (int i = 1; i < numTypes; i++) {
            String column = childTypes[i].getName();
            columnNames[i] = column;
            columnList = columnList + ',' + column;
            columnMap.put(column, childTypes[i]);
            
            columnClasses[i] = childTypes[i].getTypeClass(context);
        }
    }

    public int size() {
        int count = -1;    // -1 means unknown size
        String sql = "select count(*) from " + tableName;
        try {
            Connection connection = db.getConnection();
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            Statement s = connection.createStatement();
            ResultSet results = s.executeQuery(sql);
            if (results.next()) {
                count = results.getInt(1);
            } else {
                count = 0;
            }
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception querying database (sql = \"" + sql + "\": " + e);
        }
        return count;
    }

    public boolean isEmpty() {
        return size() <= 0;
    }

    private String getKeyString(Object key) {
        String keyString = key.toString();
        if (!Number.class.isAssignableFrom(keyClass) && (!keyClass.isPrimitive() || keyClass.equals(Character.TYPE))) {
            keyString = '\'' + keyString + '\'';
        }
        return keyString;
    }
    
    private String getFieldValueString(int n, Object value) {
        if (value == null) {
            return "NULL";
        }
        
        String valueString = (value instanceof Value ? ((Value) value).getString() : value.toString());
        if (!Number.class.isAssignableFrom(columnClasses[n]) && (!columnClasses[n].isPrimitive() || columnClasses[n].equals(Character.TYPE))) {
            valueString = '\'' + valueString + '\'';
        }
        return valueString;
    }

    
    public boolean containsKey(Object key) {
        boolean doesContain = false;
        String keyString = getKeyString(key);
        String sql = "select count(*) from " + tableName + " where " + keyName + " = " + keyString + ";";
        try {
            Connection connection = db.getConnection();
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            Statement s = connection.createStatement();
            ResultSet results = s.executeQuery(sql);
            if (results.first()) {
                if (results.getInt(1) > 0) {
                    doesContain = true;
                }
            }
            
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception querying database (sql = \"" + sql + "\": " + e);
        }
        return doesContain;
    }

    public boolean containsValue(Object value) {
        boolean doesContain = false;
        String sql = "select count(*) from " + tableName + " where ";
        int numcols = columnNames.length;
        if (numcols > 0) {
            try {
                Connection connection = db.getConnection();
                if (connection == null) {
                    throw new RuntimeException("unable to connect to database");
                }
            
                String stringVal = value.toString();
                Statement s = connection.createStatement();
                for (int i = 0; i < numcols - 1; i++) {
                    sql = sql + columnNames[i] + " = '" + stringVal + "' or ";
                }
                sql = sql + columnNames[numcols - 1] + " = '" + stringVal + "';";
            
                ResultSet results = s.executeQuery(sql);
                if (results.first()) {
                    if (results.getInt(1) > 0) {
                        doesContain = true;
                    }
                }
            
                connection.close();
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                System.out.println("Exception querying database (sql = \"" + sql + "\": " + e);
            }
        }
        return doesContain;
    }

    public Record get(Object key) {
        Record record = null;
        String sql = "select " + columnList + " from " + tableName + " where " + keyName + " = " + getKeyString(key) + ";";
        try {
            Connection connection = db.getConnection();
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            Statement s = connection.createStatement();
            ResultSet results = s.executeQuery(sql);
            if (results.first()) {
                record = new Record(tableType, columnNames, results);
            }
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception querying database (sql = \"" + sql + "\": " + e);
        }
        return record;
    }

    public Object getField(Object record, String fieldName) {
        if (record instanceof Holder) {
            Definition def = ((Holder) record).def;
            Object data = ((Holder) record).data;
            //Context context = ((Holder) record).context;
            //if (data == null && context != null && def != null) {
            //    try {
            //        data = def.getChild(new NameNode(fieldName), null, null, context, true, true);
            //        //data = def.getChildData(new NameNode(fieldName), null, resolutionContext, args);
            //    } catch (Redirection r) {
            //        System.out.println("Unable to get data from field " + fieldName + " in definition " + def.getFullName());
            //    }
            //}
            return data;
        }
        return null;
    }
    
    public Record put(String key, Record value) {
        Record record = null;
        String keyValue = getKeyString(key); //getField(value, key.toString()).toString();
        String sqlCheck = "select count(*) from " + tableName + " where " + keyName + " = " + keyValue + ";";
        String sqlInsertPre = "insert into " + tableName + " (" + columnList + ") values (";
        String sqlInsertPost = ");";
        String sqlUpdatePre = "update " + tableName + " set ";
        String sqlUpdatePost = " where " + keyName + " = " + keyValue + ";";
        int numcols = columnNames.length;
        int count = 0;
        String sql = sqlCheck;
        try {
            
            Connection connection = db.getConnection();
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            Statement s = connection.createStatement();
            ResultSet results = s.executeQuery(sql);
            if (results.first()) {
                count = results.getInt(1);
            } else {
                count = 0;
            }
            s.close();
            sql = null; 

            if (count > 0) {
                sql = sqlUpdatePre;
                for (int i = 0; i < numcols - 1; i++) {
                    if (columnNames[i].equals(keyName)) {
                        continue;
                    }
                    sql = sql + columnNames[i] + " = " + getFieldValueString(i, getField(value, columnNames[i])) + ",";
                }
                sql = sql + columnNames[numcols - 1] + " = " + getFieldValueString(numcols - 1, getField(value, columnNames[numcols - 1])) + sqlUpdatePost;
            } else {
                sql = sqlInsertPre;
                for (int i = 0; i < numcols - 1; i++) {
                    sql = sql + getFieldValueString(i, getField(value, columnNames[i])) + ", ";
                }
                sql = sql + getFieldValueString(numcols - 1, getField(value, columnNames[numcols - 1])) + sqlInsertPost;

            }
            s = connection.createStatement();
            s.executeUpdate(sql);
            s.close();
            
            connection.close();
        } catch (RuntimeException re) {
            re.printStackTrace();
            throw re;
        } catch (Exception e) {
            System.out.println("Exception writing to database\n  sql = \"" + sql + "\"\n  " + e);
        }
        return record;
    }

    public Record remove(Object key) {
        Record record = null;
        String sql = "";
        try {
            Connection connection = db.getConnection();
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception deleting from database (sql = \"" + sql + "\": " + e);
        }
        return record;
    }

    public void putAll(Map<? extends String, ? extends Record> T) {
        String sql = "";
        try {
            Connection connection = db.getConnection();
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception writing to database (sql = \"" + sql + "\": " + e);
        }
    }

    public void clear() {
        String sql = "";
        try {
            Connection connection = db.getConnection();
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception clearing database (sql = \"" + sql + "\": " + e);
        }
    }
    
    public class ColumnComparator implements Comparator {
        
        public int compare(Object o1, Object o2) {
            int col1 = Integer.MAX_VALUE;
            String name = o1.toString();
            for (int i = 0; i < columnNames.length; i++) {
                if (name.equals(columnNames[i])) {
                    col1 = i;
                    break;
                }
            }
            int col2 = Integer.MAX_VALUE;
            name = o2.toString();
            for (int i = 0; i < columnNames.length; i++) {
                if (name.equals(columnNames[i])) {
                    col2 = i;
                    break;
                }
            }
            return col1 - col2;
        }
        
    }

    public class StringFieldSet extends AbstractSet<String> {
        private String column;
        
        public StringFieldSet(String column) {
            this.column = column;
        }
        
        public Iterator<String> iterator() {
            return new StringFieldIterator(column);
        }
        
        public int size() {
            return RecordCollection.this.size();
        }
    }

    public class StringFieldIterator implements Iterator<String> {
        private Connection connection;
        private ResultSet results;
        private String column;

        public StringFieldIterator(String column) {
            this.column = column;
            String sql = "select " + column + " from " + tableName + ";";
            try {
                connection = db.getConnection();
                if (connection == null) {
                    throw new RuntimeException("unable to connect to database");
                }
                Statement s = connection.createStatement();
                results = s.executeQuery(sql);
                if (!results.first()) {
                    results = null;
                }
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                System.out.println("Exception querying database (sql = \"" + sql + "\": " + e);
            }
        }
        
        public boolean hasNext() {
            boolean more = false;
            
            if (results != null) {
                try {
                    more = !results.isAfterLast();
                    if (!more && connection != null) {
                        results.close();
                        connection.close();
                        connection = null;
                    }
                } catch (Exception e) {
                    System.out.println("Exception in database results: " + e);
                }
            }
            return more;
        }
        
        public String next() {
            String nextField = null;
            try {
                nextField = results.getString(column);
                results.next();
            } catch (Exception e) {
                System.out.println("Exception reading database results: " + e);
            }
            return nextField;
        }
        
        public void remove() {
            throw new UnsupportedOperationException("not mutable");
        }
    }
    
    public Set<String> keySet() {
        Set<String> keys = new StringFieldSet(keyName);
        return keys;
    }

    public Collection<Record> values() {
        Collection<Record> values = null;
        String sql = "";
        try {
            Connection connection = db.getConnection();
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception querying database(sql = \"" + sql + "\": " + e);
        }
        return values;
    }

    public Set<Map.Entry<String, Record>> entrySet() {
        Set<Map.Entry<String, Record>> entries = null;
        String sql = "";
        try {
            Connection connection = db.getConnection();
            if (connection == null) {
                throw new RuntimeException("unable to connect to database");
            }
            
            connection.close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            System.out.println("Exception querying database(sql = \"" + sql + "\": " + e);
        }
        return entries;
    }

    public boolean equals(Object o) {
        return (o instanceof RecordCollection && ((RecordCollection) o).tableType.equals(tableType));
    }

    public int hashCode() {
        return tableType.hashCode();
    }

}

class Record extends HashMap<String, Object> implements ValueMap {
    private static final long serialVersionUID = 1L;

    private Type type;
    private String[] columnNames;
    
    public Record(Type recType, String[] columnNames, ResultSet results) {
        if (recType == null || columnNames == null || results == null) {
            throw new NullPointerException("Null parameter passed to Record constructor");
        }
        type = recType;
        this.columnNames = columnNames;
        int numcols = columnNames.length;
        for (int i = 0; i < numcols; i++) {
            String col = columnNames[i];
            try {
                Object obj = results.getObject(col);
                Reader objReader = null;

                if (obj instanceof Clob) {
                    objReader = ((Clob) obj).getCharacterStream();
                } else if (obj instanceof Blob) {
                    objReader = new InputStreamReader(((Blob) obj).getBinaryStream());
                }

                if (objReader != null) {                	
                	char[] buf = new char[8192];
                	StringBuffer sb = new StringBuffer();
                	BufferedReader r = new BufferedReader(objReader);
                	int n = r.read(buf);
                	while (n >= 0) {
                		sb.append(buf, 0, n); 
                		n = r.read(buf);
                    }
                	obj = sb.toString();
                }
                
                put(col, obj);
            } catch (IOException ioe) {
                System.err.println("IO Exception (" + ioe +") retrieving field " + col + " in " + type.getName() + " table.");
            	
            } catch (SQLException sqle) {
                System.err.println("SQL Exception (" + sqle +") retrieving field " + col + " in " + type.getName() + " table.");
            }
        }
    }
    
    public String[] getColumnNames() {
        return columnNames;
    }
    
    public int getColumnCount() {
        return columnNames.length;
    }

    public String getString(String key) {
        Object obj = get(key);
        if (obj != null) {
            return obj.toString();
        } else {
            return null;
        }
    }

    public boolean getBoolean(String key) {
        return PrimitiveValue.getBooleanFor(get(key));
    }

    public byte getByte(String key) {
        return (byte) PrimitiveValue.getIntFor(get(key));
    }


    public char getChar(String key) {
        return PrimitiveValue.getCharFor(get(key));
    }


    public int getInt(String key) {
        return PrimitiveValue.getIntFor(get(key));
    }


    public long getLong(String key) {
        return PrimitiveValue.getLongFor(get(key));
    }


    public double getDouble(String key) {
        return PrimitiveValue.getDoubleFor(get(key));
    }


    public Object getValue(String key) {
        return get(key);
    }


    public Class<?> getValueClass(String key) {
        return get(key).getClass();
    }

    /** ValueSource interface method; returns null. **/
	public Value getValue(Context context) throws Redirection {
		return null;
	}

    
    /** A RecordDefinition represents a single object retrieved from a RecordCollection. */
//  public class RecordDefinition extends ComplexDefinition {
//      
//      private Type type;
//      private Map record;
//      
//      public RecordDefinition(Type type, Map record) {
//          this.type = type;
//          this.record = record;
//      }
//
//      /** Construct this definition with the specified arguments in the specified context. */
//      public Object instantiate(ArgumentList args, Context context) throws Redirection {
//          return record;
//      }
//
//      private Type getColumnType(String name) {
//          Type[] childTypes = type.getChildTypes();
//          for (int i = 0; i < childTypes.length; i++) {
//              if (name.equals(childTypes[i].getName())) {
//                  return childTypes[i];
//              }
//          }
//          return null;
//      }
//      
//      /** Find the child definition, if any, by the specified name; if <code>generate</code> is
//       *  false, return the definition, else instantiate it and return the result.  If <code>generate</code>
//       *  is true and a definition is not found, return UNDEFINED.
//       */
//      public Object getChild(NameNode name, ArgumentList args, Context argContext, boolean generate) throws Redirection {
//          String columnName = name.getName();
//          if (generate) {
//              Object columnData = record.get(columnName);
//              if (columnData != null) {
//                  return columnData;
//              } else {
//                  Type columnType = getColumnType(columnName);
//                  if (columnType != null) {
//                      return columnType.getDefinition().instantiate(args, argContext);
//                  } else {
//                      return UNDEFINED;
//                  }
//              }
//          } else {
//              Type columnType = getColumnType(columnName);
//              if (columnType != null) {
//                  return columnType.getDefinition();
//              } else {
//                  return null;
//              }
//          }
//      }
//
//
//      /** Returns the definition by the specified name if it belongs to this definition.  This
//       *  version of getChildDefinition does not attempt to resolve the definition for a
//       *  particular set of arguments, so no argument list is required.  The context is provided
//       *  purely to resolve the parent's definition if it is aliased.
//       */
//      public Definition getChildDefinition(NameNode name, Context context) {
//          Type columnType = getColumnType(name.getName());
//          if (columnType != null) {
//              return columnType.getDefinition();
//          } else {
//              return null;
//          }
//      }
//
//      /** Instantiates a child definition with the specified name and given the specified instance
//       *  of this definition and the specified context
//       */
//      public Object getChildData(NameNode childName, Type type, Context context) throws Redirection {
//          return getChild(childName, null, context, true);
//      }
//
//
//      /** Gets a list of constructions comprising this definition. */
////      public List getConstructions(Context context);
//
//
//      /** Gets the contents of the definition as a single node, which may be a block,
//       *  an array or a single value.
//       */
////      public AbstractNode getContents();
//
//  
//  }
      
}
