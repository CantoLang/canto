/* Canto Compiler and Runtime Engine
 * 
 * SiteBuilder.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.util.*;

import canto.lang.*;
import canto.parser.*;

import java.io.*;

public class SiteBuilder {

    public final static int LOG = 1;
    public final static int DUMP_SOURCE = 2;
    public final static int DUMP_TYPES = 4;
    public final static int DUMP_PAGES = 8;


    private Exception exception = null;

    protected Core core;

    public static boolean echoSystemOut = true;

    /** Verbosity setting to minimize the information displayed on the console. **/
    public final static int TERSE = 0;

    /** Middle setting for verbosity; some but not all information is displayed on the console. **/
    public final static int MODERATE = 1;

    /** Verbosity setting to output all available information to the console. **/
    public final static int VERBOSE = 2;

    public static int verbosity = TERSE;
    private static PrintStream ps = null;
    private static String logFile = "(none)";
    
    
    
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
    }

    public static void err(String str) {
        System.err.println(str);
        if (ps != null) {
            ps.println("ERROR: " + str);
        }
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
                parseResult.jjtAccept(new Logger(), null);
            }

            log("--- INIT PASS ---");
            parseResult.jjtAccept(new Initializer(core), core);

            log("--- RESOLVE PASS ---");
            parseResult.jjtAccept(new Resolver(), null);

            log("--- VALIDATE PASS ---");
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
                    log("--- SOURCE DUMPING PASS ---");
                    parseResult.jjtAccept(new Dumper(), null);
                }
                if ((actions & DUMP_TYPES) != 0) {
                    log("--- TYPE DUMPING PASS ---");
                    parseResult.jjtAccept(new TypeDumper(), context);
                }
                if ((actions & DUMP_PAGES) != 0) {
                    log("--- PAGE DUMPING PASS ---");
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
            log(node.toString());
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
                log(str);
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
                    log("\nPage " + pagenum + ": " + nd.getName() + " (" + nd.getFullName() + ")\n");
                    //Instantiation instance = new Instantiation(owner, nd);
                    Instantiation instance = new Instantiation(nd);
                    try {
                        log(instance.getText((Context) data) + "\n");
                    } catch (Redirection r) {
                        log("    ***** Error instantiating page: " + r.getMessage());
                    }
                    pagenum++;
                    return data;
                }
            }
            return super.handleNode(node, data);
        }
    }
}
