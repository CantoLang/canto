/* Canto Compiler and Runtime Engine
 * 
 * Holder.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import canto.lang.ArgumentList;
import canto.lang.Definition;
import canto.lang.ResolvedInstance;

/**
 *  A simple container for associating a definition with an instantiated
 *  object.  A Holder is useful for lazy instantiation (avoiding instantiation
 *  till the last possible moment) and for providing name, type, source code
 *  and other metadata about the object that can be obtained from the 
 *  definition   
 *
 * 
 *  @author mash
 *
 */

public class Holder {
    public Definition nominalDef;
    public ArgumentList nominalArgs;
    public Definition def;
    public ArgumentList args;
    public Object data;
    public ResolvedInstance resolvedInstance;
    
    public Holder() {
        this(null, null, null, null, null, null, null);
    }
    
    public Holder(Definition nominalDef, ArgumentList nominalArgs, Definition def, ArgumentList args, Context context, Object data, ResolvedInstance resolvedInstance) {
        this.nominalDef = nominalDef;
        this.nominalArgs = nominalArgs;
        this.def = def;
        this.args = args;
        this.data = data;
        this.resolvedInstance = resolvedInstance;
    }

    public String toString() {
        return "{ nominalDef: "
             + (nominalDef == null ? "(null)" : nominalDef.getName())
             + "\n  nominalArgs: "
             + (nominalArgs == null ? "(null)" : nominalArgs.toString())
             + "\n  def: "
             + (def == null ? "(null)" : def.getName())
             + "\n  args: "
             + (args == null ? "(null)" : args.toString())
             + "\n  data: "
             + (data == null ? "(null)" : data.toString())
             + "\n  resolvedInstance: "
             + (resolvedInstance == null ? "(null)" : resolvedInstance.getName())
             + "\n}";
    }

}
