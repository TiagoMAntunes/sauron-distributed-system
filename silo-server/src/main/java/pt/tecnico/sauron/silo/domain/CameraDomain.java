package pt.tecnico.sauron.silo.domain;

import com.google.type.LatLng;

public class CameraDomain {

    private String name;
    private LatLng coords;

    public CameraDomain(String name, LatLng coords) {
        this.name = name;
        this.coords = coords;

    }

    public String getName() { return name; }
    public LatLng getCoords() { return coords; }

}