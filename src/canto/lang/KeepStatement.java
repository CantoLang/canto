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

    private Instantiation defInstance;
    private Instantiation asInstance;
    private NameNode defName;
    private NameNode asName = null;
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

    protected void setByName(NameNode name) {
        byName = name;
    }
    
    public NameNode getDefName() {
        return defName;
    }
    
    public NameNode getAsName() {
        return asName;
    }
    
    public boolean getAsIncluded() {
        return asIncluded;
    }
    
    public NameNode getByName() {
        return byName;
    }

    public boolean contains(String name) {
        if (defName != null && defName.getName().equals(name)) {
            return true;
        }
        return false;
    }

    synchronized public ResolvedInstance getResolvedDefInstance(Context context) {
        if (defInstance != null) {
            return new ResolvedInstance(defInstance, context, false, true);
        } else {
            return null;
        }
    }

    synchronized public ResolvedInstance getResolvedAsInstance(Context context) throws Redirection {
        if (asInstance != null) {
            return new ResolvedInstance(asInstance, context, false, true);

        } else if (asName != null && asName.getName() == Name.THIS) { 
            return new ResolvedInstance(context.getDefiningDef(), context, null, null);
        } else {
            return null;
        }
    }
        
    public Definition[] getDefs(Context context) throws Redirection {
        int len = (defName != null ? 1 : 0);
        NameNode as = getAsName();
        boolean included = getAsIncluded();
        boolean addAs = (as != null && !included);
        
        if (addAs) {
            len++;
        }

        Definition[] defs = new Definition[len];
        if (addAs && as.getName() == Name.THIS) {
            len--;
            defs[len] = context.getDefiningDef();
        }

        if (defName != null) {
            defs[0] = defInstance.getDefinition(context, null, true);
            if (defs[0] == null) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Undefined name in keep statement: " + defInstance.getName());
            }
        }
        if (addAs && as.getName() != Name.THIS) {
            int i = len - 1;
            defs[i] = asInstance.getDefinition(context);
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

    public void createInstances() {
        Definition owner = getOwner();
        if (defName != null) {
            defInstance = new Instantiation(defName);
            defInstance.setOwner(owner);
            defInstance.resolve(null);
        }
        if (asName != null && asName.getName() != Name.THIS) {
            asInstance = new Instantiation(asName);
            asInstance.setOwner(owner);
            asInstance.resolve(null);
        }
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix);
        sb.append("keep ");
        if (defName != null) {
            sb.append(defName.getName());
        }
        if (asName != null) {
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
