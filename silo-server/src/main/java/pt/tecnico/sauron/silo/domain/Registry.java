package pt.tecnico.sauron.silo.domain;

import com.google.protobuf.Timestamp;

import pt.tecnico.sauron.silo.grpc.Silo.Camera;

public class Registry {

    private Camera camera;
    private String type;
    private String identifier;
    private Timestamp time;

    public Registry(Camera cam, String type, String id, Timestamp time) {
        this.camera = cam;
        this.type = type;
        this.identifier = id;
        this.time = time;
    }

    public Camera getCamera() { return camera; }
    public String getType() { return type; }
    public String getIdentifier() { return identifier; }
    public Timestamp getTime() { return time; }
}