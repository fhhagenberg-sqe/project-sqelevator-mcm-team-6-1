package at.fhhagenberg.sqelevator.model;

import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDateTime;

public class Alarm {
    private String message;
    private LocalDateTime timestamp;

    public Alarm(String message, LocalDateTime timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
