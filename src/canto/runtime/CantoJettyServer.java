/* Canto Compiler and Runtime Engine
 * 
 * CantoJettyServer.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import canto.lang.*;
import canto.parser.Node;

import java.io.*;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Standalone Canto-programmable HTTP server
 *
 * This HTTP server is based on the Jetty HTTP server.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.9 $
 */

public class CantoJettyServer extends Server implements CantoStandaloneServer {

    /* These are passed in to the constructor */
	private CantoServer cantoServer;

	/* These are optional and set via setter methods */
	private String virtualHost = null;
    private boolean filesFirst = false;
    private String address = null;
    
	private CantoHandler cantoHandler = null;
	
	/* if this is non-null, then something went wrong.  Responses are disabled. */
	private Exception exception = null;

	public CantoJettyServer() {}
	
	public void setServer(CantoServer cantoServer) {
        this.cantoServer = cantoServer;
    }

    public void startServer() throws Exception {
        try {
        	CantoSite mainSite = cantoServer.getMainSite();
        	
            // Get the addresses to listen to
            Connector[] connectors = null;
            Object serverAddr[] = null;

            String addr = cantoServer.getSpecifiedAddress();
            if (addr == null) {
            	site_config sc = mainSite.getSiteConfig();
                if (sc != null) {
                	serverAddr = sc.listen_to();
                }
                if (serverAddr == null || serverAddr.length == 0) {
                	serverAddr = mainSite.getPropertyArray("listen_to");
                }
            } else {
                serverAddr = new Object[1];
                serverAddr[0] = addr;
            }


            if (serverAddr == null || serverAddr.length == 0) {
                connectors = new Connector[1];
                HttpConfiguration httpConfiguration = new HttpConfiguration();
                ServerConnector connector = new ServerConnector(this, new HttpConnectionFactory(httpConfiguration));
                if (virtualHost != null) {
                    connector.setHost(virtualHost);
                }
                address = connector.getHost();
                connectors[0] = connector;
            } else {
            	address = null;
                int numConnectors = serverAddr.length;
                connectors = new Connector[numConnectors];
                for (int i = 0; i < serverAddr.length; i++) {
                    int port = 80;
                    String host = virtualHost;
                    addr = serverAddr[i].toString();
                    int ix = addr.indexOf(':');
                    if (ix >= 0) {
                        port = Integer.parseInt(addr.substring(ix + 1));
                        if (ix > 0) {
                            host = addr.substring(0, ix);
                        }
                    }
                    HttpConfiguration httpConfiguration = new HttpConfiguration();
                    ServerConnector connector = new ServerConnector(this, new HttpConnectionFactory(httpConfiguration));
                    connector.setPort(port);
                    if (host != null) {
                        connector.setHost(host);
                    }
                    connectors[i] = connector;
                    if (address == null) {
                    	address = addr;
                    } else {
                        address = address + ", " + addr;
                    }
                }
            }

            HandlerCollection handlers = new HandlerCollection();

            String fileBase = mainSite.getProperty("file_base", ".");
            if (filesFirst) {
                ResourceHandler fileHandler = new FileHandler();
                fileHandler.setResourceBase(fileBase);
                handlers.addHandler(fileHandler);
            }
            
            String contextPath = mainSite.getProperty("context_path", "/");
            cantoHandler = new CantoHandler(cantoServer, contextPath);
            handlers.addHandler(cantoHandler);

            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setResourceBase(fileBase);
            handlers.addHandler(resourceHandler);

            handlers.addHandler(new CantoDefaultHandler());

            setConnectors(connectors);
            setHandler(handlers);
            start();
            cantoServer.recordState(CantoServer.SERVER_STARTED);

        } catch (Exception e) {
            cantoServer.recordState(CantoServer.SERVER_FAILED);
        	exception = e;
            throw e;
        }
    }

    public void stopServer() throws Exception {
        Connector[] connectors = getConnectors();
        for (Connector c: connectors) {
            if (c instanceof ServerConnector) {
                ((ServerConnector) c).close();
            }
        }
        stop();
        cantoServer.recordState(CantoServer.SERVER_STOPPED);
    }
    
    static void link(Node[] parseResults) {
        for (int i = 0; i < parseResults.length; i++) {
            parseResults[i].jjtAccept(new SiteLoader.Linker(), null);
        }
    }

//    public void addSite(CantoSite site) {
//        if (cantoHandler != null) {
//            cantoHandler.addSite(site);
//        }
//    }
    


    /** Gets the setting of the files first option.  If true, the server looks for
     *  files before Canto objects to satisfy a request.  If false, the server looks
     *  for Canto objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public boolean getFilesFirst() {
        return filesFirst;
    }

    /** Set the files first option.  If this flag is present, then the server looks for
     *  files before Canto objects to satisfy a request.  If not present, the server looks
     *  for Canto objects first, and looks for files only when no suitable object by the
     *  requested name exists.
     */
    public void setFilesFirst(boolean filesFirst) {
        this.filesFirst = filesFirst;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    /** Sets the optional virtual host name.
     */
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public class CantoHandler extends ServletContextHandler {
 
        ServletHolder holder = null;

        public CantoHandler(CantoServer cantoServer, String contextPath) {
            super(SESSIONS);
            setContextPath(contextPath);
            if (cantoServer.getMainSite() != null && exception == null) {
                holder = new ServletHolder(cantoServer);
                addServlet(holder, "/*");
            }
        }

        // problem with jetty 6.1.3
        public String getContextPath() {
            String path = super.getContextPath();
            return (path == null ? "" : path);
        }
        
        public synchronized void doStart() throws Exception {
            if (holder != null) {
                super.doStart();
            } else {
                System.out.println("Can't start CantoHandler because the site was not initialized due to an error.");
            }
        }

        public void doHandle(String pathInContext, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {

        	Map<String, CantoSite> sites = cantoServer.getSiteMap();
        	CantoSite mainSite = cantoServer.getMainSite();
        	
            if (sites != null) {
                String ru = pathInContext;
                int ix = ru.indexOf('/');
                while (ix == 0) {
                    ru = ru.substring(1);
                    ix = ru.indexOf('/');
                }
                if (ix < 0) {
                    if (sites.containsKey(ru)) {
                        mainSite = (CantoSite) sites.get(ru);
                    }
                } else if (ix > 0) {
                    String siteName = ru.substring(0, ix);
                    if (sites.containsKey(siteName)) {
                        mainSite = (CantoSite) sites.get(siteName);
                    }
                }
            }
            
            String pageName = mainSite.getPageName(pathInContext);
            if (mainSite.canRespond(pageName)) {
                try {
                    System.out.println("== CantoHandler: request path " + pathInContext);
                    super.doHandle(pathInContext, baseRequest, httpRequest, httpResponse);
                } catch (Exception e) {
                    System.out.println("== CantoHandler: exception handling request " + e.toString());
                    httpResponse.reset();
                    baseRequest.setHandled(false);
                }
            } else {
                System.out.println("== CantoHandler: cannot respond to " + pathInContext);
                httpResponse.reset();
                baseRequest.setHandled(false);
            }
        }
    }

    public class FileHandler extends ResourceHandler {

    	public void handle(String pathInContext, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse, int dispatch) throws IOException, ServletException {
    		// handle only files; pass directories through
            System.out.println("== FileHandler: request path " + pathInContext);
            if (pathInContext != null && pathInContext.length() > 0 && !pathInContext.endsWith("/")) {
            	super.handle(pathInContext, baseRequest, httpRequest, httpResponse);
            }
        }
    }

    public class CantoDefaultHandler extends DefaultHandler {

        public CantoDefaultHandler() {
            setServeIcon(false);
        }
        
        public void handle(String pathInContext, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse, int dispatch) throws IOException, ServletException {
            System.out.println("== DefaultHandler: request path " + pathInContext);
            super.handle(pathInContext, baseRequest, httpRequest, httpResponse);
        }
    }
}
