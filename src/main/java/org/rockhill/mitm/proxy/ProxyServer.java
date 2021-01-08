package org.rockhill.mitm.proxy;

import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.ServerConnector2;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.rockhill.mitm.proxy.https.SelfSignedSslEngineSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.Objects;

public class ProxyServer {
    private final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    private final Server server;
    private int port;
    private int securePort;

    public ProxyServer(int port) {
        this.port = port;
        this.securePort = port;
        server = new Server(port);
        // Establish listening connector

        // Add SSL connector
        SslContextFactory sslContextFactory = new SslContextFactory.Server.Server();

        SelfSignedSslEngineSource contextSource = new SelfSignedSslEngineSource();
        SSLContext sslContext = contextSource.getSslContext();

        sslContextFactory.setSslContext(sslContext);

        sslContextFactory.setIncludeProtocols("SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3");

        ServerConnector2 connector = new ServerConnector2(server, sslContextFactory);

     //   ServerConnector connector = new ServerConnector(server);
        //connector.setPort(port);
        connector.setIdleTimeout(0);
        server.addConnector(connector);

        // Setup proxy handler to handle CONNECT methods
        ConnectHandler proxy = new ConnectHandler();
        server.setHandler(proxy);

        // Setup proxy servlet
        ServletContextHandler context = new ServletContextHandler(proxy, "/", ServletContextHandler.SESSIONS);
        ServletHolder proxyServlet = new ServletHolder(ProxyServlet.class);

        //proxyServlet.setInitParameter("blackList", "www.eclipse.org");
        context.addServlet(proxyServlet, "/*");

    }

    public void start(int timeout) throws Exception {
        try {
            server.start();
            for (Connector c: server.getConnectors()) {
                if (c instanceof ServerConnector) {
                    ((ServerConnector) c).setStopTimeout(timeout);
                }
                if (c instanceof ServerConnector2) {
                    ((ServerConnector2) c).setStopTimeout(timeout);
                }
            }
            //detect port
            port = findLocalHttpPort(server);
            securePort = findLocalHttpsPort(server);
            logger.info("Proxy Server started on ports [http:{}] [https:{}]", port, securePort);
        } catch (Exception e) {
            logger.error("Cannot start proxy server", e);
            throw e;
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            logger.error("Exception occurred during Proxy Server stop", e);
        } finally {
            logger.info("Proxy Server stopped.");
        }
    }

    public int getPort() {
        return port;
    }

    /**
     * Finds the port the specified server is listening for HTTP connections on.
     *
     * @param webServer started web server
     * @return HTTP port, or -1 if no HTTP port was found
     */
    private int findLocalHttpPort(Server webServer) {
        for (Connector connector : webServer.getConnectors()) {
            if (!Objects.equals(connector.getDefaultConnectionFactory().getProtocol(), "SSL")) {
                int port = -1;
                if (connector instanceof ServerConnector) {
                    port = ((ServerConnector) connector).getLocalPort();
                }
                if (connector instanceof ServerConnector2) {
                    port = ((ServerConnector2) connector).getLocalPort();
                }

                return port;
            }
        }
        return -1;
    }

    /**
     * Finds the port the specified server is listening for HTTPS connections on.
     *
     * @param webServer started web server
     * @return HTTP port, or -1 if no HTTPS port was found
     */
    private int findLocalHttpsPort(Server webServer) {
        for (Connector connector : webServer.getConnectors()) {
            if (Objects.equals(connector.getDefaultConnectionFactory().getProtocol(), "SSL")) {
                return ((ServerConnector2) connector).getLocalPort();
            }
        }

        return -1;
    }

}
