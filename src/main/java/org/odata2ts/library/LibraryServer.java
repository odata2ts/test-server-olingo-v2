package org.odata2ts.library;

import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.core.servlet.ODataServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Entry point: embedded Jetty in front of Olingo's plain {@code ODataServlet}.
 *
 * <p>No JAX-RS runtime is involved. Olingo ships a servlet of its own, so CXF or Jersey - which the
 * project's own samples pull in - are not needed, and neither is a WAR, a servlet container to deploy
 * into, or Spring.
 */
public final class LibraryServer {

  /** Mirrors the path of the other test servers, with the version segment saying what this one speaks. */
  static final String SERVICE_PATH = "/odata/v2/library";
  private static final int DEFAULT_PORT = 4004;

  private LibraryServer() {}

  public static void main(final String[] args) throws Exception {
    int port = port();
    Server server = new Server(port);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
    context.setContextPath("/");

    ServletHolder holder = new ServletHolder(new ODataServlet());
    holder.setInitParameter(ODataServiceFactory.FACTORY_LABEL, LibraryServiceFactory.class.getName());
    // No PATH_SPLIT: that option exists to carve a service root out of the *path info*, and the servlet
    // mapping below already consumes the whole prefix, leaving the resource path alone. Setting it to 3
    // makes Olingo try to eat three more segments and answer "The URL is too short".
    context.addServlet(holder, SERVICE_PATH + "/*");
    context.addFilter(DataServiceVersionFilter.class, SERVICE_PATH + "/*",
        java.util.EnumSet.of(javax.servlet.DispatcherType.REQUEST));

    server.setHandler(context);
    server.start();

    System.out.println("Library OData V2 service listening on http://localhost:" + port + SERVICE_PATH + "/");
    server.join();
  }

  private static int port() {
    String configured = System.getenv("PORT");
    return configured == null || configured.trim().isEmpty() ? DEFAULT_PORT : Integer.parseInt(configured.trim());
  }
}
