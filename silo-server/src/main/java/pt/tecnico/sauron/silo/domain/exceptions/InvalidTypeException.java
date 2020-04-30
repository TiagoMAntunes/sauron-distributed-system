package pt.tecnico.sauron.silo.domain.exceptions;

/**
 * The type Invalid type exception.
 */
public class InvalidTypeException extends Exception {

    private String type;

    /**
     * Instantiates a new Invalid type exception.
     *
     * @param type the type
     */
    public InvalidTypeException(String type) {
        this.type = type;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public String getType() { return type; }
}
