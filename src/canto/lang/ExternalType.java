/* Canto Compiler and Runtime Engine
 * 
 * ExternalType.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A Type corresponding to an externally defined class.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.11 $
 */
public class ExternalType extends ComplexType {

    public ExternalType(ExternalDefinition def) {
        super(def, def.getExternalTypeName(), def.getDims(), def.getArguments());
    }

    public Class<?> getTypeClass(Context context) {
        return ((ExternalDefinition) getDefinition()).getExternalClass(context);
    }
}


