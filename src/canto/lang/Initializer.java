/* Canto Compiler and Runtime Engine
 * 
 * Initializer.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */


package canto.lang;

import java.util.HashMap;
import java.util.Map;

import canto.runtime.*;

public class Initializer extends CantoVisitor {

    private Core core;
    private Site site;
    private boolean allowOverwrite = false;

    public Initializer(Core core) {
        this.core = core;
        site = core;
    }

    public Initializer(Core core, Site site) {
        this.core = core;
        this.site = site;
    }

    public Initializer(Core core, Site site, boolean allowOverwrite) {
        this.core = core;
        this.site = site;
        this.allowOverwrite = allowOverwrite;
    }

    public Object handleNode(CantoNode node, Object data) {
        NamedDefinition def = (NamedDefinition) data;
        ((AbstractNode) node).setOwner(def);
        NamedDefinition subdef = def;

        try {

            if (node instanceof Core) {
                if (allowOverwrite) {
                    throw new UnsupportedOperationException("Overwriting core is not supported.");
                }
                subdef = core;
                if (node != core) {
                    ((Core) node).setDefinitionTable(core.getDefinitionTable());
                    core.addContents((Site) node);
                }

            } else if (def == core && node instanceof Site) {
                if (allowOverwrite) {
                    throw new UnsupportedOperationException("Overwriting site is not supported.");
                }
                site = (Site) node;
                String name = site.getName();
                Map<String, DefinitionTable> defTableTable = core.getDefTableTable();
                DefinitionTable defTable = (DefinitionTable) defTableTable.get(name);
                if (defTable != null) {
                	site.setDefinitionTable(defTable);
                } else {
                	defTable = site.setNewDefinitionTable();
                	defTableTable.put(name, defTable);
                }
                Map<String, Map<String, Object>> globalKeepTable = core.getGlobalKeepTable();
                Map<String, Object> globalKeep = globalKeepTable.get(name);
                if (globalKeep == null) {
                    globalKeep = new HashMap<String, Object>();
                    globalKeepTable.put(name,  globalKeep);
                }
                site.setGlobalKeep(globalKeep);
                subdef = site;

            } else if (node instanceof ExternStatement) {
                if (allowOverwrite) {
                    throw new UnsupportedOperationException("Overwriting extern is not supported.");
                }
                site.addExtern((ExternStatement) node);

            } else if (node instanceof AdoptStatement) {
                if (allowOverwrite) {
                    throw new UnsupportedOperationException("Overwriting adopt is not supported.");
                }
                site.addAdopt((AdoptStatement) node);

            } else if (node instanceof KeepStatement) {
                def.addKeep((KeepStatement) node);

            } else if (node instanceof AbstractConstruction) {
                if (((AbstractConstruction) node).hasSub()) {
                    def.setHasSub(true);
                }
                if (((AbstractConstruction) node).hasNext()) {
                    def.setHasNext(true);
                }
                if (node instanceof Instantiation) {
                    Instantiation instance = (Instantiation) node;
                    if (instance.getParent() instanceof ConcurrentCantoBlock) {
                        instance.setConcurrent(true);
                    } else {
                        ArgumentList args = instance.getArguments();
                        if (args != null && args.isConcurrent()) {
                            instance.setConcurrent(true);
                        }
                    }
                }

            } else if (node instanceof Type) {
                if (Name.THIS.equals(node.getName())) {
                    CantoNode sibling = node.getNextSibling();
                    
                    ((NameNode) node).setName(sibling.getName());    
                }
            
            } else if (node instanceof NamedDefinition) {
                subdef = (NamedDefinition) node;
                subdef.setDefinitionTable(def.getDefinitionTable());
                if (def.isGlobal() && subdef.getDurability() == Definition.IN_CONTEXT) {
                    subdef.setDurability(Definition.GLOBAL);
                }
                
                // anonymous collections nested in collections represent multidimensional
                // collections; as such, their supertype is their owner's supertype
                if (node instanceof CollectionDefinition && def instanceof CollectionDefinition && ((CollectionDefinition) node).getSuper() == null) {
                    CollectionDefinition anonCollection = (CollectionDefinition) node;
                    anonCollection.setSuper(def.getSuper());
                }
            }
            super.handleNode(node, subdef);

            if (def == core && node instanceof Site && !(node instanceof Core)) {
                // if a site by this name already exists, it's not overwritten; a new
                // multi-site object containing all the content from both sites is built 
                core.addSite((Site) node);
                subdef = core.getSite(node.getName());
            }

            if (subdef != def && subdef.getName().length() > 0) {
                // sites automatically overwrite because that's how multifile sites
                // get handled
                boolean overwrite = allowOverwrite || (subdef instanceof Site);
                def.addDefinition(subdef, overwrite);
            }
        
            

        } catch (Exception e) {
            SiteBuilder.log("Error handling node " + node.getName() + " owned by " + (def instanceof Core ? "core" : def.getFullName()) + ": " + e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
        }

        return data;
    }
}
