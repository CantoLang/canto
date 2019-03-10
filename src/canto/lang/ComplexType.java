/* Canto Compiler and Runtime Engine
 * 
 * ComplexType.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.parser.Initializable;
import canto.runtime.Context;

/**
* Base class for types.
*
* @author Michael St. Hippolyte
* @version $Revision: 1.47 $
*/

public class ComplexType extends AbstractType implements Initializable {

   private List<Dim> dims = null;
   private ArgumentList args = null;


   public ComplexType() {
       super();
   }

   public ComplexType(Definition def, String typename) {
       super();
       setChild(0, new NameNode(typename));
       setName(typename);
       setDefinition(def);
       dims = new EmptyList<Dim>();
       args = new ArgumentList(new EmptyList<Construction>());
   }

   public ComplexType(Definition def, String typename, List<Dim> dims, ArgumentList args) {
       super();
       this.dims = (dims == null ? new EmptyList<Dim>() : dims);
       this.args = (args == null ? new ArgumentList(new EmptyList<Construction>()) : args);

       setChild(0, new NameNode(typename));
       setName(typename);
       setDefinition(def);
   }

   public ComplexType(Type baseType, List<Dim> additionalDims, ArgumentList args) {
       setName(baseType.getName());
       this.args = (args == null ? new ArgumentList(new EmptyList<Construction>()) : args);
       dims = baseType.getDims();
       if (dims == null) {
           dims = additionalDims;

       } else if (additionalDims != null) {
           dims = Context.newArrayList(dims);
           dims.addAll(additionalDims);
       }
   }

   public void init() {
       if (children != null && children.length > 0) {
           String name = ((Name) children[0]).getName();
           for (int i = 1; i < children.length; i++) {
               if (children[i] instanceof Any) {
                   children[i] = new ArgumentList(new SingleItemList<Construction>((Construction) children[i]));
                   continue;

               } else if (!(children[i] instanceof Name)) {
                   break;
               }
               name = name + '.' + ((Name) children[i]).getName();
           }
           setName(name);

       } else {
           setName(DefaultType.TYPE.getName());
       }
   }

   /** Find the definition associated with this type.
    */
   public void resolve() {
       if (getDefinition() != null) {
           return;
       }
       CantoNode parent = getParent();
       boolean allowCircular = (parent instanceof Expression);

       Definition owner = getOwner();
       while (owner != null) {
           owner = owner.getOwner();
           if (owner instanceof ComplexDefinition) {
               break;
           }
       }
       if (owner == null) {
           throw new NullPointerException("ComplexType '" + getName() + "' has no owner, cannot resolve");
       }

       ComplexDefinition container = (ComplexDefinition) owner;
       owner = getOwner();

       String checkName = this.getName();
       vlog("Resolving type " + checkName + " in " + owner.getFullName() + "...");
       Definition def = null;
       while ((def == null || (def.equals(owner) && !allowCircular)) && container != null) {
           def = container.getExplicitChildDefinition(this);
           
           // if not found, check superdefinitions
           if (def == null || (def.equals(owner) && !allowCircular)) {
               NamedDefinition superdef = container.getSuperDefinition(null);
               while (superdef != null) {
                   def = superdef.getExplicitChildDefinition(this);
                   if (def != null && (!def.equals(owner) || allowCircular)) {
                       break;
                   }
                   superdef = superdef.getSuperDefinition(null);
               }
               
           }
           container = (ComplexDefinition) container.getOwner();
       }

       // avoid circular type definitions
       if (def != null && !allowCircular && owner.equals(def)) {
           def = null;
       }

       // if not found yet, look for explicit or external definition
       if (def == null) {
           DefinitionTable defTable = ((NamedDefinition) owner).getDefinitionTable();
           def = defTable.getDefinition((NamedDefinition) owner, this);
           // avoid circular type definitions
           if (def != null && !allowCircular && owner.equals(def)) {
               def = null;
           }
       }

       if (def != null) {
           setDefinition(def);

           if (def.getAccess() == Definition.LOCAL_ACCESS) {
               vlog("   ...type " + checkName + " refers to local definition " + def.getFullName());
           } else if (checkName.equals(def.getFullName())) {
               vlog("   ...type " + checkName + " is an explicit definition reference");
           } else {
               vlog("   ...type " + checkName + " refers to class definition " + def.getFullName());
           }

       } else {
           vlog("   ...type " + checkName + " could not be resolved");
       }

   }

   public Value convert(Value val) {
       // TO DO: convert by calling the definition
       // with the passed type as a single parameter

//       try {
           if (getNumChildren() == 1) {
               CantoNode node = getChild(0);
               if (node instanceof Type) {
                   Type type = (Type) node;
                   return type.convert(val);
               }
           }
           List<Construction> args = Context.newArrayList(1, Construction.class);
           args.add((Construction) val);
           Definition def = getDefinition();
           Instantiation instance = new Instantiation(def, new ArgumentList(args), null);
           instance.setOwner(getOwner());
           Value result = val;
           try {
               result = instance.getValue(null);    // new Context(def.getSite()));
           } catch (Redirection r) {
               vlog("Error converting value to " + getName() + ": " + r.getMessage());
           }
           return result;

//       } catch (ClassCastException cce) {
//           throw new Redirection(Redirection.STANDARD_ERROR, "don't know how to convert a " + val.getValueClass().getName() + " to a " + getName());
//       }
   }

   
   public List<Dim> getDims() {
       if (dims == null) {
           int len = getNumChildren();
           int ix;
           CantoNode node = null;
           for (ix = 0; ix < len; ix++) {
               node = getChild(ix);
               if (node instanceof Dim) {
                   break;
               }
           }
           int num = len - ix;
           if (num == 0) {
               dims = new EmptyList<Dim>();
           } else if (num == 1) {
               dims = new SingleItemList<Dim>((Dim) node);
           } else {
               dims = Context.newArrayList(num, Dim.class);
               dims.add((Dim) node);
               for (++ix; ix < len; ix++) {
                   dims.add((Dim) getChild(ix));
               }
           }
       }
       return dims;
   }


   public ArgumentList getArguments(Context context) {
       if (args == null) {
           int len = getNumChildren();
           if (len > 0) {
               CantoNode node = getChild(len - 1);
               if (node instanceof ArgumentList) {
                   args = (ArgumentList) node;
               }
           }
           if (args == null) {
               args = new ArgumentList(new EmptyList<Construction>());
           }
       }
       if (args.size() >= 1 && args.get(0) instanceof Any) {
           Context.Entry entry = context.peek();
           Definition def = entry.superdef;
           
           // Don't resolve params to args if the supertype has already been pushed (this
           // may happen as a result of a call to pushSupers).
           //
           // This works for defs with one supertype, so either the def being instantiated
           // or the superdef is on top.  Not sure about cases where the superdef is
           // sandwiched in the middle of a def hierarchy with 3 or more levels.  Need
           // to test and find out.
           
           if (def != null && this.equals(def.getType())) {
               return entry.args;   
           }
           
           ParameterList params = context.peek().params;
           int len = (params == null ? 0 : params.size());
           ArgumentList argsFromParams = new ArgumentList(len);
           Definition owner = getOwner();
           for (DefParameter param: params) {
               Instantiation instance = new Instantiation(param.getNameNode(), owner);
               argsFromParams.add(instance);
           }
           return argsFromParams;
           
       }
       return args;
   }

   /** Returns the type, not including dimensions, represented by
    *  this complex type.
    */
   public Type getBaseType() {
       int len = getNumChildren();
       int n;
       for (n = 0; n < len; n++) {
           if (getChild(n) instanceof Dim) {
               break;
           }
       }
       if (n == 0) {
           return DefaultType.TYPE;
       } else if (n == 1) {
           CantoNode node = getChild(0);
           if (node instanceof Type) {
               return ((Type) node).getBaseType();
           } else if (len == 1) { // && (dims == null || dims.size() == 0)) {  // no dimensions
               Definition def = getDefinition();
               if (def != null) {
            	   if (def instanceof CollectionDefinition) {
                       return ((CollectionDefinition) def).getElementType();
                   } else {
            	       Type st = def.getSuper();
            	       if (st != null && st.isCollection()) {
            	            return st.getBaseType();
                       }
                   }
               }
               return this;

           } else {
               ComplexType baseType = new ComplexType();
               baseType.copyChildren(this, 0, 1);
               baseType.init();
               baseType.setOwner(getOwner());
               baseType.resolve();
               return baseType;
           }
       } else if (len == n) { // && (dims == null || dims.size() == 0)) {  // no dimensions
           Definition def = getDefinition();
           if (def != null && def.isCollection()) {
               return ((CollectionDefinition) def).getElementType();
           } else {
               return this;
           }
       } else {
           ComplexType baseType = new ComplexType();
           baseType.copyChildren(this, 0, n);
           baseType.setOwner(getOwner());
           baseType.resolve();
           return baseType;
       }
   }

   public Class<?> getTypeClass(Context context) {
       Class<?> typeClass = null;
       Type baseType = getBaseType();
       if (baseType instanceof PrimitiveType) {
           typeClass = baseType.getTypeClass(context);
       } else {
           Definition def = getDefinition();
           if (def == null) {
               typeClass = String.class;
           } else {
               // if the definition is an alias, look for an external definition at the
               // end of the alias chain, and use that class as the type class
               
               if (def.isAlias() && context != null) {
                   int numPushes = 0;
                   try {
                       Definition aliasDef = def;
                       ArgumentList aliasArgs = args;
                       ParameterList aliasParams = def.getParamsForArgs(args, context);
                       while (aliasDef.isAlias()) {
                           context.push(aliasDef, aliasParams, aliasArgs, false);
                           numPushes++;
                           Instantiation aliasInstance = aliasDef.getAliasInstance();
                           aliasDef = aliasInstance.getDefinition(context);
                           if (aliasDef == null || aliasDef instanceof ExternalDefinition) {
                               break;
                           }
                           aliasArgs = aliasInstance.getArguments();   // aliasInstance.getUltimateInstance(context).getArguments();
                           aliasParams = aliasDef.getParamsForArgs(aliasArgs, context);
                       }
                       if (aliasDef != null && aliasDef instanceof ExternalDefinition) {
                           def = aliasDef;
                       }
                       
                   } catch (Throwable t) {
                       ;
                   } finally {
                       while (numPushes-- > 0) {
                           context.pop();
                       }
                   }
                   
               
               }    
               if (def instanceof ExternalDefinition) {
                   typeClass = ((ExternalDefinition) def).getExternalClass(context);
               } else {
                   Type superType = def.getSuper();
                   if (superType != null) {
                       return superType.getTypeClass(context);
                   } else {
                       typeClass = String.class;
                   }
               }
           }
       }
       List<Dim> dims = getDims();
       if (dims.size() > 0) {
           String className;
           if (typeClass.isPrimitive() || typeClass.equals(String.class)) {
               className = "[Ljava.lang.Object;";
           } else if (Map.class.isAssignableFrom(typeClass)) {
        	   className = typeClass.getName();
           } else {
                className = "[L" + typeClass.getName() + ';';
           }

           Iterator<Dim> it = dims.iterator();
           while (it.hasNext()) {
               Dim dim = it.next();
               if (dim.isTable()) {
                   if (it.hasNext()) {
                       className = "Ljava.util.Map;";
                   } else {
                       className = "java.util.Map";
                   }
               } else {
                   className = '[' + className;
               }
           }
           try {
               typeClass = Class.forName(className);
           } catch (ClassNotFoundException cnfe) {
               log("Unable to load class " + className);
               typeClass = (new Object[0]).getClass();
           }
       }
       return typeClass;
   }
}
