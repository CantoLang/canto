/* Canto Compiler and Runtime Engine
 * 
 * BinaryExpression.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Iterator;

import canto.runtime.Context;

/**
 * A BinaryExpression is a construction based on a binary operator.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.12 $
 */

public class BinaryExpression extends Expression {

    public BinaryExpression() {
        super();
    }

    private BinaryExpression(BinaryExpression expression) {
        super(expression);
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        // a binary expression can have multiple instances of a binary
        // operator, e.g. <code>a + b + c</code> is parsed into a single
        // BinaryExpression which owns three values and two instances of
        // AddOperator.
        int len = getNumChildren();
        ValueSource val = (ValueSource) getChild(0);
        for (int i = 1; i < len - 1; i += 2) {
            BinaryOperator op = (BinaryOperator) getChild(i);
            ValueSource nextVal = (ValueSource) getChild(i + 1);
            if (nextVal instanceof ForStatement) {
                Iterator<Construction> vals = ((ForStatement) nextVal).generateConstructions(context).iterator();
                while (vals.hasNext()) {
                    val = op.operate(val, vals.next(), context);
                }
                
            } else {
                val = op.operate(val, nextVal, context);
            }
        }
        return val;

//        Value val = getChildValue(context, 0);
//        for (int i = 1; i < len - 1; i += 2) {
//            BinaryOperator op = (BinaryOperator) getChild(i);
//            Value nextVal = new DeferredValue((ValueSource) getChild(i + 1), context);
//            val = op.operate(val, nextVal);
//        }
//        return val;
    }

    public Type getType(Context context, boolean generate) {
        int len = getNumChildren();
        Type type = getChildType(context, generate, 0);
        for (int i = 1; i < len - 1; i += 2) {
            BinaryOperator op = (BinaryOperator) getChild(i);
            type = op.getResultType(type, getChildType(context, generate, i + 1), context);
        }
        return type;
    }

    public Expression resolveExpression(Context context) {
        BinaryExpression resolvedExpression = new BinaryExpression(this);
        resolvedExpression.resolveChildrenInPlace(context);
        return resolvedExpression;
    }
}
