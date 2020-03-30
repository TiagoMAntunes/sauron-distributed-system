package pt.tecnico.sauron.silo.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;


public class SiloServer {

    private Map<String, ArrayList<Observation>> observationsHashMap = new HashMap<String, ArrayList<Observation>>();

    public synchronized boolean clear() {
        return true;
    }

    public synchronized void inputObservation(String identifier , Observation observation) {
        observationsHashMap.get(identifier).add(observation);
    }

    public synchronized List<Observation> getObservations(String identifier) {
        return observationsHashMap.get(identifier);
    }  

}