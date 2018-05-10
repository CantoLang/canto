/* Canto Compiler and Runtime Engine
 * 
 * MappedArray.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

import java.lang.reflect.Array;

/**
 * MappedArray wraps an array or list with a Map interface, converting the key to an integer index in order to access
 * items in the array.  The items are dereferenced as they are accessed.
 *
 * @author Michael St. Hippolyte
 */

public class MappedArray implements Map {
    private Object array = null;
    private List list = null;
    private Context context;

    public MappedArray(Object obj, Context context) {
        if (obj == null) {
            throw new NullPointerException("attempt to wrap MappedArray around a null map");
        }
        
        if (obj instanceof List) {
            this.list = (List) obj;
        } else if (obj.getClass().isArray()) {
            this.array = obj;
        } else {
            throw new IllegalArgumentException("attempt to wrap MappedArray around a non-array object");
        }
        
        this.context = (Context) context.clone();
    }

    public int size() {
        if (array != null) {
            return Array.getLength(array);
        } else {
            return list.size();
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean containsKey(Object key) {
        int k;
        
        if (key instanceof Number) {
            k = ((Number) key).intValue();
        } else {
            k = Integer.parseInt(key.toString());
        }
        return (k >= 0 && k < size());
    }

    public boolean containsValue(Object value) {
        if (array != null) {
            int len = Array.getLength(array);
            for (int i = 0; i < len; i++) {
                Object element = Array.get(array, i);
                if (element == null && value == null) {
                    return true;
                } else if (element.equals(value)) {
                    return true;
                }
            }
            return false;
        } else {
            return list.contains(value);
        }
    }

    public Object get(Object key) {
        int k;
        
        if (key instanceof Number) {
            k = ((Number) key).intValue();
        } else {
            k = Integer.parseInt(key.toString());
        }

        Object obj;
        if (array != null) {
            obj = Array.get(array, k);
        } else {
            obj = list.get(k);
        }

        if (obj == null) {
            return null;
        } else if (obj instanceof ElementDefinition) {
            obj = ((ElementDefinition) obj).getElement();
        } else if (obj instanceof ExternalDefinition) {
            obj = ((ExternalDefinition) obj).getObject();
        }
        obj = AbstractNode.getObjectValue(context, obj);
        return obj;
    }

    public Object put(Object key, Object value) {
        int k;
        Object oldValue;
        
        if (key instanceof Number) {
            k = ((Number) key).intValue();
        } else {
            k = Integer.parseInt(key.toString());
        }

        if (array != null) {
            oldValue = Array.get(array, k);
            Array.set(array, k, value);
        } else {
            oldValue = list.get(k);
            list.set(k, value);
        }
        
        return oldValue;
    }

    public Object remove(Object key) {
        // this class doesn't support remove, for now anyway
        throw new UnsupportedOperationException("MappedArray does not support remove()");
    }

    public void putAll(Map t) {
        // we'll cross this bridge only if we have to...
        throw new UnsupportedOperationException("MappedArray doesn't support putAll");
    }

    public void clear() {
        if (array != null) {
            Class componentType = array.getClass().getComponentType();
            array = Array.newInstance(componentType, 0);
        } else {
            list.clear();
        }
    }

    public Set keySet() {
        int len = size();
        Set keys = new HashSet(len);
        for (int i = 0; i < len; i++) {
            keys.add(new Integer(i));
        }
        return keys;
    }

    public Collection values() {
        if (array != null) {
            if (array instanceof Object[]) {
                return Arrays.asList((Object[]) array);
            } else {
                // not very efficient; could be improved by creating appropriate wrapper classes for various primitive arrays
                // also, could be cached (as long as cached value is cleared if array is modified)
                int len = Array.getLength(array);
                List valueList = new ArrayList(len);
                for (int i = 0; i < len; i++) {
                    valueList.set(i, Array.get(array, i));
                }
                return valueList;
            }
        } else {
            return list;
        }
    }

    public Set entrySet() {
        // could be cached (as long as cached value is cleared if array is modified)
        // hold off for now 
        throw new UnsupportedOperationException("MappedArray doesn't support entrySet yet");
    }

    public boolean equals(Object o) {
        // this isn't really correct because it fails if o is a true map equivalent to this one
        if (array != null) {
            return array.equals(o);
        } else {
            return list.equals(o);
        }
    }

    public int hashCode() {
        if (array != null) {
            return array.hashCode();
        } else {
            return list.hashCode();
        }
    }
  
}
