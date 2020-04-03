package pt.tecnico.sauron.silo.domain;

public class CameraDomain {

    private String name;
    private double lat, lon;

    public CameraDomain(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;

    }

    public String getName() { return name; }
    public double getLatitude() { return lat; }
    public double getLongitude() { return lon; }

}