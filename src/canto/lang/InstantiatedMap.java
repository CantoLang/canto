/* Canto Compiler and Runtime Engine
 * 
 * InstantiatedMap.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;
import canto.runtime.Holder;

/**
 * InstantiatedMap wraps a Map and dereferences items in the map as they are accessed.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.18 $
 */

public class InstantiatedMap implements Map<String, Object> {
    private Map<String,Object> map;
    private Map<String,Object> xmap = null;
    private Map<String,Object> vmap = null;
    private int xsize = 0;
    private Context context;
    private CollectionDefinition collectionDef;
    private boolean valMapNeeded = true;

    public InstantiatedMap(Map<String,Object> map, CollectionDefinition collectionDef, Context context) {
        if (map == null) {
            throw new NullPointerException("attempt to wrap InstantiatedMap around a null map");
        }
        this.map = map;
        
        Collection<Object> vals = map.values();
        Iterator<Object> it = vals.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof ValueGenerator || obj instanceof ElementDefinition || obj instanceof ExternalDefinition) {
                valMapNeeded = true;
                break;
            }
        }
       
        this.context = (Context) context.clone();
    }

    public int size() {
        return map.size() + xsize;
    }

    public boolean isEmpty() {
        if (!map.isEmpty()) {
            return false;
        } else if (xmap != null) {
            return xmap.isEmpty();
        } else {
            return true;
        }
    }

    public boolean containsKey(Object key) {
        if (map.containsKey(key)) {
            return true;
        } else if (xmap != null) {
            return xmap.containsKey(key);
        } else {
            return false;
        }
    }

    public boolean containsValue(Object value) {
        if (!valMapNeeded) {
            return map.containsValue(value);
        } else {
            instantiateValues();
            return vmap.containsValue(value);
        }
    }

    public Object get(Object keyObj) {
        Object obj;
        String key = keyObj.toString();
        
        if (vmap != null) {
            obj = vmap.get(key);
            if (obj != null) {
                if (obj instanceof Holder) {
                    return ((Holder) obj).data;
                } else {
                    return obj;
                }
            }
        }
        return getValue(key);    
    }
    
    public Object getElement(Object key) {
        Object element = null;
        if (xmap != null) {
            element = xmap.get(key);
        }
        if (element == null) {
            element = map.get(key);
        }
        return element;
    }
    
    public void putElement(String key, Object element) {
        if (xmap == null) {
            xmap = new HashMap<String,Object>(map.size());
        }
        xmap.put(key, element);
    }
    
    
    private Object getValue(String key) {
        Object obj = (xmap != null ? xmap.get(key) : null);
        if (obj == null) {
            obj = map.get(key);
        }
        if (obj == null) {
            return null;
        } else if (obj instanceof ElementDefinition) {
            obj = ((ElementDefinition) obj).getElement();
            putElement(key, obj);
        } else if (obj instanceof ExternalDefinition) {
            obj = ((ExternalDefinition) obj).getObject();
            putElement(key, obj);
        }
        
        Object val = AbstractNode.getObjectValue(context, obj);

        if (vmap != null) {
            vmap.put(key, val);
        }
        
        return val;
    }

    public Object put(String key, Object value) {
        if (xmap == null) {
            xmap = new HashMap<String,Object>(map.size());
        }
        Object oldVal = xmap.put(key, value);  // TODO: should be element for value
        if (map.get(key) == null) {
            xsize++;
        }
        if (vmap != null) {
            return vmap.put(key, value);
        } else {
            return oldVal;
        }
    }

    public Object remove(Object key) {
        if (vmap != null && vmap.get(key) != null) {
            return vmap.remove(key);
        } else {
            return null;
        }
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        // For the extension map to properly handle this because it would
        // have to examine each element in the map to see if it is a totally 
        // new element or if it is a replacement of an element already in the
        // underlying map in order to calculate the correct xsize value.
        //
        // HashMap, conveniently, calls put recursively in its implementation
        // of this method, so xsize is correct.  Immutable underlying maps,
        // however, will probably throw an exception, 
        map.putAll(t);
    }

    public void clear() {
        if (xmap != null) {
            xmap.clear();
            xmap = null;
            xsize = 0;
        }
        map.clear();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<Object> values() {
        instantiateValues();
        return new AbstractCollection<Object>() {
            
                    // TODO: fix logic for overrides in xmap
                    public Iterator<Object> iterator() {
                        return new Iterator<Object>() {
                            private Iterator<Entry<String, Object>> it = map.entrySet().iterator();
                            private boolean handledBase = false;

                            public boolean hasNext() {
                                return it.hasNext();
                            }

                            public Object next() {
                                String key = it.next().getKey();
                                
                                // move on to the extended map if appropriate
                                if (!it.hasNext() && !handledBase) {
                                    handledBase = true;
                                    if (xmap != null) {
                                        it = xmap.entrySet().iterator();
                                    }
                                }
                                return vmap.get(key);
                            }

                            public void remove() {
                                throw new UnsupportedOperationException("InstantiatedMap.values is immutable");
                            }
                        };
                    }

                    public int size() {
                        return vmap.size();
                    }

                    public boolean isEmpty() {
                        return vmap.isEmpty();
                    }

                    public void clear() {
                        throw new UnsupportedOperationException("InstantiatedMap.values is immutable");
                    }

                    public boolean contains(Object v) {
                        return vmap.containsValue(v);
                    }
                };
    }

    public Set<Entry<String,Object>> entrySet() {
        instantiateValues();
        return vmap.entrySet();
    }

    public boolean equals(Object o) {
        if (o == map || o == xmap) {
            return true;
        }
        instantiateValues();
        return vmap.equals(o);
    }

    public int hashCode() {
        return map.hashCode();
    }

    public String toString() {
        try {
            return TableBuilder.getTextForMap(collectionDef, this, context);
        } catch (Redirection r) {
            return null;
        }
    }
    
    
    private void ensureValueMap() {
        if (vmap == null) {
            if (xsize == 0) {
                
            }
            vmap = new HashMap<String,Object>(map.size() + xsize);
        }
    }    

    private void instantiateValues() {
        ensureValueMap();
        if (map.size() > 0 || xsize > 0) {
            Set<Entry<String,Object>> entries;
            Iterator<Entry<String,Object>> it;  
            entries = map.entrySet();
            it = entries.iterator();  
            while (it.hasNext()) {
                Map.Entry<String,Object> entry = (Map.Entry<String,Object>) it.next();
                String key = entry.getKey();
                // getValue puts the value in vmap
                getValue(key);
            }
            if (xmap != null && xmap.size() > 0) {
                entries = xmap.entrySet();
                it = entries.iterator();  
                while (it.hasNext()) {
                    Map.Entry<String,Object> entry = (Map.Entry<String,Object>) it.next();
                    String key = entry.getKey();
                    if (vmap.get(key) == null) {
                        // getValue puts the value in vmap
                        getValue(key);
                    }
                }
            }
        }
    }


}
