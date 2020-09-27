/* Canto Compiler and Runtime Engine
 * 
 * Logger.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import canto.lang.*;
import canto.parser.*;

public class Logger implements CantoParserVisitor {

    // global Instantiation logging
    public static void logInstantiation(Definition definition) {
        logInstantiation(null, definition);
    }

    // global Instantiation logging
    public static void logInstantiation(Context context, Object obj) {
        int levels = (context == null ? 0 : context.size());
        StringBuffer logbuf = new StringBuffer();
        for (int i = 0; i < levels; i++) {
            logbuf.append("    ");
        }

        if (obj instanceof Name) {
            String name = ((Name) obj).getName();
            if (obj instanceof Definition) {
                String what;
                Definition definition = (Definition) obj;
                String fullName = definition.getFullName();
                if (definition.isExternal()) {
                    what = "n external definition named " + fullName;
                } else if (definition instanceof ComplexDefinition) {
                    what = " complex definition named " + fullName;
                } else if (definition instanceof ElementDefinition) {
                    Object element = ((ElementDefinition) definition).getElement();
                    String str = (element instanceof Value ? ('"' + ((Value) element).getString() + '"') : element.getClass().getName());
                    what = "n element definition containing " + str;
                } else if (definition instanceof ElementReference) {
                    what = "n element reference named " + fullName;
                } else if (definition instanceof NamedDefinition) {
                    what = " named definition named " + fullName;
                } else if (definition instanceof TypeDefinition) {
                    what = " type definition named " + ((TypeDefinition) definition).def.getFullName();
                } else if (definition instanceof CollectionDefinition) {
                    if (((CollectionDefinition) definition).isTable()) {
                        what = " table definition named " + fullName;
                    } else {
                        what = "n array definition named " + fullName;
                    }
                } else if (definition.isAnonymous()) {
                    what = "n anonymous definition";
                } else {
                    what = " " + definition.getClass().getName() + " named " + fullName;
                }
                logbuf.append("* Instantiating a");
                logbuf.append(what);

            } else if (obj instanceof NameNode) {
                logbuf.append("=== looking up ");
                logbuf.append(name);

            } else {
                logbuf.append("**** UNKNOWN OBJECT: ");
                logbuf.append(name);
            }
        } else {
            return;
        }
        SiteBuilder.vlog(logbuf.toString());
        //log("          *** Contents: " + definition.getContents().getClass().getName());
    }


    private String indent = "";

    public Logger() {}

    public static void vlog(String string) {
        SiteBuilder.vlog(string);
    }

    private void log(String string) {
        SiteBuilder.log(indent + string);
    }
    private void logIfVerbose(String string) {
        SiteBuilder.vlog(indent + string);
    }

    public static String getDescription(AbstractNode node) {
        String desc;
        if (node instanceof Type) {
            desc = "TYPE " + ((Type) node).getName();
        } else if (node instanceof Definition) {
            desc = "DEFINITION " + ((Definition) node).getName();
        } else if (node instanceof Name) {
            desc = "NAME " + ((Name) node).getName();
        } else if (node instanceof Instantiation) {
            String name = "<<unresolved reference>>";
            CantoNode reference = ((Instantiation) node).getReference();
            if (reference instanceof Definition) {
                name = ((Definition) reference).getName();
            } else if (reference instanceof ValueExpression) {
                name = "<<expression>>";
            } else if (reference instanceof Name) {
                name = ((Name) reference).getName();
            } else {
                name = "<<unexpected reference type: " + reference.getClass().getName() + ">>";
            }
            desc = "INSTANTIATION " + name;
        } else if (node instanceof Expression) {
            desc = "EXPRESSION";
        } else if (node instanceof ConditionalStatement) {
            desc = "CONDITIONAL " + (((ConditionalStatement) node).getElse() == null ? "if" : "if-else");
        } else if (node instanceof SubStatement) {
            desc = "CONSTRUCTION sub";
        } else if (node instanceof SuperStatement) {
            desc = "CONSTRUCTION super";
        } else if (node instanceof Construction) {
            desc = "CONSTRUCTION";
        } else if (node instanceof CantoStatement) {
            desc = "STATEMENT";
        } else if (node instanceof Block) {
            Block block = (Block) node;
            int nc = block.getConstructions().size();
            desc = "BLOCK " + (node.isDynamic() ? "dynamic, " : "static, ") + nc + " constructions";
        } else if (node instanceof StaticText) {
            String text = ((StaticText) node).getText();
            if (text == null) {
                text = "*** null***";
            } else {
                text = text.trim();
                int len = text.length();
                if (len == 0) {
                    text = "*** empty ***";
                } else {
                    int ilf = text.indexOf('\n');
                    int icr = text.indexOf('\r');
                    int ix = Math.min(len, 40);
                    ix = (ilf >= 0 && ilf < ix ? ilf : ix);
                    ix = (icr >= 0 && icr < ix ? icr : ix);
                    if (ix < len) {
                        text = text.substring(0, ix) + "...";
                    }
                }
            }
            desc = "STATIC TEXT " + text;

        } else {
            desc = "(other type: " + node.getClass().getName() + ")";
        }
        return desc;
    }


    private void logNodeInfo(CantoNode cantoNode) {
    	AbstractNode node = (AbstractNode) cantoNode;
        log(getDescription(node));
        if (SiteBuilder.verbosity >= SiteBuilder.VERBOSE) {
            int n = node.jjtGetNumChildren();
            log("    " + n + " children");
            for (int i = 0; i < n; i++) {
                log("        child " + i + " is a " + node.jjtGetChild(i).getClass().getName());
            }
            log("\n");
        }
    }

    protected Object handleNode(CantoNode node, Object data) {
        logNodeInfo(node);
        String oldindent = indent;
        indent = indent + "    ";
        data = ((AbstractNode) node).childrenAccept(this, data);
        indent = oldindent;
        return data;
    }
    public Object visit(SimpleNode node, Object data) {
        logIfVerbose("SimpleNode!!!");
        data = node.childrenAccept(this, data);
        return data;
    }
    public Object visit(CantoNode node, Object data) {
        logIfVerbose("CantoNode!!!");
        return data;
    }
    public Object visit(ParsedRoot node, Object data) {
        logIfVerbose("parse root:");
        return handleNode(node, data);
    }
    public Object visit(ParsedStaticText node, Object data) {
        logIfVerbose("StaticText:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLiteralText node, Object data) {
        logIfVerbose("LiteralText:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNameWithArguments node, Object data) {
        logIfVerbose("NameWithArgs:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNameWithIndexes node, Object data) {
        logIfVerbose("NameWithIndexes:");
        return handleNode(node, data);
    }
    public Object visit(ParsedName node, Object data) {
        logIfVerbose("Name:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSpecialName node, Object data) {
        logIfVerbose("SpecialName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSiteStatement node, Object data) {
        logIfVerbose("SiteStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedCoreStatement node, Object data) {
        logIfVerbose("CoreStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefaultStatement node, Object data) {
        logIfVerbose("DefaultStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedStaticBlock node, Object data) {
        logIfVerbose("StaticBlock:");
        return handleNode(node, data);
    }
    public Object visit(ParsedCantoBlock node, Object data) {
        logIfVerbose("CantoBlock:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDynamicElementBlock node, Object data) {
        logIfVerbose("DynamicElementBlock:");
        return handleNode(node, data);
    }
    public Object visit(ParsedConcurrentCantoBlock node, Object data) {
        logIfVerbose("ConcurrentCantoBlock:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAdoptStatement node, Object data) {
        logIfVerbose("AdoptStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedExternStatement node, Object data) {
        logIfVerbose("ExternStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedKeepStatement node, Object data) {
        logIfVerbose("KeepStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedRedirectStatement node, Object data) {
        logIfVerbose("RedirectStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedContinueStatement node, Object data) {
        logIfVerbose("ContinueStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedConditionalExpression node, Object data) {
        logIfVerbose("ConditionalExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedWithPredicate node, Object data) {
        logIfVerbose("WithPredicate:");
        return handleNode(node, data);
    }
    public Object visit(ParsedWithoutPredicate node, Object data) {
        logIfVerbose("WithoutPredicate:");
        return handleNode(node, data);
    }
    public Object visit(ParsedForExpression node, Object data) {
        logIfVerbose("ForExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedIteratorValues node, Object data) {
        logIfVerbose("IteratorValues:");
        return handleNode(node, data);
    }
    public Object visit(ParsedBreakStatement node, Object data) {
        logIfVerbose("BreakStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNextConstruction node, Object data) {
        logIfVerbose("NextConstruction:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSubConstruction node, Object data) {
        logIfVerbose("SubConstruction:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSuperConstruction node, Object data) {
        logIfVerbose("SuperConstruction:");
        return handleNode(node, data);
    }
    public Object visit(ParsedConstruction node, Object data) {
        logIfVerbose("Construction:");
        return handleNode(node, data);
    }
    public Object visit(ParsedComplexName node, Object data) {
        logIfVerbose("ComplexName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAnonymousDefinition node, Object data) {
        logIfVerbose("AnonymousDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedCollectionDefinition node, Object data) {
        logIfVerbose("CollectionDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedComplexDefinition node, Object data) {
        logIfVerbose("ComplexDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedElementDefinition node, Object data) {
        logIfVerbose("ElementDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedExternalDefinition node, Object data) {
        logIfVerbose("ExternalDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedExternalCollectionDefinition node, Object data) {
        logIfVerbose("ExternalCollectionDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAnonymousArray node, Object data) {
        logIfVerbose("AnonymousArray:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAnonymousTable node, Object data) {
        logIfVerbose("AnonymousTable:");
        return handleNode(node, data);
    }
    public Object visit(ParsedType node, Object data) {
        logIfVerbose("Type:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefTypeName node, Object data) {
        logIfVerbose("DefTypeName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefElementName node, Object data) {
        logIfVerbose("DefElementName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefCollectionName node, Object data) {
        logIfVerbose("DefCollectionName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefParameter node, Object data) {
        logIfVerbose("DefParameter:");
        return handleNode(node, data);
    }
    public Object visit(ParsedParameterList node, Object data) {
        logIfVerbose("ParameterList:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAny node, Object data) {
        logIfVerbose("Any:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAnyAny node, Object data) {
        logIfVerbose("AnyAny:");
        return handleNode(node, data);
    }
    public Object visit(ParsedPrimitiveType node, Object data) {
        logIfVerbose("PrimitiveType:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDim node, Object data) {
        logIfVerbose("Dim:");
        return handleNode(node, data);
    }
    public Object visit(ParsedValueExpression node, Object data) {
        logIfVerbose("ValueExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedUnaryExpression node, Object data) {
        logIfVerbose("UnaryExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedBinaryExpression node, Object data) {
        logIfVerbose("BinaryExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedChoiceExpression node, Object data) {
        logIfVerbose("ChoiceExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLogicalOrOperator node, Object data) {
        logIfVerbose("LogicalOrOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLogicalAndOperator node, Object data) {
        logIfVerbose("LogicalAndOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedOrOperator node, Object data) {
        logIfVerbose("BitwiseOrOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedXorOperator node, Object data) {
        logIfVerbose("XorOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAndOperator node, Object data) {
        logIfVerbose("BitwiseAndOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedEqualsOperator node, Object data) {
        logIfVerbose("EqualsOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNotEqualsOperator node, Object data) {
        logIfVerbose("NotEqualsOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedIsaExpression node, Object data) {
        logIfVerbose("IsaExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLessThanOperator node, Object data) {
        logIfVerbose("LessThanOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedGreaterThanOperator node, Object data) {
        logIfVerbose("GreaterThanOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLessThanOrEqualOperator node, Object data) {
        logIfVerbose("LessThanOrEqualOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedGreaterThanOrEqualOperator node, Object data) {
        logIfVerbose("GreaterThanOrEqualOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedInOperator node, Object data) {
        logIfVerbose("ParsedInOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLeftShiftOperator node, Object data) {
        logIfVerbose("LeftShiftOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedRightShiftOperator node, Object data) {
        logIfVerbose("RightShiftOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedRightUnsignedShiftOperator node, Object data) {
        logIfVerbose("RightUnsignedShiftOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAddOperator node, Object data) {
        logIfVerbose("AddOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSubtractOperator node, Object data) {
        logIfVerbose("SubtractOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedMultiplyOperator node, Object data) {
        logIfVerbose("MultiplyOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDivideByOperator node, Object data) {
        logIfVerbose("DivideByOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedModOperator node, Object data) {
        logIfVerbose("ModOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedPowerOperator node, Object data) {
        logIfVerbose("PowerOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNegateOperator node, Object data) {
        logIfVerbose("NegateOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedBitflipOperator node, Object data) {
        logIfVerbose("BitflipOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLogicalNotOperator node, Object data) {
        logIfVerbose("LogicalNotOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedTypeOperator node, Object data) {
        logIfVerbose("TypeOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedIntegerLiteral node, Object data) {
        logIfVerbose("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedFloatingPointLiteral node, Object data) {
        logIfVerbose("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedCharLiteral node, Object data) {
        logIfVerbose("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedStringLiteral node, Object data) {
        logIfVerbose("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedBooleanLiteral node, Object data) {
        logIfVerbose("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNullLiteral node, Object data) {
        logIfVerbose("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedArguments node, Object data) {
        logIfVerbose("ArgumentList:");
        return handleNode(node, data);
    }
    public Object visit(ParsedIndex node, Object data) {
        logIfVerbose("CollectionIndex:");
        return handleNode(node, data);
    }
    public Object visit(ParsedTableElement node, Object data) {
        logIfVerbose("TableElement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedTypeList node, Object data) {
        logIfVerbose("TypeList:");
        return handleNode(node, data);
    }
    public Object visit(ParsedEllipsis node, Object data) {
        return handleNode(node, data);
    }
}
