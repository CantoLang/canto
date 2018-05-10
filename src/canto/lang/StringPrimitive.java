/* Canto Compiler and Runtime Engine
 * 
 * StringPrimitive.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * A StringPrimitive is a polymorphic representation of text data.  The
 * underlying data can be either a StringBuffer, part of a StringBuffer
 * or a String, and which one it is may change during the life of the
 * StringPrimitive.  This allows character data to be managed efficiently
 * but invisibly.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public class StringPrimitive {

    public static int defaultSize = 23;

    private Object stringObject;    // either a String or a StringBuffer

    public StringPrimitive(StringBuffer sb) {
        setStringBuffer(sb);
    }

    public StringPrimitive(String string) {
        setString(string);
    }

    public void setStringBuffer(StringBuffer sb) {
        stringObject = sb;
    }

    public boolean isNull() {
        return (stringObject == null);
    }


    public void setString(String string) {
        stringObject = string;
    }

    public void setCapacity(int capacity) {
        int oldCapacity;
        if (stringObject == null) {
            oldCapacity = 0;
        } else if (stringObject instanceof String) {
            oldCapacity = ((String) stringObject).length();

        } else {
            oldCapacity = ((StringBuffer) stringObject).capacity();
        }

        if (capacity != oldCapacity) {
            StringBuffer sb = new StringBuffer(capacity);
            if (stringObject != null) {
                if (capacity > oldCapacity) {
                    sb.append(stringObject.toString());
                } else if (stringObject instanceof String) {
                    sb.append(((String) stringObject).substring(0, capacity));
                } else {
                    sb.append(((StringBuffer) stringObject).substring(0, capacity));
                }
            }
            stringObject = sb;
        }
    }

    public int capacity() {
        if (stringObject == null) {
            return 0;
        } else if (stringObject instanceof StringBuffer) {
            return ((StringBuffer) stringObject).capacity();
        } else {
            return ((String) stringObject).length();
        }
    }

    public int length() {
        if (stringObject == null) {
            return 0;
        } else if (stringObject instanceof StringBuffer) {
            return ((StringBuffer) stringObject).length();
        } else {
            return ((String) stringObject).length();
        }
    }

    public void append(char c) {
        if (stringObject == null) {
            stringObject = new StringBuffer(defaultSize);
        } else if (stringObject instanceof String) {
            stringObject = new StringBuffer((String) stringObject);
        }

        StringBuffer sb = (StringBuffer) stringObject;
        sb.append(c);
    }

    public void append(String str) {
        if (stringObject == null) {
            stringObject = new StringBuffer(defaultSize);
        } else if (stringObject instanceof String) {
            stringObject = new StringBuffer((String) stringObject);
        }

        StringBuffer sb = (StringBuffer) stringObject;
        sb.append(str);
    }

    public String toString() {
        if (stringObject == null) {
            return null;
        } else if (stringObject instanceof StringBuffer) {
            return new String((StringBuffer) stringObject);
        } else {
            return (String) stringObject;
        }
    }

    public StringBuffer getStringBuffer() {
        if (stringObject == null) {
            stringObject = new StringBuffer(defaultSize);
        } else if (stringObject instanceof String) {
            stringObject = new StringBuffer((String) stringObject);
        }
        return (StringBuffer) stringObject;
    }

    public String substring(int start) {
        if (stringObject == null) {
            return null;
        } else if (stringObject instanceof StringBuffer) {
            return ((StringBuffer) stringObject).substring(start);
        } else {
            return ((String) stringObject).substring(start);
        }
    }

    public String substring(int start, int end) {
        if (stringObject == null) {
            return null;
        } else if (stringObject instanceof StringBuffer) {
            return ((StringBuffer) stringObject).substring(start, end);
        } else {
            return ((String) stringObject).substring(start, end);
        }
    }

    public char charAt(int index) {
        if (stringObject == null) {
            throw new IndexOutOfBoundsException();
        } else if (stringObject instanceof StringBuffer) {
            return ((StringBuffer) stringObject).charAt(index);
        } else {
            return ((String) stringObject).charAt(index);
        }
    }

    public int indexOf(char c, int start) {
        if (stringObject == null) {
            return -1;
        } else if (stringObject instanceof StringBuffer) {
            StringBuffer sb = (StringBuffer) stringObject;
            for (int i = start; i < sb.length(); i++) {
                if (sb.charAt(i) == c) {
                    return i;
                }
            }
            return -1;
        } else {
            return ((String) stringObject).indexOf(c, start);
        }
    }
}
