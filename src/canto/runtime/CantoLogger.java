/* Canto Compiler and Runtime Engine
 * 
 * Logger.java
 *
 * Copyright (c) 2018-2022 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import canto.lang.*;
import canto.parser.*;

public class CantoLogger implements CantoParserVisitor {

    public static boolean echoSystemOut = false;

    /** Verbosity setting to minimize the information displayed on the console. **/
    public final static int TERSE = 0;

    /** Middle setting for verbosity; some but not all information is displayed on the console. **/
    public final static int MODERATE = 1;

    /** Verbosity setting to output all available information to the console. **/
    public final static int VERBOSE = 2;

    public static int verbosity = TERSE;
    private static PrintStream ps = null;
    private static String logFile = "(none)";
    
    private static Logger logger = LoggerFactory.getLogger(CantoServer.class);

    
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
        vlog(logbuf.toString());
        //log("          *** Contents: " + definition.getContents().getClass().getName());
    }


    private String indent = "";

    public CantoLogger() {}

    public static void log(String str) {
        log(str, false);
    }
    
    public static void log(String str, boolean urgent) {
        if (echoSystemOut || urgent) {
            System.out.println(str);
        }
        if (ps != null) {
            ps.println(str);
        }
        if (urgent) {
            logger.warn(str);
        } else {
            logger.info(str);
        }
    }

    public static void err(String str) {
        System.err.println(str);
        if (ps != null) {
            ps.println("ERROR: " + str);
        }
        logger.error(str);
    }

    public static void setLogFile(String logFileName, boolean append) throws FileNotFoundException {
        logFile = logFileName;
        ps = new PrintStream(new FileOutputStream(logFileName, append));
        Date now = new Date();
        log("\n=========== begin logging " + now.toString() + " ============");
    }

    public static String getLogFile() {
        return logFile;
    }

    public static void setPrintStream(PrintStream printStream) {
        ps = printStream;
        if (ps.equals(System.out)) {
            echoSystemOut = false;
        }
    }

    public static void vlog(String str) {
        if (verbosity >= VERBOSE) {
            log(str);
        }
    }

    public static void mlog(String str) {
        if (verbosity >= MODERATE) {
            log(str);
        }
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
        if (verbosity >= VERBOSE) {
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
        vlog("SimpleNode!!!");
        data = node.childrenAccept(this, data);
        return data;
    }
    public Object visit(CantoNode node, Object data) {
        vlog("CantoNode!!!");
        return data;
    }
    public Object visit(ParsedRoot node, Object data) {
        vlog("parse root:");
        return handleNode(node, data);
    }
    public Object visit(ParsedStaticText node, Object data) {
        vlog("StaticText:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLiteralText node, Object data) {
        vlog("LiteralText:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNameWithArguments node, Object data) {
        vlog("NameWithArgs:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNameWithIndexes node, Object data) {
        vlog("NameWithIndexes:");
        return handleNode(node, data);
    }
    public Object visit(ParsedName node, Object data) {
        vlog("Name:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSpecialName node, Object data) {
        vlog("SpecialName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSiteStatement node, Object data) {
        vlog("SiteStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedCoreStatement node, Object data) {
        vlog("CoreStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefaultStatement node, Object data) {
        vlog("DefaultStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedStaticBlock node, Object data) {
        vlog("StaticBlock:");
        return handleNode(node, data);
    }
    public Object visit(ParsedCantoBlock node, Object data) {
        vlog("CantoBlock:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDynamicElementBlock node, Object data) {
        vlog("DynamicElementBlock:");
        return handleNode(node, data);
    }
    public Object visit(ParsedConcurrentCantoBlock node, Object data) {
        vlog("ConcurrentCantoBlock:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAdoptStatement node, Object data) {
        vlog("AdoptStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedExternStatement node, Object data) {
        vlog("ExternStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedKeepStatement node, Object data) {
        vlog("KeepStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedRedirectStatement node, Object data) {
        vlog("RedirectStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedContinueStatement node, Object data) {
        vlog("ContinueStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedConditionalExpression node, Object data) {
        vlog("ConditionalExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedWithPredicate node, Object data) {
        vlog("WithPredicate:");
        return handleNode(node, data);
    }
    public Object visit(ParsedWithoutPredicate node, Object data) {
        vlog("WithoutPredicate:");
        return handleNode(node, data);
    }
    public Object visit(ParsedForExpression node, Object data) {
        vlog("ForExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedIteratorValues node, Object data) {
        vlog("IteratorValues:");
        return handleNode(node, data);
    }
    public Object visit(ParsedBreakStatement node, Object data) {
        vlog("BreakStatement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNextConstruction node, Object data) {
        vlog("NextConstruction:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSubConstruction node, Object data) {
        vlog("SubConstruction:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSuperConstruction node, Object data) {
        vlog("SuperConstruction:");
        return handleNode(node, data);
    }
    public Object visit(ParsedConstruction node, Object data) {
        vlog("Construction:");
        return handleNode(node, data);
    }
    public Object visit(ParsedComplexName node, Object data) {
        vlog("ComplexName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAnonymousDefinition node, Object data) {
        vlog("AnonymousDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedCollectionDefinition node, Object data) {
        vlog("CollectionDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedComplexDefinition node, Object data) {
        vlog("ComplexDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedElementDefinition node, Object data) {
        vlog("ElementDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedExternalDefinition node, Object data) {
        vlog("ExternalDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedExternalCollectionDefinition node, Object data) {
        vlog("ExternalCollectionDefinition:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAnonymousArray node, Object data) {
        vlog("AnonymousArray:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAnonymousTable node, Object data) {
        vlog("AnonymousTable:");
        return handleNode(node, data);
    }
    public Object visit(ParsedType node, Object data) {
        vlog("Type:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefTypeName node, Object data) {
        vlog("DefTypeName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefElementName node, Object data) {
        vlog("DefElementName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefCollectionName node, Object data) {
        vlog("DefCollectionName:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDefParameter node, Object data) {
        vlog("DefParameter:");
        return handleNode(node, data);
    }
    public Object visit(ParsedParameterList node, Object data) {
        vlog("ParameterList:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAny node, Object data) {
        vlog("Any:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAnyAny node, Object data) {
        vlog("AnyAny:");
        return handleNode(node, data);
    }
    public Object visit(ParsedPrimitiveType node, Object data) {
        vlog("PrimitiveType:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDim node, Object data) {
        vlog("Dim:");
        return handleNode(node, data);
    }
    public Object visit(ParsedValueExpression node, Object data) {
        vlog("ValueExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedUnaryExpression node, Object data) {
        vlog("UnaryExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedBinaryExpression node, Object data) {
        vlog("BinaryExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedChoiceExpression node, Object data) {
        vlog("ChoiceExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLogicalOrOperator node, Object data) {
        vlog("LogicalOrOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLogicalAndOperator node, Object data) {
        vlog("LogicalAndOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedOrOperator node, Object data) {
        vlog("BitwiseOrOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedXorOperator node, Object data) {
        vlog("XorOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAndOperator node, Object data) {
        vlog("BitwiseAndOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedEqualsOperator node, Object data) {
        vlog("EqualsOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNotEqualsOperator node, Object data) {
        vlog("NotEqualsOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedIsaExpression node, Object data) {
        vlog("IsaExpression:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLessThanOperator node, Object data) {
        vlog("LessThanOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedGreaterThanOperator node, Object data) {
        vlog("GreaterThanOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLessThanOrEqualOperator node, Object data) {
        vlog("LessThanOrEqualOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedGreaterThanOrEqualOperator node, Object data) {
        vlog("GreaterThanOrEqualOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedInOperator node, Object data) {
        vlog("ParsedInOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLeftShiftOperator node, Object data) {
        vlog("LeftShiftOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedRightShiftOperator node, Object data) {
        vlog("RightShiftOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedRightUnsignedShiftOperator node, Object data) {
        vlog("RightUnsignedShiftOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedAddOperator node, Object data) {
        vlog("AddOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedSubtractOperator node, Object data) {
        vlog("SubtractOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedMultiplyOperator node, Object data) {
        vlog("MultiplyOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedDivideByOperator node, Object data) {
        vlog("DivideByOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedModOperator node, Object data) {
        vlog("ModOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedPowerOperator node, Object data) {
        vlog("PowerOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNegateOperator node, Object data) {
        vlog("NegateOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedBitflipOperator node, Object data) {
        vlog("BitflipOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedLogicalNotOperator node, Object data) {
        vlog("LogicalNotOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedTypeOperator node, Object data) {
        vlog("TypeOperator:");
        return handleNode(node, data);
    }
    public Object visit(ParsedIntegerLiteral node, Object data) {
        vlog("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedFloatingPointLiteral node, Object data) {
        vlog("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedCharLiteral node, Object data) {
        vlog("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedStringLiteral node, Object data) {
        vlog("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedBooleanLiteral node, Object data) {
        vlog("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedNullLiteral node, Object data) {
        vlog("Literal:");
        return handleNode(node, data);
    }
    public Object visit(ParsedArguments node, Object data) {
        vlog("ArgumentList:");
        return handleNode(node, data);
    }
    public Object visit(ParsedIndex node, Object data) {
        vlog("CollectionIndex:");
        return handleNode(node, data);
    }
    public Object visit(ParsedTableElement node, Object data) {
        vlog("TableElement:");
        return handleNode(node, data);
    }
    public Object visit(ParsedTypeList node, Object data) {
        vlog("TypeList:");
        return handleNode(node, data);
    }
    public Object visit(ParsedEllipsis node, Object data) {
        return handleNode(node, data);
    }
}
