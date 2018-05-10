/* Canto Compiler and Runtime Engine
 * 
 * Session.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import javax.servlet.http.*;

import java.util.*;

/**
 * Session support for Canto.  Supports two kinds of sessions: a session based on
 * an HTTP request, and a session based on a provided id.  The latter is used for
 * direct server-to-server requests that do not use HTTP.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public class Session {
    
    /** Default maximum interval, in seconds, between requests.  If a new request
     *  isn't received within this interval then the session times out and is 
     *  invalidated.
     *  
     *  An interval value less than or equal to zero means the session never
     *  times out. 
     **/
    public static int DEFAULT_INACTIVE_INTERVAL = 0;

    private String id = null;
    private HttpSession session = null;
    private Map<String, Object> attribute = null;
    private long createdTime = 0L;
    private long lastAccessedTime = 0L;
    private int maxInactive = 0;
    private boolean invalidated = false;
    

    public Session(HttpSession session) {
        this.session = session;
        attribute = new AttributeMap();
        createdTime = session.getCreationTime();
        lastAccessedTime = session.getLastAccessedTime();
        maxInactive = session.getMaxInactiveInterval();
        id = session.getId();
    }

    public Session(String sessionId) {
        attribute = new HashMap<String, Object>();
        createdTime = lastAccessedTime = System.currentTimeMillis();
        maxInactive = DEFAULT_INACTIVE_INTERVAL;
        id = sessionId;
    }

    public Map<String, Object> attributes() {
        return attribute;
    }

    public Object getAttribute(String key) {
        return attribute.get(key);
    }

    public void setAttribute(String key, Object value) {
        attribute.put(key, value);
    }

    public void removeAttribute(String key) {
        attribute.remove(key);
    }

    public long created() {
        return createdTime;
    }

    public long accessed() {
        if (session != null) {
            return Math.max(session.getLastAccessedTime(), lastAccessedTime);
        } else {
            return lastAccessedTime;
        }
    }

    public String id() {
        return id;
    }

    public int max_inactive(int max) {
        if (session != null) {
            session.setMaxInactiveInterval(max);
            maxInactive = session.getMaxInactiveInterval();
        } else {
            maxInactive = max;
        }
        return maxInactive;
    }

    public int max_inactive() {
        if (session != null) {
            maxInactive = session.getMaxInactiveInterval();
        }
        return maxInactive;
    }
    
    public void invalidate() {
        if (session != null) {
            session.invalidate();
        }
        invalidated = true;
    };
    
    public void updateAccessedTime() {
        long now = System.currentTimeMillis();
        if (maxInactive > 0 && (now - lastAccessedTime)/1000 > maxInactive) {
            invalidated = true;
        } else {
            lastAccessedTime = now;
        }
    }
    
    public boolean is_valid() {
        return !invalidated;
    }

    class AttributeMap extends AbstractMap<String, Object> {

        public Set<Map.Entry<String, Object>> entrySet() {
            return (Set<java.util.Map.Entry<String, Object>>) new AttributeSet();
        }

        public Object get(Object key) {
            return session.getAttribute(key.toString());
        }

        public Object put(String key, Object value) {
            Object oldValue = session.getAttribute(key);
            session.setAttribute(key, value);
            return oldValue;
        }
        
        public Object remove(String key) {
            Object oldValue = session.getAttribute(key);
            if (oldValue != null) {
                session.removeAttribute(key);
            }
            return oldValue;
        }
        
    }

    class AttributeSet extends AbstractSet<Map.Entry<String, Object>> {
        public Iterator<Map.Entry<String, Object>> iterator() {
            return new AttributeIterator();
        }
        public int size() {
            Enumeration<String> nameEnum = session.getAttributeNames();
            int count = 0;
            while (nameEnum.hasMoreElements()) {
                count++;
                nameEnum.nextElement();
            }
            return count;
        }
    }

    class AttributeIterator implements Iterator<Map.Entry<String, Object>> {
        private Enumeration<String> names;
        private List<String> removeList;
        private String lastName;

        public AttributeIterator() {
            names = session.getAttributeNames();
            removeList = null;
            lastName = null;
        }

        public boolean hasNext() {
            if (!names.hasMoreElements()) {
                if (removeList != null) {
                    Iterator<String> it = removeList.iterator();
                    while (it.hasNext()) {
                        session.removeAttribute(it.next());
                    }
                }
                return false;
            } else {
                return true;
            }
        }

        public Map.Entry<String, Object> next() {
            String name = (String) names.nextElement();

            // remember this name for possible removal
            lastName = name;

            Object attribute = session.getAttribute(name);
            return new AttributeEntry(name, attribute);
        }

        public void remove() {
            if (lastName != null) {
                if (removeList == null) {
                    removeList = new ArrayList<String>();
                }
                removeList.add(lastName);
            }
        }
    }

    class AttributeEntry implements Map.Entry<String, Object> {
        private String key;
        private Object value;
        public AttributeEntry(String key, Object value) {
            if (key == null) {
                throw new NullPointerException("AttributeEntry key must be be nonnull");
            }
            if (value == null) {
                throw new NullPointerException("AttributeEntry value must be be nonnull");
            }
            this.key = key;
            this.value = value;
        }

        public boolean equals(Object o) {
            if (o instanceof Map.Entry) {
                @SuppressWarnings("unchecked")
                Map.Entry<String, Object> e = (Map.Entry<String, Object>) o;
                return (key.equals(e.getKey()) && value.equals(e.getValue()));
            }
            return false;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public int hashCode() {
            return (key.hashCode() ^ value.hashCode());
        }

        public Object setValue(Object value) {
            if (value == null) {
                throw new NullPointerException("AttributeEntry value must be be nonnull");
            }
            Object oldValue = this.value;
            this.value = value;
            session.setAttribute(key.toString(), value);
            return oldValue;
        }
    }
}
