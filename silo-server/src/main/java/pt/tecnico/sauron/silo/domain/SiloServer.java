package pt.tecnico.sauron.silo.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.tecnico.sauron.silo.grpc.Silo.Camera; //TODO remove import of grpc entity


public class SiloServer {

    private Map<String, ArrayList<Registry>> registriesMap = new HashMap<>();
    private Map<String, Camera> cameras = new HashMap<>();

    public synchronized boolean clear() {
        return true;
    }

    public synchronized void inputRegistry(String identifier , Registry reg) {
        registriesMap.get(identifier).add(reg);
    }

    public synchronized List<Registry> getRegistries(String identifier) {
        return registriesMap.get(identifier);
    }  

    public synchronized boolean cameraExists(String cameraName) {
        return cameras.containsKey(cameraName);
    }

    public synchronized void addRegistries(List<Registry> registries) {
        for (Registry r : registries)
            if (registriesMap.containsKey(r.getIdentifier()))
                registriesMap.get(r.getIdentifier()).add(r);
            else {
                ArrayList<Registry> list = new ArrayList<>();
                list.add(r);
                registriesMap.put(r.getIdentifier(), list);
            }
    }

}