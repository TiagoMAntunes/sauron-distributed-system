package pt.tecnico.sauron.silo.domain;

import java.util.Date;

/**
 * The type Registry.
 */
public class Registry {

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