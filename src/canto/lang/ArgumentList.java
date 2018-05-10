/* Canto Compiler and Runtime Engine
 * 
 * ArgumentList.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * An ArgumentList is a list of arguments.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.24 $
 */
public class ArgumentList extends ListNode<Construction> {

    /** Object inserted into argument lists on the stack to represent missing arguments. */
    public final static Construction MISSING_ARG = new Construction() {

        public boolean getBoolean(Context context)                   { return false; }
        public String getText(Context context)                       { return null; }
        public Object getData(Context context)                       { return null; }
        public Object getData(Context context, Definition def)       { return null; }
        public boolean isAbstract(Context context)                   { return false; }
        public Type getType(Context context, boolean generate)       { return null; }
        public String getDefinitionName()                            { return null; }
        public Construction getUltimateConstruction(Context context) { return this; }
        public String toString()                                     { return "(missing arg)"; }
        public String getString(Context context) throws Redirection  { return null; }
        public byte getByte(Context context) throws Redirection      { return 0; }
        public char getChar(Context context) throws Redirection      { return 0; }
        public int getInt(Context context) throws Redirection        { return 0; }
        public long getLong(Context context) throws Redirection      { return 0; }
		public double getDouble(Context context) throws Redirection  { return 0; }
		public Value getValue(Context context) throws Redirection    { return NullValue.NULL_VALUE; }
		public boolean isPrimitive()                                 { return true; }
    };

    private boolean dynamic = false;
    private boolean concurrent = false;
    private boolean array = false;
    private boolean table = false;

    public ArgumentList() {
        super();
    }

    public ArgumentList(boolean dynamic) {
        super(1);
        this.dynamic = dynamic;
    }

    public ArgumentList(int capacity) {
        super(capacity);
    }

    public ArgumentList(ArgumentList args) {
        super(Context.newArrayList(args));
        setDynamic(args.isDynamic());
        setConcurrent(args.isConcurrent());
    }
    
    public ArgumentList(List<Construction> list) {
        super(list);
    }
    
    public ArgumentList(CantoNode[] nodes) {
        super();
        init(nodes);
    }    

    protected void init(CantoNode[] nodes) {
        
        int len = (nodes == null ? 0 : nodes.length);       
        
        List<Construction> list = new ArrayList<Construction>(len);
        
        for (int i = 0; i < len; i++) {
            CantoNode node = nodes[i];
            
            if (node instanceof Construction) {
                list.add((Construction) node);
            } else if (node instanceof Definition) {
                Instantiation instance = new Instantiation(node);
                list.add(instance);
            } else {
                list.add(new PrimitiveValue(node));
            }
        }
        
        setList(list);
    }

    public boolean equals(Object obj) {
        if (obj instanceof List<?> && ((List<?>) obj).size() == size()) {
            Iterator<?> thisIt = iterator();
            Iterator<?> otherIt = ((List<?>) obj).iterator();
            while (thisIt.hasNext()) {
                if (!thisIt.next().equals(otherIt.next())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /** Returns true if these are dynamic arguments, i.e. enclosed in (: :) rather
     *  than ( )
     **/
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    /** Returns true if this a concurrent argument list, i.e. enclosed in (+ +)
     *  rather than ( )
     **/
    public boolean isConcurrent() {
        return concurrent;
    }

    public void setConcurrent(boolean concurrent) {
        this.concurrent = concurrent;
    }

    /** Returns true if this list defines the elements of an array, i.e. enclosed
     *  in [ ] rather than ( )
     **/ 
    public boolean isArray() {
        return array;
    }
    
    /** Sets the flag indicating whether or not this list defines the elements of an
     *  array.
     **/ 
    protected void setArray(boolean array) {
        this.array = array;
    }

    /** Returns true if this list defines the elements of an table, i.e. enclosed
     *  in { } rather than ( )
     **/ 
    public boolean isTable() {
        return table;
    }
    
    /** Sets the flag indicating whether or not this list defines the elements of an
     *  table.
     **/ 
    protected void setTable(boolean table) {
        this.table = table;
    }


    public Object clone() {
        return new ArgumentList(this);
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        Iterator<Construction> it = iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

}
