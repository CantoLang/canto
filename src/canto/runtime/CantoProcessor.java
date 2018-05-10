/* Canto Compiler and Runtime Engine
 * 
 * CantoProcessor.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import canto.lang.*;


/**
 * This interface extends canto_server (and therefore canto_processor) to include methods
 * to adjust the behavior of a Canto processor.
 */
public interface CantoProcessor extends canto_server {

    /** Sets the files first option.  If this flag is present, then the server looks for
     *  files before Canto objects to satisfy a request.  If not present, the server looks
     *  for Canto objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public void setFilesFirst(boolean filesFirst);

    /** Sets the base directory where the server should read and write files. **/
    public void setFileBase(String fileBase);
}


