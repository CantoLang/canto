/* Canto Compiler and Runtime Engine
 * 
 * CantoStandaloneServer.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

/**
 * Interface for standalone Canto-programmable HTTP server
 *
 * This allows CantoServer to avoid a dependency on a specifig implementation (e.g. Jetty)
 *
 * @author Michael St. Hippolyte
 */

public interface CantoStandaloneServer {

	public void setServer(CantoServer cantoServer);

	public void startServer() throws Exception;

    public void stopServer() throws Exception;
    
    public boolean isRunning();

    /** Gets the setting of the files first option.  If true, the server looks for
     *  files before canto objects to satisfy a request.  If false, the server looks
     *  for canto objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public boolean getFilesFirst();

    /** Set the files first option.  If this flag is present, then the server looks for
     *  files before canto objects to satisfy a request.  If not present, the server looks
     *  for canto objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public void setFilesFirst(boolean filesFirst);

    /** Gets the optional virtual host name.
     */
    public String getVirtualHost();

    /** Sets the optional virtual host name.
     */
    public void setVirtualHost(String virtualHost);

    public void join() throws InterruptedException;
}
