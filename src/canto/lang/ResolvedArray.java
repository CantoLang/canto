/* Canto Compiler and Runtime Engine
 * 
 * ResolvedArray.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Iterator;
import java.util.List;

import canto.runtime.Context;

public class ResolvedArray extends ResolvedCollection {

    private CollectionDefinition collectionDef = null;
    protected CantoArray array = null;

    public ResolvedArray(Definition def, Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        super(def, context, args, indexes);
        context = getResolutionContext();
        this.collectionDef = def.getCollectionDefinition(context, args);
        if (collectionDef.hasStaticData()) {
            array = (CantoArray) collectionDef.getStaticData();
        } else {
            array = createArray(collectionDef, context, args);
            // has no effect if def is not static
            collectionDef.setStaticData(array);
        }
    }

    public ResolvedArray(Definition def, Context context, ArgumentList args, List<Index> indexes, Object arrayData) throws Redirection {
        super(def, context, args, indexes);
        
        this.collectionDef = def.getCollectionDefinition(context, args);

        if (arrayData instanceof Instantiation) {
            arrayData = ((Instantiation) arrayData).getData(context);
            if (arrayData instanceof Value) {
                arrayData = ((Value) arrayData).getValue();
            }
        }
        
    	if (arrayData instanceof CantoArray) {
            array = (CantoArray) arrayData;
        } else if (arrayData instanceof CollectionDefinition) {
            // this doesn't support args in anonymous arrays 
            CollectionDefinition arrayDef = (CollectionDefinition) arrayData;
            if (arrayDef.equals(collectionDef)) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Array " + def.getName() + " is circularly defined.");
            }
            array = arrayDef.getArray(context, null, null);
        } else if (arrayData instanceof Object[]) {
            array = new FixedArray((Object[]) arrayData);
        } else if (arrayData instanceof List<?>) {
            array = new GrowableArray((List<?>) arrayData);
        } else if (arrayData instanceof ResolvedArray) {
            array = ((ResolvedArray) arrayData).getArray();
        } else if (arrayData != null) {
            throw new Redirection(Redirection.STANDARD_ERROR, "Unable to initialize array " + def.getName() + "; data in context of wrong type: " + arrayData.getClass().getName());
        }
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        return array;
    }

    /** Creates an array based on the definition.  If the contents of the definition are
     *  of an unexpected type, a ClassCastException is thrown.
     */
    private static CantoArray createArray(Definition def, Context context, ArgumentList args) throws Redirection {
        boolean pushed = false;
        Context.Entry entry = context.peek();
        
        if (!def.equals(entry.def) || (args != null && !args.equals(entry.args))) {
            ParameterList params = def.getParamsForArgs(args, context);
            context.push(def, params, args, false);
            pushed = true;
        }
        try {
            CollectionDefinition collectionDef = def.getCollectionDefinition(context, args);
            if (collectionDef == null) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Unable to create " + def.getName() + "; no collection definition provided.");
            }
            List<Dim> dims = collectionDef.getDims();
            Dim majorDim = (Dim) dims.get(0);
            Dim.TYPE dimType = majorDim.getType();
            boolean fixed = (dimType == Dim.TYPE.DEFINITE);
            Object contents = def.getContents();

            CantoArray array = null;

            // empty array
            if (contents == null) {
                array = allocate(fixed, 0);

            // array defined with an ArrayInitExpression
            } else if (contents instanceof ArgumentList) {
                ArgumentList elements = (ArgumentList) contents;
                int size = 0;
                if (dimType == Dim.TYPE.DEFINITE) {
                    size = majorDim.getSize();
                } else if (elements != null) {
                    size = elements.size();
                }

                // for now just handle one dimension
                array = allocate(fixed, size);

                if (elements != null) {
                    if (fixed) {
                        for (int i = 0; i < elements.size(); i++) {
                            Construction element = elements.get(i);
                            element = resolveElement(collectionDef, element, context);
                            array.set(i, element);
                        }

                    } else {
                        Iterator<Construction> it = elements.iterator();
                        while (it.hasNext()) {
                            Construction element = it.next();
                            if (element instanceof SuperStatement) {
                                Definition superDef = def.getSuperDefinition(context);
                                if (superDef != null) {
                                    CollectionDefinition superCollection = superDef.getCollectionDefinition(context, ((SuperStatement) element).getArguments());
                                    if (superCollection != null) {
                                        List<Object> superElements = superCollection.instantiate_array(context);
                                        array.addAll(superElements);
                                    }
                                }

                            } else {
                                element = resolveElement(collectionDef, element, context);
                                array.add(element);
                            }
                        }
                    }
                }
                if (collectionDef.isDynamic()) {
                    array = new DynamicArray(array, context);
                }

            } else if (contents instanceof Value) {
                array = new ArrayInstance(((Value) contents).getValue());

            } else if (contents instanceof ValueGenerator) {
                array = new ArrayInstance((ValueGenerator) contents, context);
            }

            return array;
        } finally {
            if (pushed) {
                context.pop();
            }
        }
    }

    private static Construction resolveElement(CollectionDefinition collectionDef, Construction element, Context context) throws Redirection {
        if (element instanceof Instantiation) {
            return resolveInstance((Instantiation) element, context, false);

        } else if (element instanceof Expression) {
            return ((Expression) element).resolveExpression(context);
            
        } else if (element instanceof Value) {
            Value value = (Value) element;
            Type ownerType = collectionDef.getElementType();
            if (ownerType != null) {
                Class<?> collectionClass = ownerType.getTypeClass(null);
                Class<?> elementClass = value.getValueClass();
                if (!collectionClass.isAssignableFrom(elementClass)) {
                    return new PrimitiveValue(element, collectionClass);
                }
            }
            return element;
            
        } else {
            return element;
        }
    }
   
    private static CantoArray allocate(boolean fixed, int size) {
        if (fixed) {
            return new FixedArray(size);
        } else {
            return new GrowableArray(size);
        }
    }

    /** Returns the ArrayDefinition defining this collection. */
    public CollectionDefinition getCollectionDefinition() {
        return collectionDef;
    }


    /** Creates an iterator for the array.
     */
    public Iterator<Definition> iterator() {
        return new ArrayIterator();
    }

    public Iterator<Construction> constructionIterator() {
        return new ArrayConstructionIterator();
    }

    public Iterator<Index> indexIterator() {
        return new ArrayIndexIterator();
    }

    public class ArrayIterator implements Iterator<Definition> {
        int ix = 0;
        Iterator<Object> it;

        public ArrayIterator() {
        	it = array.iterator();
        }

        public boolean hasNext() {
            return it.hasNext();  //ix < array.getSize();
        }

        public Definition next() {
            Object element = it.next();  // array.get(ix++);
            return getElementDefinition(element);
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in array iterators");
        }
    }

    public class ArrayConstructionIterator implements Iterator<Construction> {
        int ix = 0;
        Iterator<Object> it;

        public ArrayConstructionIterator() {
            it = array.iterator();
        }

        public boolean hasNext() {
            return it.hasNext();  //ix < array.getSize();
        }

        public Construction next() {
            Object element = it.next();  // array.get(ix++);
            return getConstructionForElement(element);
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in array iterators");
        }
    }


    public class ArrayIndexIterator implements Iterator<Index> {
        int ix = 0;

        public ArrayIndexIterator() {}

        public boolean hasNext() {
            return ix < array.getSize();
        }

        public Index next() {
            return new Index(new PrimitiveValue(ix++));
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported in index iterators");
        }
    }

    public Object getCollectionObject() throws Redirection {
        Object collectionObj = array.getArrayObject();
//        if (collectionObj instanceof List || collectionObj.getClass().isArray()) {
//            collectionObj = ArrayDefinition.instantiateElements(collectionObj, getResolutionContext());
//        }
        return collectionObj;
    }

    public CantoArray getArray() {
        return array;
    }

    protected void setArray(CantoArray array) {
        this.array = array;
    }

    public Object get(int n) {
        return array.get(n);
    }

    public Definition getElement(Index index, Context context) {
        if (context == null) {
            context = getResolutionContext();
        }

        // If the index is a regular array index, get its value as
        // an int and return a definition for the element at that
        // position in the array.  If the index is a table index,
        // do a search for an element containing the table index's
        // key value.  If found, return the index of the string in
        // the array as an integer value; if not found, return -1 as
        // an integer value.

        if (index.isNumericIndex(context)) {
            int ix = index.getIndexValue(context).getInt();
            Object element = (ix >= 0 && ix < array.getSize() ? array.get(ix) : null);
            return getElementDefinition(element);

        } else {
            // retrieve the element which matches the index key value.  There
            // are two ways an element can match the key:
            //
            // -- if the element is a definition which owns a child named "key"
            //    compare its instantiated string value to the index key
            //
            // -- if the element doesn't have such a "key" field, compare the
            //    string value of the element itself to the index key.

            String key = index.getIndexValue(context).getString();

            if (key == null) {
                return null;
            }
            NameNode keyName = new NameNode("key");
            int size = array.getSize();
            for (int i = 0; i < size; i++) {
                Object element = array.get(i);
                Object object = null;

                try {
                    if (element instanceof Definition) {
                        Definition keyDef = ((Definition) element).getChildDefinition(keyName, context);
                        if (keyDef != null) {
                            object = context.construct(keyDef, null);
                            if (object == null) {
                                // null key, can't match
                                continue;
                            }
                        }
                    }

                    if (object == null) {
                        object = collectionDef.getObjectForElement(element);
                        if (object == null) {
                            continue;
                        }
                    }

                    String elementKey;
                    if (object instanceof String) {
                        elementKey = (String) object;
                    } else if (object instanceof Value) {
                        elementKey = ((Value) object).getString();
                    } else if (object instanceof Chunk) {
                        elementKey = ((Chunk) object).getText(context);
                    } else if (object instanceof ValueGenerator) {
                        elementKey = ((ValueGenerator) object).getString(context);
                    } else {
                        elementKey = object.toString();
                    }

                    if (key.equals(elementKey)) {
                        return getElementDefinition(new Integer(i));
                    }

                } catch (Redirection r) {
                    // don't redirect, we're only checking
                    continue;
                }
            }
            return getElementDefinition(new Integer(-1));
        }
    }

    /** If the collection has been resolved, return a ResolvedInstance representing the element. 
     */
    public ResolvedInstance getResolvedElement(Index index, Context context) {
        if (context == null) {
            context = getResolutionContext();
        }
        
        int ix = index.getIndexValue(context).getInt();
        Object element = (ix >= 0 && ix < array.getSize() ? array.get(ix) : null);
        if (element instanceof ResolvedInstance) {
            return (ResolvedInstance) element;
        }
        
        return null;
    }
    
    public int getSize() {
        if (array != null) {
            return array.getSize();
        } else {
            return 0;
        }
    }

    public boolean isGrowable() {
        return array.isGrowable();
    }

    public void add(Object element) {
        array.add(getElementDefinition(element));
    }

    public void set(int n, Object element) {
        array.set(n, getElementDefinition(element));
    }

    public String getText(Context context) throws Redirection {
        StringBuffer sb = new StringBuffer();
        
        sb.append("[ ");

        int len = array.getSize();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Construction construction = getConstructionForElement(array.get(i));
                sb.append('"');
                sb.append(construction.getText(context));
                sb.append('"');
            }
        }
        
        sb.append(" ]");
        
        return sb.toString();
    }

}
