/* Canto Compiler and Runtime Engine
 * 
 * canto_processor.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Map;

/**
 * This interface corresponds to the Canto canto_processor object, called by Canto code
 * to obtain environment information and compile Canto code at runtime.
 */
public interface canto_processor {
    /** Returns the name of this processor (generally the name of a class or
     *  interface).
     **/
    public String name();

    /** The highest Canto version number supported by this processor. **/
    public String version();

    /** Properties associated with this processor. **/
    public Map<String, Object> props();

    /** Compile the Canto source files found at the locations specified in <code>cantopath</code>
     *  and return a canto_domain object.  If a location is a directory and <code>recursive</code>
     *  is true, scan subdirectories recursively for Canto source files.  If <code>autoloadCore</code>
     *  is true, and the core definitions required by the system cannot be found in the files
     *  specified in <code>cantopath</code>, the processor will attempt to load the core
     *  definitions automatically from a known source (e.g. from the same jar file that the
     *  processor was loaded from).
     *
     *  <code>siteName</code> is the name of the main site; it may be null, in which case the
     *  default site must contain a definition for <code>main_site</code>, which must yield the
     *  name of the main site.
     */
    public canto_domain compile(String siteName, String cantopath, boolean recursive, boolean autoloadCore);

    /** Compile Canto source code passed in as a string and return a canto_domain object.  If
     *  <code>autoloadCore</code> is true, and the core definitions required by the system cannot
     *  be found in the passed text, the processor will attempt to load the core definitions
     *  automatically from a known source (e.g. from the same jar file that the processor was
     *  loaded from).
     *
     *  <code>siteName</code> is the name of the main site; it may be null, in which case the
     *  default site must contain a definition for <code>main_site</code>, which must yield the
     *  name of the main site.
     */
    public canto_domain compile(String siteName, String cantotext, boolean autoloadCore);

    /** Compile Canto source code passed in as a string and merge the result into the specified
     *  canto_domain.  If there is a fatal error in the code, the result is not merged and
     *  a Redirection is thrown.
     */
    public void compile_into(canto_domain domain, String cantotext) throws Redirection;
    
    /** Returns the domain type.  The default for the primary domain is "site".
     */
    public String domain_type();
}


