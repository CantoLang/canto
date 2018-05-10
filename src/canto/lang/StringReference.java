/* Canto Compiler and Runtime Engine
 * 
 * StringReference.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.io.*;
import java.net.*;

/**
 * A StringReference is a flexible reference to text data.  The text is
 * represented in two ways: transiently by a StringPrimitive, and more
 * permanently by a pointer to the source of the string (file, url etc.)
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.4 $
 */

public class StringReference {
    private StringPrimitive sp;    // either a String or a StringBuffer
    protected Object source;
    protected long sourceOffset;
    private int offset;
    private int length;

    public StringReference(Object source, long sourceOffset, StringPrimitive sp) {
        this(source, sourceOffset, sp, 0, sp.length());
    }

    public StringReference(Object source, long sourceOffset, StringPrimitive sp, int offset, int length) {
        this.sp = sp;
        this.source = source;
        this.sourceOffset = sourceOffset;
        this.offset = offset;
        this.length = length;
    }

    public Object getSource() {
        return source;
    }

    public long getSourceOffset() {
        return sourceOffset;
    }

    public void clear() {
        sp = null;
    }

    public Reader getReader() {
        if (sp == null || sp.isNull()) {
            return new SourceReader();
        } else {
            Reader reader = new StringReader(sp.toString());
            if (offset > 0) {
                try {
                    reader.skip(offset);
                } catch (Exception e) {
                    System.out.println("Unable to advance reader to proper offset in StringReference: " + e);
                }
            }
            return reader;
        }
    }

    synchronized private void load() throws java.io.IOException {
        Reader reader = new SourceReader();
        if (length < 0) {
            length = Integer.MAX_VALUE;
        }
        sp = new StringPrimitive(new StringBuffer(length < Integer.MAX_VALUE ? length : 23));
        for (int i = 0; i < length; i++) {
            int retval = reader.read();
            if (retval == -1) {
                length = i;
                break;
            }
            sp.append((char) retval);
        }
        offset = 0;
    }

    public int getLength() {
        return length;
    }

    public boolean equals(Object object) {
        if (object instanceof StringReference) {
            return getString().equals(((StringReference) object).getString());
        } else {
            return getString().equals(object.toString());
        }
    }

    public boolean isAdjacent(StringReference stref) {
        if (sp.equals(stref.sp)) {
            if (offset < stref.offset) {
                return (stref.offset == offset + length);
            } else {
                return (offset == stref.offset + stref.length);
            }
        } else {
            return false;
        }
    }

    public char charAt(int index) {
        return sp.charAt(offset + index);
    }

    public int indexOf(char c) {
        int ix = sp.indexOf(c, offset);
        if (ix >= offset + length) {
            ix = -1;
        }
        return ix;
    }

    public boolean merge(StringReference stref) {
        if (!isAdjacent(stref)) {
            //throw new IllegalArgumentException("can't merge; passed reference is not adjacent.");

            return false; // this is more efficient than an exception -- don't have
                          // to check for adjacency twice
        }
        if (offset > stref.offset) {
            offset = stref.offset;
        }
        length += stref.length;
        return true;
    }

    public String getString() {
        if (sp == null) {
            try {
                load();
            } catch (Exception e) {
                System.out.println("Unable to load string reference: " + e);
                return null;
            }
        }
        if (offset == 0 && length == sp.length()) {
            return sp.toString();
        } else {
            return sp.substring(offset, offset + length);
        }
    }

    public StringReference getSubreference(int start, int end) {
        return new StringReference(source, sourceOffset, sp, offset + start, end - start);
    }

    public StringReference getSubreference(int start) {
        return new StringReference(source, sourceOffset, sp, offset + start, length - start);
    }

    public class SourceReader extends Reader {

        private Reader in = null;
        public SourceReader() {
            if (source != null) {
                try {
                    if (source instanceof Reader) {
                        in = (Reader) source;
                    } else if (source instanceof InputStream) {
                        in = new BufferedReader(new InputStreamReader((InputStream) source));
                    } else if (source instanceof String) {
                        URL url = new URL((String) source);
                        InputStream inStream = url.openStream();
                        in = new BufferedReader(new InputStreamReader(inStream));
                    }
                    if (in != null) {
                        in.skip(sourceOffset);
                    }

                } catch (Exception e) {
                    System.out.println("Unable to read source " + source + ": " + e);
                }
            }
        }

        public int read() throws IOException {
            return in.read();
        }

        public int read(char cbuf[], int offset, int len) throws IOException {
            return in.read(cbuf, offset, len);
        }

        public void close() throws IOException {
            in.close();
        }

        public long skip(long n) throws IOException {
            return in.skip(n);
        }
    }
}
