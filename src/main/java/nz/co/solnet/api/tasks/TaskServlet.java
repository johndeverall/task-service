package nz.co.solnet.api.tasks;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.gson.GsonBuilder;
import nz.co.solnet.database.DatabaseContext;
import nz.co.solnet.database.TaskRepository;
import nz.co.solnet.model.ConstraintViolation;
import nz.co.solnet.model.Task;
import org.apache.logging.log4j.Logger;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.apache.logging.log4j.LogManager.*;

/**
 * Servlet implementation to manage the creation, retrieval, update and deletion of tasks.
 */
public class TaskServlet extends HttpServlet {

    private static final Logger logger = getLogger(TaskServlet.class);

    /**
     * Create a task.
     * @param request HTTP request object
     * @param response HTTP response object
     * @throws IOException
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        TaskRepository repository = DatabaseContext.getInstance().getTaskRepository();

        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }

        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new GsonLocalDateAdapter()).create();

        Task task = null;
        List<ConstraintViolation> constraintViolations = new ArrayList<>();
        try {
            task = gson.fromJson(buffer.toString(), Task.class);
        } catch (DateTimeParseException e) {
            constraintViolations.add(new ConstraintViolation(e.getMessage(), buffer, e.getParsedString()));
        }

        if (task == null) {
            writeResponse(response, constraintViolations, HttpServletResponse.SC_BAD_REQUEST);
        } else {
            constraintViolations.addAll(task.validate());
            if (constraintViolations.isEmpty()) {
                task = repository.createTask(task);
                writeResponse(response, task, HttpServletResponse.SC_CREATED);
            } else {
                writeResponse(response, constraintViolations, HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    /**
     * Get a single task, a collection of tasks, or a collection of tasks within a date range.
     * Retrieving tasks within a date range can be useful for overdue task and calendar views.
     * @param request HTTP request object
     * @param response HTTP response object
     * @throws IOException
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TaskRepository repository = DatabaseContext.getInstance().getTaskRepository();

        if (requestHasResourceId(request)) {
            List<ConstraintViolation> violations = new ArrayList<>();
            Integer resourceId = getResourceIdFromRequest(request, violations);
            if (!violations.isEmpty()) {
                writeResponse(response, violations, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (resourceId != null) {
                Optional<Task> task = repository.getTask(resourceId);
                if (task.isPresent()) {
                    writeResponse(response, task.get(), HttpServletResponse.SC_OK);
                } else {
                    writeResponse(response, HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST);
            }
        } else { // request is for a collection of resources
            if (request.getRequestURI().equals("/api/tasks")) {
                String startDate = request.getParameter("startDate");
                String endDate = request.getParameter("endDate");
                String status = request.getParameter("status");

                List<ConstraintViolation> violations = new ArrayList<>();
                validateDateAndAddViolation("startDate", startDate, violations);
                validateDateAndAddViolation("endDate", endDate, violations);
                convertStringToEnumArray(status, violations);


                if (!violations.isEmpty()) {
                    writeResponse(response, violations, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                List<Task> tasks;
                if (startDate == null && endDate == null && status == null) {
                    tasks = repository.getTasks();
                } else {
                    tasks = repository.getTasksBetweenDates(
                            startDate == null ? null : LocalDate.parse(startDate),
                            endDate == null ? null : LocalDate.parse(endDate),
                            convertStringToEnumArray(status, violations));
                }

                writeResponse(response, tasks, HttpServletResponse.SC_OK);
            } else {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    private List<ConstraintViolation> validateDateAndAddViolation(String dateParameterName, String date, List<ConstraintViolation> violations) {
        if (date != null) {
            try {
                LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                violations.add(new ConstraintViolation("Date format must be YYYY-MM-DD", "", dateParameterName));
            }
        }
        return violations;
    }

    /**
     * Update a task.
     * The task to be updated is identified by the id in the request URI.
     * @param request HTTP request object
     * @param response HTTP response object
     * @throws IOException
     */
     /*
       Wrapping try block is a hack to work around https://github.com/eclipse/jetty.project/issues/163 or follow ons.
      */
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (!requestHasResourceId(request)) {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            List<ConstraintViolation> violations = new ArrayList<>();
            Integer taskId = getResourceIdFromRequest(request, violations);
            if (!violations.isEmpty()) {
                writeResponse(response, violations, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            StringBuilder buffer = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            Task task = null;
            Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new GsonLocalDateAdapter()).create();
            try {
                task = gson.fromJson(buffer.toString(), Task.class);
            } catch (DateTimeParseException e) {
                writeResponse(response, new ConstraintViolation(e.getMessage(), buffer, e.getParsedString()), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (task != null) {
                List<ConstraintViolation> constraintViolations = task.validate();
                if (constraintViolations.isEmpty()) {
                    task.setId(taskId);
                    TaskRepository repository = DatabaseContext.getInstance().getTaskRepository();
                    Optional<Task> existingTask = repository.getTask(taskId);
                    if (existingTask.isPresent()) {
                        Task updatedTask = repository.updateTask(task).get();
                        writeResponse(response, updatedTask, HttpServletResponse.SC_OK);
                    } else {
                        writeResponse(response, HttpServletResponse.SC_NOT_FOUND);
                    }
                } else {
                    writeResponse(response, constraintViolations, HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        } catch (Exception e) { // Hack to work around https://github.com/eclipse/jetty.project/issues/163 or follow ons.
            writeErrorResponse(response);
        }
    }

    private static void writeErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String errorMessage = "An error occurred";
        String jsonResponse = "{\"error\": \"" + errorMessage + "\"}";
        PrintWriter writer = response.getWriter();
        writer.println(jsonResponse);
    }

    /**
     * Delete a task.
     * @param request HTTP request object
     * @param response HTTP response object
     * @throws IOException
     */
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {

        /*
          Wrapping try block is a hack to work around https://github.com/eclipse/jetty.project/issues/163 or follow ons.
         */
        try {
            TaskRepository repository = DatabaseContext.getInstance().getTaskRepository();

            if (requestHasResourceId(request)) {
                List<ConstraintViolation> violations = new ArrayList<>();
                Integer resourceId = getResourceIdFromRequest(request, violations);
                if (!violations.isEmpty()) {
                    writeResponse(response, violations, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                if (resourceId != null && repository.getTask(resourceId).isPresent()) {
                    repository.deleteTask(resourceId);
                    writeResponse(response, HttpServletResponse.SC_NO_CONTENT);
                } else {
                    writeResponse(response, HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch(Exception e) { // Hack to work around
            writeErrorResponse(response);
        }
    }

    private void writeResponse(HttpServletResponse response, Object data, int statusCode) throws IOException {
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new GsonLocalDateAdapter()).create();
        String json = gson.toJson(data);
        response.getWriter().println(json);
        writeResponse(response, statusCode);
    }

    private void writeResponse(HttpServletResponse response, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
    }

    private boolean requestHasResourceId(HttpServletRequest request) {
        return getRequestParts(request).length == 3;
    }

    private Integer getResourceIdFromRequest(HttpServletRequest request, List<ConstraintViolation> constraintViolations) {
        String resourceId = getRequestParts(request)[2];
        int id = 0;
        try {
            id = Integer.parseInt(resourceId);
        } catch (NumberFormatException e) {
            constraintViolations.add(new ConstraintViolation("Invalid task id", "", resourceId));
            return null;
        }
        if (request.getRequestURI().equals("/api/tasks/" + resourceId)) {
            return id;
        } else {
            return null;
        }
    }

    private String[] getRequestParts(HttpServletRequest request) {
        return Arrays.stream(request.getRequestURI().split("/"))
                .filter(str -> !str.isEmpty())
                .toArray(String[]::new);
    }

    private static <E extends Enum<E>> E[] createArray(Class<E> enumClass) {
        return enumClass.getEnumConstants();
    }

    public static List<Task.Status> convertStringToEnumArray(String statusesAsString, List<ConstraintViolation> violations) {
        if (statusesAsString == null) {
            return null;
        } else {
            String[] stringArray = statusesAsString.split(",");
            List<Task.Status> statuses = new ArrayList<>();

            for (String value : stringArray) {
                try {
                    Task.Status enumValue = Task.Status.valueOf(value.trim());
                    statuses.add(enumValue);
                } catch (IllegalArgumentException e) {
                    violations.add(new ConstraintViolation("Invalid status: " + value + ". Status must be one of TODO, IN_PROGRESS or DONE", "", value));
                }
            }
            return statuses;
        }
    }

}
