/* Canto Compiler and Runtime Engine
 * 
 * Block.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.*;

/**
 * A CantoNode container.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.34 $
 */
abstract public class Block extends AbstractNode implements Construction, ConstructionContainer {

    private List<Construction> constructions = null;
    private Block catchBlock = null;
    private String catchIdentifier = null;

    public Block() {
        super();
    }

    public Block(AbstractNode[] children) {
        super();
        this.children = children;
    }

    abstract public boolean isDynamic();

    abstract public boolean isStatic();

    /** Returns true if this chunk is abstract, i.e., if it cannot be 
     *  instantiated because to do so would require instantiating an abstract
     *  definition.
     */
    public boolean isAbstract(Context context) {
        List<Construction> constructions = getConstructions(context);
        if (constructions != null) {
            Iterator<Construction> it = constructions.iterator();
            while (it.hasNext()) {
                Construction node = it.next();
                if (node == null) {
                    continue;
                }
                if (node.isAbstract(context)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    public Object getData(Context context) throws Redirection {
        return getData(context, null);
    }
        
    public Object getData(Context context, Definition def) throws Redirection {
        Object data = null;

        // only put the text generation in a try statement if there is
        // a catch block; otherwise we would have to rethrow the exception,
        // which would require a "throws Throwable" clause, which would
        // unnecessarily burden callers of this method.
        if (catchBlock != null) {
            try {
                Block block = this;
                if (block instanceof DynamicObject) {
                    block = (Block) ((DynamicObject) block).initForContext(context, null, null);
                }
                
                data = constructData(context);
            } catch (Redirection r) {
                if (catchIdentifier != null && catchIdentifier.length() > 0) {
                    String location = r.getLocation();
                    if (catchIdentifier.equals(location)) {
                        data = catchBlock.getData(context);
                    } else {
                        throw r;
                    }
                } else {
                    data = catchBlock.getData(context);
                }
            } catch (Throwable t) {
                if (catchIdentifier == null || catchIdentifier.length() == 0) {
                    data = catchBlock.getData(context);
                } else {
                    String className = t.getClass().getName();
                    String message = t.getMessage();
                    if (message == null) {
                        message = className;
                    } else {
                        message = className + ": " + message;
                    }
                    throw new Redirection(Redirection.STANDARD_ERROR, message);
                }
            }
        } else {
            
            data = constructData(context);
        }
        return data;
    }

    private Object constructData(Context context) throws Redirection {
        Block block = this;
        if (block instanceof DynamicObject) {
            block = (Block) ((DynamicObject) block).initForContext(context, null, null);
        }       
    
        return context.construct(block.getConstructions());
    }

    public Type getType(Context context, boolean generate) {
        Construction construction = getUltimateConstruction(context);
        if (construction == null) {
            return PrimitiveType.VOID;
        } else if (construction != this) {
            return construction.getType(context, generate);
        } else {
            return DefaultType.TYPE;
        }
    }

    /** Returns the name of the definition being constructed */
    public String getDefinitionName() {
        return "";
    }

    public boolean getBoolean(Context context) {
        try {
            Object data = getData(context);
            return PrimitiveValue.getBooleanFor(data);
        } catch (Redirection r) {
            return false;
        }
    }
    
    public String getText(Context context) throws Redirection {
        Object data = getData(context);
        return PrimitiveValue.getStringFor(data);
//        if (data == null) {
//            return null;
//        } else if (data instanceof String) {
//            return (String) data;
//        } else if (data instanceof Value) {
//            return ((Value) data).getString();
//        } else {
//            return data.toString();
//        }
    }

    public List<Construction> getConstructions(Context context) {
        return getConstructions();
    }
    
    public List<Construction> getConstructions() {
        if (constructions == null) {
            int n = getNumChildren();
            if (n > 0) {
                Iterator<CantoNode> it = getChildren();
                while (it.hasNext()) {
                    CantoNode node = it.next();
    
                    if (node instanceof Block) {
                        if (!(node.equals(catchBlock))) {
                            List<Construction> list = ((Block) node).getConstructions();
                            if (list != null && list.size() > 0) {
                                if (constructions == null) {
                                    // if you want you can change the allocation back to the centralized method
                                    constructions = new ArrayList<Construction>(list); // Context.newArrayList(list);
                                } else {
                                    constructions.addAll(list);
                                }
                            }
                        }

                    } else if (node instanceof Construction) {
                        if (constructions == null) {
                            // if you want you can change the allocation back to the centralized method
                            constructions = new ArrayList<Construction>(Math.min(n, TYPICAL_LIST_SIZE)); //Context.newArrayList(Math.min(n, TYPICAL_LIST_SIZE));
                        }
                        constructions.add((Construction) node);
                    }
                }
            }
            if (constructions == null) {
                constructions = new EmptyList<Construction>();
            } else {
                ((ArrayList<Construction>) constructions).trimToSize();
            }
        }
        return constructions;
    }

    public List<Definition> getDefinitions() {
        List<Definition> definitions = null;
        int n = getNumChildren();
        if (n > 0) { 
            Iterator<CantoNode> it = getChildren();
            while (it.hasNext()) {
                CantoNode node = (CantoNode) it.next();
                if (node instanceof Block) {
                    if (!(node.equals(catchBlock))) {
                        List<Definition> list = ((Block) node).getDefinitions();
                        if (list != null && list.size() > 0) {
                            if (definitions == null) {
                                // if you want you can change the allocation to the centralized method
                                definitions = new ArrayList<Definition>(list); // Context.newArrayList(list);
                            } else {
                                definitions.addAll(list);
                            }
                        }
                    }
                } else if (node instanceof Definition) {
                    if (definitions == null) {
                        // if you want you can change the allocation to the centralized method
                        definitions = new ArrayList<Definition>(Math.min(n, TYPICAL_LIST_SIZE)); //Context.newArrayList(Math.min(n, TYPICAL_LIST_SIZE));
                    }
                    definitions.add((Definition) node);
                }
            }
        }
        if (definitions == null) {
            definitions = new EmptyList<Definition>();
        } else {
            ((ArrayList<Definition>) definitions).trimToSize();
        }
        return definitions;
    }

    /** Return the construction that this construction resolves to, if it
     *  is a wrapper or alias of some sort, or else return this construction.
     */
    public Construction getUltimateConstruction(Context context) {
        List<Construction> constructions = getConstructions();
        if (constructions.size() == 1) {
            return constructions.get(0);
        } else {
            Construction singleConstruction = null;
            Iterator<Construction> it = constructions.iterator();
            while (it.hasNext()) {
                Construction construction = it.next();
                if (construction instanceof Instantiation) {
                    if ("eval".equals(((Instantiation) construction).getReferenceName().getName())) {
              	        continue;
                    }
                }
                if (singleConstruction == null) {
                    singleConstruction = construction;
                } else {
                    singleConstruction = null;
                    break;
                }
            }
            if (singleConstruction != null) {
                return singleConstruction;
            }
        }
        return this;
    }
    
//    public Iterator getPrimitiveChunks() {
//        return new PrimitiveChunkIterator(getChildren());
//    }

    public Block getCatchBlock() {
        return catchBlock;
    }

    protected void setCatchBlock(Block block) {
        catchBlock = block;
    }

    public String getCatchIdentifier() {
        return catchIdentifier;
    }

    public void setCatchIdentifier(Name catchName) {
        catchIdentifier = catchName.getName();
    }

    public String getTokenString(String prefix) {
        return getChildrenTokenString(prefix) + "\n";
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer();
        int numChildren = getNumChildren();
        if (numChildren > 0) {
            String indent = prefix + "    ";
            for (int i = 0; i < numChildren; i++) {
                AbstractNode node = (AbstractNode) getChild(i);
                if (node instanceof Block) {
                    if (!(node.equals(catchBlock))) {
                        sb.append(((Block) node).toString(indent, indent));
                        sb.append('\n');
                    }
                } else {
                    sb.append(node.toString(indent));
                }
            }
        }
        return sb.toString();
    }

    public String toString(String firstPrefix, String prefix) {
        return toString(prefix);
    }

	public String getString(Context context) throws Redirection {
		return getText(context);
	}

	public byte getByte(Context context) throws Redirection {
        try {
            Object data = getData(context);
            return (byte) PrimitiveValue.getIntFor(data);
        } catch (Redirection r) {
            return 0;
        }
	}

	public char getChar(Context context) throws Redirection {
        try {
            Object data = getData(context);
            return PrimitiveValue.getCharFor(data);
        } catch (Redirection r) {
            return 0;
        }
	}

	public int getInt(Context context) throws Redirection {
        try {
            Object data = getData(context);
            return PrimitiveValue.getIntFor(data);
        } catch (Redirection r) {
            return 0;
        }
	}

	public long getLong(Context context) throws Redirection {
        try {
            Object data = getData(context);
            return PrimitiveValue.getLongFor(data);
        } catch (Redirection r) {
            return 0;
        }
	}

	public double getDouble(Context context) throws Redirection {
        try {
            Object data = getData(context);
            return PrimitiveValue.getDoubleFor(data);
        } catch (Redirection r) {
            return 0;
        }
	}

	public Value getValue(Context context) throws Redirection {
        try {
            Object data = getData(context);
            return new PrimitiveValue(data);
        } catch (Redirection r) {
            return NullValue.NULL_VALUE;
        }
	}

//    class PrimitiveChunkIterator extends Stack implements Iterator {
//        private Iterator it;
//
//        public PrimitiveChunkIterator(Iterator it) {
//            this.it = it;
//        }
//
//        public boolean hasNext() {
//            return (it == null ? false : it.hasNext());
//        }
//
//        public Object next() {
//            CantoNode node = (CantoNode) it.next();
//            while (node instanceof Block) {
//                push(it);
//                it = node.getChildren();
//                while (!it.hasNext()) {
//                    it = (Iterator) pop();
//                    if (it == null) {
//                        return null;
//                    }
//                }
//                node = (CantoNode) it.next();
//            }
//
//            while ( !it.hasNext() && size() > 0) {
//                it = (Iterator) pop();
//            }
//
//            return node;
//        }
//
//        public void remove() {
//            throw new UnsupportedOperationException("PrimitiveChunkIterator is a read-only iterator");
//        }
//
//    }

}
