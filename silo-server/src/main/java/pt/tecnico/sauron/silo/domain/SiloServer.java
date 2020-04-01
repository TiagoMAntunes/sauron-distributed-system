package pt.tecnico.sauron.silo.domain;

import java.util.*;

import pt.tecnico.sauron.silo.grpc.Silo;
import pt.tecnico.sauron.silo.grpc.Silo.Camera; //TODO remove import of grpc entity


public class SiloServer {

    private Map<String, ArrayList<Registry>> registriesMap = new HashMap<>();
    private Map<String, Camera> cameras = new HashMap<>();

    public synchronized boolean clear() {
        registriesMap.clear();
        return true;
    }

    public synchronized void inputRegistry(String identifier , Registry reg) {
        registriesMap.get(identifier).add(reg);
    }

    public synchronized List<Registry> getRegistries(String identifier) {
        return registriesMap.get(identifier);
    }

    public synchronized  boolean registryExists(String identifier) {
        return registriesMap.containsKey(identifier);
    }
    
    public synchronized Registry getMostRecentRegistry(String identifier) {
        //Gets list of registries for a given identifier
        if(registriesMap.containsKey(identifier)) {
            List<Registry> registries = registriesMap.get(identifier);
            Registry mostRecentRegistry = registries.get(0);

            //Gets the most recent registry
            for (Registry r : registries) {
                if (r.before(mostRecentRegistry))
                    mostRecentRegistry = r;
            }

            return mostRecentRegistry;
        }
        else return null;
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

    //Returns list of registries from the most recent to the oldest
    public synchronized ArrayList<Registry> getSortedRegistries(String identifier) {
        ArrayList<Registry> registries = registriesMap.get(identifier);
        registries.sort(Registry::compareTo);
        return registries;
    }
}