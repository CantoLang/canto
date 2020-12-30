/* Canto Compiler and Runtime Engine
 * 
 * SiteLoader.java
 *
 * Copyright (c) 2018-2020 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.io.*;
import java.net.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import canto.parser.*;
import canto.runtime.Context;
import canto.runtime.CantoObjectWrapper;
import canto.runtime.CantoServer;
import canto.runtime.CantoVisitor;
import canto.runtime.SiteBuilder;
import cantocore.CoreSource;

/**
 * A SiteLoader loads a Canto path.  Parsing occurs in a background thread.  This
 * allows SiteLoader to process multiple files simultaneously.
 *
 * @author Michael St. Hippolyte
 */
public class SiteLoader {


    public static class LoadOptions {
        public boolean multiThreaded = false;
        public boolean autoLoadCore = false;
        public boolean configurable = true;
        public boolean allowUnresolvedInstances = false;
        public int errorThreshhold = Context.EVERYTHING;
    }
    
    public static LoadOptions getDefaultLoadOptions() {
        return new LoadOptions();
    }

    
    private static final long LOAD_SLEEP = 200L;

    private static final int actions = 0; //SiteBuilder.LOG | SiteBuilder.DUMP_SOURCE | SiteBuilder.DUMP_PAGES;

    protected static void log(String str) {
        SiteBuilder.log(str);
    }

    private HashMap<String, String> loadedFiles = new HashMap<String, String>();
    private List<CantoSourceLoader> loaders = new ArrayList<CantoSourceLoader>();
    protected Core core;
    private String siteName;
    private String externalUrl;
    private String externalPath;
    private String internalPath;
    private String sourceString;
    private boolean recursive;
    private boolean internalRecursive;
    private boolean multiThreaded;
    private boolean loadCore;
    private boolean configurable;
    private String filter;
    private Object[] sources = null;
    private Node[] parseResults = null;
    private Exception[] exceptions = null;
    private site_config siteConfig = null;

    public SiteLoader(Core core, String siteName, String path, String filter, boolean recursive, LoadOptions options) {
        this.core = core;
        this.siteName = siteName;
        this.externalPath = path;
        this.filter = filter;
        this.recursive = recursive;
        this.multiThreaded = options.multiThreaded;
        this.loadCore = options.autoLoadCore;
        this.configurable = options.configurable;
    }

    public SiteLoader(Core core, String siteName, String src, boolean isUrl) {
        this.core = core;
        this.siteName = siteName;
        if (isUrl) {
            this.externalUrl = src;
        } else {
            this.sourceString = src;
        }
        this.multiThreaded = false;
        this.loadCore = true;
        this.configurable = false;
    }

    /** Load the Canto source code for this site.  The loading occurs in three
     *  steps:
     *  <ol>
     *  <li>If config.can is present as a resource in the classpath, it is loaded; else all .can files in the externally specified cantopath are loaded.</li>
     *  <li>If there is a definition for "cantopath" in a file loaded in the first step,
     *  all .can files in the path it specifies are loaded.</li>
     *  <li>If the Canto core was not loaded after the first two steps, and the loadCore
     *  property for this site is true, then the core can files are loaded from the
     *  corepath, if a definition for "corepath" has been loaded, or else as a resource
     *  from the cantocore directory in the classpath.</li>
     */
    public void load() {
        URL url = null;

    	// if the loadCore flag is set, load core canto files useing the loader
        // from the CoreSource class, which will most often be loading from canto.jar

        if (loadCore) {
            String[] corePaths = CoreSource.getCorePaths();
            Class<CoreSource> cs = CoreSource.class;
            try {
                for (int i = 0; i < corePaths.length; i++) {
                    String corePath = corePaths[i];
                    url = cs.getResource(corePath);
                    if (url == null) {
                        log(corePath + " not found");
                        continue;
                    }
                    loadURL(url, loaders, true);
                    log(corePath + " autoloaded");
                }

            } catch (Exception e) {
                if (corePaths.length == 0) {
                    log("Unable to autoload: " + e + " (also no core path specified)");
                } else {
                    String corePath = corePaths[0];
                    for (int i = 1; i < corePaths.length; i++) {
                        corePath = ", " + corePaths[i];
                    }
                    log("Unable to autoload " + corePath + ": " + e);
                }
            }
        }

    	String configPath = "/config.canto";
        Class<?> c = getClass();
        if (externalUrl != null) {
            log("Requesting source from " + externalUrl);
            try {
                url = new URL(null, externalUrl);
            } catch (MalformedURLException murl) {
                log("Bad URL");
                return;
            }
        } else if (configurable) {
            url = c.getResource(configPath);
            if (url != null) {
                log(configPath + " found in classpath");
            }
        }
        if (url != null) {
            loadURL(url, loaders, true);
            log(siteName + " loaded from " + url.toString());
        } else if (sourceString != null) {            
            loadString(sourceString, loaders, true);
            log(siteName + " loaded from source code");
        } else if (externalPath != null) {
            String[] paths = parsePath(externalPath);
            for (int i = 0; i < paths.length; i++) {
                loadFile(new File(paths[i]), filter, loaders, recursive, true);
            }
        }

        int firstStepSize = loaders.size();

        // wait for all the loader threads
        waitForLoaders(0, firstStepSize);


        // the second step requires querying the site as loaded up to this point
        // for its cantopath

        // determine the site to query
        Site site = core.getSite(Name.DEFAULT);
        if (site == null) {
            site = core;
        }

        log("site name is " + (siteName == null ? "null; running default site" : siteName));
        
        internalRecursive = recursive;
        if (configurable) {
            internalPath = null;
            try {
                Context context = null;
                if (siteName != null && siteName.length() > 0 && !siteName.equals(Name.DEFAULT)) {
                    Site thisSite = core.getSite(siteName);
                    if (thisSite != null) {
                        site = thisSite;
                        context = new Context(site);
                    }
                } else {
                    context = new Context(site);
                    String name = getProperty("sitename", site, context);
                    if (name != null && name.length() > 0 && !name.equals(Name.DEFAULT)) {
                    	siteName = name;
                        Site thisSite = core.getSite(siteName);
                        if (thisSite != null) {
                            site = thisSite;
                            context = new Context(site);
                        }
                    }
                }
                if (context == null) {
                    context = new Context(site);
                }
                
            	CantoObjectWrapper mainSite = getPropertyObject("main_site", site, context);
                Object[] sites = (Object[]) getPropertyArray("all_sites", site, context);
                
                if (sites == null || sites.length == 0) {
                	if (mainSite == null) {
                        throw new Redirection(Redirection.STANDARD_ERROR, "No definition for main_site or all_sites");
                	}
                	site_config sc = new CantoServer.site_config_wrapper(mainSite);
                	String name = sc.name();
                	if (siteName.equals(name)) {
                		siteConfig = sc;
                		internalPath = sc.cantopath();
                        internalRecursive = sc.recursive();
                	}
                	site.setSiteConfig(sc);
                } else {
                    for (Object siteObj: sites) {
                    	CantoObjectWrapper obj = (CantoObjectWrapper) siteObj;
                    	site_config sc = new CantoServer.site_config_wrapper(obj);
                    	String name = sc.name();
                    	if (siteName.equals(name)) {
                    		siteConfig = sc;
                    		internalPath = sc.cantopath();
                    		internalRecursive = sc.recursive();
                    		break;
                    	}
                    }
                }
                
                
            } catch (Redirection r) {
                log("Problem loading site: unable to determine site properties: " + r.getMessage());
                throw new RuntimeException(r.getMessage());
            }

            if (internalPath != null && internalPath.length() > 0 && !internalPath.equals(externalPath)) {
                String[] paths = parsePath(internalPath);
                for (int i = 0; i < paths.length; i++) {
                    loadFile(new File(paths[i]), filter, loaders, internalRecursive, true);
                }

                int secondStepSize = loaders.size() - firstStepSize;

                // wait for the loader threads added in the second step
                waitForLoaders(firstStepSize, secondStepSize);
            }
        }
        


        // everything is loaded, now link
        link(loaders);
        
        synchronized (loadedFiles) {
            int size = loaders.size();
            sources = new Object[size];
            parseResults = new Node[size];
            exceptions = new Exception[size];

            for (int i = 0; i < size; i++) {
                CantoSourceLoader loader = loaders.get(i);
                Object source = loader.getSource();
                String sourceId = (source instanceof File ? ((File) source).getAbsolutePath() : source.toString());
                Exception e = loader.getException();
                if (e != null) {
                    loadedFiles.put(sourceId, "Exception: " + e.toString());
                } else {
                    loadedFiles.put(sourceId, "OK");
                }
                sources[i] = source;
                parseResults[i] = loader.getParseResult(); 
                exceptions[i] = e;
            }
        }
    }

    private String getProperty(String name, Site site, Context context) {
        String prop = null;
        Instantiation instance = null;
        NameNode reference = null;
        if (name.indexOf('.') > 0) {
        	reference = new ComplexName(name);
        } else {
        	reference = new NameNode(name);
        }
        instance = new Instantiation(reference, site);

        try {
            prop = instance.getText(context);
        } catch (Redirection r) {
            log("Problem getting property " + name + ", redirects to " +  r.getLocation());
        }

        return prop;
    }

    private CantoObjectWrapper getPropertyObject(String name, Site site, Context context) {
        CantoObjectWrapper prop = null;
        Instantiation instance = null;
        NameNode reference = null;
        if (name.indexOf('.') > 0) {
        	reference = new ComplexName(name);
        } else {
        	reference = new NameNode(name);
        }
        instance = new Instantiation(reference, site);

        try {
        	Object obj = instance.getData(context);
        	if (obj instanceof canto.lang.NullValue) {
        		prop = null;
        	} else {
                prop = (CantoObjectWrapper) obj;
        	}
        } catch (Redirection r) {
            log("Problem getting property " + name + ", redirects to " +  r.getLocation());
        }

        return prop;
    }

    private Object[] getPropertyArray(String name, Site site, Context context) {
        Object[] props = null;
        Object propsObj = getPropertyCollectionObject(name, site, context);
            
        if (propsObj instanceof List<?>) {
            int len = ((List<?>) propsObj).size();
            props = new Object[len];
            props = ((List<?>) propsObj).toArray(props);
               
        } else if (propsObj instanceof Object[]) {
            props = (Object[]) propsObj;
        }

        return props;
    }

    private Object getPropertyCollectionObject(String name, Site site, Context context) {
        Object collectionObj = null;
        Instantiation instance = null;
        NameNode reference = new NameNode(name);
        instance = new Instantiation(reference, site);
        try {
            collectionObj = instance.getData(context);
            if (collectionObj instanceof CantoArray) {
                collectionObj = ((CantoArray) collectionObj).instantiateArray(context);
            } 
                
            if (collectionObj instanceof ResolvedCollection) {
                collectionObj = ((ResolvedCollection) collectionObj).getCollectionObject();
            }

            if (collectionObj instanceof List || collectionObj.getClass().isArray()) {
                collectionObj = ArrayBuilder.instantiateElements(collectionObj, context);
            } else if (collectionObj instanceof Map) {
                collectionObj = TableBuilder.instantiateElements(collectionObj, context);
            }
      
        } catch (Redirection r) {
            log("Problem getting property " + name + ", redirects to " +  r.getLocation());
        }

        return collectionObj;

    }
    
    private final void waitForLoaders(int startIx, int endIx) {
        synchronized (loaders) {
            while (true) {
                boolean stillRunning = false;
                for (int i = startIx; i < endIx; i++) {
                    CantoSourceLoader loader = loaders.get(i);
                    if (loader.isLoading()) {
                        stillRunning = true;
                        break;
                    }
                }
                if (stillRunning) {
                    try {
                        Thread.sleep(LOAD_SLEEP);
                    } catch (InterruptedException ie) {
                        ;
                    }
                } else {
                    return;
                }
            }
        }
    }


    public Core getCore() {
        return core;
    }

    public Object[] getSources() {
        return sources;
    }

    public Node[] getParseResults() {
        return parseResults;
    }

    public Exception[] getExceptions() {
        return exceptions;
    }
    
    public site_config getSiteConfig() {
    	return siteConfig;
    }

    /** See if the passed file represents a wildcard specification. This includes any
     *  file whost path includes * or ?. 
     * @param file
     * @return
     */
    private boolean isWildCard(File file) {
        String path = file.getPath();
        int lastSeparator = path.lastIndexOf('/');
        return path.indexOf('*') > lastSeparator || path.indexOf('?') > lastSeparator;
    }
    
    private File[] expandWildCard(File file) throws IOException {
        List<File> fileList = new ArrayList<File>();
        File[] files = new File[0];
        String pathStr = file.getPath();
        int lastSeparator = pathStr.lastIndexOf('/');

        Path path = lastSeparator >= 0 ? Paths.get(pathStr.substring(0,  lastSeparator)) : Paths.get(".");
        String glob = lastSeparator >= 0 ? pathStr.substring(lastSeparator + 1) : pathStr;
        DirectoryStream<Path> stream =  Files.newDirectoryStream(path, glob);
        for (Path entry: stream) {
            fileList.add(entry.toFile());
        }
        return (File[]) fileList.toArray(files);
    }
    
    private void loadFile(File path, String filter, List<CantoSourceLoader> loaders, boolean recursive, boolean wait) {
        try {

            // recurse subdirectories
            if (path.isDirectory() || isWildCard(path)) {
                File[] files = null;
                if (path.isDirectory()) {
                    log("Scanning directory " + path.getPath());
                    files = path.listFiles();
                } else {
                    files = expandWildCard(path);
                    // don't filter expanded wildcards
                    filter = null;
                }
                int last = files.length - 1;
                for (int i = 0; i < last; i++) {
                    if (files[i].isDirectory()) {
                        if (recursive) {
                            loadFile(files[i], filter, loaders, recursive, !multiThreaded);
                        }
                    } else if (matches(files[i], filter)) {
                        loadFile(files[i], filter, loaders, recursive, !multiThreaded);
                    } else {
                        log("Skipping " + files[i].getAbsolutePath());
                    }
                }
                if (last >= 0 ) {
                    if (files[last].isDirectory()) {
                        if (recursive) {
                            loadFile(files[last], filter, loaders, recursive, !multiThreaded);
                        }
                    } else if (matches(files[last], filter)) {
                        loadFile(files[last], filter, loaders, recursive, !multiThreaded);
                    } else {
                        log("Skipping " + files[last].getAbsolutePath());
                    }
                }
            } else {
                String abspath = path.getAbsolutePath();
                boolean load = false;
                synchronized (loadedFiles) {
                    if (loadedFiles.get(abspath) == null) {
                        loadedFiles.put(abspath, "loading");
                        load = true;
                    }
                }

                if (load) {
                    log("Loading " + abspath + "...");
                    CantoSourceLoader fileLoader = new CantoSourceLoader(path, actions);
                    fileLoader.load(wait);
                    loaders.add(fileLoader);
                    System.out.flush();
                }
            }

        } catch (Exception e) {
            log("Exception loading file: " + path.getAbsolutePath() + ": " + e);
            System.out.flush();
            e.printStackTrace();
        }
    }

    private void loadURL(URL url, List<CantoSourceLoader> loaders, boolean wait) {
        try {
            String urlname = url.toString();
            boolean load = false;
            synchronized (loadedFiles) {
                if (loadedFiles.get(urlname) == null) {
                    loadedFiles.put(urlname, "loading");
                    load = true;
                }
            }

            if (load) {
                log("Loading " + url.toString() + "...");
                CantoSourceLoader urlLoader = new CantoSourceLoader(url, actions);
                urlLoader.load(wait);
                loaders.add(urlLoader);
            }

        } catch (Exception e) {
            log("Exception loading URL: " + url.toString() + ": " + e);
            System.out.flush();
            e.printStackTrace();
        }
    }

    private void loadString(String src, List<CantoSourceLoader> loaders, boolean wait) {
        try {
            log("Loading source from string...");
            
            Reader reader = new StringReader(src);
            String srcName = reader.toString();
            synchronized (loadedFiles) {
                if (loadedFiles.get(srcName) == null) {
                    loadedFiles.put(srcName, "loading");
                }
            }

            CantoSourceLoader srcLoader = new CantoSourceLoader(reader, actions);
            srcLoader.load(wait);
            loaders.add(srcLoader);

        } catch (Exception e) {
            log("Exception loading source code: " + e);
            System.out.flush();
            e.printStackTrace();
        }
    }

    private void link(List<CantoSourceLoader> loaders) {
        for (int i = 0; i < loaders.size(); i++) {
            CantoSourceLoader loader = loaders.get(i);
            Node parseResult = loader.getParseResult();
            if (parseResult != null) {
                log("--- LINK PASS ---");
                parseResult.jjtAccept(new Linker(), null);
            }
        }
    }
    
    public static class LinkException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public LinkException(String message) {
            super(message);
        }
        
    }

    public static class Linker extends CantoVisitor {

    	private boolean errorOnUnresolvedType = false;
    	
        public Linker() { }
        
        public Linker(boolean errorOnUnresolvedType) {
        	this.errorOnUnresolvedType = errorOnUnresolvedType;
        }
    	
    	public Object handleNode(CantoNode node, Object data) {
            if (node instanceof Instantiation) {
                Instantiation instance = (Instantiation) node;
                if (!instance.isDynamic()) {
                    if (instance.getKind() == Instantiation.DYNAMICALLY_RESOLVED
                         || instance.getKind() == Instantiation.UNRESOLVED) {
    
                        instance.resolve(data);
                    }
                    int kind = instance.getKind();
                    if (kind == Instantiation.UNRESOLVED) {
                        System.err.println(instance.getName() + " UNRESOLVED!!!");
                    }
                }
            } else if (node instanceof Type) {
                Type type = (Type) node;
                if (type.getDefinition() == null && !type.isPrimitive() && !type.isSpecial()) {
                	type.resolve();
                    if (errorOnUnresolvedType && type.getDefinition() == null && !type.isExternal()) {
                        throw new LinkException("Unable to resolve type " + type.getName());
                    }
                }

            } else if (node instanceof NamedDefinition) {
                // resolve children first
                super.handleNode(node, data);
                
                NamedDefinition def = (NamedDefinition) node;
                def.resolveKeeps();
                
                if (def instanceof CollectionDefinition && ((CollectionDefinition) def).getDims() == null) {
                    Type type = def.getType();
                    if (type != null) {
                        if (type.getDefinition() == null && !type.isPrimitive()) {
                            type.resolve();
                        }
                        for (Type t = type.getSuper(); t != null; t = t.getSuper()) {
                            if (t.getDefinition() == null && !t.isPrimitive()) {
                                t.resolve();
                            }
                            if (t.isCollection() && ((CollectionDefinition) t.getDefinition()).getDims() != null) {
                                ((CollectionDefinition) def).setDims(((CollectionDefinition) t.getDefinition()).getDims());
                                break;
                            }
                        }
                    }
                }
                
                return data;
                
//            } else if (node instanceof ExternalDefinition) {
//                // ExternalDefinitions don't get treated like other definitions; they
//                // don't automatically support the "definition" interface
//               
//            } else if (node instanceof DefParameter) {
//                // these are also not automatically made into "definition" types
//                
//            } else if (node instanceof NamedDefinition) {
//                NamedDefinition def = (NamedDefinition) node;
//                if (node instanceof Site) {
//                    Site site = (Site) node;
//                    definitionDef = site.getDefinition("definition");
//                    definitionType = definitionDef.getType();
//                }
//                // avoid circularity -- this means that ancestors of definition cannot
//                // themselves be definitions, even though technically they are
//                if (definitionType != null && !def.equalsOrExtends(definitionDef) && !definitionDef.equalsOrExtends(def)) {
//                    Type st = def.getSuper();
//                    // only set the super to definition for nodes at the top of their
//                    // hierarchy (i.e., they have no supers themselves)
//                    if (st == null) {
//                        log("Setting definition as supertype of " + def.getName());
//                        def.setSuper(definitionType);
//                    }
//                }
//                
//                // FIXME: need to resolve the keeps that weren't resolved before
//                //((NamedDefinition) node).resolveKeeps();
//            
            }
            return super.handleNode(node, data);
        }
    }
    
    private static boolean matches(File file, String filter) {
        if (filter != null) {
            // for now, just match extensions
            int filterExt = filter.lastIndexOf('.');
            if (filterExt > -1) {
                String name = file.getName();
                int fileExt = name.lastIndexOf('.');
                return (fileExt >= 0 && name.substring(fileExt).equals(filter.substring(filterExt)));
            }
        }
        // no or unrecognized filter
        return true;
    }

    private static String[] parsePath(String path) {
        StringTokenizer toker = new StringTokenizer(path, File.pathSeparator);
        String[] subpaths = new String[toker.countTokens()];
        for (int i = 0; i < subpaths.length; i++) {
            subpaths[i] = toker.nextToken().trim();
        }
        return subpaths;
    }



    private class CantoSourceLoader implements Runnable {

        private Node parseResult = null;
        private Exception exception = null;
        private Object source;
        private Thread loaderThread = null;
        private Object semaphore = new Object();
        private int actions;

        public CantoSourceLoader(Object source, int actions) {
            this.source = source;
            this.actions = actions;
        }

        public Node getParseResult() {
            return parseResult;
        }
        
        public Object getSource() {
            return source;
        }

        public String getSourceName() {
            if (source instanceof File || source instanceof URL) {
                return source.toString();
            } else {
                return "passed source code";
            }
        }

        
        private CantoParser getCantoParser() throws IOException {
            if (source instanceof Reader) {
                return new CantoParser((Reader) source);
            } else {
                return new CantoParser(getInputStream());
            }
        }

        private InputStream getInputStream() throws IOException {
            InputStream is = null;
            if (source instanceof InputStream) {
                is = (InputStream) source;
            } if (source instanceof File) {
                is = new BufferedInputStream(new FileInputStream((File) source));
            } else if (source instanceof URL) {
                is = new BufferedInputStream(((URL) source).openStream());
            } else {
                throw new IOException("Invalid source type: " + source.getClass());
            }
            return is;
        }

        protected void load(boolean wait) {
            if (loaderThread != null && loaderThread.isAlive()) {
                throw new ConcurrentModificationException("Document already loading");
            }
            loaderThread = new Thread(this);
            loaderThread.setDaemon(true);
            loaderThread.start();
            if (wait) {
                finishLoading();
            }
        }

        protected boolean isLoading() {
            return loaderThread != null;
        }

        private void finishLoading() {
            if (Thread.currentThread() == loaderThread) {
                throw new IllegalStateException("can't call finishLoading() from the loader thread");
            }
            if (isLoading()) {
                synchronized (semaphore) {
                    try {
                        semaphore.wait();
                    } catch (InterruptedException ie) {
                        ;
                    }
                }
            }
        }


        /**
         * Loader thread.
         */
        public void run() {

            SiteBuilder siteBuilder = new SiteBuilder(core);
            try {
                CantoParser parser = getCantoParser();
                Node parseResult = parser.parse(getSourceName());
                siteBuilder.build(parseResult, actions);
                exception = siteBuilder.getException();
                this.parseResult = parseResult;

            } catch (ParseException pe) {
                SiteBuilder.log("...syntax error in " + getSourceName() + ": " + pe.getMessage());
                exception = pe;

            } catch (DuplicateDefinitionException dde) {
                SiteBuilder.log("...duplicate definition in " + getSourceName() + ": " + dde.getMessage());
                exception = dde;

            } catch (Exception e) {
                exception = e;
                SiteBuilder.log("...exception loading " + getSourceName() + ": " + e);
                System.out.flush();
                e.printStackTrace();

            } catch (canto.parser.TokenMgrError error) {
                exception = new ParseException(error.toString());
                SiteBuilder.log("...error loading " + getSourceName() + ": " + error);
                System.out.flush();
                error.printStackTrace();

            } finally {
                loaderThread = null;
                synchronized (semaphore) {
                    semaphore.notifyAll();
                }
            }
        }

        protected Exception getException() {
            return exception;
        }
    }
}

