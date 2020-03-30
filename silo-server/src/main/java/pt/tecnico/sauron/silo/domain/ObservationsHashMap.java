package pt.tecnico.sauron.silo.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;

public class ObservationsHashMap {

    private Map<String, ArrayList<Observation>> observationsHashMap = new HashMap<String, ArrayList<Observation>>();

    public ArrayList<Observation> getObservations(String identifier) {
        return observationsHashMap.get(identifier);
    }

    public void inputObservation(String identifier , Observation observation) {
        
        ArrayList<Observation> observations = getObservations(identifier);
        observations.add(observation);

        //Replaces current list with the old list plus the new observation
        observationsHashMap.put(identifier, observations);

    }
}