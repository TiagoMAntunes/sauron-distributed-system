package pt.tecnico.sauron.silo.domain;

import java.util.Date;

/**
 * The registry class
 */
public class Registry implements Comparable<Registry> {

    private CameraDomain camera;
    private String type;
    private String identifier;
    private Date time;

    /**
     * Instantiates a new Registry.
     *
     * @param cam  the cam
     * @param type the type
     * @param id   the id
     * @param time the time
     */
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

    /**
     * Gets camera.
     *
     * @return the camera
     */
    public CameraDomain getCamera() { return camera; }

    /**
     * Gets type.
     *
     * @return the type
     */
    public String getType() { return type; }

    /**
     * Gets identifier.
     *
     * @return the identifier
     */
    public String getIdentifier() { return identifier; }

    /**
     * Gets time.
     *
     * @return the time
     */
    public Date getTime() { return time; }
}