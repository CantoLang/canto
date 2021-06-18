/* Canto Compiler and Runtime Engine
 * 
 * ExternalDefinition.java
 *
 * Copyright (c) 2018-2020 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.lang.reflect.*;
import java.util.*;

import canto.runtime.*;

/**
* Facade class to make a Java object available as a Canto definition.
*
* @author Michael St. Hippolyte
* @version $Revision: 1.163 $
*/
public class ExternalDefinition extends ComplexDefinition {

    /**
     * Table to cache class lookups, to avoid all the exception
     * throwing with Class.forName calls.
     */
    private static Map<String, Class<?>> classes = new HashMap<String, Class<?>>();


    /**
     * Dummy classes used as special entries in the class lookup table.
     */
    private static class Unusable {}
    private static class Bogus {}
    
    
    /**
     * Creates a prototype definition.
     */
    public static ExternalDefinition createPrototype(ComplexName nameNode, Type superType, int access, int dur) {
        return (ExternalDefinition) createForName(null, nameNode, superType, access, dur, null);
    }
    
    /**
     * Creates a definition corresponding to the passed complex name.
     */
    public static Definition createForName(Definition owner, ComplexName nameNode, Type superType, int access, int dur, Context context) {
        // find the left-most class named by the passed complex name.
        Class<?> externalClass = null;
        String name = "";
        int n = 0;
        int numParts = nameNode.getNumChildren();
        NameNode namePart = null;
        while (n < numParts && externalClass == null) {
            namePart = (NameNode) nameNode.getChild(n);
            if (n == 0) {
                name = namePart.getName();
            } else {
                name = name + '.' + namePart.getName();
            }

            Class<?> entry = classes.get(name);
            if (entry == null) {
                try {
                    externalClass = Class.forName(name);
                    classes.put(name, externalClass);
                } catch (NoClassDefFoundError ncdfe) {
                    classes.put(name, Unusable.class);
                } catch (ClassNotFoundException cnfe) {
                    classes.put(name, Bogus.class);
                }
            } else if (entry != Unusable.class && entry != Bogus.class) {
                externalClass = entry;
            }

            n++;
        }

        // can't create a definition with nothing to point to
        if (externalClass == null) {
            SiteBuilder.vlog("No external definition for " + name);
            return null;
        }


        if (n == numParts) {
            return new ExternalDefinition(nameNode, (CantoNode) owner, owner, superType, access, dur, externalClass, namePart.getArguments());
        }
        
        ExternalDefinition externalDef = new ExternalDefinition(name, (CantoNode) owner, owner, superType, access, dur, externalClass, namePart.getArguments());
        if (context == null) {
            NameNode[] parts = new NameNode[numParts - n];
            for (int i = n; i < numParts; i++) {
                parts[i - n] = (NameNode) nameNode.getChild(i);
            }
            return new PartialDefinition(externalDef, parts);
        }

        try {
            Definition def = null;
            while (n < numParts) {
                namePart = (NameNode) nameNode.getChild(n);
                if (externalDef != null) {
                    def = externalDef.getDefForContext(context, null);
                    if (def instanceof ExternalDefinition) {
                        externalDef = (ExternalDefinition) def;
                        def = externalDef.getExternalChildDefinition(namePart, context);
                    } else {
                        externalDef = null;
                    }
                }
                
                // either externalDef was null to begin with, or getDefForContext above
                // returned a non-ExternalDefinition.
                if (externalDef == null) {
                    def = def.getChildDefinition(namePart, namePart.getArguments(), namePart.getIndexes(), null, context, null);
                }
                if (def == null) {
                    SiteBuilder.log("No " + namePart.getName() + " belonging to external definition " + nameNode.getName());
                    return null;
                }
                n++;
            }
            return def;
        } catch (Throwable t) {
            SiteBuilder.log("Problem initing external definition " + externalDef.getFullName() + ": " + t.toString());
            return null;
        }
    }

    private Class<?> c = null;
    private Object object = null;
    private ArgumentList args = null;
    private List<Dim> dims = null;

    public ExternalDefinition() {
        super();
        //setDurability(DYNAMIC);
    }

    public ExternalDefinition(ExternalDefinition def) {
        this(def.getNameNode(), def.getParent(), def.getOwner(), def.getSuper(), def.getAccess(), def.getDurability(), def.getObject(), null);
    }

    public ExternalDefinition(ExternalDefinition def, ArgumentList args) {
        this(def.getNameNode(), def.getParent(), def.getOwner(), def.getSuper(), def.getAccess(), def.getDurability(), def.getObject(), args);
    }

    //public ExternalDefinition(Definition owner, Type superType, int access, int dur, Object object, ArgumentList args) {
    //    this(null, (CantoNode) owner, owner, superType, access, dur, object, args);
    //}

    public ExternalDefinition(String internalName, CantoNode parent, Definition owner, Type superType, int access, int dur, Object object, ArgumentList args) {
        this(createNameNode(internalName), parent, owner, superType, access, dur, object, args);
    }
        
        
    public ExternalDefinition(NameNode name, CantoNode parent, Definition owner, Type superType, int access, int dur, Object object, ArgumentList args) {
        super();
        setAccess(access);
        setDurability(dur);
        setOwner(owner);
        ComplexDefinition complexOwner = ComplexDefinition.getComplexOwner(owner);
        if (complexOwner != null) {
            setDefinitionTable(complexOwner.getDefinitionTable());
        }
        jjtSetParent((AbstractNode) parent);
        setObject(object);
        setArguments(args);
        if (object instanceof Value) {
            c = ((Value) object).getValueClass();
        } else if (object instanceof ExternalDefinition) {
            c = ((ExternalDefinition) object).getExternalClass();
        } else if (object instanceof Definition) {
            c = Definition.class;
        } else if (object instanceof Class<?>) {
            c = (Class<?>) object;
        } else {
            c = object.getClass();
        }
        if (name == null) {
            name = new ComplexName(c.getName());
        }
        setName(name);
        AbstractNode contents = null;
        if (object instanceof AbstractNode) {
            contents = (AbstractNode) object;
        } else if (object instanceof Class<?>) {
            contents = getConstructor();
        } else {
            contents = new PrimitiveValue(object, c);
        }
        init(superType, name, contents);
    }


    public ExternalDefinition(ExternalDefinition def, Context context, ArgumentList args) throws Redirection {
        super();
        setAccess(def.getAccess());
        setDurability(def.getDurability());
        setOwner(def.getOwner());
        setObject(def.getObject());
        
        Class<?> clazz = def.getExternalClass(context);
        //if (CollectionDefinition.class.isAssignableFrom(clazz)) {
        //    Object obj = def.generateInstance(context);
        //    clazz = obj.getClass();
        //}
        
        setExternalClass(clazz);
        setArguments(args);
        AbstractNode contents = def.getContents();
        if (contents instanceof PartialDefinition) {
            def = ((PartialDefinition) contents).completeForContext(context);
            contents = def.getContents();
        }

        if (contents instanceof ExternalConstruction) {
            contents = ((ExternalConstruction) contents).initExternalObject(context, args);
        }
        initContext = (Context) context.clone();
        init(def.getSuper(), def.getNameNode(), contents);
    }
   
    Context getInitContext() {
        return initContext;
    }

    public ExternalDefinition newForArgs(ArgumentList args) {
        return new ExternalDefinition(this, args);
    }

    /** External definitions are always dynamic. **/    
//    public int getDurability() {
//        return DYNAMIC;
//    }

    /** Returns <code>true</code> */
    public boolean isExternal() {
        return true;
    }

    public String getFullName() {
        String name = getName();
        
        // this is a bit of an approximation -- what we probably want is to check the
        // name against the extern list.
        if (name.indexOf('.') > 0) {
            return name;
        } else {
            return super.getFullName();
        }
    }

    /** Returns the full name in context.  For external definitions, the full name
     *  in context is just the full name.
     */
    public String getFullNameInContext(Context context) {
        return getFullName();
    }
    

    protected Type createType() {
        return new ExternalType(this);
    }

//   protected Definition getExplicitChildDefinition(NameNode node) {
//       Definition def = getDefinitionTable().getDefinition(getFullName(), node);
//
//
//   }

    public List<Dim> getDims() {
        if (dims == null) {
            Class<?> c = getExternalClass();
            
            if (c == null) {
                dims = new EmptyList<Dim>();

            // this needs to be fixed to handle fixed size arrays properly, and
            // also to handle arrays of maps
            } else if (c.isArray() || List.class.isAssignableFrom(c) || CantoArray.class.isAssignableFrom(c)) {

                // this needs to be fixed to handle fixed size arrays properly
                int ndims = 0;
                if (c.isArray()) {
                    String className = c.getName();
                    while (className.charAt(ndims) == '[') {
                        ndims++;
                    }
                } else {
                    ndims = 1;
                }
                
                dims = new ArrayList<Dim>(ndims);
                for (int i = 0; i < ndims; i++) {
                    Dim d = new Dim();
                    dims.add(d);
                }
                
            // this needs to be fixed to handle multidimensional maps            
            } else if (Map.class.isAssignableFrom(c)) {
                Dim dim = new Dim(Dim.TYPE.INDEFINITE, 0);
                dim.setTable(true);
                dims = new ArrayList<Dim>(1);
                dims.add(dim);
                
            } else {
                dims = new EmptyList<Dim>();
            }
        }
        return dims;
    }

    protected void setDims(List<Dim> dims) {
        this.dims = dims;
    }

    public String getExternalTypeName() {
        if (c == null) {
            return "Definition";
        } else {
            return c.getName();
        }
    }
    
    public Class<?> getExternalClass(Context context) {
        Class<?> clazz = getExternalClass();
        if (clazz == null) {
            if (context != null) {
                System.out.println("no external class for " + getName());
            }
            if (clazz == null) {
                clazz = Definition.class;
            }
        } else if (clazz == Void.class || clazz == Void.TYPE) {
            clazz = Object.class;
        }
        return clazz;
    }

    public Class<?> getExternalClass() {
        return c;
    }

    public Class<?> getInstanceClass(Context context) {
        return c;
    }

    protected void setExternalClass(Class<?> c) {
        this.c = c;
    }

    public Object getObject() {
        return object;
    }

    protected void setObject(Object object) {
        this.object = object;
    }

    public ArgumentList getArguments() {
        return args;
    }

    protected void setArguments(ArgumentList args) {
        this.args = args;
    }

    /** Returns true. */
    public boolean hasChildDefinitions() {
        return true;
    }

    public Definition getExplicitChildDefinition(NameNode node) {
        return getExternalChildDefinition(node, null);
    }
    
    public ExternalDefinition getExternalChildDefinition(NameNode node, Context context) {
        if (context == null) {
            if (initContext == null) {
                return null;
            }
            context = initContext;
        }
        try {
            DefinitionInstance defInstance = (DefinitionInstance) getChild(node, node.getArguments(), node.getIndexes(), null, context, false, true, null, null);
            return (ExternalDefinition) (defInstance == null ? null : defInstance.def);
        } catch (Redirection r) {
            log("Unable to find definition for " + node.getName() + " in external definition " + getFullName());
            return null;
        }
    }

    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context context, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {

        ComplexName restOfName = null;
        int numNameParts = node.numParts();
        if (numNameParts > 1) {
            restOfName = new ComplexName(node, 1, numNameParts);
            node = node.getFirstPart();
            DefinitionInstance defInstance = (DefinitionInstance) getChild(node, node.getArguments(), node.getIndexes(), parentArgs, context, false, trySuper, parentObj, resolver);
            if (defInstance != null && defInstance.def != null) {
                return defInstance.def.getChild(restOfName, args, indexes, parentArgs, context, generate, trySuper, null, resolver);
            } else {
                return (generate ? UNDEFINED : null);
            }
        }
        
        String name = node.getName();
        Class<?>[] params = null;
        Definition paramDef = null;
        int numArgs = args != null ? args.size() : 0;
        if (numArgs > 0) {
            params = new Class[numArgs];
            for (int i = 0; i < numArgs; i++) {
                Object obj = args.get(i);
                Type paramType = null;
                
                if (obj instanceof Definition) {
                    paramDef = (Definition) obj;
                } else if (obj instanceof Instantiation) {
                    Instantiation instance = (Instantiation) obj;
                    int numUnpushes = 0;
                    try {
                        if (instance.isParameterKind()) {
                            Definition paramOwner = instance.getOwner();
                            Context.Entry entry = context.peek();
                            while (!entry.covers(paramOwner)) {
                                Context.Entry link = entry.getPrevious();
                                if (link == null || link.equals(context.getRootEntry())) {
                                    while (numUnpushes-- > 0) {
                                        context.repush();
                                    }
                                    break;
                                }
                                numUnpushes++;
                                entry = link;
                                context.unpush();
                            }
                        }
                        paramDef = instance.getDefinition(context);
                        if (paramDef != null) {
                            paramType = paramDef.getType();
                        }
                        
                    } finally {
                        while (numUnpushes-- > 0) {
                            context.repush();
                        }
                    }
                } else {
                    paramDef = null;
                }
                
//                if (paramDefs[i] != null) {
//                    Definition udef = paramDefs[i].getUltimateDefinition(context);
//                    // this comparison purposefully avoids using equals() so that it will
//                    // catch proxies such as AliasedDefinition designed to spoof equals()
//                    if (paramDefs[i] != udef) {
//                        paramDefs[i] = udef;
//                        params[i] = udef.getType().getTypeClass(context);
//                    }
//                }
                if (params[i] == null) {
                    
                    if (paramType != null) {
                        params[i] = paramType.getTypeClass(context);
                    
                    } else if (obj instanceof Value) {
                        params[i] = ((Value) obj).getValueClass();
                    } else if (obj instanceof Construction) {
                        paramType = ((Construction) obj).getType(context, generate);
                        if (paramType == null) {
                            params[i] = Object.class;
                        } else {
                            params[i] = paramType.getTypeClass(context);
                        }
                    } else if (obj instanceof ValueGenerator) {
                        try {
                            Value value = ((ValueGenerator) obj).getValue(context);
                            params[i] = value.getValueClass();
                        } catch (Redirection r) {
                            params[i] = Object.class;
                        }
                    } else {
                        params[i] = Object.class;
                    }
                }
            }
        }

        Method method = null;
        Class<?> clazz = (parentObj != null ? parentObj.getClass() : getInstanceClass(context));
        if (clazz == null) {
            return (generate ? UNDEFINED : null);
        }
        
        try {
            method = clazz.getMethod(name, params);

        } catch (NoSuchMethodException nsme) {
            method = getClosestMethod(name, params, clazz);
            if (method != null) {
                // unfortunate that this causes a clone of the paramTypes array
                Class<?>[] mParams = method.getParameterTypes();
                if (mParams.length > numArgs && Context.class.isAssignableFrom(mParams[0])) {
                    ArgumentList newArgs = new ArgumentList(numArgs + 1);
                    newArgs.add(new PrimitiveValue(context));
                    if (numArgs > 0) {
                        newArgs.addAll(args);
                    }
                    args = newArgs;
                }
                
            } else {    
//               vlog("No method " + name + " in class " + clazz.getName());
            }

        } catch (Exception e) {
            vlog("Exception finding method " + name + " in class " + clazz.getName() + ": " + e);
            method = null;
        }
        if (method != null) {
            MethodDefinition mdef = null;
            ExternalDefinition ownerInContext = this;  //new ExternalDefinition(this, context, getArguments()); 
            if (node.hasIndexes()) {
                mdef = new IndexedMethodDefinition(ownerInContext, method, args, node.getIndexes());
            } else {
                mdef = new MethodDefinition(ownerInContext, method, args);
            }
            if (parentObj != null) {
                mdef.setObject(parentObj);
            }

//           AbstractNode contents = mdef.getContents();
//           if (contents instanceof ExternalConstruction) {
//               try {
//                   ((ExternalConstruction) contents).initExternalObject(context);
//               } catch (Redirection r) {
//                   log("Initialization of " + mdef.getFullName() + " failed: " + r.getMessage());
//                   mdef = null;
//               }
//           }

            if (generate) {
                Object data = null;
                AbstractNode contents = mdef.getContents();
                if (contents instanceof Chunk) {
                    try {
                        data = ((Chunk) contents).getData(context);
                        if (data instanceof Value) {
                            data = ((Value) data).getValue();
                        } else if (data instanceof AbstractNode) {
                            initNode((AbstractNode) data);
                        }
                    } catch (Redirection r) {
                        ;
                    }
                }
                return data;
            } else {
                return mdef.getDefInstance(args, indexes);
            }
        }

        // not a method; look for a field (providing there are no arguments)
        if (args == null || args.size() == 0) {
            Field field = null;
            try {
                field = clazz.getField(name);

            } catch (NoSuchFieldException nsme) {
//               vlog("No field " + name + " in class " + clazz.getName());
                
                // no explicit method or field by the specified name.  Look
                // for a special collection name:
                //     -- if the name is "count", create a count definition
                //     -- if the name is "keys", create a keys definition
                if (Name.COUNT.equals(name)) {
                    Definition countDef = new CountDefinition(this);  // , context, getArguments());
                    if (generate) {
                        return countDef.instantiate(context);
                    } else {
                        return countDef.getDefInstance(null, null);
                    }
                } else if (Name.KEYS.equals(name)) {
                    ExternalCollectionDefinition collectionDef = new ExternalCollectionDefinition(this, context, getArguments(), java.util.Map.class);
                    Definition keysDef = new KeysDefinition(collectionDef, context, null, null);
                    if (generate) {
                        return keysDef.instantiate(context);
                    } else {
                        return keysDef.getDefInstance(null, null);
                    }
                } else if (parentObj instanceof ResolvedInstance) {
                    ResolvedInstance ri = (ResolvedInstance) parentObj;
                    Definition parentDef = ri.getDefinition();
                    return parentDef.getChild(node, args, indexes, parentArgs, ri.getResolutionContext(), generate, trySuper, parentObj, resolver);

                } else {
                    field = null;
                }

            } catch (Exception e) {
                vlog("Exception finding field " + name + " in class " + clazz.getName() + ": " + e);
                field = null;
            }
            if (field != null) {
                ExternalDefinition ownerInContext = new ExternalDefinition(this, context, getArguments()); 
                Definition fieldDef = new FieldDefinition(ownerInContext, field);
                return fieldDef.getDefInstance(null, indexes);
            }
        }

        return null;
    }
    
    public int getObjectSize() {
        Object obj = getObject();
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return java.lang.reflect.Array.getLength(obj);
            
        } else if (obj instanceof CantoArray) {
            return ((CantoArray) obj).getSize();

        } else if (obj instanceof List<?>) {
            return ((List<?>) obj).size();
            
        } else if (obj instanceof Map<?,?>) {
            return ((Map<?,?>) obj).size();

        } else {
            return 1;
        }
    }
    
    
    static boolean  isCollectionClass(Class<?> clazz) {
         return (clazz.isArray() || 
                 CantoArray.class.isAssignableFrom(clazz) ||
                 List.class.isAssignableFrom(clazz) ||
                 Map.class.isAssignableFrom(clazz));
    }
    
    static Constructor<?> getClosestConstructor(Class<?> c, Class<?>[] params, Definition[] paramDefs) {
        Constructor<?>[] constructors = c.getConstructors();
        Constructor<?> closest = null;
        int score = NO_MATCH;

        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> ctor = constructors[i];
            Class<?>[] cParams = ctor.getParameterTypes();
            int cplen = (cParams == null ? 0 : cParams.length);
            int plen = (params == null ? 0 : params.length);
            if (cplen == 0 && plen == 0) {
                // no parameters; no need to calculate score
                return ctor;

            } else if (cplen == plen) {
                int s = calcParamScore(cParams, 0, params);
                if (s < score) {
                    closest = ctor;
                    score = s;
                }
            }
        }
        return closest;
    }

    static Method getClosestMethod(String name, Class<?>[] params, Class<?> c) {
        Method[] methods = c.getMethods();
        Method closest = null;
        int score = NO_MATCH;

        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getName().equals(name)) {
                Class<?>[] mParams = m.getParameterTypes();
                int mplen = (mParams == null ? 0 : mParams.length);
                int plen = (params == null ? 0 : params.length);
                if (mplen == 0 && plen == 0) {
                    // no parameters; no need to calculate score
                    return m;

                } else if (mplen == plen) {
                    int s = calcParamScore(mParams, 0, params);
                    if (s < score) {
                        closest = m;
                        score = s;
                    }

                } else if (mplen == plen + 1 && Context.class.isAssignableFrom(mParams[0])) {
                    int s = calcParamScore(mParams, 1, params);
                    if (s < score) {
                        closest = m;
                        score = s;
                    }
                }
            }
        }
        return closest;
    }

    static private int calcParamScore(Class<?>[] params, int startIx, Class<?>[] args) {
        // this has the effect of penalizing a point for an assumed first parameter
        int score = startIx;
        int len = (args == null ? 0 : args.length);
        for (int i = 0; i < len; i++) {
            Class<?> gen = params[i + startIx];
            Class<?> spec = args[i];
            if (spec == null) {
                spec = Object.class;
            }
            
            if (gen.equals(spec)) {
                continue;
            }
            
            if (isCorrespondingPrimitive(gen, spec)) {
                score++;
            
            // check for numeric conversions
            } else if (isNumeric(gen) && isNumeric(spec)) {
                score += 2;
                
            // if the parameter class is String, then anything can satisfy it
            // Note that the arg class cannot also be String, since equality was
            // checked above.  We don't want this to supersede a different 
            // param list that matches fairly closely, so add a somewhat 
            // arbitrary number to score.
            } else if (gen == String.class) {
                score += 100;

            } else if (!gen.isAssignableFrom(spec)) {
                // String and Object args might by dynamic (untyped) args 
                if (spec.equals(String.class) || spec.equals(Object.class)) {
                    return QUESTIONABLE_MATCH;
                } else {
                    return NO_MATCH;
                }

            } else if (gen.isInterface()) {
                int n = calcInterfaceScore(gen, spec);
                if (n >= NO_MATCH) {
                    return n;
                } else {
                    score += n;
                }

            } else {
                while (gen.isArray() && spec.isArray()) {
                    gen = gen.getComponentType();
                    spec = spec.getComponentType();
                }
                while (!gen.equals(spec)) {
                    spec = spec.getSuperclass();
                    if (spec == null) {
                        if (gen == Object.class) {
                            return QUESTIONABLE_MATCH;
                        } else {
                            return NO_MATCH;
                        }
                    }
                    score++;
                }
            }
        }
        return score;
    }

    static private int calcNumericScore(Class<?> gen, Class<?> spec) {
        if (gen.equals(Boolean.class) || gen.equals(Boolean.TYPE)) {
            if (spec.equals(Boolean.class) || spec.equals(Boolean.TYPE)) {
                return 0;
            }
        } else if (gen.equals(Byte.class) || gen.equals(Byte.TYPE)) {
            if (spec.equals(Byte.class) || spec.equals(Byte.TYPE)) {
                return 0;
            }
        } else if (gen.equals(Character.class) || gen.equals(Character.TYPE)) {
            if (spec.equals(Character.class) || spec.equals(Character.TYPE)) {
                return 0;
            }
        } else if (gen.equals(Double.class) || gen.equals(Double.TYPE)) {
            if (spec.equals(Double.class) || spec.equals(Double.TYPE)) {
                return 0;
            }
        } else if (gen.equals(Float.class) || gen.equals(Float.TYPE)) {
            if (spec.equals(Float.class) || spec.equals(Float.TYPE)) {
                return 0;
            }
        } else if (gen.equals(Long.class) || gen.equals(Long.TYPE)) {
            if (spec.equals(Long.class) || spec.equals(Long.TYPE)) {
                return 0;
            }
        } else if (gen.equals(Integer.class) || gen.equals(Integer.TYPE)) {
            if (spec.equals(Integer.class) || spec.equals(Integer.TYPE)) {
                return 0;
            }
        }
        return 1;
    }
    
    static private boolean isCorrespondingPrimitive(Class<?> c1, Class<?> c2) {
        if (((c1 == Character.TYPE || c1 == Character.class) && (c2 == Character.TYPE || c2 == Character.class))
             || ((c1 == Byte.TYPE || c1 == Byte.class) && (c2 == Byte.TYPE || c2 == Byte.class))
             || ((c1 == Integer.TYPE || c1 == Integer.class) && (c2 == Integer.TYPE || c2 == Integer.class))
             || ((c1 == Short.TYPE || c1 == Short.class) && (c2 == Short.TYPE || c2 == Short.class))
             || ((c1 == Long.TYPE || c1 == Long.class) && (c2 == Long.TYPE || c2 == Long.class))
             || ((c1 == Float.TYPE || c1 == Float.class) && (c2 == Float.TYPE || c2 == Float.class))
             || ((c1 == Double.TYPE || c1 == Double.class) && (c2 == Double.TYPE || c2 == Double.class))) {
            
            return true;
        } else {
            return false;
        }
    }

    /** Returns true if the specified class represents a numeric type.  In Canto,
     *  boolean is considered numeric.
     */
    static private boolean isNumeric(Class<?> c) {
        if (c.isPrimitive() && !c.equals(Void.TYPE)) {
            return true;
        } else if (Number.class.isAssignableFrom(c)) {
            return true;
        } else if (c.equals(Boolean.class)) {
            return true;
        } else {
            return false;
        }
    }

    static private int calcInterfaceScore(Class<?> gen, Class<?> spec) {
        Class<?>[] interfaces = spec.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (gen.equals(interfaces[i])) {
                return 0;
            } else if (gen.isAssignableFrom(interfaces[i])) {
                return (1 + calcInterfaceScore(gen, interfaces[i]));
            }
        }
        return NO_MATCH;
    }

    public void setModifiers(int access, int dur) {
        setAccess(access);
        setDurability(dur);
    }

    protected ExternalConstruction getConstructor() {
        return new ExternalConstruction(this);
    }

    public Definition getDefForContext(Context context, ArgumentList args) throws Redirection {
        Definition def = this;
        if (!context.isCompatible(initContext)) {
            Class<?> c = getExternalClass(context);
            
            args = context.getUltimateArgs(args);
            if (ExternalCollectionDefinition.isConstructibleFrom(c)) {
                def = new ExternalCollectionDefinition(this, context, args, c);
            } else {
                def = new ExternalDefinition(this, context, args);
            }
            initNode((AbstractNode) def);
        }
        return def;
    }

    public List<ParameterList> getParamLists() {
        List<ParameterList> paramLists = super.getParamLists();
        if (paramLists == null) {
            paramLists = generateParamLists();
            setParamLists(paramLists);
        }
        return paramLists;
    }

    protected List<ParameterList> generateParamLists() {
        Class<?> c = getExternalClass();
        List<ParameterList> paramLists = null;
        if (c != null) {
            Constructor<?>[] constructors = c.getConstructors();
            paramLists = Context.newArrayList(constructors.length, ParameterList.class);
            for (int i = 0; i < constructors.length; i++) {
                Class<?>[] paramTypes = constructors[i].getParameterTypes();
                List<DefParameter> params = Context.newArrayList(paramTypes.length, DefParameter.class);
                for (int j = 0; j < paramTypes.length; j++) {
                    params.add(new DefParameter(paramTypes[j]));
                }
                paramLists.add(new ParameterList(params));
            }
        } else {
            paramLists = new EmptyList<ParameterList>();
        }
        return paramLists;
    }

    public Object generateInstance(Context context) throws Redirection {
       
        Object object = null;
        int numUnpushes = 0;
        int numPushes = 0;
        try {
            ExternalDefinition owner = (ExternalDefinition) getOwner();
            ArgumentList ownerArgs = owner.getArguments();
            Context ownerContext = owner.getInitContext();
            if (ownerContext == null) {
                //numPushes = context.pushSupersAndAliases(owner, this, getArguments());
                ownerContext = context;
            }
           
            List<Index> ownerIndexes = null;
            if (owner instanceof IndexedMethodDefinition) {
                ownerIndexes = ((IndexedMethodDefinition) owner).getIndexes();
            }
            AbstractConstruction.CacheabilityInfo cacheInfo = AbstractConstruction.NOT_CACHEABLE_INFO;
            AbstractNode contents = owner.getContents();
            if (contents instanceof AbstractConstruction) {
                cacheInfo = ((AbstractConstruction) contents).getCacheability(context, null);
                if ((cacheInfo.cacheability & AbstractConstruction.CACHE_RETRIEVABLE) == AbstractConstruction.CACHE_RETRIEVABLE) {
                    object = context.getData(owner, owner.getName(), ownerArgs, ownerIndexes);
                }
            }

            if (object == null) {
                 object = getObject();
                 if (object instanceof Value) {
                     object = ((Value) object).getValue();

                 } else if (object instanceof ValueGenerator) {
                     object = ((ValueGenerator) object).getData(ownerContext);

                 } else if (object instanceof Value) {
                     object = ((Value) object).getValue();
                 }
                 if ((cacheInfo.cacheability & AbstractConstruction.CACHE_STORABLE) == AbstractConstruction.CACHE_STORABLE) {
                     context.putData(owner, ownerArgs, ownerIndexes, owner.getName(), object);
                 }
            }
            if (object instanceof Class<?>) {
                object = owner.getConstructor().generateData(ownerContext, cacheInfo.def);
            } else if (object instanceof CollectionDefinition && !owner.getExternalClass(ownerContext).isAssignableFrom(object.getClass())) {
                object = ((CollectionDefinition) object).getCollectionInstance(context, ownerArgs, ownerIndexes);
            }
        } catch (Exception e) {
            String message = "Exception initializing external method: " + e;
            log(message);
            message = "Context:\n" + context.toString();
            log(message);
            e.printStackTrace();
            throw new Redirection(Redirection.STANDARD_ERROR, message);
        } finally {
            if (numPushes > 0) {
                if (numUnpushes == 0) {
                    context.unpush();
                    numUnpushes = 1;
                }
                while (numPushes > 0) {
                    context.pop();
                    numPushes--;
                }
            }
            if (numUnpushes > 0) {
                context.repush();
            }
        }
        return object;
    }
 
}

class PartialDefinition extends ExternalDefinition {

    protected NameNode[] nameParts;
    protected ExternalDefinition baseDef;

    public PartialDefinition(ExternalDefinition externalDef, NameNode[] parts) {
        super(externalDef);
        if (externalDef instanceof PartialDefinition) {
            baseDef = ((PartialDefinition) externalDef).baseDef;
        } else {
            baseDef = externalDef;
        }
        nameParts = parts;
    }

    public PartialDefinition(ExternalDefinition externalDef, ArgumentList args, NameNode[] parts) {
        super(externalDef, args);
        if (externalDef instanceof PartialDefinition) {
            baseDef = ((PartialDefinition) externalDef).baseDef;
        } else {
            baseDef = externalDef;
        }
        nameParts = parts;
    }

    public ExternalDefinition newForArgs(ArgumentList args) {
        return new PartialDefinition(this, args, nameParts);
    }

    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        Definition def = completeForContext(argContext);
        if (def == null) {
            return (generate ? null : UNDEFINED);
        } else {
            return def.getChild(node, args, indexes, parentArgs, argContext, generate, trySuper, parentObj, resolver);
        }
    }


    ExternalDefinition completeForContext(Context context) {
        ExternalDefinition externalDef = baseDef;
        for (int i = 0; i < nameParts.length; i++) {
            ExternalDefinition childDef = externalDef.getExternalChildDefinition(nameParts[i], context);
            if (childDef == null) {
                vlog("No " + nameParts[i].getName() + " belonging to external definition " + externalDef.getFullName());
                return null;
            }
            externalDef = childDef;
        }
        return externalDef;
    }
}


class ExternalConstruction extends AbstractConstruction implements ValueGenerator {

    private ExternalDefinition externalDef;

    // context-specific fields
    private Constructor<?> constructor = null;
    private ExternalDefinition def = null;
    protected Object[] argObjects = null;
    protected Object contextMarker = null;

    public ExternalConstruction(ExternalDefinition def) {
        externalDef = def;
    }

    public ExternalDefinition getExternalDefinition() {
        return externalDef;
    }
    
    public Object[] getArgObjects() {
        return argObjects;
    }

    /** Returns the name of the definition being constructed. */
    public String getDefinitionName() {
        return externalDef.getName();
    }

    public ExternalConstruction initExternalObject(Context context, ArgumentList instanceArgs) throws Redirection {
        // a hack, but one that works because external defs are instantiated at runtime
        // per call rather than once at compile time
        if (context.equalsOrPrecedes(contextMarker)) {
            return this;
        }
        int numExtraUnpushes = 0;
        int numUnpushes = 0;
        boolean unpushed = false;
       
        try {
            constructor = null;
            def = externalDef;
            if (externalDef instanceof PartialDefinition) {
                def = ((PartialDefinition) externalDef).completeForContext(context);
            }

            String name = def.getFullName();
            vlog("Initializing external object " + name);

            ArgumentList args = def.getArguments();
            if (instanceArgs != null && instanceArgs.isDynamic()) {
                args = instanceArgs;
            }
            int numArgs = (args != null ? args.size() : 0);

            // don't count trailing MISSING_ARGs
            for (int i = numArgs - 1; i >= 0; i--) {
                if (args.get(i) == ArgumentList.MISSING_ARG) {
                    numArgs--;
                } else {
                    break;
                }
            }
            
            Class<?>[] params = new Class[numArgs];
            Definition[] paramDefs = new Definition[numArgs];
    
            argObjects = new Object[numArgs];
    
            while (context.peek().def.isExternal() && context.size() > 1) {
                context.unpush();
                numExtraUnpushes++;
            }
       
            if (context.peek().def.equals(externalDef) && context.size() > 1) {
                context.unpush();
                unpushed = true;
            } else {
                vlog("***> external def not at top of stack in initExternalObject");
            }
    
            for (int i = 0; i < numArgs; i++) {
                Object arg = args.get(i);
                if (arg == ArgumentList.MISSING_ARG) {
                    arg = null;
                }
                if (arg instanceof Instantiation && context.size() > 1) {
                    Definition argOwner = ((Instantiation) arg).getOwner();
                    for (Context.Entry entry = context.peek(); !(entry.covers(argOwner)); entry = context.peek()) {
                        numUnpushes++;
                        context.unpush();
                        if (context.size() == 1) {
                            if (context.peek().def != argOwner) {
                                while (numUnpushes > 0) {
                                    context.repush();
                                    numUnpushes--;
                                }
                            }
                            break;
                        }
                    }
                }
               
                if (arg instanceof Definition) {
                    paramDefs[i] = (Definition) arg;
                } else if (arg instanceof Instantiation) {
                    paramDefs[i] = ((Instantiation) arg).getDefinition(context);
                } else {
                    paramDefs[i] = null;
                }
               
                Value val = null;
                if (arg instanceof Value) {
                    val = (Value) arg;
                } else if (arg instanceof ValueGenerator) {
                    val = ((ValueGenerator) arg).getValue(context);
                } else {
                    throw new Redirection(Redirection.STANDARD_ERROR, "Argument " + i + " for external object " + def.getName() + " is illegal");
                }

                argObjects[i] = val.getValue();
                params[i] = val.getValueClass();
               
                while (numUnpushes > 0) {
                    context.repush();
                    numUnpushes--;
                }
            }
            Class<?> instanceClass = def.getInstanceClass(context);
            try {
                constructor = instanceClass.getConstructor(params);
            } catch (NoSuchMethodException nsme) {
                constructor = ExternalDefinition.getClosestConstructor(instanceClass, params, paramDefs);
                if (constructor == null) {
                    vlog("No constructor found for class " + instanceClass.getName());
                }
            }

        } catch (Exception e) {
            String message = "Exception initializing external object " + def.getName() + ": " + e.toString();
            log(message);
            String contextMsg = "Context:\n" + context.toString();
            log(contextMsg);
            e.printStackTrace();
            throw new Redirection(Redirection.STANDARD_ERROR, message);
        } finally {
            while (numUnpushes-- > 0) {
                context.repush();
            }
            if (unpushed) {
                context.repush();
            }
            while (numExtraUnpushes-- > 0) {
                context.repush();
            }
        }

        contextMarker = context.getMarker(contextMarker);
        return this;
    }
   
    protected CacheabilityInfo getCacheability(Context context, Definition def) {
        int cacheability;
        if (def == null) {
            def = getExternalDefinition();
        }
        if (def == null) {
            return NOT_CACHEABLE_INFO;
        } else if (def.getName().equals("cache") && def.getOwner().getName().equals("here")) {
            return NOT_CACHEABLE_INFO;
        } else if (def.getDurability() == Definition.DYNAMIC) {
            cacheability = CACHE_STORABLE;
        } else {
            ArgumentList args = (def instanceof ExternalDefinition ? ((ExternalDefinition) def).getArguments() : getArguments());
            if (args != null && args.isDynamic()) {
                cacheability = CACHE_STORABLE;
            } else {
                cacheability = FULLY_CACHEABLE;
            }
        }

// removed to match how Instantiation works
//        
//        ArgumentList args = def.getArguments();
//        if (args != null) {
//            Iterator<Construction> it = args.iterator();
//            while (it.hasNext()) {
//                Construction arg = it.next();
//                if (arg instanceof AbstractConstruction) {
//                    int argCacheability = ((AbstractConstruction) arg).getCacheability(context).cacheability;
//                    cacheability &= argCacheability;
//   
//                    // if not cacheable, no need to go further
//                    if (cacheability == NOT_CACHEABLE) {
//                        return NOT_CACHEABLE_INFO;
//                    }
//    
//                } else {
//                    return NOT_CACHEABLE_INFO;
//                }
//            }
//        }

        return new CacheabilityInfo(cacheability, def);
    }

    public Object generateData(Context context, Definition def) throws Redirection {

        Object obj = initExternalObject(context, null);
        if  (obj != this) {
            return ((ExternalConstruction) obj).generateData(context, def);
        }

        String name = def.getFullName();
        vlog("Generating data for external object " + name);

//       context.unpush();
        try {
            if (constructor != null) {
                return constructor.newInstance(argObjects);
            } else {
                return null;
            }

        } catch (Exception e) {
            String message = "Exception constructing external object " + def.getName() + ": " + e.toString();
            log(message);
            throw new Redirection(Redirection.STANDARD_ERROR, message);
        } finally {
//           context.repush();
        }
    }

    public String getString(Context context) {
        try {
            return getValue(context).getString();
        } catch (Redirection r) {
            return "";
        }
    }

    public boolean getBoolean(Context context) {
        try {
            return getValue(context).getBoolean();
        } catch (Redirection r) {
            return false;
        }
    }

    public byte getByte(Context context) {
        try {
            return getValue(context).getByte();
        } catch (Redirection r) {
            return (byte) 0;
        }
    }

    public char getChar(Context context) {
        try {
            return getValue(context).getChar();
        } catch (Redirection r) {
            return (char) 0;
        }
    }

    public int getInt(Context context) {
        try {
            return getValue(context).getInt();
        } catch (Redirection r) {
            return 0;
        }
    }

    public long getLong(Context context) {
        try {
            return getValue(context).getLong();
        } catch (Redirection r) {
            return 0L;
        }
    }

    public double getDouble(Context context) {
        try {
            return getValue(context).getDouble();
        } catch (Redirection r) {
            return 0.0d;
        }
    }
   
    public String toString(String prefix) {
        return toString(prefix, getExternalDefinition());
    }
      
}

class MethodDefinition extends ExternalDefinition {
    private Method method;

    public MethodDefinition(ExternalDefinition owner, Method method, ArgumentList args) {
        super();
        this.method = method;

        setOwner(owner);
        setObject(owner.getContents());
        setAccess(owner.getAccess());
        setDurability(DYNAMIC);
        setName(new NameWithArgs(method.getName(), args));
        setArguments(args);
        setExternalClass(method.getReturnType());
        setType(createType());
        setContents(createConstruction());
    }

    protected ExternalConstruction createConstruction() {
        return new MethodConstruction(this);
    }

    protected List<ParameterList> generateParamLists() {
        List<ParameterList> paramLists = null;
        if (method != null) {
            paramLists = Context.newArrayList(1, ParameterList.class);
            Class<?>[] paramTypes = method.getParameterTypes();
            List<DefParameter> params = Context.newArrayList(paramTypes.length, DefParameter.class);
            ArgumentList args = getArguments();
            int numArgs = (args != null ? args.size() : 0);
            int j = 0;
            if (paramTypes.length > numArgs) {
                params.add(new DefParameter(paramTypes[j++]));
            }
            
            for (int i = 0; i < numArgs; i++) {
                Construction arg = args.get(i);
                if (arg != null && arg instanceof Instantiation) {
                    params.add(new DefParameter(((Instantiation) arg).getReferenceName(), paramTypes[j++]));
                } else {
                    params.add(new DefParameter(paramTypes[j++]));
                }
            }
            paramLists.add(new ParameterList(params));
        } else {
            paramLists = new EmptyList<ParameterList>();
        }
        return paramLists;
    }

    protected Method getMethod() {
        return method;
    }

    public Definition getDefForContext(Context context, ArgumentList args) throws Redirection {
        MethodConstruction mc = (MethodConstruction) getContents();
        mc.initExternalObject(context, args);
        return this;
    }
    
    public Object getChild(NameNode node, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        if (parentObj == null && generate == true) {
            ExternalConstruction construction = (ExternalConstruction) getContents();
            if (construction != null) {
                parentObj = construction.generateData(argContext, this);
            }
        }
        if (parentObj instanceof CantoObjectWrapper) {
            return ((CantoObjectWrapper) parentObj).getChild(node, args, indexes, parentArgs, generate, trySuper, null, resolver);
        } else {
            return super.getChild(node, args, indexes, parentArgs, argContext, generate, trySuper, parentObj, resolver);
        }
    }
    
    
}


class MethodConstruction extends ExternalConstruction {

    // context-specific fields
    private Method method = null;
    private Object instance = null;

    public MethodConstruction(MethodDefinition def) {
        super(def);
    }

    protected MethodDefinition getMethodDefinition() {
        return (MethodDefinition) getExternalDefinition();
    }

    public ExternalConstruction initExternalObject(Context context, ArgumentList args) throws Redirection {
        // a hack, but one that works because external defs are instantiated at runtime
        // per call rather than once at compile time
        if (context.equalsOrPrecedes(contextMarker)) {
            return this;
        }
    
        instance = null;
        MethodDefinition mdef = getMethodDefinition();
        method = mdef.getMethod();
        Class<?>[] paramTypes = method.getParameterTypes();
 
        // the args parameter is ignored in favor of the arguments specified in the
        // method definition
        args = mdef.getArguments();
        int numArgs = (args != null ? args.size() : 0);
        argObjects = new Object[numArgs];
        for (int i = 0; i < numArgs; i++) {
            Object arg = args.get(i);

            if (arg instanceof Value) {
                argObjects[i] = ((Value) arg).getValue();
            
            // dereference value generators
            } else if (arg instanceof ValueGenerator) {
                Object argVal = ((ValueGenerator) arg).getData(context);
                if ((argVal == null || argVal.equals(NullValue.NULL_VALUE)) && arg instanceof CantoNode) {
                    Definition argOwner = ((CantoNode) arg).getOwner();
                    int numUnpushes = 0;
                    try {
                        for (Context.Entry entry = context.peek(); context.size() > 1 && !(entry.covers(argOwner)); entry = context.peek()) {
                            numUnpushes++;
                            context.unpush();
                        }
                        if (numUnpushes > 0) {
                            argVal = ((ValueGenerator) arg).getData(context);
                        }
                    } catch (Throwable t) {
                        String message = "Unable to initialize argument for external method: " + t.toString();
                        log(message);
                       
                    } finally {
                        while (numUnpushes-- > 0) {
                            context.repush();
                        }
                    }
                }
//                if (argVal instanceof CantoObjectWrapper) {
//                    argVal = ((CantoObjectWrapper) argVal).getData();
//                    if (argVal instanceof CantoObjectWrapper) {
//                        argVal = null;
//                    }
//                }
                arg = argVal;
            }
           
            // dereference Value again in case the ValueGenerator yielded a Value
            if (arg instanceof Value) {
                argObjects[i] = ((Value) arg).getValue();

            // dereference collections represented as CantoArray objects
            } else if (arg instanceof CantoArray) {
                argObjects[i] = ((CantoArray) arg).instantiateArray(context);
            
            // dereference resolved collections
            } else if (arg instanceof ResolvedCollection) {
                argObjects[i] = ((ResolvedCollection) arg).getCollectionObject();
                
            } else if (arg instanceof CantoObjectWrapper) {
                argObjects[i] = ((CantoObjectWrapper) arg).getData();
 
            } else {
                argObjects[i] = arg;
            } 

            // convert array to list or vice versa if necessary
            if (paramTypes[i].isArray() && argObjects[i] instanceof List<?>) {
                argObjects[i] = ((List<?>) argObjects[i]).toArray();
            } else if (argObjects[i] instanceof Object[] && List.class.isAssignableFrom(paramTypes[i])) {
                argObjects[i] = Arrays.asList((Object[]) argObjects[i]);
            }
            
            // special handling for collections: instantiate elements if necessary
            argObjects[i] = instantiateElements(argObjects[i], context);

            // cache instantiated arrays
            if (args.get(i) instanceof AbstractConstruction && CollectionDefinition.isCollectionObject(argObjects[i])) {
                AbstractConstruction argInstance = (AbstractConstruction) args.get(i);
                String name = argInstance.getDefinitionName();
                ArgumentList argArgs = argInstance.getArguments();
                Definition argDef = context.getDefinition(name, null, argArgs);
                if (argDef == null) {
                    argDef = argInstance.getDefinition(context);
                }
                if (argDef != null) {
                    context.putData(argDef, argArgs, argInstance.getIndexes(), name, argObjects[i]);
                }
            }

            if (argObjects[i] == null) {
                argObjects[i] = getNullForClass(paramTypes[i]);
            }
        }
        if (Modifier.isStatic(method.getModifiers())) {
            instance = null;
        } else {
            instance = mdef.generateInstance(context);
        }
        
        contextMarker = context.getMarker(contextMarker);
        return this;
    }

    /** Instantiate any uninstantiated constructions among the elements in a 
     *  collection, and return the collection.  If the passed object is not a 
     *  collection, do nothing and return the passed object.
     *  
     *  If any elements are collections, recursively instantiate their elements.
     *  
     *  In the current implementation, instantiated elements replace the
     *  original elements in the collection, so the original collection is
     *  returned.  This might change at some point, and this method may return
     *  a copy of the original collection with instantiated elements in place
     *  of any constructions. 
     *   
     * @param obj
     * @param context
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Object instantiateElements(Object obj, Context context) throws Redirection {
        if (obj instanceof Value) {
            obj = ((Value) obj).getValue();
        }
        
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof Object[]) {
           Object[] array = (Object[]) obj;
           for (int i = 0; i < array.length; i++) {
               Object element = array[i];
               if (element instanceof Chunk) {
                   element = ((Chunk) element).getData(context);
               }
               element = instantiateElements(element, context);
               
               array[i] = element;
           }
           return array;
        } else if (obj instanceof List<?>) {
           List<Object> list = (List<Object>) obj;
           int len = list.size();
           for (int i = 0; i < len; i++) {
               Object element = list.get(i);
               if (element instanceof Chunk) {
                   element = ((Chunk) element).getData(context);
               }
               element = instantiateElements(element, context);
               
               list.set(i, element);
           }
           return list;
        } else if (obj instanceof Map<?,?>) {
            Map<Object, Object> map = (Map<Object, Object>) obj;
            Set<Object> keys = map.keySet();
            Iterator<Object> it = keys.iterator();
            while (it.hasNext()) {
                Object key = it.next();
                Object element = map.get(key);
                if (element instanceof Chunk) {
                    element = ((Chunk) element).getData(context);
                }
                element = instantiateElements(element, context);
                map.put(key, element);
            }
            return map;
        } else {
            return obj;
        }
    }
    
    
    private static Object getNullForClass(Class<?> c) {
        if (c.equals(Boolean.class) || c.equals(Boolean.TYPE)) {
            return new Boolean(false);
        } else if (c.equals(Byte.class) || c.equals(Byte.TYPE)) {
            return new Byte((byte) 0);
        } else if (c.equals(Character.class) || c.equals(Character.TYPE)) {
            return new Character((char) 0);
        } else if (c.equals(Double.class) || c.equals(Double.TYPE)) {
            return new Double(0.0);
        } else if (c.equals(Float.class) || c.equals(Float.TYPE)) {
            return new Float(0.0f);
        } else if (c.equals(Integer.class) || c.equals(Integer.TYPE)) {
            return new Integer(0);
        } else if (c.equals(Short.class) || c.equals(Short.TYPE)) {
            return new Short((short) 0);
        } else if (c.equals(Long.class)) {
            return new Long(0L);
        } else {
            return null;
        }
    }

    
    
    public Object generateData(Context context, Definition def) throws Redirection {
        ExternalConstruction obj = initExternalObject(context, null);
        if  (obj != this) {
            return obj.generateData(context, def);
        }
        
        // Some methods are indirect -- the method object was initialized
        // for a definition, but should be called on the object created by
        // by the definition.  See if that's the case here, and if so, 
        // dynamically discover the proper method and call it.
        Method runtimeMethod = method;
        Class<?> clazz = method.getDeclaringClass();
        Class<?>[] params = method.getParameterTypes();
        int numParams = params.length;
        int numArgs = argObjects == null ? 0 : argObjects.length;
        
        if (instance != null) {
            Class<?> runtimeClazz = instance.getClass();
            if (!clazz.isAssignableFrom(runtimeClazz)) {
                String name = method.getName();

                try {
                    runtimeMethod = runtimeClazz.getMethod(name, params);
    
                } catch (NoSuchMethodException nsme) {
                    runtimeMethod = ExternalDefinition.getClosestMethod(name, params, runtimeClazz);
                    if (runtimeMethod == null) {
                        String message = "Unable to find method " + name + " in class " + instance.getClass().getName();
                        log(message);
                        throw new Redirection(Redirection.STANDARD_ERROR, message);
                    }
    
                } catch (Exception e) {
                    String message = "Exception finding method " + name + " in class " + instance.getClass().getName() + ": " + e;
                    log(message);
                    e.printStackTrace();
                    throw new Redirection(Redirection.STANDARD_ERROR, message);
                }
            }
        }
        
        Object[] args = null;
        if (numParams > 0) {
            args = new Object[numParams];
            int j = 0;
            if (numParams > numArgs && Context.class.isAssignableFrom(params[0])) {
                args[j++] = context;
            }
            
            for (int i = 0; i < argObjects.length; i++) {
                Object arg = argObjects[i];
                if (arg != null) {
                    if (arg instanceof DynamicObject) {
                        arg = ((DynamicObject) arg).initForContext(context, null, null);
                    }
                    if (arg instanceof Construction) {
                        arg = ((Construction)arg).getData(context);
                    }
                    if (params != null && numParams > j && params[j].equals(String.class) && !(arg instanceof String)) {
                        arg = arg.toString();
                    }
                }
                args[j++] = arg;
            }
        }

        
//        if (args != null && args.length > 0) {
//            
//            // if any of the arguments is a Construction, clone the array
//            // and construct all such arguments
//            for (int i = 0; i < args.length; i++) {
//                Object arg = args[i];
//                if (arg instanceof Construction) {
//                    if (args == argObjects) {
//                        args = new Object[args.length];
//                        if (i > 0) {
//                            System.arraycopy(argObjects, 0, args, 0, i);
//                        }
//                    }
//                    arg = ((Construction)arg).getData(context);
//                    if (arg instanceof DynamicObject) {
//                        arg = ((DynamicObject) arg).initForContext(context, null);
//                    }
//                    args[i] = arg;                    
//                }
//            }            
//        }

        try {
            return runtimeMethod.invoke(instance, args);

        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof Redirection) {
                throw (Redirection) t;
            } else {
                String message = "Exception in external method " + method.getName() + ": " + t;
                log(message);
                t.printStackTrace();
                throw new Redirection(Redirection.SERVER_ERROR_STATUS, Redirection.STANDARD_ERROR, message);
            }
 
        } catch (Exception e) {
            String message = "Exception generating data via external method " + method.getName() + ": " + e;
            log(message);
            e.printStackTrace();
            throw new Redirection(Redirection.SERVER_ERROR_STATUS, Redirection.STANDARD_ERROR_PAGE, message);
        }
    }
}




class FieldDefinition extends ExternalDefinition {
    protected Field field;
 
    public FieldDefinition(ExternalDefinition owner, Field field) {
        this.field = field;
        setOwner(owner);
        setObject(owner.getContents());
        setName(new NameNode(field.getName()));
        setExternalClass(field.getType());
        setType(createType());
        setContents(new FieldConstruction());
    }

    protected List<ParameterList> generateParamLists() {
        return new EmptyList<ParameterList>();
    }

    public Definition getDefForContext(Context context, ArgumentList args) throws Redirection {
        return this;
    }
    
    public class FieldConstruction extends AbstractConstruction {
        public Object generateData(Context context, Definition def) throws Redirection {
            try {
                Object object = getObject();
                if (object instanceof Value) {
                    object = ((Value) object).getValue();
                } else if (object instanceof ValueGenerator) {
                    object = ((ValueGenerator) object).getData(context);
                }

                Object instance;
                if (object instanceof Class<?>) {
                    instance = null;
                } else {
                    instance = object;
                }
                return field.get(instance);
            } catch (Exception e) {
                log("Exception generating data via external field: " + e);
                throw new Redirection(Redirection.SERVER_ERROR_STATUS, Redirection.STANDARD_ERROR_PAGE, e.toString());
            }
        }
    }
}

