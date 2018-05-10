/* Canto Compiler and Runtime Engine
 * 
 * Scoped.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.parser;

import canto.lang.*;

/**
 * Interface for parsed nodes which can have a scoper modifier (public, private
 * or protected).
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.6 $
 */
public interface Scoped extends Initializable {
    public void setModifiers(int access, int dur);
    public NameNode getDefName();
}
