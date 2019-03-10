/* Canto Compiler and Runtime Engine
 * 
 * ComplexDefinition.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
* ComplexDefinition is a named definition which may contain other named definitions.
*
* @author Michael St. Hippolyte
* @version $Revision: 1.108 $
*/

public class ComplexDefinition extends NamedDefinition {

    public static ComplexDefinition getComplexOwner(Definition owner) {
        while (owner != null) {
            if (owner instanceof ComplexDefinition) {
                return (ComplexDefinition) owner;
            }
            owner = owner.getOwner();
        }
        return null;
    }

    private DefinitionTable definitions;

    public ComplexDefinition() {
        super();
    }

    public ComplexDefinition(Definition def, Context context) {
        super(def, context);
        if (def instanceof ComplexDefinition) {
            definitions = ((ComplexDefinition)def).definitions;
        }
    }

    public List<Dim> getDims() {
        int numChildren = getNumChildren();
        int ix = numChildren;
        while (ix > 0) {
            if (getChild(ix - 1) instanceof Dim) {
                ix--;
                continue;
            } else {
                break;
            }
        }
        int n = numChildren - ix;
        if (n == 0) {
            return new EmptyList<Dim>();
        } else if (n == 1) {
            return new SingleItemList<Dim>((Dim) getChild(ix));
        } else {
            List<Dim> dims = Context.newArrayList(n, Dim.class);
            for (int i = ix; i < ix + n; i++) {
                dims.add((Dim) getChild(i));
            }
            return dims;
        }
    }
   
    /** Returns true if this definition can have child definitions. */
    public boolean canHaveChildDefinitions() {
        return true;
    }

    void setDefinitionTable(DefinitionTable table) {
        definitions = table;
    } 

    DefinitionTable getDefinitionTable() {
        if (definitions == null) {
            return super.getDefinitionTable();
        } else {
            return definitions;
        }
    }

    public void addDefinition(Definition def, boolean replace) throws DuplicateDefinitionException {
        // don't add definitions without a proper name
        NameNode name = def.getNameNode();
        if (name == null) {
            return;
        }

        Definition currentDef = definitions.getDefinition((NamedDefinition) def.getOwner(), name);

        // Parameters can appear in multiple parameter lists, as long
        // as the type is the same.
        if (def.isFormalParam()) {
            if (currentDef != null && currentDef.getSite() == def.getSite()) {
                if (!(currentDef.isFormalParam())) {
                    throw new DuplicateDefinitionException(name.getName() + " already defined");
                } else if (!currentDef.getType().equals(def.getType())) {
                    throw new DuplicateDefinitionException("parameter " + name.getName() + " declared with more than one type");

                } else {
                    return;
                }
            }

        } else {
            setIsOwner(true);
        }

        synchronized (definitions) {
            definitions.addDefinition(def, replace);
        }
    }

    Definition getClassDefinition(NameNode type) {
        Definition def = null;
        for (NamedDefinition nd = getSuperDefinition(); nd != null; nd = nd.getSuperDefinition()) {
            def = nd.getExplicitChildDefinition(type);
            if (def != null) {
                break;
            }
        }
        return def;
    }


    protected Definition getExplicitDefinition(NameNode node, ArgumentList args, Context context) throws Redirection {
        Definition def = getExplicitChildDefinition(node);
        if (def != null) {
            if (def.isFormalParam()) {

                // we should allow DefParameters if this is being called in scope,
                // but I haven't figured the best way to do that
                def = null;
               
            } else if (def instanceof NamedDefinition && args != null) {
                NamedDefinition ndef = (NamedDefinition) def;
                int numUnpushes = 0;
                try {
                    NamedDefinition argsOwner = (NamedDefinition) args.getOwner();
                    if (argsOwner != null) {
                        int limit = context.size() - 1;
                        while (numUnpushes < limit) {
                            Definition nextdef = context.peek().def;
                            if (argsOwner.equals(nextdef) || argsOwner.isSubDefinition(nextdef)) {
                                break;
                            }
                            numUnpushes++;
                            context.unpush();
                        }
                        // if we didn't find the owner in the stack, put it back the way it came
                        if (numUnpushes == limit) {
                            while (numUnpushes-- > 0) {
                                context.repush();
                            }
                        }
                    }
                    def = ndef.getDefinitionForArgs(args, context);

                } finally {
                    while (numUnpushes-- > 0) {
                        context.repush();
                    }
                }
            }
        }
        return def;
    }

    public Definition getExplicitChildDefinition(NameNode node) {
        if (definitions == null) {
            vlog(getFullName() + " has no definition table, cannot look up " + node.getName());
            return null;
        }
        return definitions.getDefinition(this, node);
    }
}

