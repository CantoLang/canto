/* Canto Compiler and Runtime Engine
 * 
 * site_config.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * This interface corresponds to the site_config object, defined in the
 * default site_config.can file.  It represents a website configuration --
 * name, cantopath, base directory for file-based resources, and
 * network address.
 */
public interface site_config {

    /** Returns the name of the site. **/
    public String name();
    
    /** The directories and/or files containing the Canto source
     *  code for this site.
     **/
    public String cantopath();
    
    /** The directories and/or files containing the Canto source
     *  code for core.
     **/
    public String corepath();
    
    /** The directories and/or files containing the Canto source
     *  code specific to this site (not including core).
     **/
    public String sitepath();

        /** If true, directories found in cantopath are searched recursively
     *  for Canto source files.
     **/
    public boolean recursive();
    
    /** The base directory for file-based resources. **/
    public String filepath();

    /** The files first setting.  If true, the server should look for files 
     *  before Canto objects to satisfy a request.  If false, the server 
     *  should look for Canto objects first, and look for files only when no 
     *  suitable Canto object by the requested name exists.
     */
    public boolean files_first();
    
    /** The external interfaces (address and port) that the server should
     *  respond to for this site.  If null the globally defined value is used.
     **/
    public Object[] listen_to();    
}


