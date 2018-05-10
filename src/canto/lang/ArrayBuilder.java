/* Canto Compiler and Runtime Engine
 * 
 * ArrayBuilder.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;
import canto.runtime.CantoObjectWrapper;
import canto.runtime.Logger;


/**
 * ArrayBuilder constructs arrays and instantiates their contents.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.7 $
 */

public class ArrayBuilder extends CollectionBuilder {

    static public Object instantiateElements(Object arrayObject, Context context) throws Redirection {
        Object arrayInstance = arrayObject;
        if (arrayObject instanceof Object[]) {
            int size = ((Object[]) arrayObject).length;
            for (int i = 0; i < size; i++) {
                Object data = ((Object[]) arrayObject)[i];

                if (data instanceof CollectionInstance) {
                     data = ((CollectionInstance) data).getCollectionObject();
                }

                if (data instanceof ElementDefinition) {
                    data = ((ElementDefinition) data).getElement(context);
                }

                if (data instanceof Value) {
                    data = ((Value) data).getValue();

                } else if (data instanceof ValueGenerator) {
                    data = ((ValueGenerator) data).getData(context);
                }

                if (data != ((Object[]) arrayInstance)[i]) {
                    if (arrayInstance == arrayObject) {
                        arrayInstance = new Object[size];
                        System.arraycopy(arrayObject, 0, arrayInstance, 0, size);
                    }
                    ((Object[]) arrayInstance)[i] = data;
                }
            }

        } else if (arrayObject instanceof List<?>) {
            List<Object> list = new ArrayList<Object>(((List<?>) arrayObject).size());
            Iterator<?> it = ((List<?>) arrayObject).iterator();
            while (it.hasNext()) {
                Object data = it.next();

                if (data instanceof CollectionInstance) {
                     data = ((CollectionInstance) data).getCollectionObject();
                }

                if (data instanceof ElementDefinition) {
                    data = ((ElementDefinition) data).getElement(context);
                }

                if (data instanceof Value) {
                    data = ((Value) data).getValue();

                } else if (data instanceof ValueGenerator) {
                    data = ((ValueGenerator) data).getData(context);
                }

                list.add(data);
            }
            arrayInstance = list;
        }
        return arrayInstance;
    }

    protected CollectionDefinition arrayDef = null;

    public ArrayBuilder(CollectionDefinition arrayDef) {
        this.arrayDef = arrayDef;
    }

    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        // this could be cached
        return new ResolvedArray(arrayDef, context, args, indexes);
    }

    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes, Object collectionData) throws Redirection {
        // this could be cached
        return new ResolvedArray(arrayDef, context, args, indexes, collectionData);
    }

    public CantoArray getArray(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
    	ResolvedArray array = (ResolvedArray) arrayDef.getCollectionInstance(context, args, indexes);
        return array.getArray();
    }

    /** Generates a list of constructions for a context. */
    public List<Construction> generateConstructions(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        CollectionInstance collection = arrayDef.getCollectionInstance(context, args, indexes);
        Iterator<?> it = collection.iterator();
        int n = collection.getSize();
        List<Construction> constructions = Context.newArrayList(n, Construction.class);
        while (it.hasNext()) {
            Object object = it.next();
            Object element = (object instanceof ElementDefinition ? ((ElementDefinition) object).getElement(context) : object);
            if (element instanceof ConstructionGenerator) {
                constructions.addAll(((ConstructionGenerator) element).generateConstructions(context));
            } else {
                constructions.add((Construction) element);
            }
        }
        return constructions;
    }


}


class FixedArray implements CantoArray {
    private Object[] array = null;
//    private Object[] arrayObj = null;
    private int size;

    public FixedArray(int size) {
        this.size = size;
    }

    public FixedArray(Object[] array) {
        this.array = array;
        size = array.length;
    }

    public Object getArrayObject() {
        return array;        
        
//        if (arrayObj == null) {
//            arrayObj = new Object[size];
//            for (int i = 0; i < size; i++) {
//                if (array[i] instanceof CollectionInstance) {
//                    arrayObj[i] = ((CollectionInstance) array[i]).getCollectionObject();
//                } else {
//                    arrayObj[i] = array[i];
//                }
//            }
//        }
//        return arrayObj;
    }

    public Object instantiateArray(Context context) throws Redirection {
        return ArrayBuilder.instantiateElements(array, context);
    }

    public Object get(int n) {
        if (array == null) {
            return null;
        } else {
            return array[n];
        }
    }

    public int getSize() {
        return size;
    }

    public boolean isGrowable() {
        return false;
    }

    public boolean add(Object element) {
        return false;
    }

    public boolean addAll(List<Object> list) {
        return false;
    }

    public Object set(int n, Object element) {
        if (array == null) {
            array = new Object[size];
        }
        Object oldElement = array[n];
        array[n] = element;
        return oldElement;
    }

    public Iterator<Object> iterator() {
        return Arrays.asList(array).iterator();
    }
}

class GrowableArray implements CantoArray {
    private List<Object> array;
//    private List<Object> arrayObj = null;
    private int initialSize;
    private final static Object[] EMPTY_ARRAY = new Object[0];

    public GrowableArray(int size) {
        initialSize = size;
    }

    @SuppressWarnings("unchecked")
    public GrowableArray(List<?> array) {
        this.array = (List<Object>) array;
        initialSize = array.size();
    }

    private void init() {
        array = Context.newArrayList(initialSize, Object.class);
    }

    public Object getArrayObject() {
        if (array == null) {
            return EMPTY_ARRAY;
        } else {
            return array;
        }
//        if (arrayObj == null && array != null) {
//            arrayObj = new ArrayList(array);
//            int len = arrayObj.size();
//            for (int i = 0; i < len; i++) {
//                Object item = arrayObj.get(i);
//                if (item instanceof CollectionInstance) {
//                    arrayObj.set(i, ((CollectionInstance) item).getCollectionObject());
//                }
//            }
//        }
//        Object[] arrayObj = (array == null ? EMPTY_ARRAY : array.toArray());
//        for (int i = 0; i < arrayObj.length; i++) {
//            if (arrayObj[i] instanceof CollectionInstance) {
//                arrayObj[i] = ((CollectionInstance) arrayObj[i]).getCollectionObject();
//            }
//        }
//        return arrayObj;
    }

    public Object instantiateArray(Context context) throws Redirection {
        return ArrayBuilder.instantiateElements(array, context);
    }

    public Object get(int n) {
        if (array == null) {
            return null;
        } else {
            return array.get(n);
        }
    }

    public int getSize() {
        if (array == null) {
            return initialSize;
        } else {
            return array.size();
        }
    }

    public boolean isGrowable() {
        return true;
    }

    public boolean add(Object element) {
        if (array == null) {
            init();
        }
        array.add(element);
        return true;
    }

    public boolean addAll(List<Object> list) {
        if (array == null) {
            init();
        }
        array.addAll(list);
        return true;
    }

    public Object set(int n, Object element) {
        if (array == null) {
            init();
        }
        Object oldElement = array.get(n);
        array.set(n, element);
        return oldElement;
    }

    public Iterator<Object> iterator() {
    	if (array == null) {
    		init();
    	}
        return array.iterator();
    }
}

class ArrayInstance implements CantoArray, DynamicObject {
    private ValueGenerator valueGen;
    private CantoArray array = null;
    private Object data = null;
    private Context initContext = null;

    public ArrayInstance(Object data) {
        this.data = data;
    }

    public ArrayInstance(ValueGenerator valueGen, Context context) throws Redirection {
        this.array = null;
        this.valueGen = valueGen;
        List<Index> indexes = null;
        initContext = (Context) context.clone();
        if (valueGen instanceof Instantiation) {
            Instantiation instance = (Instantiation) valueGen;
            Instantiation ultimateInstance = instance.getUltimateInstance(context);
            indexes = (instance == ultimateInstance ? null : instance.getIndexes());
            Definition def = ultimateInstance.getDefinition(context);
            if (def == null) {
                data = null;
            } else {
                // commented out to make cached_array_test work
                //ArgumentList args = ultimateInstance.getArguments();
                //ParameterList params = def.getParamsForArgs(args, context);
                //context.push(def, params, args, false);
                try {
                    data = ultimateInstance.generateData(context, def);
                } finally {
                //    context.pop();
                }
            }
        } else if (valueGen instanceof IndexedMethodConstruction) {
            data = ((IndexedMethodConstruction) valueGen).getCollectionObject(context);
            
        } else {
            data = valueGen.getData(context);
        }
        if (data != null) {
            if (data instanceof Value) {
                data = ((Value) data).getValue();
            }
            if (data instanceof DynamicObject) {
                data = ((DynamicObject) data).initForContext(context, null, null);
            }
            if (indexes != null) {
                data = context.dereference(data, indexes);
            }
        }
    }


    public Object initForContext(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        if (initContext == null && data == null) {
            return new ArrayInstance(valueGen, context);
        } else {
            return this;
        }
    }

    public boolean isInitialized(Context context) {
        return (initContext != null && initContext.equals(context));
    }

    private void init_array() {
        if (data == null) {
            Logger.vlog("data for array instance is null; initializing to empty array");
            array = new FixedArray(new Object[0]);
        } else {
            Object obj = data;
            if (data instanceof CantoObjectWrapper) {
                try {
                    obj = ((CantoObjectWrapper) data).getData();
                } catch (Redirection r) {
                    throw new UninitializedObjectException("Unable to initialize array: " + r.getMessage());
                }
            } else if (data instanceof ResolvedInstance) {
                try {
                    obj = ((ResolvedInstance) data).generateData();
                } catch (Redirection r) {
                    throw new UninitializedObjectException("Unable to initialize array: " + r.getMessage());
                }
            }

            // see if one of the above data fetches returned null
            if (obj == null) {
                throw new UninitializedObjectException("Unable to initialize array, array object has no data");
            }
        
            if (obj instanceof CantoArray) {
                array = (CantoArray) obj;
            } else if (obj instanceof Object[]) {
                array = new FixedArray((Object[]) obj);
            } else if (obj instanceof List<?>) {
                array = new GrowableArray((List<?>) obj);
            } else if (obj instanceof ResolvedArray) {
                array = ((ResolvedArray) obj).getArray();
            } else {
                throw new UninitializedObjectException("Unable to initialize array, data type not supported: " + obj.getClass().getName());
            }
        }
    }

    public Object getArrayObject() {
        if (array == null) {
            init_array();
        }
        return array.getArrayObject();
    }

    public Object instantiateArray(Context context) throws Redirection {
        return array.instantiateArray(context);
    }

    public Object get(int n) {
        if (array == null) {
            init_array();
        }
        return array.get(n);
    }

    public int getSize() {
        if (array == null) {
            init_array();
        }
        return array.getSize();
    }

    public boolean isGrowable() {
        if (array == null) {
            init_array();
        }
        return array.isGrowable();
    }

    public boolean add(Object element) {
        if (array == null) {
            init_array();
        }
        array.add(element);

        return true;
    }
    
    public boolean addAll(List<Object> list) {
        if (array == null) {
            init_array();
        }
        array.addAll(list);

        return true;
    }

    public Object set(int n, Object element) {
        if (array == null) {
            init_array();
        }
        Object oldElement = array.get(n);
        array.set(n, element);
        return oldElement;
    }

    public Iterator<Object> iterator() {
        if (array == null) {
            init_array();
        }
        return array.iterator();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        Iterator<Object> it = iterator();
        while (it.hasNext()) {
            Object item = it.next();
            if (item != null) {
                sb.append(item.toString());
            }
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }
}

class DynamicArray implements CantoArray, DynamicObject {
    private CantoArray statements;
    private Context initContext;
    private transient List<Object> generatedArray = null;


    public DynamicArray(CantoArray statements) {
        this.statements = statements;
    }

    public DynamicArray(CantoArray statements, Context context) throws Redirection {
        this.statements = statements;
        initContext = (Context) context.clone();
        generateForContext(context);
    }

    public Object initForContext(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        if (initContext == null) {
            return new DynamicArray(statements, context);
        } else {
            return this;
        }
    }

    public boolean isInitialized(Context context) {
        return (initContext != null && initContext.equals(context));
    }

    private void generateForContext(Context context) throws Redirection {
        boolean fixedSize = !statements.isGrowable();
        int n = fixedSize ? statements.getSize() : statements.getSize() * 2;
        generatedArray = Context.newArrayList(n, Object.class);

        Iterator<Object> it = statements.iterator();
        while (it.hasNext()) {
            addAllConstructions(context, it.next(), generatedArray);
            if (fixedSize && generatedArray.size() >= n) {
                break;
            }
        }
    }
    
    private static void addAllConstructions(Context context, Object object, List<Object> array) throws Redirection {
        Object element = (object instanceof ElementDefinition ? ((ElementDefinition) object).getElement(context) : object);
        List<Construction> constructions = null;
        if (element instanceof ConstructionGenerator) {
            constructions = ((ConstructionGenerator) element).generateConstructions(context);
            array.addAll(constructions);
        } else if (element instanceof ConstructionContainer) {
            constructions = ((ConstructionContainer) element).getConstructions(context);
            if (constructions != null) {
                Iterator<Construction> it = constructions.iterator();
                while (it.hasNext()) {
                    addAllConstructions(context, it.next(), array);
                }
            }
        } else {
            array.add(element);
        }
    }

    public Object getArrayObject() {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        Object[] arrayObj = generatedArray.toArray();
        for (int i = 0; i < arrayObj.length; i++) {
            if (arrayObj[i] instanceof CollectionInstance) {
                try {
                    arrayObj[i] = ((CollectionInstance) arrayObj[i]).getCollectionObject();
                } catch (Redirection r) {
                    System.err.println("Error getting collection instance: " + r);
                }
            }
        }
        return arrayObj;
    }

    public Object instantiateArray(Context context) throws Redirection {
        return ArrayBuilder.instantiateElements(getArrayObject(), context);
    }

    public Object get(int n) {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        return generatedArray.get(n);
    }

    public int getSize() {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        return generatedArray.size();
    }

    public boolean isGrowable() {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        return statements.isGrowable();
    }

    public boolean add(Object element) {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        statements.add(element);
        return true;
    }

    public boolean addAll(List<Object> list) {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        statements.addAll(list);
        return true;
    }

    public Object set(int n, Object element) {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        Object oldElement = generatedArray.get(n);
        statements.set(n, element);
        return oldElement;
    }

    public Iterator<Object> iterator() {
        if (generatedArray == null) {
            throw new RuntimeException("Dynamic array not initialized in a context");
        }
        return generatedArray.iterator();
    }
}

