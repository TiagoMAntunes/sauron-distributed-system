package pt.tecnico.sauron.silo.domain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiloServer {

    private static class RegistryKey {
        /*
            This class removes the limitation of different types of objects having the same id
            Which would cause collisions 
        */

        private String type, id;
        
        private RegistryKey(String type, String id) {
            this.type = type.toUpperCase();
            this.id = id.toUpperCase();
        }

        public static RegistryKey getKey(Registry r) {
            return new RegistryKey(r.getType(), r.getIdentifier());
        }

        public static RegistryKey getKey(String type, String id) {
            return new RegistryKey(type, id);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null ||  ! (o instanceof RegistryKey)) 
                return false;
            RegistryKey k = (RegistryKey) o;
            return type.equals(k.type) && id.equals(k.id);
        }

        @Override
        public int hashCode() {
            return (type + id).hashCode();
        }

        @Override
        public String toString() {return type + " " + id; }

    }

    private Map<RegistryKey, TreeSet<Registry>> registriesMap = new HashMap<>();
    private Map<String, CameraDomain> cameras = new HashMap<>();
    private VectorClockDomain clock;
    public SiloServer(VectorClockDomain clock) {
        this.clock = clock;
    }

    public synchronized boolean clear() {
        cameras.clear();
        registriesMap.clear();
        clock.clear();
        return true;
    }

    public synchronized Collection<Registry> getRegistries(String type, String identifier) {
        return registriesMap.get(RegistryKey.getKey(type, identifier));
    }

    public synchronized boolean registryExists(String type, String identifier) {
        return registriesMap.containsKey(RegistryKey.getKey(type, identifier));
    }

    public synchronized Registry getMostRecentRegistry(String type, String identifier) {
        if (registriesMap.containsKey(RegistryKey.getKey(type, identifier))) {
            return registriesMap.get(RegistryKey.getKey(type, identifier)).last();
        } else return null;
    }

    public synchronized boolean cameraExists(String cameraName) {
        return cameras.containsKey(cameraName);
    }

    public synchronized void addCamera(CameraDomain camObj, int origin) {
        cameras.put(camObj.getName(), camObj);
        clock.incUpdate(origin);
    }

    public synchronized CameraDomain getCamera(String cameraName) {
        return cameras.get(cameraName);
    }


    public synchronized void addRegistries(List<Registry> registries, int origin) {
        //When Adding Registries update the vector clock regarding this replica
        for (Registry r : registries) {
            addRegistry(r); // only add
        }
        System.out.println("Incrementing multiple registries");
        clock.incUpdate(origin);
    }

    //Increment origin and add
    public synchronized void addRegistry(Registry r, int origin) {
        System.out.println("Incrementing single registry");
        clock.incUpdate(origin);
        addRegistry(r);
    }

    //Only add
    public synchronized void addRegistry(Registry r) {
        if (!registriesMap.containsKey(RegistryKey.getKey(r))) //Adds treemap
            registriesMap.put(RegistryKey.getKey(r), new TreeSet<Registry>());
        registriesMap.get(RegistryKey.getKey(r)).add(r);
    }

   

    public synchronized ArrayList<Registry> getAllRecentRegistries(String type, String partialIdentifier) {
        Pattern p = Pattern.compile(partialIdentifier.toUpperCase().replace("*",".*"));
        Matcher m;
        ArrayList<Registry> registries = new ArrayList<>();

        for (RegistryKey key : registriesMap.keySet()) {
            if (!key.type.equals(type.toUpperCase())) continue; //different types
            String identifier = key.id;
            m = p.matcher(identifier);
            if (m.matches()) {
                registries.add(getMostRecentRegistry(type, identifier));
            }
        }
        return registries;
    }

    public synchronized boolean noRegistries() {
        return registriesMap.isEmpty();
    }

    public synchronized VectorClockDomain getClock() {
        return this.clock;
    }

}
