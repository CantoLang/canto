/* Canto Compiler and Runtime Engine
 * 
 * Value.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Interface for objects which can participate in expressions.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.8 $
 */

public interface Value extends ValueSource {

    public String getString();

    public boolean getBoolean();

    public byte getByte();

    public char getChar();

    public int getInt();

    public long getLong();

    public double getDouble();

    public Object getValue();

    public Class<?> getValueClass();
}
