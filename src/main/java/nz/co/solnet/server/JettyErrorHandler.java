package nz.co.solnet.server;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Used by Jetty to returns a JSON error response when an exception is thrown by the application.
 */
// Note that this handler is not used by DELETE and PUT requests due to
// this issue (and subsequent issues) here https://github.com/eclipse/jetty.project/issues/163
// Therefore the DELETE and PUT operations supply their own error handling logic.
public class JettyErrorHandler extends ErrorHandler {

    private static final Logger logger = LogManager.getLogger(JettyErrorHandler.class);

    @Override
    public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        Throwable throwable = (Throwable) baseRequest.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (throwable != null) {
            String formattedMessage = MessageFormatter.format("An error occurred: {}", throwable.getMessage()).getMessage();
            logger.error(formattedMessage, throwable);
        }

        // Create the error JSON response
        String errorMessage = "An error occurred";
        String jsonResponse = "{\"error\": \"" + errorMessage + "\"}";

        PrintWriter writer = response.getWriter();
        writer.println(jsonResponse);
        writer.flush();

        baseRequest.setHandled(true);
    }
}

