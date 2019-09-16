/* Canto Compiler and Runtime Engine
 * 
 * ForStatement.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.lang.reflect.Array;
import java.util.*;

import canto.runtime.Context;
import canto.runtime.CantoObjectWrapper;

/**
 * ForStatement implements Canto <code>for</code> statements.
 *
 * @author Michael St. Hippolyte
 */

public class ForStatement extends AbstractConstruction implements ConstructionContainer, ConstructionGenerator {

    private IteratorValues vals;
    private Block body;

    public ForStatement() {
        super();
    }

    protected void setBody(Block body) {
        this.body = body;
    }

    protected void addIteratorValues(IteratorValues vals) {
        if (this.vals == null) {
            this.vals = vals;
        } else {
            this.vals.addNext(vals);
        }
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean isDynamic() {
        return true;
    }

    public boolean isStatic() {
        return false;
    }

    public ParameterList getParameters() {
        ArrayList<DefParameter> params = Context.newArrayList(1 + (vals.getNext() == null ? 0 : 1), DefParameter.class);
        for (IteratorValues iv = vals; iv != null; iv = iv.getNext()) {
            DefParameter param = iv.getDefParameter();
            if (param != null) {
                params.add(iv.getDefParameter());
            }
        }
        return new ParameterList(params);
    }

    public List<Construction> getConstructions(Context context) {
        List<Construction> constructions = Context.newArrayList(TYPICAL_LIST_SIZE, Construction.class);
        for (IteratorValues iv = vals; iv != null; iv = iv.getNext()) {
            Construction in = iv.getIn();
            if (in != null) {
                constructions.add(in);
            }
        }
        ValueSource where = vals.getWhere();
        if (where != null) {
            if (where instanceof ConstructionContainer) {
                constructions.addAll(((ConstructionContainer) where).getConstructions(context));
            } else if (where instanceof Construction) {
                constructions.add((Construction) where);
            }
        }
        ValueSource until = vals.getUntil();
        if (until != null) {
            if (until instanceof ConstructionContainer) {
                constructions.addAll(((ConstructionContainer) until).getConstructions(context));
            } else if (until instanceof Construction) {
                constructions.add((Construction) until);
            }
        }
        if (body != null) {
            constructions.addAll(body.getConstructions(context));
        }
        return constructions;
    }

    public List<Construction> generateConstructions(Context context) throws Redirection {

        List<Construction> constructions = Context.newArrayList(TYPICAL_LIST_SIZE, Construction.class);

        try {
            Iterator<Construction> it = vals.iterator(context);
            ValueSource until = vals.getUntil();
            ValueSource where = vals.getWhere();
            int loopIx = context.getLoopIndex();
            context.resetLoopIndex();
            if (it != null) {
                while (it.hasNext()) {
                    context.nextLoopIndex();
                    int n = pushParams(context, it.next());
                    if (until != null) {
                        if (valueOf(until, context).getBoolean()) {
                            popParams(context, n);
                            break;
                        }
                    }
                    if (where != null) {
                        if (!valueOf(where, context).getBoolean()) {
                            popParams(context, n);
                            continue;
                        }
                    }
                    if (body instanceof ConstructionGenerator) {
                        List<Construction> bodyConstructions = ((ConstructionGenerator) body).generateConstructions(context.clone(false));
                        constructions.addAll(bodyConstructions);

                    } else {
                        Object data = body.getData(context);
                        //if (data instanceof Value && !(data instanceof ResolvedInstance)) {
                        //    data = ((Value) data).getValue();
                        //}
                        if (data != null) {
                            if (data instanceof Construction) {
                                constructions.add((Construction) data);
                            } else {
                                constructions.add(getConstructionForElement(data, context));
                            }
                        }
                    }
                    popParams(context, n);
                }
                context.setLoopIndex(loopIx);
            }
        } catch (Redirection r) {
            ;
        }

        ((ArrayList<Construction>) constructions).trimToSize();
        return constructions;
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        StringBuffer sb = null;
        Object data = null;

        // get an iterator
        Iterator<Construction> it = vals.iterator(context);
        ValueSource until = vals.getUntil();
        ValueSource where = vals.getWhere();
        if (it != null) {
            while (it.hasNext()) {
                context.nextLoopIndex();
                int n = pushParams(context, it.next());
                if (until != null) {
                    if (valueOf(until, context).getBoolean()) {
                        popParams(context, n);
                        break;
                    }
                }
                if (where != null) {
                    if (!valueOf(where, context).getBoolean()) {
                        popParams(context, n);
                        continue;
                    }
                }
                Object nextData = body.getData(context);
                if (nextData != null) {
                    if (data == null) {
                        data = nextData;
                    } else {
                	    if (sb == null) {
                            sb = new StringBuffer(getTextForData(data));
                        }
                        sb.append(getTextForData(nextData));
                    }
                }
                popParams(context, n);
            }
            context.resetLoopIndex();
        }
        if (sb != null) {
            return sb.toString();
        } else {
            return data;
        }
    }
    
    private String getTextForData(Object data) throws Redirection {
        if (data instanceof CantoObjectWrapper) {
            data = ((CantoObjectWrapper) data).getData();
        }
        if (data instanceof Value) {
            return ((Value) data).getString();
        } else {
            return data.toString();
        }
    }

    private int pushParams(Context context, Object argObj) {
        int n = 0;
        
        if (argObj instanceof ArgumentList) {
            Iterator<Construction> it = ((ArgumentList) argObj).iterator();
            IteratorValues iv = vals;
            while (it.hasNext()) {
                context.pushParam(iv.getDefParameter(), it.next());
                iv = iv.getNext();
                n++;
            }

        } else if (argObj != null) {
            Construction arg = null;
            if (argObj instanceof Construction) {
                arg = (Construction) argObj;
          //  } else if (argObj instanceof Definition) {
          //      arg = new Instantiation((Definition) argObj, getOwner());
            } else {
                arg = new PrimitiveValue(argObj);
            }
            context.pushParam(vals.getDefParameter(), arg);
            n = 1;
        }

        return n;
    }

    private void popParams(Context context, int n) {
        for (int i = 0; i < n; i++) {
            context.popParam();
        }
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        sb.append("for ");
        IteratorValues vals = this.vals;
        while (vals != null) {
            sb.append(vals.toString());
            sb.append(' ');
            vals = vals.getNext();
            if (vals != null) {
                sb.append("and ");
            }
        }
        sb.append(body.toString(prefix));
        return sb.toString();
    }

    public static class IteratorValues extends AbstractNode {
        private DefParameter forDef;
        private Construction in;
        private ValueSource from;
        private ValueSource to;
        private ValueSource through;
        private ValueSource by;
        private ValueSource where;
        private ValueSource until;
        private IteratorValues next;

        public IteratorValues() {}

        /** Returns <code>false</code> */
        public boolean isPrimitive() {
            return false;
        }

        /** Returns <code>false</code> */
        public boolean isStatic() {
            return false;
        }

        /** Returns <code>false</code> */
        public boolean isDynamic() {
            return false;
        }

        /** Returns <code>false</code> */
        public boolean isDefinition() {
            return false;
        }

        public DefParameter getDefParameter() {
            return forDef;
        }

        protected void set(DefParameter forDef, Construction in, ValueSource from, ValueSource to, ValueSource through, ValueSource by, ValueSource where, ValueSource until, boolean optional) {
            this.forDef = forDef;
            this.in = in;
            this.from = from;
            this.to = to;
            this.through = through;
            this.by = by;
            this.where = where;
            this.until = until;
        }

        public void addNext(IteratorValues next) {
            if (this.next == null) {
                this.next = next;
            } else {
                this.next.addNext(next);
            }
        }

        public IteratorValues getNext() {
            return next;
        }

        public Construction getIn() {
            return in;
        }

        /** Return the effective where clause, combining the where clause defined
         *  in the this set of iterator values with the where clause from the
         *  next set.
         */
        public ValueSource getWhere() {
            if (next != null && next.getWhere() != null) {
                if (where != null) {
                    AbstractNode[] nodes = new AbstractNode[3];
                    nodes[0] = (AbstractNode) where;
                    nodes[1] = new LogicalAndOperator();
                    nodes[2] = (AbstractNode) next.getWhere();
                    ValueExpression exp = new ValueExpression();
                    exp.children = nodes;
                    return exp;
                } else {
                    return next.getWhere();
                }
            } else {
                return where;
            }
        }

        /** Return the effective until clause, combining the until clause defined
         *  in the this set of iterator values with the until clause from the
         *  next set.
         */
        public ValueSource getUntil() {
            if (next != null && next.getUntil() != null) {
                if (until != null) {
                    AbstractNode[] nodes = new AbstractNode[3];
                    nodes[0] = (AbstractNode) until;
                    nodes[1] = new LogicalAndOperator();
                    nodes[2] = (AbstractNode) next.getUntil();
                    ValueExpression exp = new ValueExpression();
                    exp.children = nodes;
                    return exp;
                } else {
                    return next.getUntil();
                }
            } else {
                return until;
            }
        }

        /** Returns the appropriate iterator for this set of values. */
        public Iterator<Construction> iterator(Context context) throws Redirection {
            Iterator<Construction> it = null;
            int numPushes = 0;
            try {
                if (in != null && in instanceof Instantiation) {
                    Instantiation instance = (Instantiation) in;
                    Instantiation lastInstance = null;
                    Definition def = null;
                    Definition lastDef = null;
                    Object data = null;
                    
                    while (!instance.equals(lastInstance)) {
                        lastInstance = instance;
                        def = instance.getUltimateDefinition(context);
                        if (def != null && def.getDurability() != Definition.DYNAMIC && !instance.isDynamic() && !instance.getReferenceName().hasIndexes()) {
                            data = context.getData(def, instance.getName(), instance.getArguments(), instance.getIndexes());
                        }
                        if (data != null) {
                        	break;
                        }
                        
                        if (def != null && def != lastDef) {
                        	lastDef = def;
                            if (!def.isExternal()) {
                                NameNode nameNode = instance.getReferenceName();
                                if (nameNode != null && nameNode.isComplex()) {
                                    numPushes += context.pushParts(instance);
                                }                    
                            }
    
                            if (def.isAliasInContext(context)) {
                                ArgumentList args = instance.getArguments();
                                ParameterList params = def.getParamsForArgs(args, context);
                                context.push(def, params, args, true);
                                numPushes++;
                                instance = def.getAliasInstanceInContext(context);
                                if (instance == null) {
                                    instance = lastInstance;
                                    break;
                                }
                            }
                        } else if (def == null) {
                            log("Cannot find definition for instance " + instance.getName() + " in for statement.");
                        } 
                    }
                    

                    if (data == null && def instanceof CollectionDefinition) {

                        // push the collection definition on the stack, because getting
                        // the iterator may trigger the instantiation of the array

                        ArgumentList args = instance.getArguments();
                        List<Index> indexes = instance.getIndexes();
                        CollectionInstance collection = ((CollectionDefinition) def).getCollectionInstance(context, args, indexes);
                        it  = collection.constructionIterator();

//                        ParameterList params = collection.getParamsForArgs(args, context);
//                        context.push(collection, params, args);
//                        it = collection.initCollection(context, args).iterator(context);
//                        context.pop();
                    } else {
                    	if (data == null) {
                            data = instance.generateData(context, def);
                    	}
                        if (data instanceof CantoObjectWrapper) {
                            data = ((CantoObjectWrapper) data).getData();
                        }
                        if (data instanceof Value && !(data instanceof ResolvedCollection)) {
                        	data = ((Value)data).getValue();
                        }
                        if (data == null) {
                        	it = new EmptyIterator<Construction>();
                        } else if (data instanceof Iterator<?>) {
                            it = new ConstructionObjectIterator((Iterator<?>) data);
                        } else if (data instanceof Map<?,?>) {
                            @SuppressWarnings("unchecked")
                            Collection<Construction> values = ((Map<String, Construction>) data).values();
                            if (values == null) {
                                it = new EmptyIterator<Construction>();
                            } else {
                                it = values.iterator();
                            }
                        } else if (data instanceof ResolvedCollection) {
                            it  = ((ResolvedCollection) data).constructionIterator();
                        } else if (data instanceof Collection<?>) {
                            it = new ConstructionObjectIterator(((Collection<?>) data).iterator());
                        } else if (data instanceof CantoArray) {
                            it = new ConstructionObjectIterator(((CantoArray) data).iterator());
                        } else if (data.getClass().isArray()) {
                            Type type = null;
                            if (forDef != null) {
                                type = forDef.getType();
                            }
                        	it = new ConstructionArrayIterator(data, type);
                        } else {
                            if (!(data instanceof Construction)) {
                                data = new PrimitiveValue(data);
                            }
                        	it = new SingleItemIterator<Construction>((Construction) data);
                        }
                    }

                } else if (from != null) {
                    Value fromValue = valueOf(from, context);
                    Value toValue = (to != null ? valueOf(to, context) : null);
                    Value throughValue = (through != null ? valueOf(through, context) : null);
                    Value byValue = (by != null ? valueOf(by, context) : null);
                    it = new FromIterator(fromValue, toValue, throughValue, byValue, context);

                } else {
                    it = new InfiniteIterator();
                }
            //} catch (Redirection r) {
            //    return null;
            } finally {
                while (numPushes-- > 0) {
                    context.pop();
                }
                
            }
            if (next != null) {
                Iterator<Construction> nextIt = next.iterator(context);
                if (it != null) {
                    it = new CombinedIterator(it, nextIt);
                } else {
                	it = nextIt;
                }
            }
            return it;
        }
        
        public String toString(String prefix) {
            StringBuffer sb = new StringBuffer(prefix);
            if (forDef != null) {
                forDef.toString("");
            }
            if (in != null) {
                sb.append(" in ");
                sb.append(in.toString());
            } else {
                sb.append(" from ");
                sb.append(from.toString());
                if (to != null) {
                    sb.append(" to ");
                    sb.append(to.toString());
                }
                if (through != null) {
                    sb.append(" through ");
                    sb.append(through.toString());
                }
                if (by != null) {
                    sb.append(" by ");
                    sb.append(by.toString());
                }
            }
            if (where != null) {
                sb.append(" where ");
                sb.append(where.toString());
            }
            if (until != null) {
                sb.append(" until ");
                sb.append(until.toString());
            }
            return sb.toString();
        }
    }
}

class ConstructionObjectIterator implements Iterator<Construction> {
    private Iterator<?> it;
    
    public ConstructionObjectIterator(Iterator<?> it) {
        this.it = it;
    }

    public boolean hasNext() {
        return it.hasNext();
    }

    public Construction next() {
        return AbstractConstruction.getConstructionForObject(it.next());
    }

    public void remove() {
        throw new UnsupportedOperationException("ConstructionObjectIterator doesn't support remove");
    }

}

class ConstructionArrayIterator implements Iterator<Construction> {
	private Object array;
	private int ix = 0;
	private boolean isDefinitionArray = false;
	
	public ConstructionArrayIterator(Object array, Type type) {
		this.array = array;
		if (type != null && type.includes("definition")) {
		    isDefinitionArray = true;
		}
	}
	
	public boolean hasNext() {
		return (ix < Array.getLength(array));
	}
	
    public Construction next() {
        Object obj = Array.get(array, ix++);
        if (obj instanceof Definition && !isDefinitionArray) {
            return new Instantiation((Definition) obj);
        } else {
		    return AbstractConstruction.getConstructionForObject(obj);
        }
	}
	
    public void remove() {
        throw new UnsupportedOperationException("ArrayIterator doesn't support remove");
    }
}

class FromIterator implements Iterator<Construction> {

    static final Value plusOne = new PrimitiveValue(1);
    static final Value minusOne = new PrimitiveValue(-1);
    static final Value zero = new PrimitiveValue(0);

    private Construction value;
    private Value toValue;
    private Value throughValue;
    private Value byValue;
    private boolean goingUp;
    
    private Context context;

    private AddOperator addOp = new AddOperator();
    private LessThanOperator ltOp = new LessThanOperator();
    private GreaterThanOperator gtOp = new GreaterThanOperator();
    private LessThanOrEqualOperator leOp = new LessThanOrEqualOperator();
    private GreaterThanOrEqualOperator geOp = new GreaterThanOrEqualOperator();

    public FromIterator(Value fromValue, Value toValue, Value throughValue, Value byValue, Context context) {
        this.toValue = toValue;
        this.throughValue = throughValue;
        this.context = context;
        
        ltOp = new LessThanOperator();
        gtOp = new GreaterThanOperator();

        if (throughValue != null) {
            leOp = new LessThanOrEqualOperator();
            geOp = new GreaterThanOrEqualOperator();
        }


        if (byValue == null) {
            // from without to, through or by defaults to incrementing by one
            if (toValue != null) {
                if (ltOp.getBoolean(fromValue, toValue)) {
                    byValue = plusOne;
                } else if (gtOp.getBoolean(fromValue, toValue)) {
                    byValue = minusOne;
                } else {
                    byValue = zero;
                }
            } else if (throughValue != null) {
                if (ltOp.getBoolean(fromValue, throughValue)) {
                    byValue = plusOne;
                } else if (gtOp.getBoolean(fromValue, throughValue)) {
                    byValue = minusOne;
                } else {
                    byValue = zero;
                }
            } else {
                byValue = plusOne;
            }
        }
        this.byValue = byValue;
        goingUp = gtOp.getBoolean(byValue, zero);
        value = AbstractConstruction.getConstructionForObject((Object) fromValue);
    }
    
 
    public boolean hasNext() {
        try {
            if (toValue != null) {
                if (goingUp) {
                    return ltOp.getBoolean(valueFor(value, context), toValue);
    
                } else {    // going down or not going anywhere
                    return gtOp.getBoolean(valueFor(value, context), toValue);
                }
            } else if (throughValue != null) {
                if (goingUp) {
                    return leOp.getBoolean(valueFor(value, context), throughValue);
    
                } else {    // going down or not going anywhere
                    return geOp.getBoolean(valueFor(value, context), throughValue);
                }
    
            } else {
                return true;
            }
        } catch (Throwable t) {
            return false;
        }
   }

    public Construction next() {
        Construction returnVal = value;
        try {
            value = AbstractConstruction.getConstructionForObject(addOp.operate(valueFor(value, context), byValue));
        } catch (Throwable t) {
            value = null;
        }
        return returnVal;
    }

    public void remove() {
        throw new UnsupportedOperationException("FromIterator doesn't support remove");
    }
    
    private Value valueFor(Construction val, Context context) throws Redirection {
        if (val instanceof Value) {
            return (Value) val; 
        } else {
            return new PrimitiveValue(val.getData(context));
        }
    }
}

class InfiniteIterator implements Iterator<Construction> {
    public boolean hasNext() {
        return true;
    }

    public Construction next() {
        return null;
    }

    public void remove() {
        throw new UnsupportedOperationException("InfiniteIterator doesn't support remove");
    }
}

class EmptyIterator<E> implements Iterator<E> {
    public boolean hasNext() {
        return false;
    }

    public E next() {
        throw new NoSuchElementException("Attempt to call next() on empty iterator");
    }

    public void remove() {
        throw new UnsupportedOperationException("EmptyIterator doesn't support remove");
    }
}

class CombinedIterator implements Iterator<Construction> {

    private Iterator<Construction> it1;
    private Iterator<Construction> it2;

    public CombinedIterator(Iterator<Construction> it1, Iterator<Construction> it2) {
        this.it1 = it1;
        this.it2 = it2;
    }

    public boolean hasNext() {
        return it1.hasNext() && it2.hasNext();
    }

    public Construction next() {
        return new Combo(it1.next(), it2.next());
    }

    public void remove() {
        it1.remove();
        it2.remove();
    }
}

class Combo extends ArgumentList implements Construction {

    public Combo(Construction obj1, Construction obj2) {
        super();
        int n = (obj1 instanceof Combo ? ((Combo) obj1).size() : 1) + (obj2 instanceof Combo ? ((Combo) obj2).size() : 1);
        List<Construction> list = Context.newArrayList(n, Construction.class);
        if (obj1 instanceof Combo) {
            list.addAll((Combo) obj1);
        } else {
            list.add((Construction) obj1);
        }
        if (obj2 instanceof Combo) {
            list.addAll((Combo) obj2);
        } else {
            list.add((Construction) obj2);
        }
        setList(list);
    }

    public String getDefinitionName()                            { return null; }
    public Type getType(Context context, boolean generate)       { return null; }
    public boolean getBoolean(Context context)                   { return false; }
    public Object getData(Context context)                       { return null; }
    public Object getData(Context context, Definition def)       { return null; }
    public String getText(Context context)                       { return null; }
    public boolean isAbstract(Context context)                   { return false; }
    public Construction getUltimateConstruction(Context context) { return this; }

    public String getString(Context context) throws Redirection  { return null; }
    public byte getByte(Context context) throws Redirection      { return 0; }
    public char getChar(Context context) throws Redirection      { return 0; }
    public int getInt(Context context) throws Redirection        { return 0; }
    public long getLong(Context context) throws Redirection      { return 0; }
    public double getDouble(Context context) throws Redirection  { return 0; }
	public Value getValue(Context context) throws Redirection    { return NullValue.NULL_VALUE; }
}
