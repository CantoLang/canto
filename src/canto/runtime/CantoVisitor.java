/* Canto Compiler and Runtime Engine
 * 
 * CantoVisitor.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import canto.lang.*;
import canto.parser.*;

/** Base class of canto tree visitors.  Implements CantoParserVisitor, a
 *  jjtree-generated interface implementing the Visitor design pattern.
 *  Subclasses of CantoVisitor may override the handleNode method for general
 *  node handling, and/or the visit method for any particular node types in
 *  which it is interested.
 *
 * @author Michael St. Hippolyte
 */
abstract public class CantoVisitor implements CantoParserVisitor {

    public CantoVisitor() {}


    /** By default, recursively visit chidren only if it is not
     *  primitive.
     */
    protected Object handleNode(CantoNode node, Object data) {
        //if (!node.isPrimitive()) {
            data = ((AbstractNode) node).childrenAccept(this, data);
        //}
        return data;
    }

    /** Needed to satisfy the CantoParserVisitor interface; should never
     *  be called.
     */
    public Object visit(SimpleNode node, Object data) {
        throw new UnsupportedOperationException("SimpleNodes not supported");
    }

    /** Needed to satisfy the CantoParserVisitor interface; should never
     *  be called.
     */
    public Object visit(CantoNode node, Object data) {
        throw new UnsupportedOperationException("visit must be called with a concrete subclass");
    }

    public Object visit(ParsedRoot node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedStaticText node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedLiteralText node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedNameWithArguments node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedNameWithIndexes node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedName node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedSpecialName node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedSiteStatement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedCoreStatement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedDefaultStatement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedStaticBlock node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedCantoBlock node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedDynamicElementBlock node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedConcurrentCantoBlock node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedAdoptStatement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedExternStatement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedKeepStatement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedRedirectStatement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedContinueStatement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedConditionalExpression node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedWithPredicate node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedWithoutPredicate node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedForExpression node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedIteratorValues node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedBreakStatement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedNextConstruction node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedSubConstruction node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedSuperConstruction node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedConstruction node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedComplexName node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedAnonymousDefinition node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedCollectionDefinition node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedComplexDefinition node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedElementDefinition node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedExternalDefinition node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedExternalCollectionDefinition node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedAnonymousArray node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedAnonymousTable node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedType node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedDefTypeName node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedDefElementName node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedDefCollectionName node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedParameterList node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedDefParameter node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedAny node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedAnyAny node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedPrimitiveType node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedDim node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedValueExpression node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedUnaryExpression node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedBinaryExpression node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedChoiceExpression node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedLogicalOrOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedLogicalAndOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedOrOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedXorOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedAndOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedEqualsOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedNotEqualsOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedIsaExpression node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedLessThanOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedGreaterThanOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedLessThanOrEqualOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedGreaterThanOrEqualOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedInOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedLeftShiftOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedRightShiftOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedRightUnsignedShiftOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedAddOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedSubtractOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedMultiplyOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedDivideByOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedModOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedPowerOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedNegateOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedBitflipOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedLogicalNotOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedTypeOperator node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedIntegerLiteral node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedFloatingPointLiteral node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedCharLiteral node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedStringLiteral node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedBooleanLiteral node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedNullLiteral node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedArguments node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedIndex node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedTableElement node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedTypeList node, Object data) {
        return handleNode(node, data);
    }
    public Object visit(ParsedEllipsis node, Object data) {
        return handleNode(node, data);
    }
}
