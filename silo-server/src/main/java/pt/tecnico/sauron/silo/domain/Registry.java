package pt.tecnico.sauron.silo.domain;

import java.util.Date;

public class Registry {

    private CameraDomain camera;
    private String type;
    private String identifier;
    private Date time;

    public Registry(CameraDomain cam, String type, String id, Date time) {
        this.camera = cam;
        this.type = type.toUpperCase();
        this.identifier = id.toUpperCase();
        this.time = time;
    }

    public CameraDomain getCamera() { return camera; }
    public String getType() { return type; }
    public String getIdentifier() { return identifier; }
    public Date getTime() { return time; }
}