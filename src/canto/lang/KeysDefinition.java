/* Canto Compiler and Runtime Engine
 * 
 * KeysDefinition.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * KeysDefinition represents the built-in <code>keys</code> field which belongs
 * to every table and is a list of the keys in the table.
 * 
 * @author Michael St. Hippolyte
 * @version $Revision: 1.17 $
 */

public class KeysDefinition extends CollectionDefinition {

    private static Type keysType = PrimitiveType.STRING;
    public Definition def;
    private Dim dim;
    private List<String> keys = null;

    public KeysDefinition(Definition def, Context context) {
    	this(def, context, null, null);
    }

    public KeysDefinition(Definition def, Context context, ArgumentList args, List<Index> indexes) {
        super();
        if (def instanceof ExternalDefinition) {
        	try {
        		def = ((ExternalDefinition) def).getDefForContext(context, args);
        	} catch (Throwable t)  {
        		def = null;
        	}
        }
        if (def instanceof DynamicObject) {
        	try {
                def = (Definition) ((DynamicObject) def).initForContext(context, args, indexes);
        	} catch (Throwable t)  {
        		def = null;
        	}
        }
        if (def instanceof KeysDefinition) {
            def = ((KeysDefinition) def).def;
        }
        this.def = def;

        if (def instanceof CollectionDefinition) {
        	List<Dim> dims = ((CollectionDefinition) def).getDims();
            Dim majorDim = dims.get(dims.size() - 1);
        	dim = new Dim(majorDim.getType(), majorDim.getSize());
        
        } else {
        	dim = new Dim();
        }
        
        List<Dim> dims = new ArrayList<Dim>(1);
        dims.add(dim);
        setDims(dims);

        CollectionInstance instance = null;
        CollectionDefinition tableDef = null;
        ResolvedInstance ri = null;
        try {
            while (def instanceof ElementDefinition) {
                Object obj = ((ElementDefinition) def).getElement(context);
                if (obj instanceof ResolvedInstance) {
                    ri = (ResolvedInstance) obj;
                    def = ri.getDefinition();
                } else {
                    break;
                }
            }
            
            if (def instanceof CollectionDefinition) {
                tableDef = (CollectionDefinition) def;
            } else {
                tableDef = new CollectionDefinition();
                tableDef.setName(def.getNameNode());
                tableDef.setOwner(def.getOwner());
                if (ri == null) {
                    ri = new ResolvedInstance(def, context, args, indexes);
                }
                tableDef.setContents(ri);
                if (def instanceof ElementDefinition) {
                    Object obj = ((ElementDefinition) def).getElement(context);
                    Dim tableDim = Dim.createForObject(obj);
                    List<Dim> tableDims = new ArrayList<Dim>(1);
                    tableDims.add(tableDim);
                    tableDef.setDims(tableDims);
                }
            }

            instance = tableDef.getCollectionInstance(context, args, indexes);
            if (instance != null) {
                Map<String,?> map = (Map<String,?>) instance.getCollectionObject();
                if (map != null && map.size() > 0) {
                    keys = new ArrayList<String>(new TreeSet<String>(map.keySet()));
                }
        	}
        } catch (Throwable t)  {
      		t.printStackTrace();
        }
        
        if (keys == null) {
            keys = new EmptyList<String>();
        }

        super.init(keysType, new NameNode(Name.KEYS), new ArgumentList(new StringValueList(keys)));
    }

 
    /** Returns <code>false</code>.
     */
    public boolean isAbstract(Context context) {
        return false;
    }

    /** Returns <code>PUBLIC_ACCESS</code>. */
    public int getAccess() {
        return PUBLIC_ACCESS;
    }

    /** Returns the durability of the encapsulated definition. */
    public int getDurability() {
        return def.getDurability();
    }

    /** Returns the type corresponding to this definition. */
    public Type getType() {
        ComplexType type = new ComplexType(this, "keys");
        type.setOwner(this);
        return type;
    }

    /** Returns primitive string type. */
    public Type getSuper() {
        return keysType;
    }


    /** Returns true only if the name is "string"
     */
    public boolean isSuperType(String name) {
        return "string".equals(name);
    }

    /** Returns the encapsulated definition's full name, with ".keys" appended.  */
    public String getFullName() {
        return def.getFullName() + ".keys";
    }

    /** Returns the encapsulated definition's full name in context, with ".keys" appended.  */
    public String getFullNameInContext(Context context) {
        return def.getFullNameInContext(context) + ".keys";
    }

    public String getName() {
        return "keys";
    }

    /** Returns the encapsulated definition. */
    public Definition getOwner() {
        return def;
    }

    static class StringValueList extends AbstractList<Construction> {
        private List<String> strings;
        private Construction[] constructions;
        
        public StringValueList(List<String> strings) {
            this.strings = strings;
            constructions = new Construction[strings.size()];
        }

        public Construction get(int n) {
            if (constructions[n] == null) {
                constructions[n] = new PrimitiveValue(strings.get(n));
            }
            return constructions[n];
        }

        public int size() {
            return strings.size();
        }
    }
        
}

