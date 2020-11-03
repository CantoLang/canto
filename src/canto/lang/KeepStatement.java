/* Canto Compiler and Runtime Engine
 * 
 * KeepStatement.java
 *
 * Copyright (c) 2018-2020 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * KeepStatement represents a statement with a cache prefix, which provides caching 
 * strategy hints to the Canto processor.
 *
 * @author Michael St. Hippolyte
 */
public class KeepStatement extends CantoStatement {

    transient private Instantiation[] instances;
    private NameNode defName;
    private NameNode asName = null;
    private Chunk asNameGenerator = null;
    private NameNode byName = null;
    private boolean asIncluded = false;
    private Instantiation tableInstance;
    private boolean inContainer = false;

    public KeepStatement() {
        super();
    }

    public boolean isDynamic() {
        return false;
    }

    protected void setInContainer(boolean inContainer) {
        this.inContainer = inContainer;
    }

    public boolean isInContainer() {
        return inContainer;
    }

    public boolean isInContainer(Context context) {
        if (inContainer) {
            return true;
        }
        return false;
    }

    public void setDefName(NameNode name) {
        defName = name;
        checkIfAsIncluded();
    }

    protected void setAsName(NameNode name) {
        asName = name;
        checkIfAsIncluded();
    }

    private void checkIfAsIncluded() {
        asIncluded = false;
        if (asName != null && defName != null) {
            if (asName.equals(defName)) {
                asIncluded = true;
            }
        }
    }

    protected void setAsNameGenerator(CantoNode node) {
        asNameGenerator = (Chunk) node;
    }
    
    protected void setByName(NameNode name) {
        byName = name;
    }
    
    public NameNode getDefName() {
        return defName;
    }
    
    public NameNode getAsName(Context context) {
        if (asNameGenerator != null) {
            try {
                return new NameNode(asNameGenerator.getText(context));
            } catch (Redirection r) {
                return null;
            }
        } else {
            return asName;
        }
    }
    
    public boolean getAsIncluded(Context context) {
        if (asNameGenerator != null) {
            try {
                Name as = new NameNode(asNameGenerator.getText(context));
                if (as != null && defName != null) {
                    if (as.equals(defName)) {
                        return true;
                    }
                }
            } catch (Redirection r) {
                ;
            }
            return false;
        } else {
            return asIncluded;
        }
    }
    
    public NameNode getByName() {
        return byName;
    }

    public boolean contains(String name) {
        if (defName != null && defName.equals(name)) {
            return true;
        }
        return false;
    }

    synchronized public ResolvedInstance[] getResolvedInstances(Context context) throws Redirection {
        int len = (defName != null ? 1 : 0);
        NameNode as = getAsName(context);
        boolean included = getAsIncluded(context);
        boolean addAs = (as != null && !included);
        
        if (addAs) {
            len++;
        }

        ResolvedInstance[] resolvedInstances = new ResolvedInstance[len];
        if (addAs && as.getName() == Name.THIS) {
            len--;
            resolvedInstances[len] = new ResolvedInstance(context.getDefiningDef(), context, null, null);
        }

        if (instances == null) {
            Definition owner = getOwner();
            instances = new Instantiation[len];
            if (defName != null) {
                instances[0] = new Instantiation(defName);
                instances[0].setOwner(owner);
                instances[0].resolve(null);
            }
            if (addAs && as.getName() != Name.THIS) {
                int i = len - 1;
                instances[i] = new Instantiation(as);
                instances[i].setOwner(owner);
            }
        }
        if (defName != null) {
            resolvedInstances[0] = new ResolvedInstance(instances[0], context, false, true);
        }
        if (addAs && as.getName() != Name.THIS) {
            int i = len - 1;
            resolvedInstances[i] = new ResolvedInstance(instances[i], context, false);
        }
        return resolvedInstances;
    }

    public Definition[] getDefs(Context context) throws Redirection {
        int len = (defName != null ? 1 : 0);
        NameNode as = getAsName(context);
        boolean included = getAsIncluded(context);
        boolean addAs = (as != null && !included);
        
        if (addAs) {
            len++;
        }

        Definition[] defs = new Definition[len];
        if (addAs && as.getName() == Name.THIS) {
            len--;
            defs[len] = context.getDefiningDef();
        }

        if (instances == null) {
            Definition owner = getOwner();
            instances = new Instantiation[len];
            if (defName != null) {
                instances[0] = new Instantiation(defName);
                instances[0].setOwner(owner);
                instances[0].resolve(null);
            }
            if (addAs && as.getName() != Name.THIS) {
                int i = len - 1;
                instances[i] = new Instantiation(as);
                instances[i].setOwner(owner);
            }
        }
        if (defName != null) {
            defs[0] = instances[0].getDefinition(context, null, true);
            if (defs[0] == null) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Undefined name in keep statement: " + instances[0].getName());
            }
        }
        if (addAs && as.getName() != Name.THIS) {
            int i = len - 1;
            defs[i] = instances[i].getDefinition(context);
            if (defs[i] == null) {
                if (defs[0].hasChildDefinition(as.getName(), true)) {
                    defs[i] = defs[0].getChildDefinition(as, context);
                //} else {    
                //    throw new Redirection(Redirection.STANDARD_ERROR, "Undefined name in keep statement: " + as.getName());
                }
            }
//        } else if (addBy) {
//            if (defs[0].hasChildDefinition(by.getName())) {
//                defs[names.length] = defs[0].getChildDefinition(by, context);
//            } else {
//                instances[names.length] = new Instantiation(by);
//                instances[names.length].setOwner(owner);
//                defs[names.length] = instances[names.length].getDefinition(context);
//            }
        }
        return defs;
    }

    protected void setTableInstance(Instantiation tableInstance) {
        this.tableInstance = tableInstance;
    }

    public Instantiation getTableInstance() {
        return tableInstance;
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix);
        sb.append("keep ");
        if (defName != null) {
            sb.append(defName.getName());
        }
        if (asNameGenerator != null) {
            sb.append(" as ");
            sb.append(asNameGenerator.toString());
        } else if (asName != null) {
            sb.append(" as ");
            sb.append(asName.getName());
        } else if (byName != null) {
            sb.append(" by ");
            sb.append(byName.getName());
        }
        if (tableInstance != null) {
            sb.append(" in ");
            sb.append(tableInstance.getDefinitionName());
        }
        sb.append("\n");
        return sb.toString();
    }
}
