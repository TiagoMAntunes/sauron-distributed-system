package pt.tecnico.sauron.silo.domain;

import java.util.Date;

import pt.tecnico.sauron.silo.grpc.Silo.Camera;

public class Registry {

    private Camera camera;
    private String type;
    private String identifier;
    private Date time;

    public Registry(Camera cam, String type, String id, Date time) {
        this.camera = cam;
        this.type = type;
        this.identifier = id;
        this.time = time;
    }

    public Camera getCamera() { return camera; }
    public String getType() { return type; }
    public String getIdentifier() { return identifier; }
    public Date getTime() { return time; }
}