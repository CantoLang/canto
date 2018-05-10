/* Canto Compiler and Runtime Engine
 * 
 * DefinitionTable.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
* A DefinitionTable supports the lookup of definitions by name.
*
* @author Michael St. Hippolyte
* @version $Revision: 1.10 $
*/
public interface DefinitionTable {

   /** Returns the global definition for the indicated name. */
   public Definition getDefinition(String name);

   /** Returns the definition for the indicated name in the context of the
    *  indicated owner.  The definition may be global, local or external.  If the
    *  definition is local, either <code>owner.getFullName()<code> or <code>name</code>
    *  must start with the site name as a prefix.
    */
   public Definition getDefinition(NamedDefinition owner, NameNode name);

   /** Returns the internal definition (i.e., external definitions are
    *  ignored) for the indicated name in the context of the indicated owner,
    *  or null if no such definition exists.  Either <code>ownerName<code> or
    *  <code>name</code> must start with the site name.
    */
   public Definition getInternalDefinition(String ownerName, String name);

   /** Adds a definition to the table.   If a definition by the same full name is already in the
    *  table, and replace is true, the old definition is replaced by the passed definition; if
    *  replace is false, a DuplicateDefinitionException is thrown.
    */
   public void addDefinition(Definition def, boolean replace) throws DuplicateDefinitionException;

   /** Returns the number of definitions in the table. */
   public int size();
}
