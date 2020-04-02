package pt.tecnico.sauron.silo.domain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pt.tecnico.sauron.silo.grpc.Silo;
import pt.tecnico.sauron.silo.grpc.Silo.Camera; //TODO remove import of grpc entity


public class SiloServer {

    private Map<String, ArrayList<Registry>> registriesMap = new HashMap<>();
    private Map<String, CameraDomain> cameras = new HashMap<>();

    public synchronized boolean clear() {
        cameras.clear();
        registriesMap.clear();
        return true;
    }

    public synchronized void inputRegistry(String identifier, Registry reg) {
        registriesMap.get(identifier).add(reg);
    }

    public synchronized List<Registry> getRegistries(String identifier) {
        return registriesMap.get(identifier);
    }

    public synchronized boolean registryExists(String identifier) {
        return registriesMap.containsKey(identifier);
    }

    public synchronized Registry getMostRecentRegistry(String identifier) {
        if (registriesMap.containsKey(identifier)) {
            return registriesMap.get(identifier).get(registriesMap.get(identifier).size() -1);
        } else return null;
    }

    public synchronized boolean cameraExists(String cameraName) {
        return cameras.containsKey(cameraName);
    }

    public synchronized void addCamera(CameraDomain camObj) {
        cameras.put(camObj.getName(), camObj);
    }

    public synchronized CameraDomain getCamera(String cameraName) {
        return cameras.get(cameraName);
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

    public synchronized ArrayList<Registry> getAllRecentRegistries(String partialIdentifier) {
        Pattern p = Pattern.compile(partialIdentifier.replace("*",".*"));
        Matcher m;
        ArrayList<Registry> registries = new ArrayList<>();

        for (String identifier : registriesMap.keySet()) {
            m = p.matcher(identifier);
            if (m.matches()) {
                registries.add(getMostRecentRegistry(identifier));
            }
        }
        return registries;
    }

    public boolean noRegistries() {
        return registriesMap.isEmpty();
    }
}
