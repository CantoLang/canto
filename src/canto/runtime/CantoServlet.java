/* Canto Compiler and Runtime Engine
 * 
 * CantoServlet.java
 *
 * Copyright (c) 2018 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 * Servlet for Canto
 *
 * @author Michael St. Hippolyte
 */

public class CantoServlet extends CantoServer {
    private static final long serialVersionUID = 1L;

    private static String[] STANDARD_FILTERS = { "png", "PNG", "gif", "GIF", "jpg", "JPG", "mp3", "MP3" };

    private static final String CONTENT_TYPE = "text/html";

    private RequestDispatcher fileHandler = null;
 
    private Filter[] filters = null;

    public CantoServlet() {}
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        if (fileHandlerName != null) {
            fileHandler = servletContext.getNamedDispatcher(fileHandlerName);
        }

        // initialize the filter array with a standard set of
        // extensions.
        int num = STANDARD_FILTERS.length;
        filters = new Filter[num];
        for (int i = 0; i < num; i++) {
            filters[i] = new ExtensionFilter(STANDARD_FILTERS[i]);
        }
        
        HashMap<String, String> initParams = new HashMap<String, String>();

        try {
            Enumeration<String> params = config.getInitParameterNames();
            while (params.hasMoreElements()) {
                String name = (String) params.nextElement();
                String param = config.getInitParameter(name);
                initParams.put(name, param);
            }

            initGlobalSettings(initParams);
            loadSite();
            
        } catch (Exception e) {
            throw new ServletException("Exception initializing CantoServer", e);
        }
    }


    protected void respond(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contextPath = request.getContextPath();
        String ru = request.getRequestURI();
        String servletPath = request.getServletPath();
        String fileBase = file_base();
        boolean filesFirst = files_first();

System.out.println("---> CantoServlet responding to " + ru);
System.out.println("  --> fileBase is " + fileBase);

        if (contextPath != null && ru != null && ru.startsWith(contextPath)) {
            ru = ru.substring(contextPath.length());
        }

        if (servletPath != null && ru != null && ru.startsWith(servletPath)) {
            ru = ru.substring(servletPath.length());
        }

        if (ru == null || ru.length() == 0) {
            //response.sendRedirect(response.encodeRedirectURL("index.html"));
            //return;

            ru = "/index.html";

        } else if (ru.endsWith("/")) {
            ru = ru + "index.html";
        }
        

        String filePath = fileBase + ru;
        String altPath = request.getPathInfo();
        if (altPath == null) {
            altPath = request.getServletPath();
        }

        CantoSite site = mainSite; 
        if (sites != null) {
            int ix = ru.indexOf('/');
            if (ix < 0) {
                if (sites.containsKey(ru)) {
                    site = (CantoSite) sites.get(ru);
                }
            } else if (ix > 0) {
                String siteName = ru.substring(0, ix - 1);
                if (sites.containsKey(siteName)) {
                    site = (CantoSite) sites.get(siteName);
                }
            }
        }

        //String pageName = site.getPageName(request);

        File file = new File(filePath);
        boolean exists = file.exists();
        boolean filtered = isFiltered(filePath);
        if (exists && (filesFirst || filtered)) {
            if (!file.isDirectory()) {
                if (fileHandler != null || site == null) {
                    slog("Forwarding request to filehandler");
                    fileHandler.forward(request, response);
                } else {
                    respondWithFile(file, site, response);
                }
                return;

            } else { // file is a directory

                if (site != null && exception == null) {
                    if (respond(site, request, response, servletContext) < 400) {
                        return;
                    }
                }

                if (fileHandler != null) {
                    fileHandler.forward(request, response);
                    return;

                } else if (!altPath.equals(request.getRequestURI())) {
                    RequestDispatcher rd = request.getRequestDispatcher(altPath);
                    rd.forward(request, response);
                    return;
                }
            }

        } else if (filtered) {
            exception = new FileNotFoundException(filePath + " not found.");

        } else {

            // try site first
            if (site != null && exception == null) {
                if (respond(site, request, response, servletContext) < 400) {
                    return;
                }
                // site doesn't have the page; look for a file
                if (exists) {
                    if (!file.isDirectory()) {
                        if (fileHandler != null) {
                            slog("Forwarding request to filehandler");
                            fileHandler.forward(request, response);
                        } else {
                            respondWithFile(file, site, response);
                        }
                        return;

                    // file is a directory
                    } else if (!altPath.equals(request.getRequestURI())) {
                        RequestDispatcher rd = request.getRequestDispatcher(altPath);
                        if (rd != null) {
                            rd.forward(request, response);
                            return;
                        }
                    }
                }

            }
        }

        String mimeType = servletContext.getMimeType(filePath);
        if (mimeType == null) {
            mimeType = servletContext.getMimeType(filePath.toLowerCase());
            if (mimeType == null) {
                mimeType = "text/plain";
            }
        }

        response.setContentType(CONTENT_TYPE);
        PrintWriter out = response.getWriter();
        out.println("<html>");
        if (site == null || exception != null) {
            out.println("<head><title>Canto Error</title></head>");
            out.println("<body BGCOLOR=\"#88ccbb\">");
            if (site == null) {
                out.println("<h2>Unable to load Canto site</h2>");
                if (exception != null) {
                    out.println("<p>Reason: " + exception.toString() + "</p>");
                } else {
                    out.println("<p>Reason unknown.</p>");
                }
            } else if (exception != null) {
                out.println("<h2>Error servicing request</h2>");
                out.println("<p>Canto site: " + site.getName() + "</p>");
                out.println("<p>Error: " + exception.toString() + "</p>");
            }
            out.println("<h3>Request Details</h3><ul>");
            if (site != null) {
                exception = null;
            }
        } else {
            String siteName = site.getName();
            String pageName = getPageName(site, request);
            slog("Page " + pageName + " not found.");
            out.println("<head><title>Site " + siteName + "</title></head>");
            out.println("<body BGCOLOR=\"#ccbb88\">");
            out.println("<h2>Page " + pageName + " not found.</h2>");
            out.println("<p>Site " + siteName + " has received this request:</p><ul>");
        }
        out.println("<li>cantopath: " + getCantoPath() + "</li>");
        out.println("<li>filepath: " + filePath);
        out.println("<li>ServletPath: " + request.getServletPath() + "</li>");
        out.println("<li>ContextPath: " + request.getContextPath() + "</li>");
        out.println("<li>PathInfo: " + request.getPathInfo() + "</li>");
        out.println("<li>PathTranslated: " + request.getPathTranslated() + "</li>");
        out.println("<li>QueryString: " + request.getQueryString() + "</li>");
        out.println("<li>RequestedSessionID: " + request.getRequestedSessionId() + "</li>");
        out.println("<li>RequestURI: " + request.getRequestURI() + "</li>");
        out.println("</ul>");
        out.println("<hr><p><i>" + NAME_AND_VERSION + "</i></p></body></html>");
    }

    private void respondWithFile(File file, CantoSite site, HttpServletResponse response) throws ServletException, IOException {
        String filePath = file.getPath();
        String mimeType = servletContext.getMimeType(filePath);
        if (mimeType == null) {
            mimeType = servletContext.getMimeType(filePath.toLowerCase());
            if (mimeType == null) {
                mimeType = "text/plain";
            }
        }
        OutputStream out = response.getOutputStream();
        if (!site.respondWithFile(file, mimeType, out)) {

            // This closely follows the logic in org.apache.tomcat.modules.generators.StaticInterceptor,
            // without a number of checks on the path or support for directory listings

            int flen = (int) file.length();
            slog("Sending file " + filePath + " (" + flen + " bytes)");

            response.setContentType(mimeType);
            response.setContentLength(flen);

            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                byte[] buf = new byte[1024];
                int read = 0;
                long total = 0L;

                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                    total += read;
                }
                slog(total + " bytes transferred");


            } catch (FileNotFoundException fnfe) {
                slog("File " + filePath + " not found.");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);

            } catch (Exception e) {
                exception = e;
                String message = "Exception on server: " + e.toString();
                slog(message);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);

            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }


    private boolean isFiltered(String path) {
        int num = filters.length;
        for (int i = 0; i < num; i++) {
            if (filters[i].filter(path)) {
                return true;
            }
        }
        return false;
    }

}

class ExtensionFilter implements Filter {
    private String ext;

    public ExtensionFilter(String ext) {
        this.ext = (ext.charAt(0) == '.' ? ext.substring(1) : ext);
    }

    public boolean filter(String path) {
        int ix = path.lastIndexOf('.');
        if (ix >= 0 && ix < path.length() - 1) {
            return ext.equals(path.substring(ix + 1));
        }
        return false;
    }
}
