package pt.tecnico.sauron.silo.domain.exceptions;

/**
 * The type Incorrect data exception.
 */
public class IncorrectDataException extends Exception {
    
    private String id, type;

    /**
     * Instantiates a new Incorrect data exception.
     *
     * @param id   the id
     * @param type the type
     */
    public IncorrectDataException(String id, String type) {
        this.id = id;
        this.type = type;
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() { return id; }

    /**
     * Gets type.
     *
     * @return the type
     */
    public String getType() { return type; }

}


