/* Canto Compiler and Runtime Engine
 * 
 * SubcollectionDefinition.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.lang.reflect.*;
import java.util.*;

import canto.runtime.Context;

/**
 * A SubcollectionDefinition contains a reference to a base collection plus a list
 * of overrides or additions.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.25 $
 */

public class SubcollectionDefinition extends CollectionDefinition {

    protected CollectionDefinition supercollection;
    protected Map<Object, Object> overrides;
    protected List<Object> additions;

    public SubcollectionDefinition(CollectionDefinition supercollection) {
        this.supercollection = supercollection;
        overrides = new HashMap<Object, Object>();
        if (supercollection.isTable()) {
            additions = null;
        } else {
            additions = Context.newArrayList(2, Object.class);
        }
    }

//    protected SubcollectionDefinition(SubcollectionDefinition proto, CollectionDefinition supercollection) throws Redirection {
//        super(proto, supercollection.initContext);
//        this.supercollection = supercollection;
//        overrides = proto.overrides;
//        additions = proto.additions;
//    }

    void setSupercollection(CollectionDefinition supercollection) {
        this.supercollection = supercollection;
    }

    public CollectionDefinition getSupercollection() {
        return supercollection;
    }

    public boolean isTable() {
        return supercollection.isTable();
    }


    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        return new ResolvedSubcollection(context, args, indexes);
    }

    public CollectionInstance createCollectionInstance(Context context, ArgumentList args, List<Index> indexes, Object collectionData) throws Redirection {
    	// this is definitely not right.  Not sure if this is even a possible scenario, though, since
    	// this only gets called I believe in the case of externally created collection objects.  How
    	// could one of those be a subcollection?  So I'm holding off on trying to figuring out what
    	// might make sense here.  It compiles.
    	return new ResolvedSubcollection(context, args, indexes);
    }

    public boolean isGrowable() {
        return supercollection.isGrowable();
    }

    public void add(Object element) {
        Definition def = (element instanceof Definition ? (Definition) element : new ElementDefinition(this, element));
        Object contents = def.getContents();
        if (contents instanceof TableElement) {
            putElement((TableElement) contents);

        } else if (contents instanceof ElementDefinition && def instanceof NamedDefinition) {
            if (isTable()) {
                overrides.put(def.getName(), contents);
            } else {
                int ix = ((NamedDefinition) def).getDimSize();
                if (ix >= 0 /* && ix < supercollection.getSize() */ ) {
                    overrides.put(new Integer(ix), contents);
                } else {
                    additions.add(contents);
                }
            }

        } else {
            additions.add(def);
        }
    }

    public void set(int n, Object element) {
        Definition def = (element instanceof Definition ? (Definition) element : new ElementDefinition(this, element));
        overrides.put(new Integer(n), def);
    }


    public void put(Object key, Object element) {
        Definition def = (element instanceof Definition ? (Definition) element : new ElementDefinition(this, element));
        overrides.put(key, def);
    }

    public void putElement(TableElement element) {
        String key = element.getKey().getString();
        overrides.put(key, element);
    }


    class ResolvedSubcollection extends ResolvedCollection {

        protected CollectionInstance superInstance;
        private Object collectionObject = null;

        public ResolvedSubcollection(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
            super(SubcollectionDefinition.this, context, args, indexes);

            // subcollection definitions are implied by the presence of subcollection element
            // definitions, and they share the arguments of the supercollection, so those
            // arguments may be validly passed to the supercollection
            superInstance = supercollection.getCollectionInstance(context, args, indexes);
        }

        /** Returns the SubcollectionDefinition defining this collection. */
        public CollectionDefinition getCollectionDefinition() {
            return SubcollectionDefinition.this;
        }

        @SuppressWarnings("unchecked")
        public Object getCollectionObject() throws Redirection {
            if (collectionObject == null) {
                Context context = getResolutionContext();
                collectionObject = superInstance.getCollectionObject();
                Class<?> c = collectionObject.getClass();
                if (c.isArray()) {
                    int superlen = Array.getLength(collectionObject);
                    int len = superlen + additions.size();
                    Object[] newArray = new Object[len];
                    System.arraycopy(collectionObject, 0, newArray, 0, superlen);
                    Iterator<Object> it = additions.iterator();
                    for (int i = superlen; i < len; i++) {
                        newArray[i] = it.next();
                        if (newArray[i] instanceof CollectionInstance) {
                            newArray[i] = ((CollectionInstance) newArray[i]).getCollectionObject();
                        }
                    }

                    it = overrides.keySet().iterator();
                    while (it.hasNext()) {
                        try {
                            Object key = it.next();
                            int ix = PrimitiveValue.getIntFor(key);
                            newArray[ix] = overrides.get(key);
                            if (newArray[ix] instanceof CollectionInstance) {
                                newArray[ix] = ((CollectionInstance) newArray[ix]).getCollectionObject();
                            }
                        } catch (NumberFormatException nfe) {
                            log("Bad format for array index in subcollection " + getFullName());

                        } catch (ArrayIndexOutOfBoundsException be) {
                            log("Out-of-bounds array index in subcollection " + getFullName());

                        } catch (Throwable t) {
                            log("Problem interpreting array index in subcollection " + getFullName() + ": " + t.toString());
                        }
                    }

                } else if (List.class.isAssignableFrom(c)) {
                    List<Object> list = (List<Object>) collectionObject; 
                    int superlen = list.size();
                    int len = superlen + additions.size();
                    List<Object> newList = new ArrayList<Object>(list);
                    Iterator<Object> it = additions.iterator();
                    for (int i = superlen; i < len; i++) {
                        Object item = it.next();
                        if (item instanceof CollectionInstance) {
                            item = ((CollectionInstance) item).getCollectionObject();
                        }
                        newList.add(item);
                    }

                    it = overrides.keySet().iterator();
                    while (it.hasNext()) {
                        try {
                            Object key = it.next();
                            int ix = PrimitiveValue.getIntFor(key);
                            Object item = overrides.get(key);
                            if (item instanceof CollectionInstance) {
                                item = ((CollectionInstance) item).getCollectionObject();
                            }
                            newList.set(ix, item);
                        } catch (NumberFormatException nfe) {
                            log("Bad format for array index in subcollection " + getFullName());

                        } catch (ArrayIndexOutOfBoundsException be) {
                            log("Out-of-bounds array index in subcollection " + getFullName());

                        } catch (Throwable t) {
                            log("Problem interpreting array index in subcollection " + getFullName() + ": " + t.toString());
                        }
                    }

                } else if (Map.class.isAssignableFrom(c)) {
                    Map<Object, Object> map = (Map<Object, Object>) collectionObject;
                    Map<Object, Object> newMap = new HashMap<Object, Object>(map.size() + overrides.size());
                    Iterator<Object> it;
                    Object item;

                    it = map.keySet().iterator();
                    while (it.hasNext()) {
                        Object key = it.next();
                        item = map.get(key);
                        if (item instanceof CollectionInstance) {
                            item = ((CollectionInstance) item).getCollectionObject();
                        }
                        newMap.put(key, item);
                    }
                    if (additions != null) {
                        it = additions.iterator();
                        while (it.hasNext()) {
                            item = it.next();
                            if (item instanceof TableElement) {
                                TableElement element = (TableElement) item;
                                Object object = element.getContents();
                                if (object instanceof CollectionInstance) {
                                    object = ((CollectionInstance) object).getCollectionObject();
                                }
                                if (element.isDynamic()) {
                                    try {
                                        newMap.put(element.getDynamicKey(context).getString(), object);
    
                                    } catch (Redirection r) {
                                        log("Problem getting dynamic key in subcollection " + getFullName() + ": " + r.toString());
                                    }
    
                                } else {
                                    newMap.put(element.getKey().getString(), object);
                                }
                            }
                        }
                    }
                    it = overrides.keySet().iterator();
                    while (it.hasNext()) {
                        Object key = it.next();
                        item = overrides.get(key);
                        if (item instanceof CollectionInstance) {
                            item = ((CollectionInstance) item).getCollectionObject();
                        }
                        newMap.put(key, item);
                    }
                }
            }
            return collectionObject;
        }




        /** Returns an iterator for the subcollection.
         */
        public Iterator<Definition> iterator() {
            return new ReconciledIterator();
        }

        public Iterator<Construction> constructionIterator() {
            return new ReconciledConstructionIterator();
        }

        public Iterator<Index> indexIterator() {
            return new ReconciledIndexIterator();
        }

        public int getSize() {
            int size = supercollection.getSize();
            if (additions != null) {
                size += additions.size();
            }
            return size;
        }

        public Definition getElement(Index index, Context context) {
            if (context == null) {
            	context = getResolutionContext();
            }
            Value indexVal = index.getIndexValue(context);
            Object key = indexVal.getValue();
            Definition element = (Definition) overrides.get(key);
            if (element == null) {
                int ix = indexVal.getInt();
                int superlen = superInstance.getSize();
                if (ix >= superlen) {
                    return (Definition) additions.get(ix - superlen);
                }
            } else {
                return element;
            }
            return superInstance.getElement(index, context);
        }

        public ResolvedInstance getResolvedElement(Index index, Context context) {
            return null;
        }
        
        public class ReconciledIterator implements Iterator<Definition> {
            Iterator<Index> indexIt;

            public ReconciledIterator() {
                indexIt = indexIterator();
            }

            public boolean hasNext() {
                return indexIt.hasNext();
            }

            public Definition next() {
                Index index = (Index) indexIt.next();
                return getElement(index, null);
            }

            public void remove() {
                throw new UnsupportedOperationException("remove not supported in index iterators");
            }
        }

        public class ReconciledConstructionIterator implements Iterator<Construction> {
            Iterator<Index> indexIt;

            public ReconciledConstructionIterator() {
                indexIt = indexIterator();
            }

            public boolean hasNext() {
                return indexIt.hasNext();
            }

            public Construction next() {
                Index index = (Index) indexIt.next();
                return getConstructionForElement(getElement(index, null));
            }

            public void remove() {
                throw new UnsupportedOperationException("remove not supported in index iterators");
            }
        }

        public class ReconciledIndexIterator implements Iterator<Index> {
            Iterator<Index> superIndexIt;
            int extralen = (additions != null ? additions.size() : 0);
            int extraIx = 0;
            int superlen = superInstance.getSize();


            public ReconciledIndexIterator() {
                superIndexIt = superInstance.indexIterator();
            }

            public boolean hasNext() {
                return (superIndexIt.hasNext() || extraIx < extralen);
            }

            public Index next() {
                if (superIndexIt.hasNext()) {
                    return superIndexIt.next();
                } else {
                    return new CollectionIndex(new PrimitiveValue(superlen + extraIx++));
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("remove method not supported in ReconciledIterator");
            }
        }
    }
}

