package pt.tecnico.sauron.silo.domain;

import java.util.Date;

public class Registry implements Comparable<Registry> {

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

    @Override
    public int compareTo(Registry t) {
        if (time.before(t.getTime()) || time.equals(t.getTime()) && camera.getName().compareTo(t.getCamera().getName()) == -1) return -1;
        else return 1;
    }

    @Override
    public boolean equals(Object o) {
        return false; //Two registries are never equal although they the same content (time is in milliseconds, which can't differentiate)
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    public CameraDomain getCamera() { return camera; }
    public String getType() { return type; }
    public String getIdentifier() { return identifier; }
    public Date getTime() { return time; }
}