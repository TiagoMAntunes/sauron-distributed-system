package pt.tecnico.sauron.silo.domain;

public class InvalidTypeException extends Exception {

    private String type;

    public InvalidTypeException(String type) {
        this.type = type;
    }

    public String getType() { return type; }
}
