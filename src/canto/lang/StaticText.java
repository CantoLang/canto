/* Canto Compiler and Runtime Engine
 * 
 * StaticText.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * StaticText holds a chunk of text.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.16 $
 */
public class StaticText extends AbstractNode implements Construction {

    private String text;
    private boolean trimLeading = false;
    private boolean trimTrailing = false;

    public StaticText() {}

    public StaticText(String text) {
        this.text = text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isPrimitive() {
        return true;
    }

    public boolean isStatic() {
        return true;
    }

    public boolean isDynamic() {
        return false;
    }

    public boolean isDefinition() {
        return false;
    }

    public boolean getBoolean(Context context) throws Redirection {
        return (text == null || text.length() == 0);
    }

    public String getText(Context context) throws Redirection {
        return trim(text);
    }

    public Object getData(Context context) throws Redirection {
        return trim(text);
    }

    public Object getData(Context context, Definition def) throws Redirection {
        return trim(text);
    }

    public boolean isAbstract(Context context) {
        return false;
    }
    
    public String getText() {
        return trim(text);
    }

    /** Gets the length of this element */
    public int getLength() {
        return text.length();
    }

    /** Returns the type of this construction in the specified context. */
    public Type getType(Context context, boolean generate) {
        return new PrimitiveType(String.class);
    }

    /** Returns the name of the definition being constructed */
    public String getDefinitionName() {
        return null;
    }

    /** Return the construction that this construction resolves to, if it
     *  is a wrapper or alias of some sort, or else return this construction.
     *  This class is not a wrapper or alias, so it returns this construction.
     */
    public Construction getUltimateConstruction(Context context) {
        return this;
    }

    public String toString() {
        return text + '\n';
    }

    public String toString(String prefix) {
        return prefix + text + '\n';
    }

    public void setTrimLeadingWhitespace(boolean trim) {
        trimLeading = trim;
    }

    public void setTrimTrailingWhitespace(boolean trim) {
        trimTrailing = trim;
    }

    /** Clean the text as returned by the parser.  All leading and trailing
     *  whitespace on each line (other than the newline) is removed.  Leading
     *  newlines are also removed.
     */
    public String clean(String str) {
        if (str.indexOf('\n') == -1) {
            return trim(str);
        }

        StringTokenizer toker = new StringTokenizer(str, "\n\r\f", true);   // returns delimiters
        StringBuffer retstr = new StringBuffer();
        while (toker.hasMoreTokens()) {
            String tok = toker.nextToken();
            if (tok.length() > 1) {
                retstr.append(trim(tok));
            } else if (tok.length() == 1) {
                char c = tok.charAt(0);
                if (c == '\n' || c == '\r' || c == '\f') {
                    if (retstr.length() > 0 || !trimLeading) {
                        retstr.append(c);
                    }
                } else if (c != '\t' && c != ' ') {
                    retstr.append(c);
                }
            }
        }
        return retstr.toString();
    }

    protected String trim(String str) {
        if (trimLeading && trimTrailing) {
            return str.trim();
        } else if (trimLeading) {
            for (int i = 0; i < str.length(); i++) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return str.substring(i);
                }
            }
            // if the string is all whitespace, return an empty string
            return "";

        } else if (trimTrailing) {
            for (int i = str.length() - 1; i >= 0; i--) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return str.substring(0, i + 1);
                }
            }
            // if the string is all whitespace, return an empty string
            return "";
       }
       return str;
    }

	public String getString(Context context) throws Redirection {
		return getText();
	}

	public byte getByte(Context context) throws Redirection {
		return (byte) PrimitiveValue.getIntFor(text);
	}

	public char getChar(Context context) throws Redirection {
		return PrimitiveValue.getCharFor(text);
	}

	public int getInt(Context context) throws Redirection {
		return PrimitiveValue.getIntFor(text);
	}

	public long getLong(Context context) throws Redirection {
		return PrimitiveValue.getLongFor(text);
	}

	public double getDouble(Context context) throws Redirection {
		return PrimitiveValue.getDoubleFor(text);
	}

	public Value getValue(Context context) throws Redirection {
		return new PrimitiveValue(text);
	}
}

