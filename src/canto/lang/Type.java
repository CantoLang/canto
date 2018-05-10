/* Canto Compiler and Runtime Engine
 * 
 * Type.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
 * A Type is a reference to a definition, which may be built-in (primitive types),
 * external (declared in an extern statement) or explicit.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.18 $
 */

public interface Type extends Name {

    /** Returns true if the passed value is an instance of this type in the
     *  specified context.
     */
    public boolean isInstance(Object obj, Context context);

    /** Returns true if this is an external type.
     */
    public boolean isExternal();

    /** Returns true if this is a primitive type.
     */
    public boolean isPrimitive();
    
    /** Returns true if this is a special type, e.g. <code>this</code>.
     */
    public boolean isSpecial();

    /** Returns true if the passed type is the same as or is a supertype of
     *  this type in the specified context.
     */
    public boolean isTypeOf(Type type, Context context);

    /** Returns true if the passed string is the name of this type or a supertype
     *  of this type.
     */
    public boolean isTypeOf(String typeName);

    /** Returns true if the passed type is the same as or is a component of
     *  this type.
     */
    public boolean includes(Type type);

    /** Returns true if a type of the passed name is the same as or is a component of
     *  this type.
     */
    public boolean includes(String typeName);

    /** Converts a value of an arbitrary type to a value of this type. */
    public Value convert(Value val);

    /** Returns the dimensions associated with this type, or an empty list
     *  if this is not a collection type.
     */
    public List<Dim> getDims();

    /** Returns the collection (array or table) type this type 
     * represents or is a subtype of, if any, else null.
     */
    public Type getCollectionType();

    /** Returns true if this type represents a collection. */
    public boolean isCollection();
    
    /** Returns true if this type or a supertype of this type represents a collection. */
    public boolean inheritsCollection();
    
    /** Returns true if this type represents an array. */
    public boolean isArray();

    /** Returns the array type this type represents or is a subtype of, if any, else null. */
    public Type getArrayType();
    	
    /** Returns true if this type represents a table. */
    public boolean isTable();

    /** Returns the table type this type represents or is a subtype of, if any, else null. */
    public Type getTableType();

    /** Returns the base type, not including dimensions, represented by this type. */
    public Type getBaseType();

    /** Returns the arguments associated with this type in the specified context, or an
     *  empty list if this type has no associated arguments.  The context is needed to
     *  select the appropriate concrete type if this is a type list.
     */
    public ArgumentList getArguments(Context context);

    /** Returns the definition associated with this type. **/
    public Definition getDefinition();
    
    /** Returns the supertype of this type, if any, otherwise null. **/
    public Type getSuper();

    /** Returns the types of all children this type defines **/
    public Type[] getChildTypes();
    
    /** Returns the types of all children this type defines that
     *  represent persistable properties.  By default, this would
     *  be all cacheable (i.e. not dynamic and not static) chlid
     *  types.
     **/
    public Type[] getPersistableChildTypes();

    /** Returns the most specific common implementation class of values of this type in the 
     *  specified context.
     **/
    public Class<?> getTypeClass(Context context);

    /** Resolves any references for this type. **/
    public void resolve();

    /** Returns the number of inheritance levels this type is below the passed 
     *  type in the specified context, or -1 if type is not at the same level or
     *  an ancestor of this type.  inheritance hierarchy as this type.
     */
    public int levelsBelow(Type type, Context context);

    /** Returns true if this type can be the supertype of a type with the specified parameters. **/
    public boolean canBeSuperForParams(ParameterList params, Context context);
}
