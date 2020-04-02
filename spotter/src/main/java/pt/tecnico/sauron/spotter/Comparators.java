package pt.tecnico.sauron.spotter;

import java.util.Comparator;

import pt.tecnico.sauron.silo.grpc.Silo.Observation;

public final class Comparators {
    public static final Comparator<Observation> OBSERVATION_ID = (Observation a, Observation b) -> a.getObservated().getIdentifier().compareTo(b.getObservated().getIdentifier());
}