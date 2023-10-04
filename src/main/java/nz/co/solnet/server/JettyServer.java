package nz.co.solnet.server;

import nz.co.solnet.api.tasks.TaskServlet;
import nz.co.solnet.database.DatabaseContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class JettyServer {

    private Server server;

    private static final Logger logger = LogManager.getLogger(JettyServer.class);

    public void start(int serverPort, String shutdownSecret) throws Exception {

        // Hide the jetty info logs (and below) unless someone explicitly sets the level
        if (System.getProperty("org.eclipse.jetty.LEVEL") == null || System.getProperty("org.eclipse.jetty.LEVEL").isEmpty()) {
            System.setProperty("org.eclipse.jetty.LEVEL", "WARN");
        }

        int maxThreads = 100;
        int minThreads = 10;
        int idleTimeout = 120;

        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

        server = new Server(threadPool);

        server.setErrorHandler(new JettyErrorHandler());

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(serverPort);
        server.addConnector(connector);

        server.addEventListener(new LifeCycle.Listener() {
            @Override
            public void lifeCycleStarted(LifeCycle lifeCycle) {
                StringBuilder builder = new StringBuilder();
                String br = System.lineSeparator();
                builder.append("Startup complete" + br);
                builder.append("------------------------------------------------------------" + br);
                builder.append("Task API Server is ready to facilitate a more organised life!" + br);
                builder.append(br);
                builder.append("See http://localhost:" + serverPort + " for as built documentation" + br);
                builder.append("------------------------------------------------------------" + br);
                builder.append(br);
                logger.info(builder.toString());
            }

            @Override
            public void lifeCycleStopped(LifeCycle lifeCycle) {
                DatabaseContext.getInstance().shutdown();
                StringBuilder builder = new StringBuilder();
                String br = System.lineSeparator();
                builder.append("Shutting down..." + br);
                builder.append("----------------------------------------------" + br);
                builder.append("In the realm of tasks and dreams we've shared," + br);
                builder.append("Where our API server lovingly cared," + br);
                builder.append("As the day draws near its gentle close," + br);
                builder.append("Our server bids farewell, with sweet repose." + br);
                builder.append("----------------------------------------------" + br);
                builder.append("Shutdown complete" + br);
                logger.info(builder.toString());
            }
        });

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setBaseResource(Resource.newResource(getWebRootUri()));
        context.setContextPath("/");
        server.setHandler(context);

        // add our servlet
        ServletHolder holderApiTasks = new ServletHolder("tasks", TaskServlet.class);
        context.addServlet(holderApiTasks, "/api/tasks/*");

        // add the default servlet (servlet spec requirement)
        ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
        holderPwd.setInitParameter("dirAllowed", "false");
        context.addServlet(holderPwd, "/");

        // Create a ShutdownHandler
        ShutdownHandler shutdownHandler = new ShutdownHandler(shutdownSecret);
        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{shutdownHandler, context});
        server.setHandler(handlers);

        server.start();
    }

    private static URI getWebRootUri() throws URISyntaxException, MalformedURLException {
        ClassLoader cl = JettyServer.class.getClassLoader();
        URL fileURL = cl.getResource("webapp");
        if (fileURL == null) {
            throw new RuntimeException("Unable to find webapp folder");
        }
        String webRoot = fileURL.toString();
        return new URI(webRoot);
    }

}
