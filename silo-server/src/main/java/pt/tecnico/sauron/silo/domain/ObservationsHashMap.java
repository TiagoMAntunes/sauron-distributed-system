package pt.tecnico.sauron.silo.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;

public class ObservationsHashMap {

    private Map<String, ArrayList<Observation>> observationsHashMap = new HashMap<String, ArrayList<Observation>>();

    public void inputObservation(String identifier , Observation observation) {
        observationsHashMap.get(identifier).add(observation);
    }

    public List<Observation> getObservations(String identifier) {
        return observationsHashMap.get(identifier);
    }  
}