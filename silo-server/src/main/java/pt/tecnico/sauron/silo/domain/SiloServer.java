package pt.tecnico.sauron.silo.domain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Silo server.
 */
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

        /**
         * Gets key.
         *
         * @param r Registry
         * @return correspondent RegistryKey
         */
        public static RegistryKey getKey(Registry r) {
            return new RegistryKey(r.getType(), r.getIdentifier());
        }

        /**
         * Gets key.
         *
         * @param type String
         * @param id   the id
         * @return correspondent RegistryKey
         */
        public static RegistryKey getKey(String type, String id) {
            return new RegistryKey(type, id);
        }

        /**
         * @param o object
         * @return boolean
         */
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

    private Map<RegistryKey, ArrayList<Registry>> registriesMap = new HashMap<>();
    private Map<String, CameraDomain> cameras = new HashMap<>();

    /**
     * Returns list of observations that match the partial identifier provided
     *
     * @return the boolean
     */
    public synchronized boolean clear() {
        cameras.clear();
        registriesMap.clear();
        return true;
    }

    /**
     * Gets registries.
     *
     * @param type       the type
     * @param identifier the identifier
     * @return the registries
     */
    public synchronized List<Registry> getRegistries(String type, String identifier) {
        return registriesMap.get(RegistryKey.getKey(type, identifier));
    }

    /**
     * Registry exists boolean.
     *
     * @param type       the type
     * @param identifier the identifier
     * @return the boolean
     */
    public synchronized boolean registryExists(String type, String identifier) {
        return registriesMap.containsKey(RegistryKey.getKey(type, identifier));
    }

    /**
     * Gets most recent registry.
     *
     * @param type       the type
     * @param identifier the identifier
     * @return the most recent registry
     */
    public synchronized Registry getMostRecentRegistry(String type, String identifier) {
        if (registriesMap.containsKey(RegistryKey.getKey(type, identifier))) {
            return registriesMap.get(RegistryKey.getKey(type, identifier)).get(registriesMap.get(RegistryKey.getKey(type, identifier)).size() -1);
        } else return null;
    }

    /**
     * Camera exists boolean.
     *
     * @param cameraName the camera name
     * @return the boolean
     */
    public synchronized boolean cameraExists(String cameraName) {
        return cameras.containsKey(cameraName);
    }

    /**
     * Add camera.
     *
     * @param camObj the cam obj
     */
    public synchronized void addCamera(CameraDomain camObj) {
        cameras.put(camObj.getName(), camObj);
    }

    /**
     * Gets camera.
     *
     * @param cameraName the camera name
     * @return the camera
     */
    public synchronized CameraDomain getCamera(String cameraName) {
        return cameras.get(cameraName);
    }

    /**
     * Add registries.
     *
     * @param registries the registries
     */
    public synchronized void addRegistries(List<Registry> registries) {
        for (Registry r : registries)
            if (registriesMap.containsKey(RegistryKey.getKey(r)))
                registriesMap.get(RegistryKey.getKey(r)).add(r);
            else {
                ArrayList<Registry> list = new ArrayList<>();
                list.add(r);
                registriesMap.put(RegistryKey.getKey(r), list);
            }
    }

    /**
     * Gets all recent registries.
     *
     * @param type              the type
     * @param partialIdentifier the partial identifier
     * @return the all recent registries
     */
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

    /**
     * No registries boolean.
     *
     * @return the boolean
     */
    public synchronized boolean noRegistries() {
        return registriesMap.isEmpty();
    }
}
