package pt.tecnico.sauron.silo;

public enum Message {

    CAMERA_NULL("Camera must not be null or empty"),
    CAMERA_SIZE("Camera name must be between 3 and 15 characters in length"),
    CAMERA_COORDINATES(
            "Coordinates are invalid. Should be in degrees and latitude should be in range [-90.0, +90.0] and longitude in [-180.0, +180.0]"),
    CAMERA_NOT_EXISTS("Camera must already exist"),

    OBSERVATIONS_NOT_NULL("Observations List must not be null"),
    OBSERVATIONS_NOT_EMPTY("Observations List must not be empty"), OBSERVATION_NOT_NULL("Observation must not be null"),

    OBSERVABLE_NOT_NULL("Observable must not be null"),

    ID_NO_OBSERVATIONS("The object has no reports"),

    LOG_ELEMENT_NOT_FOUND(
            "[ERROR] LogElement contains invalid information, element has been set as applied but not added."),

    TYPE_NOT_EXISTS("The specified type is not available in the current system"),
    IDENTIFIER_NOT_MATCHED("The specified identifier does not match the type's specification"),

    EMPTY_SERVER("Server has no data"), EMPTY_INPUT("Input cannot be empty"),
    EMPTY_NAME("Name cannot be empty or null");

    private String msg;

    private Message(String msg) {
        this.msg = msg;
    }

    public String toString() {
        return msg;
    }

}