/* Canto Compiler and Runtime Engine
 * 
 * CantoScript.java
 *
 * Copyright (c) 2018-2021 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.*;
import java.util.*;

import canto.lang.*;
import canto.parser.CantoParser;
import canto.parser.Node;
import canto.parser.ParseException;

/**
 * Run a Canto program from the command line.
 *
 * CantoScript compiles a Canto script, instantiates main and outputs the result.
 *
 * @author Michael St. Hippolyte
 */

public class CantoScript implements canto_processor {
    public static final String NAME = "CantoScript";
    public static final String VERSION = Version.VERSION;
    public static final String NAME_AND_VERSION = NAME + " " + VERSION;

    protected Site mainScript = null;
    protected Map<String, CantoSite> sites = new HashMap<String, CantoSite>();

    private boolean initedOk = false;
    private String logFileName = null;
    private boolean appendToLog = true;
    private String fileBase = ".";
    private boolean recursive = false;
    private boolean multithreaded = false;
    private boolean customCore = false;
    private String cantoPath = ".";
    private Core sharedCore = null;
    private HashMap<String, Object> properties = new HashMap<String, Object>();
    private Exception[] exceptions = null;

    /** Main entry point.  The following flags are recognized (in any order).  All flags are optional.
     *  <table><tr><th>argument</th><th>default</th><th>effect</th></tr><tr>
     *
     *  <td>  -cantopath <path>[<sep><path>]* </td><td>  current directory      </td><td> Sets the initial cantopath, which is a string of pathnames separated by the
     *                                                                                    platform-specific path separator character (e.g., colon on Unix and semicolon
     *                                                                                    on Windows).  Pathnames may specify either files or directories.  At startup,
     *                                                                                    for each pathname, the Canto server loads either the indicated file (if the
     *                                                                                    pathname specifies a file) or all the files with a .can extension in the
     *                                                                                    indicated directory (if the pathname specifies a directory).
     *  <td>  -multithreaded                  </td><td>  not multithreaded      </td><td> Multithreaded compilation.  If this flag is present, then canto
     *                                                                                    files are compiled in independent threads.  </td>
     *  <td>  -recursive                      </td><td>  not recursive          </td><td> Recursive cantopath option.  </td>
     *  <td>  -log <path>                     </td><td>  no logging             </td><td> All output messages are logged in the specified file.  The file is overwritten
     *                                                                                    if it already exists.  </td>
     *  <td>  -log.append <path>              </td><td>  no logging             </td><td> All output messages are logged in the specified file.  If the file exists, the
     *                                                                                    current content is preserved, and messages are appended to the end of the file.  </td>
     *  <td>  -verbose                        </td><td>  not verbose            </td><td> Verbose output messages for debugging.  </td>.
     *  <td>  -debug                          </td><td>  debugging not enabled  </td><td> Enable the built-in debugger.  </td>.
     *
     */
    public static void main(String[] args) {
        CantoLogger.echoSystemOut = false;
        Map<String, String> initParams = new HashMap<String, String>();
        String[] scriptArgs = processArgs(args, initParams);        
        String problems = initParams.get("problems");
        if (problems == null || problems.equals("0")) {
            CantoScript runner = new CantoScript(initParams);
            if (runner.initedOk) {
                Writer writer = new OutputStreamWriter(System.out);
                runner.runScript(writer, scriptArgs);
            }
            List<Exception> exceptions = runner.getExceptions();
            if (exceptions != null && exceptions.size() > 0) {
                if (exceptions.size() == 1) {
                    Exception e = exceptions.get(0);
                    System.err.println("Problem running CantoScript: " + e);
                    e.printStackTrace(System.err);
                } else {
                    System.err.println("Mutliple problems running CantoScript.");
                    int i = 1;
                    for (Exception e: exceptions) {
                        System.err.println("-------------------------------------");
                        System.err.println("Problem " + i++ + ": " + e);
                        e.printStackTrace(System.err);
                    }
                }
            }

        } else {
            System.out.println("Usage:");
            System.out.println("          java -jar canto.jar [flags] obj_name\n");
            System.out.println("where obj_name is the name of the object to be instantiated and\n");
            System.out.println("the optional flags are among the following (in any order):\n");
            System.out.println("Flag                              Effect");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("-cantopath <pathnames>         Sets the initial cantopath, which is a string");
            System.out.println("                             of pathnames separated by the platform-specific");
            System.out.println("                             path separator character (e.g., colon on Unix");
            System.out.println("                             and semicolon on Windows).  Pathnames may");
            System.out.println("                             specify either files or directories.  At");
            System.out.println("                             startup, for each pathname, the Canto runner");
            System.out.println("                             loads either the indicated file (if the pathname");
            System.out.println("                             specifies a file) or all the files with a .can");
            System.out.println("                             extension in the indicated directory (if the");
            System.out.println("                             pathname specifies a directory).\n");
            System.out.println("-recursive                   Recursive cantopath option.\n");
            System.out.println("-multithreaded               Multithreaded compilation.  If this flag is");
            System.out.println("                             present, then canto files are compiled in");
            System.out.println("                             independent threads.\n");
            System.out.println("-customcore                  Custom core definitions supplied in cantopath;");
            System.out.println("                             core files will not be autoloaded from");
            System.out.println("                             canto.jar.\n");
            System.out.println("-log <path>                  All output messages are logged in the specified");
            System.out.println("                             file.  The file is overwritten if it already");
            System.out.println("                             exists.\n");
            System.out.println("-log.append <path>           All output messages are logged in the specified");
            System.out.println("                             file.  If the file exists, the current content");
            System.out.println("                             is preserved, and messages are appended to the");
            System.out.println("                             end of the file./n");
            System.out.println("-verbose                     Verbose output messages for debugging.\n");
            System.out.println("-debug                       Enable the built-in debugger.\n");
            System.out.println("-?                           This screen.\n\n");
            System.out.println("Flags may be abbreviated to the initial letters, e.g. -r instead of -recursive,");
            System.out.println("or -l.a instead of -log.append.\n");
        }
    }

    /** Constructor */
    public CantoScript(Map<String, String> initParams) {
        initedOk = init(initParams);
    }

    private static String[] processArgs(String[] args, Map<String, String> initParams) {
        String scriptArgs[] = null;
        int numProblems = 0;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String nextArg = (i + 1 < args.length ? args[i + 1] : null);
            boolean noNextArg = (nextArg == null || nextArg.startsWith("-"));
            if (arg.equals("-site") || arg.equals("-s")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "site name not provided";
                    initParams.put("problem" + numProblems, msg);
                    i++;
                } else {
                    initParams.put("site", nextArg);
                }

            } else if (arg.equals("-filebase") || arg.equals("-docbase") || arg.equals("-d")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "filebase not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("filebase", nextArg);
                    i++;
                }

            } else if (arg.equals("-filesfirst") || arg.equals("-f")) {
                initParams.put("filesfirst", "true");

            } else if (arg.equals("-cantopath") || arg.equals("-b")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "cantopath not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("cantopath", nextArg);
                    i++;
                }

            } else if (arg.equals("-recursive") || arg.equals("-r")) {
                initParams.put("recursive", "true");

            } else if (arg.equals("-multithreaded") || arg.equals("-m")) {
                initParams.put("multithreaded", "true");

            } else if (arg.equals("-customcore") || arg.equals("-cc")) {
                initParams.put("customcore", "true");

            } else if (arg.equals("-log") || arg.equals("-l")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "log file not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("log", nextArg);
                    i++;
                }

            } else if (arg.equals("-log.append") || arg.equals("-l.a")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "log.append file not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("log", nextArg);
                    initParams.put("log.append", "true");
                    i++;
                }

            } else if (arg.equals("-verbose") || arg.equals("-v")) {
                initParams.put("verbose", "true");

            } else if (arg.equals("-debug")) {
                initParams.put("debug", "true");

            } else if (arg.startsWith("-")) {
                numProblems++;
                String msg = "unrecognized option: " + arg;
                initParams.put("problem" + numProblems, msg);

            } else {
                scriptArgs = new String[args.length - i];
                int j = 0;
                while (i < args.length) {
                    scriptArgs[j++] = args[i++];
                }
                break;
            }
        }
        initParams.put("problems", Integer.toString(numProblems));
        
        return scriptArgs;
    }
       
    private boolean init(Map<String, String> initParams) {
        try {    
            initGlobalSettings(initParams);
        } catch (Exception e) {
            exceptions = new Exception[1];
            exceptions[0] = e;
            return false;
        }
        return true;
    }

    protected void initGlobalSettings(Map<String, String> initParams) throws Exception {
        String param;
        
        param = initParams.get("verbose");
        if ("true".equalsIgnoreCase(param)) {
            CantoLogger.verbosity = CantoLogger.VERBOSE;
            CantoLogger.echoSystemOut = true;
        }
        
        logFileName = initParams.get("log");
        String appendLog = initParams.get("log.append");
        appendToLog = isTrue(appendLog);
        if (logFileName != null) {
            CantoLogger.setLogFile(logFileName, appendToLog);
        }

        cantoPath = initParams.get("cantopath");

        fileBase = initParams.get("filebase");
        if (fileBase == null) {
            fileBase = ".";
        }

        recursive = isTrue(initParams.get("recursive"));
        multithreaded = isTrue(initParams.get("multithreaded"));
        //shareCore = isTrue(initParams.get("sharecore"));
        //debuggingEnabled = isTrue(initParams.get("debug"));
    }

    /** Returns true if the passed string is a valid servlet parameter representation
     *  of true.
     */
    private static boolean isTrue(String param) {
        return ("true".equalsIgnoreCase(param) || "yes".equalsIgnoreCase(param) || "1".equalsIgnoreCase(param));
    }
    
    private List<Exception> getExceptions() {
        List<Exception> list = new ArrayList<Exception>();
        for (Exception e: exceptions) {
            if (e != null) {
                list.add(e);
            }
        }
        return list;
    }

   
    //
    // Run a program
    //
    
    private void runScript(Writer out, String[] args) {
        if (args == null || args.length == 0) {
            System.err.println("No script specified.");
            return;
        }
        sharedCore = new Core(true);
        try {
            load(cantoPath, recursive);
        } catch (Exception e) {
            System.err.println("Exception parsing cantopath: " + e);
        }
        
        String path = args[0];
        Node script = loadScript(path);
        if (script == null) {
            System.err.println("Unable to parse script");
            return;
        }
        
        if (script instanceof Site) {
            mainScript = (Site) script;
        } else {
            Node child = script.jjtGetChild(0);
            if (child instanceof Site) {
                mainScript = (Site) child;
            } else {
                System.err.println("Illegal script format.");
                return;
            }
        }

        Context scriptContext = null;
        try {
            scriptContext = new Context(mainScript);
        } catch (Redirection r) {
            System.err.println("Unable to instantiate script context: " + r.getMessage());
            return;
        }

        addExternalObject(scriptContext, this, "this_processor", "canto_processor");

        String expr = "main";
        expr = expr + "([";
        for (int i = 0; i < args.length; i++) {
            expr = expr + '"' + args[i] + '"';
            if (i < args.length - 1) {
                expr = expr + ',';
            }
        }
        expr = expr + "])";
        
        int exitCode = 0;
        try {
            CantoParser parser = new CantoParser(new StringReader(expr));
            Instantiation instance = parser.parseInstance();
            instance.setOwner(mainScript);
            Object data = instance.getData(scriptContext);
            System.out.print(data.toString());
        } catch (Exception e) {
            System.err.println("Exception running script: " + e);
        
        } catch (ScriptExit se) {
            exitCode = se.getStatus();
            if (exitCode == 0) {
                System.out.print(se.getTextOut());
            } else {
                if (se.getPreserveOutput()) {
                    System.out.print(se.getTextOut());
                }
                System.err.println(se.getMessage());
            }
            
        } catch (Redirection r) {
            System.err.println("Redirection running script: " + r);
        }
    }
    
    /** Writes to log file and system out. **/
    static void slog(String msg) {
        slog(msg, false);
    }

    static void slog(String msg, boolean urgent) {
        CantoLogger.log(msg, urgent);
    }

    /** Load the site files */
    public boolean load(String cantoPath, boolean recurse) throws Exception {
        boolean loadError = false;

        slog(NAME_AND_VERSION);
        slog("cantopath: " + cantoPath);

        SiteLoader.LoadOptions options = SiteLoader.getDefaultLoadOptions();
        options.multiThreaded = multithreaded;
        options.autoLoadCore = !customCore;
        options.configurable = false;

        SiteLoader loader = new SiteLoader(sharedCore, null, cantoPath, "*.can", recurse, options);
        loader.load();
        exceptions = loader.getExceptions();
        
        for (int i = 0; i < exceptions.length; i++) {
            if (exceptions[i] != null) {
                loadError = true;
                break;
            }
        }

        return loadError;
    }

    public Node loadScript(String path) {
        slog("loading script " + path);
        
        Node parseResult = null;
        Exception exception = null;
        SiteBuilder siteBuilder = new SiteBuilder(sharedCore);
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(path));
            CantoParser parser = new CantoParser(is);;
            parseResult = parser.parse(path);
            siteBuilder.build(parseResult);
            exception = siteBuilder.getException();
            if (exception != null) {
                throw exception;
            }
            slog("--- LINK PASS ---");
            parseResult.jjtAccept(new SiteLoader.Linker(), null);

        } catch (ParseException pe) {
            System.err.println("...syntax error in " + path + ": " + pe.getMessage());
            exception = pe;

        } catch (DuplicateDefinitionException dde) {
            System.err.println("...duplicate definition in " + path + ": " + dde.getMessage());
            exception = dde;

        } catch (Exception e) {
            exception = e;
            System.err.println("...exception loading " + path + ": " + e);
            System.out.flush();
            e.printStackTrace();

        } catch (canto.parser.TokenMgrError error) {
            exception = new ParseException(error.toString());
            System.err.println("...error loading " + path + ": " + error);
            System.out.flush();
            error.printStackTrace();
        }
        return parseResult;
    }

    /** Creates an external definition for the specified object and adds the definition
     *  to the core.  The external definition will have the specified name.  If supertype
     *  is non-null, and a type by that name exists, the external definition will have
     *  that type as its supertype.
     */
    public void addExternalObject(Context context, Object object, String name, String supertype) {
        Site owner = mainScript;
        Definition superdef = (supertype == null ? null : getDefinition(context, supertype));
        Type st = (superdef == null ? null : superdef.getType());
        Definition newDef = new ExternalDefinition(name, owner, owner, st, Definition.PUBLIC_ACCESS, Definition.GLOBAL, object, null);
        owner.addDefinition(newDef, true);
    }

    private Definition getDefinition(Context context, String name) {
        Definition def = null;
        NameNode nameNode = (name.indexOf('.') > 0 ? new ComplexName(name) : new NameNode(name));
        def = mainScript.getChildDefinition(nameNode, context);
        if (def == null && sharedCore != null) {
            def = sharedCore.getChildDefinition(nameNode, context);
        }
        return def;
    }
 
    //
    // CantoProcessor interface
    //

    /** Returns the name of this processor. **/
    public String name() {
        return NAME;
    }

    /** The highest Canto version number supported by this processor. **/
    public String version() {
        return VERSION;
    }

    /** Properties associated with this processor. **/
    public Map<String, Object> props() {
        return properties;
    }

    public String domain_type() {
        return Name.SCRIPT;
    }
    
    /** Compile the Canto source files found at the locations specified in <code>cantopath</code>
     *  and return a CantoDomain object.  If a location is a directory and <code>recursive</code>
     *  is true, scan subdirectories recursively for Canto source files.  If <code>autoloadCore</code>
     *  is true, and the core definitions required by the system cannot be found in the files
     *  specified in <code>cantopath</code>, the processor will attempt to load the core
     *  definitions automatically from a known source (e.g. from the same jar file that the
     *  processor was loaded from).
     */
    public canto_domain compile(String siteName, String cantopath, boolean recursive, boolean autoloadCore) {
        CantoSite site = new CantoSite(siteName, this);
        site.load(cantopath, "*.canto", recursive, multithreaded, autoloadCore, sharedCore);
        return site;
    }

    /** Compile Canto source code passed in as a string and return a canto_domain object.  If
     *  <code>autoloadCore</code> is true, and the core definitions required by the system cannot
     *  be found in the files specified in <code>cantopath</code>, the processor will attempt to
     *  load the core definitions automatically from a known source (e.g. from the same jar file
     *  that the processor was loaded from).
     */
    public canto_domain compile(String siteName, String cantotext, boolean autoloadCore) {
        return null;
    }

    /** Compile Canto source code passed in as a string and merge the result into the specified
     *  canto_domain.  If there is a fatal error in the code, the result is not merged and
     *  a Redirection is thrown.
     */
    public void compile_into(canto_domain domain, String cantotext) throws Redirection {
        ;
    }

}


