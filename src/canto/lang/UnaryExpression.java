/* Canto Compiler and Runtime Engine
 * 
 * UnaryExpression.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A unary expression is a construction based on a unary operator and a single
 * operand.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public class UnaryExpression extends Expression {

    public UnaryExpression() {
        super();
    }

    public UnaryExpression(UnaryExpression expression) {
        super(expression);
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        UnaryOperator op = (UnaryOperator) getChild(0);
        Value operand = getChildValue(context, 1);
        return op.operate(operand);
    }

    public Type getType(Context context, boolean generate) {
        UnaryOperator op = (UnaryOperator) getChild(0);
        return op.getResultType(getChildType(context, generate, 1));
    }

    public Expression resolveExpression(Context context) {
        UnaryExpression resolvedExpression = new UnaryExpression(this);
        resolvedExpression.resolveChildrenInPlace(context);
        return resolvedExpression;
    }
}
