/* Canto Compiler and Runtime Engine
 * 
 * ExternalTableBuilder.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import canto.runtime.Context;

/**
 * Facade class to make a Java object available as a Canto definition.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.1 $
 */
public class ExternalTableBuilder extends TableBuilder {

    private ExternalDefinition externalDef = null;

    public ExternalTableBuilder(ExternalCollectionDefinition collectionDef, ExternalDefinition externalDef) {
        super(collectionDef);
        this.externalDef = externalDef; 
    }

}


