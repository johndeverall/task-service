package nz.co.solnet.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task {

    private int id;

    private static final int MAX_TITLE_LENGTH = 256;
    private String title;

    private static final int MAX_DESCRIPTION_LENGTH = 1024;
    private String description;

    @SerializedName("due_date")
    private LocalDate dueDate;

    @SerializedName("creation_date")
    private LocalDate creationDate;

    private static final int MAX_STATUS_LENGTH = 10;
    private Status status;

    public enum Status {
        TODO("TODO"),
        IN_PROGRESS("INPROGRESS"),
        DONE("DONE");

        private String dbName;

        Status(String dbName) {
            if (dbName == null) {
                throw new IllegalArgumentException("Enum name or database name cannot be null");
            }
            this.dbName = dbName.substring(0, Math.min(dbName.length(), 10));
        }

        public String getDbName() {
            return dbName;
        }

        public static Status getStatusByDbName(String dbName) {
            for (Status status : Status.values()) {
                if (status.getDbName().equalsIgnoreCase(dbName)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid DB name: " + dbName);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String toString() {
        return new StringBuilder()
                .append("[")
                .append("id: ")
                .append(id)
                .append(", ")
                .append("title: ")
                .append(title)
                .append(", ")
                .append("description: ")
                .append(description)
                .append(", ")
                .append("dueDate: ")
                .append(dueDate)
                .append(", ")
                .append("creationDate: ")
                .append(creationDate)
                .append(", ")
                .append("status: ")
                .append(status)
                .append("]")
                .toString();
    }

    /**
     * Returns a possibly empty list of constraint violations.
     * @return
     */
    public List<ConstraintViolation> validate() {
        List<ConstraintViolation> violations = new ArrayList<>();

        // Validate title
        if (getTitle() == null || getTitle().isEmpty()) {
            ConstraintViolation violation = new ConstraintViolation("Title is required", "title", getTitle());
            violations.add(violation);
        } else if (getTitle().length() > MAX_TITLE_LENGTH) {
            ConstraintViolation violation = new ConstraintViolation("Title exceeds maximum length of " + MAX_TITLE_LENGTH, "title", getTitle());
            violations.add(violation);
        }

        // Validate description
        if (getDescription() != null && getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            ConstraintViolation violation = new ConstraintViolation("Description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH, "description", getDescription());
            violations.add(violation);
        }

        return violations;
    }

    public static class TaskBuilder {

        private int id;

        private String title;

        private String description;

        private LocalDate dueDate;

        private LocalDate creationDate;

        private Status status;

        public TaskBuilder withId(int id) {
            this.id = id;
            return this;
        }

        public TaskBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public TaskBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public TaskBuilder withDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public TaskBuilder withCreationDate(LocalDate creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public TaskBuilder withStatus(String status) {
            if (status == null) {
                this.status = null;
            } else {
                this.status = Status.getStatusByDbName(status);
            }
            return this;
        }

        public Task build() {
            Task t = new Task();
            t.id = id;
            t.title = title;
            t.creationDate = creationDate;
            t.dueDate = dueDate;
            t.description = description;
            t.status = status;
            return t;
        }
    }

}
