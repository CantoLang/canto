/* Canto Compiler and Runtime Engine
 * 
 * TableBuilder.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * TableBuilder constructs tables and instantiates their contents.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */
public class TableBuilder extends CollectionBuilder {

    static public Object instantiateElements(Object tableObject, Context context) throws Redirection {
        Object tableInstance = tableObject;
        if (tableObject instanceof Map<?,?>) {
        	Map<String,Object> tableMap = (Map<String,Object>) tableObject;
            Set<String> keys = tableMap.keySet();
            for (String key: keys) {
                Object data = tableMap.get(key);
                Object origData = data;

                if (data instanceof CollectionInstance) {
                     data = ((CollectionInstance) data).getCollectionObject();
                }

                if (data instanceof ElementDefinition) {
                    data = ((ElementDefinition) data).getElement(context);
                }

                if (data instanceof Value) {
                    data = ((Value) data).getValue();

                } else if (data instanceof ValueGenerator) {
                    data = ((ValueGenerator) data).getData(context);
                }

                if (!data.equals(origData)) {
                	tableMap.put(key, data);
                }
            }

        } else  {
            throw new Redirection(Redirection.STANDARD_ERROR, "Unable to instantiate elements for table, passed table object is of wrong type: " + tableObject.getClass().getName());
        }
        return tableInstance;
    }

    private CollectionDefinition tableDef = null;

    public TableBuilder(CollectionDefinition tableDef) {
        this.tableDef = tableDef;
    }

    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        return new ResolvedTable(tableDef, context, args, indexes);
    }

    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes, Object collectionData) throws Redirection {
        return new ResolvedTable(tableDef, context, args, indexes, collectionData);
    }

    public static String getTextForMap(CollectionDefinition collectionDef, Map<?,?> map, Context context) throws Redirection {
        StringBuffer sb = new StringBuffer();
        
        sb.append("{ ");

        Object[] keys = map.keySet().toArray();
        if (keys.length > 0) {
            Arrays.sort(keys);
            for (int i = 0; i < keys.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                String key = keys[i].toString();
                Object element = map.get(keys[i]);
                sb.append(key);
                sb.append(": ");
                Definition def = (collectionDef == null ? null : ResolvedCollection.getElementDefinitionForCollection(element, collectionDef, context));
                Construction construction = AbstractConstruction.getConstructionForElement(element, context);
                if (def != null && def.hasChildDefinition("decorate_element")) {
                    List<Construction> list = new SingleItemList<Construction>(construction);
                    ArgumentList args = new ArgumentList(list);
                    Object data = def.getChild(new NameNode("decorate_element"), args, null, null, context, true, true, null, null);
                    sb.append(data.toString());
               
                } else {
                    sb.append('"');
                    sb.append(construction.getText(context));
                    sb.append('"');
                }
            }
        }
        
        sb.append(" }");
        
        return sb.toString();
    }

}

class TableInstance implements Map<String, Object> {
    //private ValueGenerator valueGen;
    //private Context initContext = null;
    private Map<String, Object> map = null;

    public TableInstance(Map<String, Object> map) {
        this.map = map;
    }

    @SuppressWarnings("unchecked")
    public TableInstance(ValueGenerator valueGen, Context context) throws Redirection {
        //this.valueGen = valueGen;
        //initContext = (Context) context.clone();
        Object obj = null;
        boolean pushed = false;
        ArgumentList args = null;
        List<Index> indexes = null;
        try {
          /***  if (valueGen instanceof Instantiation) {
                Instantiation instance = (Instantiation) valueGen;
                Instantiation ultimateInstance = instance.getUltimateInstance(context);
                indexes = (instance == ultimateInstance ? null : instance.getIndexes());
                Definition def = ultimateInstance.getDefinition(context);
                if (def != null) {
                    args = ultimateInstance.getArguments();
                    obj = ultimateInstance.instantiate(context, def, args, indexes);
                    ParameterList params = def.getParamsForArgs(args, context);
                    context.push(def, params, args, false);
                    pushed = true;
                }
            } else ***/  
            
            if (valueGen instanceof IndexedMethodConstruction) {
                obj = ((IndexedMethodConstruction) valueGen).getCollectionObject(context);
                
            } else {
                obj = valueGen.getData(context);
                if (obj instanceof Value) {
                    obj = ((Value) obj).getValue();
                }
            }
            if (obj instanceof DynamicObject) {
                obj = ((DynamicObject) obj).initForContext(context, args, indexes);
            }
            
            /*** if (indexes != null) {
                obj = context.dereference(obj, indexes);
            } ***/
            
            if (obj instanceof Map<?,?>) {
                map = (Map<String, Object>) obj;
            } else if (obj instanceof List<?> || (obj != null && obj.getClass().isArray())) {
                map = new MappedArray(obj, context);
            } else if (obj instanceof CollectionInstance) {
                obj = ((CollectionInstance) obj).getCollectionObject();
                if (obj instanceof Map<?,?>) {
                    map = (Map<String, Object>) obj;
                } else {
                    map = new MappedArray(obj, context);
                }
            } else if (obj instanceof CollectionDefinition) {
                CollectionInstance collectionInstance = ((CollectionDefinition) obj).getCollectionInstance(context, args, indexes);
                obj = collectionInstance.getCollectionObject();
                if (obj instanceof Map<?,?>) {
                    map = (Map<String, Object>) obj;
                } else {
                    map = new MappedArray(obj, context);
                }

            } else if (obj == null) {
                map = null;
                
            } else {
                throw new Redirection(Redirection.STANDARD_ERROR, "Can't instantiate table in TableInstance; unsupported class " + obj.getClass().getName());
            }
    
        } finally {
            if (pushed) {
                context.pop();
            }
        }
    }


    public int size() {
        if (map == null) {
            return 0;
        } else {
            return map.size();
        }
    }

    public boolean isEmpty() {
        if (map == null) {
            return true;
        } else {
            return map.isEmpty();
        }
    }

    public boolean containsKey(Object key) {
        if (map == null) {
            return false;
        } else {
            return map.containsKey(key);
        }
    }

    public boolean containsValue(Object value) {
        if (map == null) {
            return false;
        } else {
            return map.containsValue(value);
        }
    }

    public Object get(Object key) {
        if (map == null) {
            return null;
        } else {
            return map.get(key);
        }
    }

    public Object put(String key, Object value) {
        if (map == null) {
            throw new NullPointerException("Cannot put value in table; map is null");
        }
        return map.put(key, value);
    }

    public Object remove(Object key) {
        if (map == null) {
            throw new NullPointerException("Cannot remove value from table; map is null");
        }
        return map.remove(key);
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        if (map == null) {
            throw new NullPointerException("Cannot put values into table; map is null");
        }
        map.putAll(t);
    }

    public void clear() {
        if (map != null) {
            map.clear();
        }
    }

    public Set<String> keySet() {
        if (map == null) {
            return null;
        } else {
            return map.keySet();
        }
    }

    public Collection<Object> values() {
        if (map == null) {
            return null;
        } else {
            return map.values();
        }
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        if (map == null) {
            return null;
        } else {
            return map.entrySet();
        }
    }

    public boolean equals(Object o) {
        if (map == null) {
            return (o == null || (o instanceof TableInstance && ((TableInstance) o).map == null));
        }
        return map.equals(o);
    }

    public int hashCode() {
        if (map == null) {
            return 0;
        } else {
            return map.hashCode();
        }
    }

}

