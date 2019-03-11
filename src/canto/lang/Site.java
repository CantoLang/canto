/* Canto Compiler and Runtime Engine
 * 
 * Site.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Context;

/**
* A Site is a namespace and a collection of related definitions.
*
* @author Michael St. Hippolyte
*/
public class Site extends ComplexDefinition {

    public static int defaultExternalAccess = PUBLIC_ACCESS;
    public static int defaultExternalDurability = DYNAMIC;

    protected String domainName = Name.SITE;
    protected String sitePrefix = "";
    protected List<Name> adopts = null;
    protected List<ExternStatement> externs = null;
    protected Core core = null;
    protected site_config siteConfig = null;
    protected Map<String, Object> globalKeep = null;

    public Site() {
        super();
    }

    public Site(String name) {
        super();
        setName(new NameNode(name));
        setType(createType());
    }

    public Site(String domain, String name) {
        super();
        setName(new NameNode(name));
        setType(createType());
        setDomainName(domain);
    }

    public DefinitionTable setNewDefinitionTable() {
    	DefinitionTable defTable = new DefinitionHash(); 
        setDefinitionTable(defTable);
        return defTable;
    }
    
    /** Returns the name of the site.  */
    public String getFullName() {
        return getName();
    }

    public String getFullNameInContext(Context context) {
        return getName();
    }
        
    protected void setName(NameNode name) {
        super.setName(name);
        sitePrefix = getFullName();
        if (sitePrefix.length() > 0) {
            sitePrefix = sitePrefix + '.';
        }
    }
    
    protected void setDomainName(String name) {
        domainName = name;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Site) {
            return equals((Definition) obj, null);
        } else {
            return false;
        }
    }

    public String toString(String prefix) {
        String str = '\n' + prefix + "site " + getName() + ' ' + getContents().toString(prefix) + '\n';
        return str;
    }
    
    
    public site_config getSiteConfig() {
        return siteConfig;
    }

    public void setSiteConfig(site_config siteConfig) {
        this.siteConfig = siteConfig;
    }

    void setGlobalKeep(Map<String, Object> globalKeep) {
        this.globalKeep = globalKeep;
    } 

    public Map<String, Object> getGlobalKeep() {
        return globalKeep;
    }
    
    /** Add a site's content to this site */
    synchronized void addContents(Site site) {
        AbstractNode newContents = site.getContents();
        AbstractNode oldContents = getContents();
        if (oldContents == null) {
            setContents(newContents);
        } else {
            oldContents.addChildren(newContents);
        }

        List<Name> newAdopts = site.getAdoptedSiteList();
        if (newAdopts != null) {
            if (adopts == null) {
                adopts = new ArrayList<Name>(newAdopts);
            } else {
                adopts.addAll(newAdopts);
            }
        }
        
        List<ExternStatement> newExterns = site.externs;
        if (newExterns != null) {
            if (externs == null) {
                externs = new ArrayList<ExternStatement>(newExterns);
            } else {
                externs.addAll(newExterns);
            }
        }
        
        if (siteConfig == null) {
            siteConfig = site.siteConfig;
        }
    }

    /** If the contents have not been initialized, getContents returns a block containing
     *  the definitions which are immediate children of the site.
     */
    public AbstractNode getContents() {
        AbstractNode contents = super.getContents();
        if (contents == null) {
            // getDefinitions returns all the definitions owned directly or indirectly
            // by this site; only the immmediate children will be returned as the
            // contents
            Definition[] defs = getDefinitions();
            List<Definition> list = new ArrayList<Definition>();
            for (int i = 0; i < defs.length; i++) {
                if (isOwnerOf(defs[i])) {
                    list.add(defs[i]);
                }
            }
            int n = list.size();
            AbstractNode[] nodes = new AbstractNode[n];
            nodes = (AbstractNode[]) list.toArray(nodes);
            contents = new CantoBlock(nodes);
            setContents(contents);
        }
        return contents;
    }

    protected boolean isOwnerOf(Definition def) {
        return def.getOwner().equals(this);
    }
    
    
    /** Returns the total number of definitions */
    public int getNumDefinitions() {
        return getDefinitionTable().size();
    }

    public Definition[] getDefinitions() {
        DefinitionHash defTable = (DefinitionHash) getDefinitionTable();
        synchronized (defTable) {
            Collection<Definition> defColl = (Collection<Definition>) defTable.values();
            Definition[] defs = new Definition[defColl.size()];
            defs = (Definition[]) defColl.toArray(defs);
            return defs;
        }
    }
    
    public void addDefinitionTable(DefinitionTable definitionTable) {
        DefinitionHash defTable = (DefinitionHash) getDefinitionTable();
    	Set<Map.Entry<String, Definition>> defEntries = ((Map<String, Definition>) definitionTable).entrySet();
    	Iterator<Map.Entry<String, Definition>> it = defEntries.iterator();
    	
    	while (it.hasNext()) {
    		Map.Entry<String, Definition> entry = it.next();
    		defTable.put(entry.getKey(), entry.getValue());
    	}
    }

    /** Returns the definition table for this site, in the form of a Map. **/
    public Map<String, Definition> getDefinitionMap() {
        return (Map<String, Definition>) getDefinitionTable();
    }

    /** Add the definitions of the specified type owned by this site
     *  to the list.  If this site owns other sites, add their contents
     *  by recursively calling this method.
     */
    public void addDefinitions(String type, List<Definition> list) {
        DefinitionHash defTable = (DefinitionHash) getDefinitionTable();
        Iterator<Definition> it = (Iterator<Definition>) defTable.values().iterator();
        synchronized (defTable) {
            while (it.hasNext()) {
                Definition def = (Definition) it.next();
                if (def.getName().equals(type) || def.isSuperType(type)) {
                    list.add(def);
                }

                if (def instanceof Site) {
                    ((Site) def).addDefinitions(type, list);
                }
            }
        }
    }

    /** Add the subdefinitions of the specified type owned by this site
     *  to the list.  If this site owns other sites, add their contents
     *  by recursively calling this method.
     *
     *  This method is equivalent to calling addDefinitions, and then
     *  removing the definition of the base type from the list.
     */
    public void addSubdefinitions(String type, List<Definition> list) {
        DefinitionHash defTable = (DefinitionHash) getDefinitionTable();
        Iterator<Definition> it = (Iterator<Definition>) defTable.values().iterator();
        synchronized (defTable) {
            while (it.hasNext()) {
                Definition def = (Definition) it.next();
                if (def.isSuperType(type)) {
                    list.add(def);
                }
 
                if (def instanceof Site) {
                    ((Site) def).addDefinitions(type, list);
                }
            }
        }
    }

    public Site getSite() {
        return this;
    }
    
    /** Implementation of canto_site interface **/
    public Definition definition_of_type(String typeName) {
        return getDefinition(typeName);
    }
    
    
    /** Returns the definition in this site or ancestor site of a given name. */
    public Definition getDefinition(String name) {
        DefinitionHash defTable = (DefinitionHash) getDefinitionTable();
        
        Definition def = defTable.getDefinition(this, new NameNode(name));
        if (def == null) {
            Definition owner = getOwner();
            if (owner == null) {
                return null;
            } else {
                Site parentSite = owner.getSite();
                if (parentSite == null || parentSite.equals(this)) {
                    return null;
                } else {
                    return parentSite.getDefinition(name);
                }
            }
        } else {
            return def;
        }
    }
    
    /** Returns an unsorted array of definitions of a given type belonging to this site. */
    public Definition[] getDefinitions(String type) {
        List<Definition> defList = new ArrayList<Definition>();
        addDefinitions(type, defList);
        Definition[] defs = new Definition[defList.size()];
        defs = (Definition[]) defList.toArray(defs);
        return defs;
    }

    /** Returns an unsorted array of subdefinitions of a given type belonging to this site. */
    public Definition[] getSubdefinitions(String type) {
        List<Definition> defList = new ArrayList<Definition>();
        addSubdefinitions(type, defList);
        Definition[] defs = new Definition[defList.size()];
        defs = (Definition[]) defList.toArray(defs);
        return defs;
    }

    public Core getCore() {
        if (core == null) {
            Site siteOwner = (Site) getOwner();
            while (siteOwner != null && !(siteOwner instanceof Core)) {
                siteOwner = (Site) siteOwner.getOwner();
            }
            core = (Core) siteOwner;
        }
        return core;
    }

    
    /** Returns the adopted definition (i.e., definition in a site adopted
     *  by this site) for the indicated name.
     */
    public Definition getAdoptedDefinition(String ownerName, NameNode node) {
        Definition def = null;
        List<Name> adopts = getAdoptedSiteList();
        if (adopts != null) {
            Core core = getCore();
            if (core != null) {
                String name = node.getName();
                if (ownerName != null && ownerName.length() > 0) {
                	name = ownerName + "." + name;
                }
                Iterator<Name> sitenames = adopts.iterator();
                while (sitenames.hasNext()) {
                    Name adopt = sitenames.next();
                    Site adoptedSite = core.getSite(adopt.getName());
                    if (adoptedSite != null) {
                        DefinitionTable defTable = adoptedSite.getDefinitionTable();
                        def = defTable.getInternalDefinition(adoptedSite.getName(), name);
                        if (def != null) {
                            break;
                        }
                    }
                }
            }
        }
        if (def == null) {
            Site siteOwner = (Site) getOwner();
            if (siteOwner != null) {
                def = siteOwner.getAdoptedDefinition(ownerName, node);
            }
        }
        return def;
    }

    public List<Name> getAdoptedSiteList() {
    	return adopts;
    }

    public void addAdopt(Name adopt) {
        if (adopts == null) {
            adopts = new ArrayList<Name>();
        }
        adopts.add(adopt);
    } 

    /** Returns the external definition for the indicated name. */
    public Definition getExternalDefinition(NamedDefinition owner, NameNode node, Type superType, Context context) {
        
        ComplexName name = (node instanceof ComplexName ? (ComplexName) node : new ComplexName(node));
        Definition def = null;

        // if the owner is an external definition and has a child definition with the 
        // specified name, return it
        if (owner != null && owner instanceof ExternalDefinition) {
            def = ((ExternalDefinition) owner).getExternalChildDefinition(name, context);
            if (def != null) {
                return def;
            }
        }
       
        NamedDefinition nodeOwner = (owner != null ? owner : this);

        // find the matching extern statement
        List<ExternStatement> externs = getExternList();
        if (externs != null) {
            Iterator<ExternStatement> it = externs.iterator();
            while (it.hasNext()) {
                ExternStatement extern = it.next();
                if (extern.equals(node)) {
                    def = ExternalDefinition.createForName(nodeOwner, name, superType, defaultExternalAccess, defaultExternalDurability, context);
                    break;
                }
            }
        }
        if (def == null) {
            Site siteOwner = (Site) getOwner();
            if (siteOwner != null) {
                def = siteOwner.getExternalDefinition(owner, node, superType, context);
            }
        }
        return def;
    } 

    public void addExtern(ExternStatement extern) {
        if (externs == null) {
            externs = new ArrayList<ExternStatement>();
        }
        externs.add(extern);
    }

    public boolean isExtern(Name name) {
        // this works because ExternStatement is a descendant of ComplexName
        // and the equals method in ComplexName understands * and ** expressions.
        List<ExternStatement> externs = getExternList();
        return externs.contains(name);
    } 

    protected boolean matchesExtern(String name) {
        // see if it matches an extern statement
        List<ExternStatement> externs = getExternList();
        if (externs != null) {
            Iterator<ExternStatement> it = externs.iterator();
            while (it.hasNext()) {
                ExternStatement extern = (ExternStatement) it.next();
                if (extern.matches(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<ExternStatement> getExternList() {
        return externs;
    }

    class DefinitionHash extends HashMap<String, Definition> implements DefinitionTable {

        private static final long serialVersionUID = 1L;

        public DefinitionHash() {
            super();
        }

        public void addDefinition(Definition def, boolean replace) throws DuplicateDefinitionException {
        	String fullName = def.getFullName();
            if (fullName == null || fullName.length() == 0) {
                throw new IllegalArgumentException("Attempt to add unnamed definition");
            }

            String key = fullName;
            if (sitePrefix.length() > 0 && !matchesExtern(fullName)) {
                if (!fullName.startsWith(sitePrefix)) {
                    throw new IllegalArgumentException("Attempt to add a definition not owned by this site");
                }
                key = fullName.substring(sitePrefix.length());
            }
 
            String ownerName = "";
            Definition owner = def.getOwner();
            if (owner instanceof Core) {
                ownerName = "core";
            } else if (owner != null) {
                ownerName = owner.getFullName();
            }
 
            Definition entry = (replace ? null : (Definition) get(key));
            if (entry != null) {
 
                if (entry instanceof SubcollectionDefinition) {
                    SubcollectionDefinition subdef = (SubcollectionDefinition) entry;
                    String what;
                    if (def instanceof CollectionDefinition) {
                        what = ((CollectionDefinition) def).isArray() ? "array" : "table";
                        subdef.setSupercollection((CollectionDefinition) def);
                    } else {
                        what = def.getClass().getName();
                        subdef.add(def);
                    }
                    vlog("Adding " + what + " " + fullName + " owned by " + ownerName);
 
                } else if (entry instanceof CollectionDefinition) {
                    if (def instanceof CollectionDefinition) {
                        throw new DuplicateDefinitionException("Collection " + key + " already defined");
                    }
                    String what;
                    what = def.getClass().getName();
                    CollectionDefinition colldef = (CollectionDefinition) entry;
                    SubcollectionDefinition subcoll = new SubcollectionDefinition(colldef);
                    subcoll.add(def);
                    put(key, subcoll);
                    vlog("Adding " + what + " " + fullName + " owned by " + ownerName + " to collection " + subcoll.getFullName());

                } else if (!entry.isExternal()) {
                    throw new DuplicateDefinitionException(key + " already defined");
                }

            } else {

                vlog("Adding definition " + fullName + " owned by " + ownerName);
                // filter out Site objects because getContents can be expensive for them
                if (!(def instanceof Site) && def.getContents() instanceof ElementDefinition) {
                    NameNode nameNode = null;
                    if (def instanceof NamedDefinition) {
                        nameNode = ((NamedDefinition) def).getNameNode();
                    } else if (def instanceof ElementReference) {
                        nameNode = ((ElementReference) def).getNameNode();
                    } else {
                        throw new IllegalArgumentException("Incorrect definition type for ElementDefinition owner");
                    }
                    CollectionDefinition supercollection = null;
                    // find array definition
                    for (NamedDefinition nd = getSuperDefinition(); nd != null; nd = nd.getSuperDefinition()) {
                        if (nd instanceof ComplexDefinition) {
                            Definition superDef = ((ComplexDefinition) nd).getExplicitDefinition(nameNode, null, null);
                            if (superDef != null && superDef instanceof CollectionDefinition) {
                                supercollection = (CollectionDefinition) superDef;
                                break;
                            }
                        }
                    }
                    if (supercollection == null) {
                        while (owner != null) {
                            Definition superDef = owner.getChildDefinition(nameNode, null);
                            if (superDef != null && superDef instanceof CollectionDefinition) {
                                supercollection = (CollectionDefinition) superDef;
                                break;
                            }
                            owner = owner.getOwner();
                        }
                        if (supercollection == null) {
                            throw new IllegalArgumentException("Reference to unknown array or table");
                            }
                        }
 
                        SubcollectionDefinition collection = new SubcollectionDefinition(supercollection);
                        collection.setOwner(def.getOwner());
                        collection.setName(supercollection.getNameNode());
                        collection.add(def);
                        put(key, collection);
                } else {
                    put(key, def);
                }
            }
        }

        /** Returns the global definition for the indicated name.
         */
        public Definition getDefinition(String name) {
            return getDefinition(null, new NameNode(name));
        }

        /** Returns the definition for the indicated name in the context of the
         *  indicated owner, or null if no such definition exists.  The definition
         *  might be local or external.  Unless the definition is external, either
         *  <code>ownerName<code> or <code>node</code> must start with the site name.
         */
        public Definition getDefinition(NamedDefinition owner, NameNode node) {
            String ownerName = (owner == null ? "" : owner.getFullName());
            String name = node.getName();
            boolean nameHasPrefix = false;
            boolean ownerHasPrefix = false;
            if (sitePrefix.length() > 0) {
                if (ownerName.length() > 0) {
                    // see if the owner is the site, whose name doesn't
                    // start with the site prefix because the prefix
                    // includes the trailing '.', but which counts as
                    // having the prefix for the purpose of the lookup
                    if (ownerName.equals(getName())) {
                        ownerName = "";
                    } else {
                        if (!ownerName.startsWith(sitePrefix)) {
                            return null;
                        }
                        ownerName = ownerName.substring(sitePrefix.length());
                    }
                    ownerHasPrefix = true;
                }
                if (name.startsWith(sitePrefix)) {
                    name = name.substring(sitePrefix.length());
                    nameHasPrefix = true;
                }
 
                if (!ownerHasPrefix && !nameHasPrefix && !matchesExtern(name)) {
                    return null;
                }
            }

            Object entry = null;
 
            // first see if there is a local definition by that name
            if (!nameHasPrefix) {
                if (ownerName.length() > 0) {
                    entry = get(ownerName + '.' + name);
                } else {
                    // if not local, try globally
                    entry = get(name);
                }
            } else {
                // if not local, try globally
                entry = get(name);
            } 

            // if not local or global, try external
            if (entry == null) {
                entry = getExternalDefinition(owner, node, null, null);
                if (entry != null) {
                    //vlog("would have put " + ((Definition) entry).getFullName() + " in " + name);
//                       put(name, entry);

                } else {
                    
                    Site ownerSite = (owner instanceof Site ? (Site) owner : Site.this);
                    List<Name> adopts = ownerSite.getAdoptedSiteList();
                    if (adopts != null) {
                        entry = ownerSite.getAdoptedDefinition(ownerName, node);
                    }
                    if (entry == null) {
                        // if still not found, look for a child site
                        int ix = name.indexOf('.');
                        if (ix > 0) {
                            Object subDef = get(name.substring(0, ix));
                            if (subDef != null && subDef instanceof Site) {
                                DefinitionTable subTable = ((Site)subDef).getDefinitionTable();
                                entry = subTable.getDefinition(name);
                            }
                        }
                        // finally, check default site
                        if (entry == null && !Name.DEFAULT.equals(getName())) {
                            entry = getDefaultDefinition(ownerName, node);
                        }
                    }
                }
            }

            if (entry == null) {
                return null;
            } else if (entry instanceof List) {
                return (Definition) ((List<Definition>) entry).get(0);
          //  } else if (entry instanceof ExternalDefinition) {
          //      // not the best place to handle this -- better would be a
          //      // refactoring of arguments outside of ExternalDefinition
          //      entry = fixArgs((ExternalDefinition) entry, node.getArguments());
            }
            return (Definition) entry;
        }

        private ExternalDefinition fixArgs(ExternalDefinition externalDef, ArgumentList args) {
            ArgumentList defArgs = externalDef.getArguments();
            if ((args != null && !args.equals(defArgs)) || (args == null && defArgs != null)) {
                log("would have rehaggled arguments for definition for " + externalDef.getFullName());
                return externalDef; //.newForArgs(args);
            } else {
                return externalDef;
            }
        }


        /** Returns the internal definition (i.e., external definitions are
         *  ignored) for the indicated name in the context of the indicated owner,
         *  or null if no such definition exists.  Either <code>ownerName<code> or
         *  <code>name</code> must start with the site name.
         */
        public Definition getInternalDefinition(String ownerName, String name) {
            boolean nameHasPrefix = false;
            boolean ownerHasPrefix = false;
            if (sitePrefix.length() > 0) {
                if (ownerName.length() > 0) {
                    // see if the owner is the site, whose name doesn't
                    // start with the site prefix because the prefix
                    // includes the trailing '.', but which counts as
                    // having the prefix for the purpose of the lookup
                    if (ownerName.equals(getName())) {
                        ownerName = "";
                    } else {
                        if (!ownerName.startsWith(sitePrefix)) {
                            return null;
                        }
                        ownerName = ownerName.substring(sitePrefix.length());
                    } 
                    ownerHasPrefix = true;
                }
                if (name.startsWith(sitePrefix)) {
                    name = name.substring(sitePrefix.length());
                    nameHasPrefix = true;
                }
 
                if (!ownerHasPrefix && !nameHasPrefix) {
                    return null;
                }
            }
 
            Object entry = null;
 
            // first see if there is a local definition by that name
            if (!nameHasPrefix) {
                if (ownerName.length() > 0) {
                    entry = get(ownerName + '.' + name);
                } else {
                    entry = get(name);
                }
            }

            if (entry == null) {
                return null;
            } else if (entry instanceof List) {
                return (Definition) ((List<Definition>) entry).get(0);
            } else {
                return (Definition) entry;
            }
        }

        public Collection values() {
            Collection vals = super.values();
            ArrayList defs = new ArrayList(vals.size());
            Iterator it = vals.iterator();
            while (it.hasNext()) {
                Object entry = it.next();
                if (entry instanceof List) {
                    defs.addAll((List) entry);
                } else {
                    defs.add(entry);
                }
            }
            return defs;
        }

        public Definition getDefaultDefinition(String ownerName, NameNode node) {
            Definition def = null;
            Core core = getCore();
            if (core != null) {
                Site defaultSite = core.getSite(Name.DEFAULT);
                if (defaultSite != null && defaultSite != Site.this) {
                    String name = ownerName + node.getName();
                    DefinitionTable defTable = defaultSite.getDefinitionTable();
                    def = defTable.getInternalDefinition(Name.DEFAULT, name);
                }
            }
            return def;
        }
    }
}

