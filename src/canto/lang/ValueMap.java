/* Canto Compiler and Runtime Engine
 * 
 * ValueMap.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Map;


/**
 * Interface for objects which hold named values, such as a record in a
 * database.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public interface ValueMap extends Map<String, Object>, ValueSource {

    public String getString(String key);

    public boolean getBoolean(String key);

    public byte getByte(String key);

    public char getChar(String key);

    public int getInt(String key);

    public long getLong(String key);

    public double getDouble(String key);

    public Object getValue(String key);

    public Class<?> getValueClass(String key);
}
