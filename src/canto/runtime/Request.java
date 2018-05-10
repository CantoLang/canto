/* Canto Compiler and Runtime Engine
 * 
 * Request.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import javax.servlet.http.*;

import java.util.*;

/**
 * Supports access to an HTTP request by Canto code.
 *
 * @author Michael St. Hippolyte
 */

public class Request {

    private HttpServletRequest request = null;
    private Session session = null;
    private Map<String, String> paramMap = null;

    public Request(HttpServletRequest request) {
        this.request = request;
    }

    public Request(Session session, Map<String, String> requestParams) {
        this.session = session;
        paramMap = (requestParams == null ? new HashMap<String, String>() : requestParams);
    }
    
    
    public Map<String, String> params() {
        if (paramMap == null) {
            paramMap = new SingleEntryMap(request.getParameterMap());
        }
        return paramMap;
    }

   
    public Session getSession() {
        if (session == null) {
            session = new Session(request.getSession());
        }
        return session;
    }
    

    /** This class wraps a map containing string arrays and emulates a map
     *  containing strings.  The <code>get</code> method returns the first
     *  string in the array contained in the underlying map.  Other methods
     *  expecting string arrays (<code>put</code>, <code>values</code> etc.)
     *  similarly take or return t the first string in the underlying array.
     */
    public class SingleEntryMap implements Map<String, String> {
        private Map<String, String[]> map;

        public SingleEntryMap(Map<String, String[]> map) {
            this.map = map;
        }

        public int size() {
            return map.size();
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        public boolean containsValue(Object value) {
            Collection<String[]> valArrays = (Collection<String[]>) map.values();
            for (String[] strArray: valArrays) {
                if (strArray != null) {
                    if (strArray[0].equals(value)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public String get(Object key) {
            String str = null;
            String[] strArray = map.get(key);
            if (strArray != null) {
                str = strArray[0];
            }
            return str;
        }

        public String put(String key, String value) {
            String[] valArray = new String[1];
            valArray[0] = value;
            valArray = map.put(key, valArray);
            return (valArray != null ? valArray[0] : null);
        }

        public String remove(Object key) {
            String[] valArray = map.remove(key);
            return (valArray != null ? valArray[0] : null);
        }

        public void putAll(Map<? extends String, ? extends String> t) {
            throw new UnsupportedOperationException("Request param map does not support putAll()");
        }

        public void clear() {
            map.clear();
        }

        public Set<String> keySet() {
            return map.keySet();
        }

        public Collection<String> values() {
            ArrayList<String> vals = new ArrayList<String>(map.size());
            
            Collection<String[]> valArrays = (Collection<String[]>) map.values();
            for (String[] strArray: valArrays) {
                if (strArray != null) {
                    vals.add(strArray[0]);
                }
            }
             
            return vals;
        }

        public Set<Map.Entry<String, String>> entrySet() {
            Set<Map.Entry<String, String[]>> mapEntries = map.entrySet();
            Set<Map.Entry<String, String>> entries = new HashSet<Map.Entry<String, String>>(mapEntries.size());

            for (Map.Entry<String, String[]> entry: mapEntries) {
                if (entry != null) {
                    String key = entry.getKey();
                    String[] valArray = entry.getValue();
                    if (valArray != null) {
                        entries.add(new AbstractMap.SimpleEntry<String, String>(key, valArray[0]));
                    }
                }
            }
            return entries;
        }

        public boolean equals(Object o) {
            return map.equals(o);
        }

        public int hashCode() {
            return map.hashCode();
        }

    }
}
