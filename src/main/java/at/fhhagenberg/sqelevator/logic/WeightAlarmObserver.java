package at.fhhagenberg.sqelevator.logic;

import at.fhhagenberg.sqelevator.domain.Alarm;
import at.fhhagenberg.sqelevator.domain.Elevator;
import at.fhhagenberg.sqelevator.domain.ElevatorStatus;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class WeightAlarmObserver implements AlarmObserver {

    private static final double WEIGHT_THRESHOLD = 100;

    @Override
    public void update(List<ElevatorStatus> elevatorStatuses) {
        for (var elevatorStatus : elevatorStatuses) {
            var elevator = elevatorStatus.getElevator();

            if (isOverThreshold(elevatorStatus.getPayload(), elevator.getMaximumPayload())) {
                var alarms = elevator.getAlarms();
                var weightAlarm = new Alarm("Payload approaching maximum payload!", LocalDateTime.now());

                if (alarms.isEmpty()) {
                    elevator.alarmsProperty().add(weightAlarm);
                } else {
                    var mostRecentAlarm = alarms.get(alarms.size() - 1);

                    if (mostRecentAlarm.getTimestamp().plusMinutes(1).isBefore(weightAlarm.getTimestamp())) {
                        Platform.runLater(() -> elevator.alarmsProperty().add(weightAlarm));
                    }
                }
            }
        }
    }

    private boolean isOverThreshold(double currentPayload, double maximumPayload) {
        return currentPayload > maximumPayload - WEIGHT_THRESHOLD;
    }
}
