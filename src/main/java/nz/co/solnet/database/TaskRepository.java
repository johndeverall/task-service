package nz.co.solnet.database;

import nz.co.solnet.model.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Interface for the TaskRepository.
 */
public interface TaskRepository {

    Task createTask(Task task);

    Optional<Task> getTask(int id);

    List<Task> getTasks();

    List<Task> getTasksBetweenDates(LocalDate startDate, LocalDate endDate, List<Task.Status> statuses);

    Optional<Task> updateTask(Task task);

    void deleteTask(int taskId);

}
