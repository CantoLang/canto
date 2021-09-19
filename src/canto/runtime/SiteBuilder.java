/* Canto Compiler and Runtime Engine
 * 
 * SiteBuilder.java
 *
 * Copyright (c) 2018-2021 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.util.*;
import java.io.*;

import canto.lang.*;
import canto.parser.*;


public class SiteBuilder {

    public final static int LOG = 1;
    public final static int DUMP_SOURCE = 2;
    public final static int DUMP_TYPES = 4;
    public final static int DUMP_PAGES = 8;


    private Exception exception = null;

    protected Core core;


    public SiteBuilder(Core core) {
        this.core = core;
    }

    public Exception getException() {
        return exception;
    }

    public void build(Node parseResult) throws Exception {
        build(parseResult, 0);
    }

    public void build(Node parseResult, int actions) throws Exception {
        try {
            if ((actions & LOG) != 0) {
                System.out.println("--- LOGGING PASS ---");
                parseResult.jjtAccept(new CantoLogger(), null);
            }

            CantoLogger.log("--- INIT PASS ---");
            parseResult.jjtAccept(new Initializer(core), core);

            CantoLogger.log("--- RESOLVE PASS ---");
            parseResult.jjtAccept(new Resolver(), null);

            CantoLogger.log("--- VALIDATE PASS ---");
            Validater validater = new Validater();
            parseResult.jjtAccept(validater, null);
            List<String> problems = validater.getProblems();
            if (problems.size() > 0) {
                Iterator<String> it = problems.iterator();
                String message = it.next();
                while (it.hasNext()) {
                    message = message + "  " + (String) it.next();
                }
                throw new RuntimeException(message);
            }


            if ((actions & (DUMP_SOURCE | DUMP_TYPES | DUMP_PAGES)) != 0) {
                Context context = new Context(core);

                if ((actions & DUMP_SOURCE) != 0) {
                    CantoLogger.log("--- SOURCE DUMPING PASS ---");
                    parseResult.jjtAccept(new Dumper(), null);
                }
                if ((actions & DUMP_TYPES) != 0) {
                    CantoLogger.log("--- TYPE DUMPING PASS ---");
                    parseResult.jjtAccept(new TypeDumper(), context);
                }
                if ((actions & DUMP_PAGES) != 0) {
                    CantoLogger.log("--- PAGE DUMPING PASS ---");
                    parseResult.jjtAccept(new PageDumper(), context);
                }
            }

        } catch (Exception e) {
            exception = e;
            throw e;
        } catch (Redirection r) {
            exception = new RuntimeException("Redirection on startup: " + r.getMessage());
            throw exception;
        }
    }


    public class Dumper extends CantoVisitor {
        public Object handleNode(CantoNode node, Object data) {
            CantoLogger.log(node.toString());
            return data;
        }
    }

    public class TypeDumper extends CantoVisitor {
        public Object handleNode(CantoNode node, Object data) {
            if (node instanceof NamedDefinition) {
                NamedDefinition nd = (NamedDefinition) node;
                NamedDefinition sd = nd.getSuperDefinition((Context) data);
                String supername;
                if (sd != null) {
                    supername = sd.getName();
                } else {
                    Type st = nd.getSuper();
                    if (st != null) {
                        supername = '"' + st.getName() + '"';
                    } else {
                        supername = null;
                    }
                }

                String str = "   " + nd.getName() + " == " + nd.getFullName();
                if (supername != null) {
                    str = str + "  ^ " + supername;
                }
                CantoLogger.log(str);
            }
            return super.handleNode(node, data);
        }
    }

    public class PageDumper extends CantoVisitor {

        private int pagenum = 0;

        public Object handleNode(CantoNode node, Object data) {
            if (node instanceof Site) {
            	;
            } else if (node instanceof NamedDefinition) {
                NamedDefinition nd = (NamedDefinition) node;
                if (nd.isSuperType("page")) {
                    CantoLogger.log("\nPage " + pagenum + ": " + nd.getName() + " (" + nd.getFullName() + ")\n");
                    //Instantiation instance = new Instantiation(owner, nd);
                    Instantiation instance = new Instantiation(nd);
                    try {
                        CantoLogger.log(instance.getText((Context) data) + "\n");
                    } catch (Redirection r) {
                        CantoLogger.log("    ***** Error instantiating page: " + r.getMessage());
                    }
                    pagenum++;
                    return data;
                }
            }
            return super.handleNode(node, data);
        }
    }
}
