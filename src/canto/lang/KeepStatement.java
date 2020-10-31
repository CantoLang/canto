/* Canto Compiler and Runtime Engine
 * 
 * KeepStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;
import canto.runtime.Context.Entry;

/**
 * KeepStatement represents a statement with a cache prefix, which provides caching 
 * strategy hints to the Canto processor.
 *
 * @author Michael St. Hippolyte
 */
public class KeepStatement extends CantoStatement {

    transient private Instantiation[] instances;
    private NameNode[] names;
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
//        Entry containerEntry = null;
//        // back up to the new frame entry
//        for (Entry entry = context.peek(); entry.getPrevious() != null; entry = entry.getPrevious()) {
//            if (entry.superdef == null) {
//                containerEntry = entry.getPrevious();
//                break;
//            }
//        }
//        if (containerEntry != null && containerEntry.keepMap != null) {
//            String key = context.peek().def.getName();
//            return containerEntry.keepMap.containsKey(key);
//        }        
        return false;
    }

    public void setDefName(NameNode name) {
        names = new NameNode[1];
        names[0] = name;
        checkIfAsIncluded();
    }

    protected void setNames(NameNode[] names) {
        this.names = (names == null ? new NameNode[0] : names);
        checkIfAsIncluded();
    }

    protected void setAsName(NameNode name) {
        asName = name;
        checkIfAsIncluded();
    }

    private void checkIfAsIncluded() {
        asIncluded = false;
        if (asName != null && names != null) {
            for (int i = 0; i < names.length; i++) {
                if (asName.equals(names[i])) {
                    asIncluded = true;
                }
            }
        }
    }

    protected void setAsNameGenerator(CantoNode node) {
        asNameGenerator = (Chunk) node;
    }
    
    protected void setByName(NameNode name) {
        byName = name;
    }
    
    public Name[] getNames() {
        return names;
    }
    
    public NameNode getDefName() {
        return names[0];
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
                if (as != null && names != null) {
                    for (int i = 0; i < names.length; i++) {
                        if (as.equals(names[i])) {
                            return true;
                        }
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
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                if (names[i].getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    synchronized public ResolvedInstance[] getResolvedInstances(Context context) throws Redirection {
        int len = names.length;
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
            for (int i = 0; i < names.length; i++) {
                instances[i] = new Instantiation(names[i]);
                instances[i].setOwner(owner);
                instances[i].resolve(null);
            }
        }
        for (int i = 0; i < names.length; i++) {
            resolvedInstances[i] = new ResolvedInstance(instances[i], context, false, true);
        }
        if (addAs && as.getName() != Name.THIS) {
            instances[names.length] = new Instantiation(as);
            instances[names.length].setOwner(owner);
            resolvedInstances[names.length] = new ResolvedInstance(instances[names.length], context, false);
        }
        return resolvedInstances;
    }

    public Definition[] getDefs(Context context) throws Redirection {
        int len = names.length;
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
            for (int i = 0; i < names.length; i++) {
                instances[i] = new Instantiation(names[i]);
                instances[i].setOwner(owner);
                instances[i].resolve(null);
            }
        }
        for (int i = 0; i < names.length; i++) {
            defs[i] = instances[i].getDefinition(context, null, true);
            if (defs[i] == null) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Undefined name in keep statement: " + instances[i].getName());
            }
        }
        if (addAs && as.getName() != Name.THIS) {
            instances[names.length] = new Instantiation(as);
            instances[names.length].setOwner(owner);
            defs[names.length] = instances[names.length].getDefinition(context);
            if (defs[names.length] == null) {
                if (defs[0].hasChildDefinition(as.getName(), true)) {
                    defs[names.length] = defs[0].getChildDefinition(as, context);
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
        for (int i = 0; i < names.length; i++) {
            sb.append(names[i].getName());
            if (i < names.length - 1) {
                sb.append(", ");
            }
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
