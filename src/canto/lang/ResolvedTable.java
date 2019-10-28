/* Canto Compiler and Runtime Engine
 * 
 * ResolvedTable.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import canto.runtime.Context;

public class ResolvedTable extends ResolvedCollection {

    protected Map<String, Object> table = null;
    private CollectionDefinition collectionDef = null;

    @SuppressWarnings("unchecked")
    public ResolvedTable(Definition def, Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        super(def, context, args, indexes);

        this.collectionDef = def.getCollectionDefinition(context, args);

//        String name = def.getName();
//        String modifier = Instantiation.getNameModifier(args, null);
//        Definition defInKeep = null;

//        if (def.getDurability() != Definition.DYNAMIC && (args == null || !args.isDynamic())) {
//            cachevlog("  = = =]  table: retrieving " + name + " from cache [- - - ");
//            table = (Map) context.getData(def.getFullName(), name, modifier, args, null);
//            defInKeep = context.getKeepdDefinition(def, modifier, args);
//            cachevlog("  = = =]  " + name + " table data: " + (table == null ? "null" : table.toString()));
//            cachevlog("  = = = =]  context: " + context.toString());
//        }
//        if (table == null || !def.equals(defInKeep)) {
            if (collectionDef.hasStaticData()) {
                table = (Map<String, Object>) collectionDef.getStaticData();
            } else {
                table = createTable(collectionDef, context, args);
                // has no effect if def is not static
                collectionDef.setStaticData(table);
            }
//            cachevlog("  = = =]  table: storing data for " + name + " in cache [- - - ");
//            context.putData(def, args, null, name, modifier, table);
//            cachevlog("  = = =]  " + name + " table data: " + (table == null ? "null" : table.toString()));
//        }
    }

    @SuppressWarnings("unchecked")
    public ResolvedTable(Definition def, Context context, ArgumentList args, List<Index> indexes, Object tableData) throws Redirection {
        super(def, context, args, indexes);

        this.collectionDef = def.getCollectionDefinition(context, args);

        if (tableData instanceof Map<?,?>) {
        	table = (Map<String, Object>) tableData;

        } else if (tableData instanceof CollectionDefinition) {
            // this doesn't support args in anonymous arrays 
            CollectionDefinition tableDef = (CollectionDefinition) tableData;
            if (tableDef.equals(collectionDef)) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Table " + def.getName() + " is circularly defined.");
            }
            table = tableDef.getTable(context, null, null);

        } else if (tableData != null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "Unable to initialize table " + def.getName() + "; data in context of wrong type: " + tableData.getClass().getName());
        }
        
    }

    public Map<String, Object> getTable() {
        return table;
    }
    
    public Object generateData(Context context, Definition def) throws Redirection {
        return table;
    }
    
    /** Creates a table based on the definition.  If the contents of the definition are
     *  of an unexpected type, a ClassCastException is thrown.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> createTable(CollectionDefinition def, Context context, ArgumentList args) throws Redirection {
        
        ParameterList params = def.getParamsForArgs(args, context);
        context.push(def, params, args, false);
        try {

            Map<String, Object> table = null;
            Object contents = def.getContents();
            if (contents != null) {

                // table defined with an TableInitExpression
                if (contents instanceof ArgumentList) {
                    List<Dim> dims = def.getDims();
                    Dim majorDim = dims.get(0);
                    Dim.TYPE dimType = majorDim.getType();

                    ArgumentList elements = (ArgumentList) contents;
                    int size = 0;
                    if (dimType == Dim.TYPE.DEFINITE) {
                        size = majorDim.getSize();
                    } else if (elements != null) {
                        size = elements.size();
                    }

                    // for now just handle one dimension
                    table = new HashMap<String, Object>(size);
                    addElements(context, elements, table);

                // table is aliased or externally defined
                } else if (contents instanceof ValueGenerator) {
                    table = new TableInstance((ValueGenerator) contents, context);

                } else {
                    Object obj = contents;
                    if (obj instanceof Construction) {
                    	obj = ((Construction) obj).getData(context);
                    }
                    if (obj instanceof Value) {
                        obj = ((Value) obj).getValue();
                    }
                    Map<String, Object> map;
                    if (obj == null || obj instanceof Map) {
                        map = (Map<String, Object>) obj;
                    } else if (obj instanceof List || obj.getClass().isArray()) {
                        map = new MappedArray(obj, context);
                    } else {
                        throw new ClassCastException("Table value must be a collection type (Map, List or array)");
                    }
                    table = new TableInstance(map);

                }

        //            if (table instanceof HashMap && def.getDurability() != STATIC) {
        //                table = new HashMap(table);
        //            }

                if (table instanceof DynamicObject) {
                    table = (Map<String, Object>) ((DynamicObject) table).initForContext(context, args, indexes);
                }

            } else {
                table = new HashMap<String, Object>();
            }
            return table;
        } finally {
            context.pop();
        }
    }


    private void addElements(Context context, List<Construction> elements, Map<String, Object> table) throws Redirection {
        if (elements != null) {
            Iterator<Construction> it = elements.iterator();
            while (it.hasNext()) {
                Construction item = it.next();
                if (item instanceof TableElement) {
                    TableElement element = (TableElement) item;
                    String key = (element.isDynamic() ? element.getDynamicKey(context).getString() : element.getKey().getString());
                    Definition elementDef = getElementDefinition(element);
                    table.put(key, elementDef);
                } else if (item instanceof ConstructionGenerator) {
                    List<Construction> constructions = ((ConstructionGenerator) item).generateConstructions(context);
                    addElements(context, constructions, table);
                } else if (item instanceof SuperStatement) {
                    Definition superDef = collectionDef.getSuperDefinition(context);
                    if (superDef != null) {
                        CollectionDefinition superCollection = superDef.getCollectionDefinition(context, ((SuperStatement) item).getArguments());
                        if (superCollection != null) {
                            Map<String, Object> superElements = superCollection.instantiate_table(context);
                            table.putAll(superElements);
                        }
                    }

                } else {
                    throw new Redirection(Redirection.STANDARD_ERROR, collectionDef.getFullName() + " contains invalid element type: " + item.getClass().getName());
                }
            }
        }
    }
    /** Returns the TableDefinition defining this collection. */
    public CollectionDefinition getCollectionDefinition() {
        return collectionDef;
    }

    public Object get(Object key) {
        return table.get(key);
    }

    public Definition getElement(Index index, Context context) {
        Object element = null;
        if (context == null) {
        	context = getResolutionContext();
        }
        String key = index.getIndexValue(context).getString();
        element = getElement(key);
        return getElementDefinition(element);
    }
    
    private Object getElement(Object key) {
        if (table instanceof InstantiatedMap) {
            return ((InstantiatedMap) table).getElement(key);
        } else {
            return table.get(key);
        }
        
    }

    /** If the collection has been resolved, return a ResolvedInstance representing the element. 
     */
    public ResolvedInstance getResolvedElement(Index index, Context context) {
        Object element = null;
        if (context == null) {
            context = getResolutionContext();
        }
        
        String key = index.getIndexValue(context).getString();
        element = getElement(key);
        
        if (element instanceof ResolvedInstance) {
            return (ResolvedInstance) element;
        } else {
            return null;
        }
    }

    public int getSize() {
        return (table != null ? table.size() : 0);
    }

    public Object getCollectionObject() {
        return table;
    }


    /** Returns an iterator for the elements in the table.
     */
    public Iterator<Definition> iterator() {
        return new TableElementIterator();
    }
    
    public class TableElementIterator implements Iterator<Definition> {
        Iterator<String> keys = table.keySet().iterator();
        
        public TableElementIterator() {}

        public boolean hasNext() {
            return keys.hasNext();
        }

        public Definition next() {
            String key = (String) keys.next();
            return getElementDefinition(table.get(key));
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in index iterators");
        }
    }


    public Iterator<Construction> constructionIterator() {
        return new TableConstructionIterator();
    }
            
    public class TableConstructionIterator implements Iterator<Construction> {
        Iterator<String> keys = table.keySet().iterator();
        
        public TableConstructionIterator() {}

        public boolean hasNext() {
            return keys.hasNext();
        }

        public Construction next() {
            String key = (String) keys.next();
            Object element = table.get(key);
            return getConstructionForElement(element);
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in index iterators");
        }
    }

    public Iterator<Index> indexIterator() {
        return new TableIndexIterator();
    }

    public class TableIndexIterator implements Iterator<Index> {
        Iterator<String> keys = table.keySet().iterator();
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

    public void put(Object key, Object element) {
        Definition elementDef = null;
        elementDef = getElementDefinition(element);
        String k = null;
        if (key instanceof Value) {
            k = ((Value) key).getString();
        } else if (key != null) {
            k = key.toString();
        }
        table.put(k, elementDef);
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
    
    public String getText(Context context) throws Redirection {
        return TableBuilder.getTextForMap(collectionDef, table, context);
    }
}
