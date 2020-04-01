package pt.tecnico.sauron.silo.domain;

import com.google.protobuf.Timestamp;

import pt.tecnico.sauron.silo.grpc.Silo.Camera;

public class Registry implements Comparable<Registry> {

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
    
    public boolean before(Registry r) {
        return this.getTime().getSeconds() < r.getTime().getSeconds() || 
        this.getTime().getSeconds() == r.getTime().getSeconds() &&
        this.getTime().getNanos() < r.getTime().getNanos();
    }

    @Override
    public int compareTo(Registry r) {
        if (this.time.getSeconds() != r.time.getSeconds()) {
            return (int) (this.time.getSeconds() - r.time.getSeconds());
        }
        return this.time.getNanos() - r.time.getNanos();
    }
}