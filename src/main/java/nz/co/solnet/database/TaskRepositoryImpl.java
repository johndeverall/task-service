package nz.co.solnet.database;

import nz.co.solnet.model.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Repository class that supports CRUD operations for tasks.
 */
class TaskRepositoryImpl extends Repository implements TaskRepository {

    private final Logger logger = LogManager.getLogger(TaskRepositoryImpl.class);

    public TaskRepositoryImpl(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Create a derby database if it doesn't exist and create tasks table.
     */
    void initialiseTasksTable() {

        try (Connection conn = getConnection()) {

            try (Statement statement = conn.createStatement()) {

                if (!doesTableExist("tasks")) {

                    StringBuilder sqlSB = new StringBuilder();
                    sqlSB.append("CREATE TABLE tasks (id int not null generated always as identity,");
                    sqlSB.append(" title varchar(256) not null,");
                    sqlSB.append(" description varchar(1024),");
                    sqlSB.append(" due_date date,");
                    sqlSB.append(" status varchar(10),");
                    sqlSB.append(" creation_date date not null,");
                    sqlSB.append(" primary key (id))");
                    statement.execute(sqlSB.toString());
                } else {
                    logger.info("Task table not created as it already exists.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in inserting initial data", e);
        }
    }

    void cleanTaskData() {
        try (Connection conn = getConnection(); Statement statement = conn.createStatement()) {
            String sql = "DELETE FROM tasks";
            statement.execute(sql);
        } catch (SQLException e) {
            logger.error("Error in cleaning tasks", e);
        }
    }

    /**
     * Create a new task.
     * @param task
     * @return
     */
    @Override
    public Task createTask(Task task) {
        String query = "INSERT INTO tasks (title, description, due_date, status, creation_date) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            setStatementParameters(statement, task);
            statement.executeUpdate();

            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                task.setId(resultSet.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting task " + task, e);
        }
        return getTask(task.getId()).get();
    }

    private void setStatementParameters(PreparedStatement statement, Task task) throws SQLException {
        statement.setString(1, task.getTitle());
        statement.setString(2, task.getDescription());
        statement.setDate(3, task.getDueDate() == null ? null : Date.valueOf(task.getDueDate()));
        statement.setString(4, task.getStatus() == null ? null : task.getStatus().getDbName());
        statement.setDate(5, new Date(System.currentTimeMillis()));
    }

    /**
     * Get an existing task.
     * @param id The id of the task to get.
     * @return
     */
    @Override
    public Optional<Task> getTask(int id) {
        String query = "SELECT * FROM tasks WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(query)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Task task = buildTaskFromResultSet(resultSet);
                    return Optional.of(task);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving task", e);
        }

        return Optional.empty();
    }

    /**
     * Get all tasks in the database.
     * @return
     */
    @Override
    public List<Task> getTasks() {
        List<Task> tasks = new ArrayList<>();

        String query = "SELECT * FROM tasks";

        try (Connection conn = getConnection();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                tasks.add(buildTaskFromResultSet(resultSet));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving tasks", e);
        }

        return tasks;
    }

    /**
     * Get tasks between two dates.
     * If either date is null, then the min or max date for apache derby is used.
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public List<Task> getTasksBetweenDates(LocalDate startDate, LocalDate endDate, List<Task.Status> statuses) {
        List<Task> tasks = new ArrayList<>();

        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM tasks WHERE status IN ");
        if (statuses != null) {
            query.append(getDbStatuses(statuses));
        } else {
            query.append(getDbStatuses(new ArrayList<>(Arrays.asList(Task.Status.values()))));
        }
        query.append(" AND due_date BETWEEN ? AND ?");
        LocalDate defaultStartDate = LocalDate.parse("0001-01-01"); // Min date for derby
        LocalDate defaultEndDate = LocalDate.parse("9999-12-31"); // Max date for derby

        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(query.toString())) {
            statement.setDate(1, Date.valueOf(startDate != null ? startDate : defaultStartDate));
            statement.setDate(2, Date.valueOf(endDate != null ? endDate : defaultEndDate));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Task task = buildTaskFromResultSet(resultSet);
                    tasks.add(task);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving tasks between dates", e);
        }
        return tasks;
    }

    private String getDbStatuses(List<Task.Status> statuses) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < statuses.size(); i++) {
            sb.append("'");
            sb.append(statuses.get(i).getDbName());
            sb.append("'");
            if (i < statuses.size() - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private Task buildTaskFromResultSet(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String status = resultSet.getString("status");
        String description = resultSet.getString("description");
        String title = resultSet.getString("title");
        LocalDate dueDate = resultSet.getDate("due_date") == null ? null : resultSet.getDate("due_date").toLocalDate();
        LocalDate creationDate = resultSet.getDate("creation_date").toLocalDate();

        return new Task.TaskBuilder()
                .withId(id)
                .withStatus(status)
                .withDescription(description)
                .withTitle(title)
                .withDueDate(dueDate)
                .withCreationDate(creationDate)
                .build();
    }

    /**
     * Update an existing task.
     * @param task
     * @return
     */
    @Override
    public Optional<Task> updateTask(Task task) {

        String query = "UPDATE tasks SET title = ?, description = ?, due_date = ?, status = ? WHERE id = ?";

        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, task.getTitle());
            statement.setString(2, task.getDescription());
            statement.setDate(3, task.getDueDate() == null ? null : Date.valueOf(task.getDueDate()));
            statement.setString(4, task.getStatus() == null ? null : task.getStatus().getDbName());
            statement.setInt(5, task.getId());
            statement.executeUpdate();
            return getTask(task.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Error updating task " + task, e);
        }
    }

    /**
     * Delete an existing task.
     * @param taskId
     */
    @Override
    public void deleteTask(int taskId) {
        String query = "DELETE FROM tasks WHERE id = ?";

        try (Connection conn = getConnection()) {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setInt(1, taskId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error in deleting task", e);
        }
    }
}
