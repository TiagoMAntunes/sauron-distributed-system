package pt.tecnico.sauron.silo.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;

public class ObservationsHashMap {

    private Map<String, ArrayList<Observation>> observationsHashMap = new HashMap<String, ArrayList<Observation>>();

    
    public void inputObservations(String identifier , ArrayList<Observation> observations) {
        observationsHashMap.put(identifier, observations);

    }

    public ArrayList<Observation> getObservations(String identifier) {
        return observationsHashMap.get(identifier);
    }
}