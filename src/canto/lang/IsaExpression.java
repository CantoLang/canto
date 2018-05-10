/* Canto Compiler and Runtime Engine
 * 
 * IsaExpression.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * isa operator, similar to Java instanceof operator
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.10 $
 */

public class IsaExpression extends Expression {

    public IsaExpression() {
        super();
    }

    private IsaExpression(IsaExpression expression) {
        super(expression);
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        Object child = getChild(0);
        Type type = (Type) getChild(1);
        return new PrimitiveValue(type.isInstance(child, context));
    }

    /** Always returns boolean type */
    public Type getType(Context context, boolean generate) {
        return PrimitiveType.BOOLEAN;
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        sb.append('(');
        sb.append(getChild(0).toString());
        sb.append(" isa ");
        sb.append(getChild(1).toString());
        sb.append(')');
        return sb.toString();
    }

    public Expression resolveExpression(Context context) {
        IsaExpression resolvedExpression = new IsaExpression(this);
        resolvedExpression.resolveChildrenInPlace(context);
        return resolvedExpression;
    }
}
