/* Canto Compiler and Runtime Engine
 * 
 * Table.java
 *
 * Copyright (c) 2018-2020 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import canto.lang.Construction;
import canto.lang.Core;
import canto.lang.Definition;
import canto.lang.Initializer;
import canto.lang.Redirection;
import canto.lang.Resolver;
import canto.lang.Site;
import canto.lang.Validater;
import canto.parser.CantoParser;
import canto.parser.ParsedCollectionDefinition;

/**
 *  An external runtime utility class for operations on Canto tables.
 * 
 *  @author mash
 */

public class Table {
    
    public static boolean is_table(Context context, Object obj) {
        if (obj instanceof Definition) {
            return ((Definition) obj).is_table();
        } else if (obj instanceof Construction) {
            return ((Construction) obj).getType(context, false).isTable();
        } else {
            return (obj instanceof Map<?, ?>);
        }
    }

    public static Object get(Object tableObject, Object key) {
        if (tableObject != null && tableObject instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) tableObject;
            return map.get(key.toString());
        } else {
            return null;
        }
    }

    public static void set(Object tableObject, Object key, Object element) {
        if (tableObject == null) {
             throw new NullPointerException("canto.runtime.Table.set called with null table");
        }
        if (key == null) {
            throw new NullPointerException("canto.runtime.Table.set called with null key");
        }
                
        if (tableObject instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) tableObject;
            map.put(key.toString(), element);
        }
    }

    public static int size(Object tableObject) {
        if (tableObject == null) {
            return 0;
        } else if (tableObject instanceof Map<?,?>) {
            Map<?,?> map = (Map<?,?>) tableObject;
            return map.size();
        } else {
            return 0;
        }
    }

    public static Object copy(Object tableObject) {
        if (tableObject == null) {
            return null;
        } else if (tableObject instanceof Map<?,?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = new HashMap<String, Object>((Map<String, Object>) tableObject);
            return map;
        } else {
            return null;
        }
    }
    
    public static void clear(Object tableObject) {
        if (tableObject != null && tableObject instanceof Map<?,?>) {
            Map<?,?> map = (Map<?,?>) tableObject;
            map.clear();
        }        
    }

    public static Map<String, Object> parse(Context context, String str) throws Redirection {
        try {
            CantoParser parser = new CantoParser(new StringReader("parse{}=" + str));
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

            return collectionDef.getTable(context, null, null);
            
        } catch (Redirection r) {
            System.out.println("Redirection parsing table: " + r);
            throw r;
        } catch (Exception e) {
            System.out.println("Exception parsing table: " + e);
            e.printStackTrace();
            throw new Redirection(Redirection.STANDARD_ERROR, "Exception parsing table: " + e);
        }
    }

}
