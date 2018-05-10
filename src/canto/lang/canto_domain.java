/* Canto Compiler and Runtime Engine
 * 
 * canto_domain.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Map;

/**
 * This interface corresponds to the canto_domain object, defined in core and representing
 * a Canto domain, which consists of Canto code loaded under a particular set of restrictions.
 * Multiple canto_domains may be combined together into a single Canto application, but only by
 * dividing the application into multiple sites, since a site can only have one immediate
 * parent domain.
 */
public interface canto_domain {

    /** Sites in this domain (keyed on site name). **/
    public Map<String, Site> sites();

    /** The name of the main site.  The main site is the first site to be queried when a name
     *  does not explicitly specify a site (followed by the default site and core).
     **/
    public String main_site();
    
    public String name();

    /** Returns the definition table associated with this domain. **/
    public Map<String, Definition> defs();

    /** Creates a canto_context object which can be used to construct Canto objects.  The
     *  canto_context will be able to construct objects whose definitions are in any of the
     *  sites in this domain.
     */
    public canto_context context();

    
    public Object get(String expr) throws Redirection;
    public Definition get_definition(String expr) throws Redirection;
    public Object get_instance(String expr) throws Redirection;
    public Object[] get_array(String expr) throws Redirection;
    public Map<String, Object> get_table(String expr) throws Redirection;

    /** Returns the existing child domain with a given name, or null if it does not exist. **/
    public canto_domain child_domain(String name);
    
    /** Creates a new domain which is a child of this domain. **/
    public canto_domain child_domain(String name, String type, String src, boolean isUrl);
    public canto_domain child_domain(String name, String type, String path, String filter, boolean recursive);
}


