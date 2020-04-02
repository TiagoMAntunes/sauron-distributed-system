package pt.tecnico.sauron.silo.domain;

public class IncorrectDataException extends Exception {
    
    private String id, type;

    public IncorrectDataException(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() { return id; }
    public String getType() { return type; }

}


