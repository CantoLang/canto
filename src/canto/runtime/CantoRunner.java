/* Canto Compiler and Runtime Engine
 * 
 * CantoRunner.java
 *
 * Copyright (c) 2018-2021 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.*;
import java.util.*;

import canto.lang.*;

/**
 * Run a Canto program from the command line.
 *
 * CantoRunner compiles a Canto site, instantiates an object and outputs the result.
 *
 * @author
 */

public class CantoRunner implements canto_processor {
    public static final String NAME = "CantoRunner";
    public static final String VERSION = "1.0";
    public static final String NAME_AND_VERSION = NAME + " " + VERSION;

    protected Exception exception = null;
    protected CantoSite mainSite = null;
    protected Map<String, CantoSite> sites = new HashMap<String, CantoSite>();

    private boolean initedOk = false;
    private String logFileName = null;
    private boolean appendToLog = true;
    private String fileBase = ".";
    private boolean multithreaded = false;
    private String cantoPath = ".";
    private boolean recursive = false;
    private boolean customCore = false;
    private Core sharedCore = null;
    private HashMap<String, Object> properties = new HashMap<String, Object>();
    protected String fileHandlerName = null;
    private String request = null;

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

        boolean noProblems = true;
        
        Map<String, String> initParams = paramsFromArgs(args);
        
        String problems = initParams.get("problems");
        if (problems != null && !problems.equals("0")) {
            noProblems = false;
        }
        
        if (noProblems) {
            CantoRunner runner = new CantoRunner(initParams);
            Writer writer = new OutputStreamWriter(System.out);
            if (runner.initedOk) {
                try {
                    runner.loadSite();
                    runner.run(writer);
                } catch (Throwable t) {
                    noProblems = false;
                    System.err.println("Problem running CantoRunner: " + t.getMessage());
                    t.printStackTrace(System.err);
                }
            } else {
                noProblems = false;
            }

        } else {
            System.out.println("Usage:");
            System.out.println("          java -jar canto.jar [flags] obj_name\n");
            System.out.println("where obj_name is the name of the object to be instantiated and\n");
            System.out.println("the optional flags are among the following (in any order):\n");
            System.out.println("Flag                              Effect");
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("-cantopath <pathnames>       Sets the initial cantopath, which is a string");
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
    public CantoRunner(Map<String, String> initParams) {
        initedOk = init(initParams);
        request = initParams.get("request");
    }

    private static Map<String, String> paramsFromArgs(String[] args) {
        Map<String, String> initParams = new HashMap<String, String>();
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

            } else if (arg.equals("-address") || arg.equals("-a")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "address not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("address", nextArg);
                    i++;
                }

            } else if (arg.equals("-port") || arg.equals("-p")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "port not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("port", nextArg);
                    i++;
                }

            } else if (arg.equals("-host") || arg.equals("-h")) {
                if (noNextArg) {
                    numProblems++;
                    String msg = "host not provided";
                    initParams.put("problem" + numProblems, msg);
                } else {
                    initParams.put("host", nextArg);
                    i++;
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

            } else if (arg.equals("-sharecore") || arg.equals("-sc")) {
                initParams.put("sharecore", "true");

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
                while (i++ < args.length - 1) {
                    arg = arg + " " + args[i];
                }
                initParams.put("request", arg);
                break;
            }
        }
        initParams.put("problems", Integer.toString(numProblems));
        
        return initParams;
    }
       
    private boolean init(Map<String, String> initParams) {
        try {    
            initGlobalSettings(initParams);
        } catch (Exception e) {
            exception = e;
            return false;
        }
        return true;
    }

    protected void initGlobalSettings(Map<String, String> initParams) throws Exception {
        String param;
        
        param = initParams.get("verbose");
        if ("true".equalsIgnoreCase(param)) {
            CantoLogger.verbosity = CantoLogger.VERBOSE;
        }
        
        logFileName = initParams.get("log");
        String appendLog = initParams.get("log.append");
        appendToLog = isTrue(appendLog);
        if (logFileName != null) {
            CantoLogger.setLogFile(logFileName, appendToLog);
        }

        cantoPath = initParams.get("cantopath");
        if (cantoPath == null) {
            cantoPath = ".";
        }

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

    private void loadSite() throws Exception {
        mainSite = load("[runner]", cantoPath, recursive);
        if (mainSite == null) {
            System.err.println("Unable to run.");
            return;
        } else if (mainSite.getException() != null) {
            throw mainSite.getException(); 
        }
        mainSite.globalInit();
        mainSite.siteInit();
    }
    
    //
    // Run a program
    //
    
    private void run(Writer out) throws Redirection, IOException {
        String req = request;
        CantoSite site = mainSite; 
        
        if (req != null && req.length() > 0) {
            if (sites != null) {
                int ix = req.indexOf('/');
                while (ix == 0) {
                    req = req.substring(1);
                    ix = req.indexOf('/');
                }
                if (ix < 0) {
                    if (sites.containsKey(req)) {
                        site = (CantoSite) sites.get(req);
                        req = null;
                    }
                } else if (ix > 0) {
                    String siteName = req.substring(0, ix);
                    if (sites.containsKey(siteName)) {
                        site = (CantoSite) sites.get(siteName);
                        req = req.substring(ix + 1);
                    }
                }
            }
        }
        
        if (req == null || req.length() == 0) {
            req = "run";
        }
        if (site != null) {
            site.run(req, out);
        }
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

    public CantoSite getMainSite () {
    	return mainSite;
    }
    
    public Map<String, CantoSite> getSiteMap() {
    	return sites;
    }
    
  
    /** Compile the Canto source files found at the locations specified in <code>cantopath</code>
     *  and return a canto_domain object.  If a location is a directory and <code>recursive</code>
     *  is true, scan subdirectories recursively for Canto source files.  If <code>autoloadCore</code>
     *  is true, and the core definitions required by the system cannot be found in the files
     *  specified in <code>cantopath</code>, the processor will attempt to load the core
     *  definitions automatically from a known source (e.g. from the same jar file that the
     *  processor was loaded from).
     */
    public canto_domain compile(String siteName, String cantopath, boolean recursive, boolean autoloadCore) {
        CantoSite site = new CantoSite(siteName, this);
        site.load(cantopath, "*.can", recursive, multithreaded, autoloadCore, sharedCore);
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

    public String domain_type() {
        return Name.SITE;
    }
    
    
    /** Writes to log file and system out. **/
    static void slog(String msg) {
        CantoLogger.log(msg);
        // avoid redundant echo
        if (!CantoLogger.echoSystemOut) {
            System.out.println(msg);
        }
    }

    /** Load the site files */
    public CantoSite load(String sitename, String cantoPath, boolean recurse) throws Exception {
        CantoSite site = null;

        slog(NAME_AND_VERSION);
        slog("Loading site " + (sitename == null ? "(no name yet)" : sitename));
        site = (CantoSite) compile(sitename, cantoPath, recurse, !customCore);
        Exception e = site.getException();
        if (e != null) {
            slog("Exception loading site " + site.getName() + ": " + e);
            throw e;
        }
        return site;
    }
}


