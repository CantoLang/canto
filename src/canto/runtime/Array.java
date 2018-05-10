/* Canto Compiler and Runtime Engine
 * 
 * Array.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import canto.lang.Core;
import canto.lang.Definition;
import canto.lang.CantoArray;
import canto.lang.Initializer;
import canto.lang.Redirection;
import canto.lang.Resolver;
import canto.lang.Site;
import canto.lang.Validater;
import canto.parser.CantoParser;
import canto.parser.ParseException;
import canto.parser.ParsedCollectionDefinition;

import java.io.StringReader;

/**
 *  An external runtime utility class for operations on Canto arrays.
 * 
 *  @author mash
 *
 */

public class Array {
    
    public static Object get(Object arrayObject, int index) {
        if (arrayObject == null) {
            return null;
        }
        if (arrayObject.getClass().isArray()) {
            return java.lang.reflect.Array.get(arrayObject, index);
        } else if (arrayObject instanceof List<?>) {
            List<?> list = (List<?>) arrayObject;
            return list.get(index);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static void set(Object arrayObject, int index, Object element) {
        if (arrayObject != null) {
            if (arrayObject.getClass().isArray()) {
                java.lang.reflect.Array.set(arrayObject, index, element);
            } else if (arrayObject instanceof List<?>) {
                List<Object> list = (List<Object>) arrayObject;
                list.set(index, element);
            }
        } else {
            throw new UnsupportedOperationException("Cannot set an element in a null array");
        }
    }

    @SuppressWarnings("unchecked")
    public static void add(Object arrayObject, Object element) {
        if (arrayObject != null && arrayObject instanceof List<?>) {
            List<Object> list = (List<Object>) arrayObject;
            list.add(element);
        } else {
            throw new UnsupportedOperationException("Cannot append to a null or fixed array");
        }
    }

    @SuppressWarnings("unchecked")
    public static void remove(Object arrayObject, Object element) {
        if (arrayObject != null && arrayObject instanceof List<?>) {
            List<Object> list = (List<Object>) arrayObject;
            list.remove(element);
        } else {
            throw new UnsupportedOperationException("Cannot remove element from a null or fixed array");
        }
    }

    public static int size(Object arrayObject) {
        if (arrayObject == null) {
            return 0;
        } else if (arrayObject.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(arrayObject);
        } else if (arrayObject instanceof List<?>) {
            List<?> list = (List<?>) arrayObject;
            return list.size();
        } else {
            return 0;
        }
    }
    
    public static Object copy(Object arrayObject) {
        if (arrayObject == null) {
            return null;
        } else if (arrayObject.getClass().isArray()) {
            int size = java.lang.reflect.Array.getLength(arrayObject);
            return Arrays.copyOf((Object[]) arrayObject, size);
        } else if (arrayObject instanceof List<?>) {
            List<Object> list = (List<Object>) arrayObject;
            return new ArrayList<Object>(list);
        } else {
            return null;
        }
    }
    
    public static boolean contains(Object arrayObject, Object element) {
        if (arrayObject == null) {
            return false;
        } else if (arrayObject.getClass().isArray()) {
            Object[] array = (Object[]) arrayObject;
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null) {
                    if (element == null) {
                        return(true); 
                    }
                } else {
                    if (array[i].equals(element)) {
                        return(true);
                    }
                }
            }
            return false;
        } else if (arrayObject instanceof List<?>) {
            List<?> list = (List<?>) arrayObject;
            return list.contains(element);
        } else {
            return false;
        }
    }
    
    public static Object parse(Context context, String str) throws ParseException, Redirection {
        try {
            CantoParser parser = new CantoParser(new StringReader("parse[]=" + str));
            Definition owner = context.getDefiningDef();
            
            ParsedCollectionDefinition collectionDef = (ParsedCollectionDefinition) parser.CollectionDefinition();
            collectionDef.setOwner(owner);
            collectionDef.init();
            
            Site site = owner.getSite();
            Core core = site.getCore();
            collectionDef.jjtAccept(new Initializer(core, site, true), owner);
            collectionDef.jjtAccept(new Resolver(), null);
            Validater validater = new Validater();
            collectionDef.jjtAccept(validater, null);
            String problems = validater.spoolProblems();
            if (problems.length() > 0) {
                throw new Redirection(Redirection.STANDARD_ERROR, "Problems parsing array: " + problems);
            }
            
            // should there be a Linker?

            CantoArray array = collectionDef.getArray(context, null, null);
            if (array != null) {
                return array.getArrayObject();
            } else {
                return null;
            }
            
        } catch (Redirection r) {
            System.out.println("Redirection parsing array: " + r);
            throw r;
        } catch (Exception e) {
            System.out.println("Exception parsing array: " + e);
            throw new Redirection(Redirection.STANDARD_ERROR, "Exception parsing array: " + e);
        }
    }
}
