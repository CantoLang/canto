/* Canto Compiler and Runtime Engine
 * 
 * CantoDomain.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import canto.lang.*;
import canto.parser.CantoParser;
import canto.parser.Node;

/**
 * This interface extends the canto_domain interface, which corresponds to the canto_domain
 * object defined in core.  It represents a Canto domain, which consists of Canto code loaded
 * under a particular set of restrictions.
 */
public class CantoDomain implements canto_domain {

    private static void log(String string) {
        SiteBuilder.log(string);
    }

    
    private boolean loaded = false;
    private boolean loadError = false;
    private boolean globallyInitialized = false;

    protected Site defaultSite = null;
    protected Site site = null;
    protected Core core = null;

    Context defaultContext = null;
    Context siteContext = null;
    Context coreContext = null;

    private CantoDomain mainSite;
    private String domainName;
    private String domainType;
    private canto_processor cantoProcessor;
    private Map<String, CantoDomain> childDomains = null;

    private SiteLoader.LoadOptions loadOptions = null;
    
    protected String domainPath = null;
    protected Exception[] exceptions;
    protected Object[] sources;
    protected Node[] parseResults;

    protected boolean debuggingEnabled = false;

    /** Constructs a new root CantoDomain. 
     * 
     *  A CantoDomain is capable of loading and compiling Canto source code
     *  defining a group of related site objects.
     *
     *  The name parameter specifies the main site, which is the site queried for names
     *  that do not explicitly specify a site.  The sitename may be null or an empty string,
     *  in which case the name of the site is obtained from the main_site object,
     *  loaded from the default site.
     */
    public CantoDomain(String name, canto_processor processor) {
        if ("core".equals(name)) {
            throw new IllegalArgumentException("The name \"core\" is reserved.");
        }
        mainSite = this;
        cantoProcessor = processor;
        domainName = name;
        domainType = processor.domain_type();
    }

    /** Constructs a child CantoDomain. 
     */
    private CantoDomain(CantoDomain mainSite, String name, String domainType) {
        if ("core".equals(name)) {
            throw new IllegalArgumentException("The name \"core\" is reserved.");
        } else if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("A domain name must be provided.");
        } else if (mainSite.getName().equals(name)) {  // || (mainSite.childDomains != null && mainSite.childDomains.get(name) != null)) {
            throw new IllegalArgumentException("Domain name \"" + name + "\" already in use.");
        }

        if (mainSite.childDomains == null) {
            mainSite.childDomains = new HashMap<String, CantoDomain>();
        }
        mainSite.childDomains.put(name, this);

        this.mainSite = mainSite;
        this.childDomains = mainSite.childDomains;
        this.cantoProcessor = mainSite.cantoProcessor;
        this.domainName = name;
        this.domainType = domainType;
        this.core = new Core();
    }

    public CantoDomain getChildDomain(String name, String type) {
        CantoDomain child = null;

        if (childDomains != null) {
            child = childDomains.get(name);
            if (child != null) {
                if (type != null && child.domainType == type) {
                    return child;
                } else {
                    throw new IllegalArgumentException("Domain name \"" + name + "\" in use for a different domain type (" + child.domainType + ").");
                }
            }
        }
        return child;
    }
    
    public CantoDomain createChildDomain(String name, String type) {
        return new CantoDomain(this, name, type);
    }


    /** Returns the core at the root of this domain. */
    public Core getCore() {
        return core;
    }
    
    /** Returns the core that a child domain should use.  The default is to create
     *  a new core.
     **/
    public Core getChildCore() {
        return core;
    }
    
    /** Returns the primary site object. */
    public Site getSite() {
        return site;
    }
    
    public site_config getSiteConfig() {
        return site.getSiteConfig();
    }
    
    public void setSiteConfig(site_config siteConfig) {
        site.setSiteConfig(siteConfig);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String getName() {
        return domainName;
    }

    public Node[] getParseResults() {
        return parseResults;
    }

    public Exception getException() {
        Exception e = null;
        for (int i = 0; i < exceptions.length; i++) {
            if (exceptions[i] != null) {
                if (e == null) {
                    e = exceptions[i];
                } else if (e instanceof BundledException) {
                    ((BundledException) e).add(exceptions[i]);
                } else {
                    e = new BundledException(e, exceptions[i]);
                }
            }
        }
        return e;
    }

    public static class BundledException extends Exception {
        private static final long serialVersionUID = 1L;

        List<Exception> exceptions = new ArrayList<Exception>();

        public BundledException(Exception e1, Exception e2) {
            exceptions.add(e1);
            exceptions.add(e2);
        }

        public List<Exception> getExceptions() {
            return exceptions;
        }

        public void add(Exception e) {
            exceptions.add(e);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(String.valueOf(exceptions.size()));
            sb.append(" exceptions: ");
            Iterator<Exception> it = exceptions.iterator();
            while (it.hasNext()) {
                sb.append(it.next().toString());
                if (it.hasNext()) {
                    sb.append("; ");
                }
            }
            return sb.toString();
        }
    }

   
    public boolean load(String src, boolean isUrl, Core sharedCore, SiteLoader.LoadOptions options) {
        if (sharedCore == null) {
            sharedCore = new Core();
        }
        this.core = sharedCore;
        this.loadOptions = options;
        SiteLoader loader = new SiteLoader(core, domainName, src, isUrl);

        if (reload(loader, sharedCore)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean load(String domainPath, String filter, boolean recursive, Core sharedCore, SiteLoader.LoadOptions options) {
        if (sharedCore == null) {
            sharedCore = new Core(true);
        }
        this.core = sharedCore;
        this.loadOptions = options;
        SiteLoader loader = new SiteLoader(core, domainName, domainPath, filter, recursive, options);

        if (reload(loader, sharedCore)) {
            return true;
        } else {
            return false;
        }
    }

    private synchronized boolean reload(SiteLoader loader, Core core) {
        loaded = false;
        loadError = false;
        defaultSite = null;
        defaultContext = null;
        site = null;
        siteContext = null;
        
        loader.load();
        loaded = true;
        sources = loader.getSources();
        exceptions = loader.getExceptions();
        parseResults = loader.getParseResults();
        
        for (int i = 0; i < exceptions.length; i++) {
            if (exceptions[i] != null) {
                loadError = true;
                break;
            }
        }
        try {
            coreContext = new Context(core);
            coreContext.setErrorThreshhold(loadOptions.errorThreshhold);
            
        } catch (Redirection r) {
            log("Unable to instantiate core context: " + r.getMessage());
            loadError = true;
        }

        defaultSite = core.getSite(Name.DEFAULT);
        if (defaultSite != null) {
            try {
                defaultContext = new Context(defaultSite);
                defaultContext.setErrorThreshhold(loadOptions.errorThreshhold);
            } catch (Redirection r) {
                log("Unable to instantiate default site context: " + r.getMessage());
                loadError = true;
            }
        }

        // getProperty will only see definitions in the default site at
        // this point, because the site and siteContext are still null
        if (domainName == null || domainName.equals(Name.DEFAULT)) {
            String mainSiteName = getProperty("sitename", "");
            if (mainSiteName.length() > 0 && !mainSiteName.equals(Name.DEFAULT)) {
                domainName = mainSiteName;
            } else if (domainName == null) {
                domainName = Name.DEFAULT;
            }
        }

        if (!domainName.equals(Name.DEFAULT)) {
            site = core.getSite(domainName);
            // give site a non-null value if necessary
            if (site == null) {
                log("No data for site " + domainName + " in cantopath; creating empty site");
                site = new Site(domainName);
                core.addSite(site);
            }
        } else if (defaultSite == null) {
            log("No data for site in cantopath.");
        }

        globalInit();
        
        // initialize values used by the runtime system that are defined
        // by the site itself
        log("Initializing site values");

        int v = getIntProperty("verbosity", -1);
        if (v >= 0) {
            SiteBuilder.verbosity = v;
            log("    site verbosity level is " + SiteBuilder.verbosity);
        } else {
            log("    no value provided for verbosity level; using externally defined value (" + SiteBuilder.verbosity + ")");
        }

        if (!loadError) {
            if (site != null) {
                try {
                    siteContext = new Context(site);
                    siteContext.setErrorThreshhold(loadOptions.errorThreshhold);
                } catch (Redirection r) {
                    log("Unable to instantiate site context: " + r.getMessage());
                    loadError = true;
                }
            }
            try {
                init();
            } catch (Redirection r) {
                log("Unable to initialize site: " + r.getMessage());
                loadError = true;
            }
        }
        
        return !loadError;
    }

    /** Creates an external definition for the specified object and adds the definition
     *  to the core.  The external definition will have the specified name.  If supertype
     *  is non-null, and a type by that name exists, the external definition will have
     *  that type as its supertype.
     */
    public void addExternalObject(Object object, String name, String supertype) {
        Site owner = core;
        Definition superdef = (supertype == null ? null : getDefinition(supertype));
        Type st = (superdef == null ? null : superdef.getType());
        Definition newDef = new ExternalDefinition(name, owner, owner, st, Definition.PUBLIC_ACCESS, Definition.GLOBAL, object, null);
        owner.addDefinition(newDef, true);
    }

    /** Returns an integer value defined in this site.  If no definition exists by the
     *  specified name, the value of the <code>notFound</code> argument is returned.
     */
    public String getProperty(String name, String notFound)  {
        try {
            String prop = getPropertyInContext(name, null);
            return (prop == null ? notFound : prop);
        } catch (Redirection r) {
            log("Unable to get property " + name + ": " +  r.getMessage());
            return notFound;
        }
    }        

    /** Returns a value defined in this site as a string, or a redirection if no definition 
     *  exists by the specified name.
     * @throws Redirection 
     */
    public String getProperty(String name) throws Redirection {
        return getPropertyInContext(name, null);
    }        

    public String getPropertyInContext(String name, Context context) throws Redirection {
        String prop = null;
        Instantiation instance = null;
        NameNode reference = new NameNode(name);
        if (siteContext != null) {
            instance = new Instantiation(reference, site);
            if (instance.isDefined(siteContext)) {
                if (context == null) {
                    context = siteContext;
                }
            } else { 
                instance = null;
            }
        }
       
        
        if (instance == null && defaultContext != null) {
            instance = new Instantiation(reference, defaultSite);
            if (instance.isDefined(defaultContext)) {
                if (context == null) {
                    context = defaultContext;
                }
            } else {
                instance = null;
            }
        }
        
        if (instance == null) {
            return null;
        }

        prop = instance.getText(context);

        return prop;
    }

    public Object[] getPropertyArray(String name) {
        Object[] props = null;
        Object propsObj = getPropertyCollectionObject(name);
            
        if (propsObj instanceof List<?>) {
            int len = ((List<?>) propsObj).size();
            props = new Object[len];
            props = ((List<?>) propsObj).toArray(props);
               
        } else if (propsObj instanceof Object[]) {
            props = (Object[]) propsObj;
        }

        return props;
    }

    public List<?> getPropertyList(String name) {
        List<?> propList = null;
        Object propsObj = getPropertyCollectionObject(name);
            
        if (propsObj instanceof List<?>) {
            propList = (List<?>) propsObj;
            
        } else if (propsObj instanceof Object[]) {
            Object[] props = (Object[]) propsObj;
            propList = Arrays.asList(props);
        }

        return propList;
    }

    public Object getPropertyCollectionObject(String name) {
        Object collectionObj = null;
        Context context = null;
        Instantiation instance = null;
        NameNode reference = new NameNode(name);
        if (siteContext != null) {
            instance = new Instantiation(reference, site);
            if (instance.isDefined(siteContext)) {
                context = siteContext;
            } else {
                instance = null;
            }
        }
        
        if (context == null && defaultContext != null) {
            instance = new Instantiation(reference, defaultSite);
            if (instance.isDefined(defaultContext)) {
                context = defaultContext;
            } else {
                instance = null;
            }
        }
        
        if (instance == null) {
            return null;
        }

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
                // any function equivalent to instantiateElements we can call?
            }
      
        } catch (Redirection r) {
            log("Problem getting property " + name + ", redirects to " +  r.getLocation());
        }

        return collectionObj;
    }

    /** Returns a boolean value defined in this site, or false if no definition exists by the
     *  specified name.
     */
    public boolean getBooleanProperty(String name) {
        Context context = null;
        Instantiation instance = null;
        NameNode reference = new NameNode(name);
        if (siteContext != null) {
            instance = new Instantiation(reference, site);
            if (instance.isDefined(siteContext)) {
                context = siteContext;
            } else {
                instance = null;
            }
        }
        
        if (context == null && defaultContext != null) {
            instance = new Instantiation(reference, defaultSite);
            if (instance.isDefined(defaultContext)) {
                context = defaultContext;
            } else {
                instance = null;
            }
        }
        
        if (instance == null) {
            return false;
        } else {
            return instance.getBoolean(context);
        }
    }

    /** Returns an integer value defined in this site.  If no definition exists by the
     *  specified name, the value of the <code>notFound</code> argument is returned.
     */
    public int getIntProperty(String name, int notFound) {
        Context context = null;
        Instantiation instance = null;
        NameNode reference = new NameNode(name);
        
        if (siteContext != null) {
            instance = new Instantiation(reference, site);
            if (instance.isDefined(siteContext)) {
                context = siteContext;
            } else {
                instance = null;
            }
        }
        
        if (context == null && defaultContext != null) {
            instance = new Instantiation(reference, defaultSite);
            if (instance.isDefined(defaultContext)) {
                context = defaultContext;
            } else {
                instance = null;
            }
        }
        
        if (instance != null) {
            return instance.getInt(context);
        } else {
            return notFound;
        }
    }

    /** Returns an array containing all known subdefinitions of the definition (if any) for the
     *  specified name.
     */
    public Definition[] getSubdefinitions(String name) {
        Definition[] defaultSubdefs = null;
        Definition[] siteSubdefs = null;

        if (defaultSite != null) {
            defaultSubdefs = defaultSite.getSubdefinitions(name);
        }
        if (site != null) {
            siteSubdefs = site.getSubdefinitions(name);
        }

        if (defaultSubdefs == null) {
            return siteSubdefs;
        } else if (siteSubdefs == null) {
            return defaultSubdefs;
        } else {
            Definition[] subdefs = new Definition[siteSubdefs.length + defaultSubdefs.length];
            System.arraycopy(siteSubdefs, 0, subdefs, 0, siteSubdefs.length);
            System.arraycopy(defaultSubdefs, 0, subdefs, siteSubdefs.length, defaultSubdefs.length);
            return subdefs;
        }
    }

    /** Returns true if this domain contains a definition for the specified name. */
    public boolean isDefined(String name) {
        boolean defined = false;
        if (defaultSite != null) {
            defined = defaultSite.hasChildDefinition(name);
        }
        if (!defined && site != null) {
            defined = site.hasChildDefinition(name);
        }
        return defined;
    }

    public Definition getMainOwner() {
        if (site != null) {
            return site;
        } else if (defaultSite != null) {
            return defaultSite;
        } else {
            return core;
        }
    }


    public Definition getDefinition(String name) {
        Definition def = null;
        NameNode nameNode = (name.indexOf('.') > 0 ? new ComplexName(name) : new NameNode(name));
        if (site != null) {
            def = site.getChildDefinition(nameNode, siteContext);
        }
        if (def == null) {
            if (defaultSite != null) {
                def = defaultSite.getChildDefinition(nameNode, defaultContext);
            }
            if (def == null) {
                def = core.getChildDefinition(nameNode, coreContext);
            }
        }
        return def;
    }

    public Context getNewContext() {
        Context context = null;
        if (siteContext != null) {
            // a main site context exists, make a copy with a clear cache
            context = siteContext.clone(true);
        } else if (defaultContext != null) {
            // no main site context exists, clone the default (unnamed) site context
            context = defaultContext.clone(true);
        } else if (coreContext != null) {
            // no main or default site context exists, clone the core context
            context = coreContext.clone(true);
        } else {
            // no context of any sort exists, make a new one.
            try {
                context = new Context(site);
                context.setErrorThreshhold(loadOptions.errorThreshhold);
            } catch (Redirection r) {
                log("Unable to instantiate new context: " + r.getMessage());
            }
        }
        
        try {
            context.addKeeps(getMainOwner());
        } catch (Redirection r) {
            log("Unable to instantiate new context; addKeeps failed: " + r.getMessage());
        }

        return context;
    }

    public Instantiation getInstance(String typeName, String name, ArgumentList[] argLists, Context argContext) {
        return getInstance(site, typeName, name, argLists, argContext);
    }

    public Instantiation getInstance(Site site, String typeName, String name, ArgumentList[] argLists, Context argContext) {
        Definition def = getDefinition(name);
        if (def != null) {
            @SuppressWarnings("rawtypes")
            ListNode[] paramsAndArgs = def.getMatch(argLists, argContext);
            ArgumentList args = (paramsAndArgs == null ? null : (ArgumentList) paramsAndArgs[1]);
            return new Instantiation(def, args, null);
        } else {
            return null;
        }
    }

    void globalInit() {
        if (!globallyInitialized) {
            // initialize externally defined standard objects required by the site
            log("Initializing standard objects");
            addExternalObject(this, "this_domain", "canto_domain");
            addExternalObject(cantoProcessor, "this_processor", "canto_processor");
            addExternalObject(cantoProcessor, "this_server", "canto_server");
            globallyInitialized = true;
        } else {
            log("unneeded globalInit() call; domain already globally initialized");
        }
    }
    
    private void init() throws Redirection {
        getPropertyInContext("init", siteContext);
    }

    public String toString() {
        String str = "Canto Domain " + (domainName != null ? domainName : "(no name)") + '\n';
        if (site != null) {
            str += site.toString();
        } else {
            str += "(empty)";
        }
        str += '\n';
        return str;
    }
    
    //
    // canto_domain methods
    //
    
    public Object get(String expression) throws Redirection {
        try {
            CantoParser parser = new CantoParser(new StringReader(expression));
            Instantiation instance = parser.parseInstance();
            instance.setOwner(site);
            Object data = instance.getData(siteContext);
            return data;
        } catch (Exception e) {
            System.out.println("Exception parsing get expression: " + e);
            System.out.println("Expression:\n" + expression);
            return null;
        }
    }
    
    public Definition get_definition(String expression) throws Redirection {
        try {
            CantoParser parser = new CantoParser(new StringReader(expression));
            Instantiation instance = parser.parseInstance();
            instance.setOwner(site);
            return instance.getDefinition(siteContext);
        } catch (Exception e) {
            System.out.println("Exception getting definition: " + e);
            System.out.println("Expression:\n" + expression);
            return null;
        }
    }
    
    public Object get_instance(String expression) throws Redirection {
        try {
            CantoParser parser = new CantoParser(new StringReader(expression));
            Instantiation instance = parser.parseInstance();
            instance.setOwner(site);
            Construction construction = new ResolvedInstance(instance, siteContext, false);
            return new CantoObjectWrapper(construction, this);
        } catch (Exception e) {
            System.out.println("Exception getting definition: " + e);
            System.out.println("Expression:\n" + expression);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    public Object[] get_array(String expr) throws Redirection {
        Object obj = get(expr);
        if (obj instanceof List) {
            return ((List<Object>) obj).toArray();
        } else {
            return (Object[]) obj;
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> get_table(String expr) throws Redirection {
        return (Map<String, Object>) get(expr);
    }

    
    /** Returns a map of the sites in this domain (keyed on site name).
     **/
    public Map<String, Site> sites() {
        return core.sites();
    }

    /** Returns a map of the domains that are children of this domain (keyed on
     *  domain name).
     **/
    public Map<String, CantoDomain> domains() {
        return childDomains;
    }

    /** Returns the definition table associated with this domain. **/
    public Map<String, Definition> defs() {
        // not sure the DefinitionTable is quite what we want, but it's close and it
        // already exists
        return site.getDefinitionMap();
    }

    /** Returns the name of the main site.  The main site is determined when this object is
     *  constructed, generally based on designated in the 
     *  site configuration file.
     *   
     * 
     *  The main site is the first site to be queried when 
     *  resolving a name (that does not explicitly specify a site), followed by adopted sites,
     *  the default site and finally the core.  Once initialized to a non-null value, the main
     *  site should not change for the lifetime of this object.
     **/
    public String main_site() {
        return mainSite.getName();
    }
    
    public String name() {
        return domainName;
    }

    /** Creates a canto_context object (represented by the CantoContext interface), which can
     *  be used to construct objects.  The canto_context will be able to construct objects in
     *  any of the sites in this domain.
     */
    public canto_context context() {
        return new CantoContext(this);
    }


    public canto_domain child_domain(String name) {
        return getChildDomain(name, null);
    }
    
    public canto_domain child_domain(String name, String type, String src, boolean isUrl) {
        CantoDomain child = createChildDomain(name, type);
        if (!child.isLoaded()) {
            Core childCore = new Core();
            child.load(src, isUrl, childCore, SiteLoader.getDefaultLoadOptions());
        }

        return child;
    }

    public canto_domain child_domain(String name, String type, String path, String filter, boolean recursive) {
        CantoDomain child = createChildDomain(name, type);
        if (!child.isLoaded()) {
            SiteLoader.LoadOptions options = SiteLoader.getDefaultLoadOptions();
            options.configurable = false;
            
            Core childCore = getChildCore();
            child.load(path, filter, recursive, childCore, options);
        }

        return child;
    }
}


