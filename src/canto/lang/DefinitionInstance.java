/* Canto Compiler and Runtime Engine
 * 
 * DefinitionInstance.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * DefinitionInstance is a wrapper for a definition with arguments and indexes.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.1 $
 */

public class DefinitionInstance {
    public Definition def;
    public ArgumentList args;
    public List<Index> indexes;
        
    public DefinitionInstance(Definition def, ArgumentList args, List<Index> indexes) {
        this.def = def;
        this.args = args;
        this.indexes = indexes;
    }
}	    
