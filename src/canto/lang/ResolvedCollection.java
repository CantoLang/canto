/* Canto Compiler and Runtime Engine
 * 
 * ResolvedCollection.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.*;


/**
 * An ResolvedCollection is a ResolvedInstance representing a collection.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.16 $
 */
abstract public class ResolvedCollection extends ResolvedInstance implements CollectionInstance {

    private Context elementContext = null;

    public ResolvedCollection(Instantiation instance, Context context) {
        super(instance, context, false);
    }

    public ResolvedCollection(Definition def, Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        super(def, context, args, indexes);
    }

    protected Definition getElementDefinition(Object element) {
        try {
            return getElementDefinitionForCollection(element, getCollectionDefinition(), getElementResolutionContext());
        } catch (Redirection r) {
            System.err.println("Unable to get element defintion: " + r.toString());
            return null;
        }
    }        
        
    protected static Definition getElementDefinitionForCollection(Object element, CollectionDefinition def, Context context) {
        if (element == null) {
            return null;
        }

        if (element instanceof Instantiation) {
            Instantiation instance = (Instantiation) element;
            if (instance.getReference() instanceof Definition) {
                return (Definition) instance.getReference();
            } else if (!(instance instanceof ResolvedInstance)) {
                element = new ResolvedInstance(instance, context, true);
            }
        }
        return def.getDefinitionForElement(element);
    }
    
    protected Construction getConstructionForElement(Object element) {
        try {
            return getConstructionForElement(element, getResolutionContext());
        } catch (Redirection r) {
            return NullValue.NULL_VALUE;
        }
    }
    
//    public String toString() {
//        try {
//            return getText(getResolutionContext());
//        } catch (Redirection r) {
//            return null;
//        }
//    }

    private Context getElementResolutionContext() throws Redirection {
        if (elementContext == null) {
            elementContext = (Context) getResolutionContext().clone();
            Definition def = getCollectionDefinition();
            ArgumentList args = getArguments();
            ParameterList params = def.getParamsForArgs(args, elementContext);
            elementContext.push(def, params, args, false);
        }
        return elementContext;
    }

    /** Returns the definition of this collection. */
    abstract public CollectionDefinition getCollectionDefinition();

    /** Returns the object containing the collection.  The collection is shallowly
     *  instantiated; i.e., the container (table or array) is instantiated but the
     *  individual items in the container are not necessarily instantiated.
     */
    abstract public Object getCollectionObject() throws Redirection;
    abstract public int getSize();
    abstract public Iterator<Definition> iterator();
    abstract public Iterator<Construction> constructionIterator();
    abstract public Iterator<Index> indexIterator();
    abstract public Definition getElement(Index index, Context context);
    abstract public ResolvedInstance getResolvedElement(Index index, Context context);

    /** For a growable collection, adds an element to the collection.  Throws
     *  an UnsupportedOperationException on a fixed collection.
     */
    //abstract public void add(Object element);
}
