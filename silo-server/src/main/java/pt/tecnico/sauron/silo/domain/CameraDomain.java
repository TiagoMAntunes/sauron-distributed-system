package pt.tecnico.sauron.silo.domain;

/**
 * The type Camera domain.
 */
public class CameraDomain {

    private String name;
    private double lat, lon;

    /**
     * Instantiates a new Camera domain.
     *
     * @param name the name
     * @param lat  the lat
     * @param lon  the lon
     */
    public CameraDomain(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;

    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() { return name; }

    /**
     * Gets latitude.
     *
     * @return the latitude
     */
    public double getLatitude() { return lat; }

    /**
     * Gets longitude.
     *
     * @return the longitude
     */
    public double getLongitude() { return lon; }

}