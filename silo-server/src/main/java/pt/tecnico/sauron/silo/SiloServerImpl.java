package pt.tecnico.sauron.silo;

import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Status;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.domain.SiloServer;
import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.domain.Registry;

import static io.grpc.Status.INVALID_ARGUMENT;

import java.util.ArrayList;
import java.util.List;

import io.grpc.stub.StreamObserver;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;

import com.google.protobuf.Timestamp;


public class SiloServerImpl extends SauronGrpc.SauronImplBase {

    private final SiloServer silo = new SiloServer();

    @Override
    public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        
        String camName = request.getCameraName();
        List<Observation> observations = request.getObservationsList();
        
        ReportResponse response;
        if (camName == null) {
            response = ReportResponse.newBuilder().setResponseStatus(Status.INVALID_ARG).build();
        } else if (camName.equals("") || !silo.cameraExists(camName)) {
            response = ReportResponse.newBuilder().setResponseStatus(Status.INVALID_CAM).build();
        } else if (observations == null) {
            response = ReportResponse.newBuilder().setResponseStatus(Status.NULL_OBS).build();
        } else if (observations.isEmpty()) {
            response = ReportResponse.newBuilder().setResponseStatus(Status.INVALID_OBS).build();
        } else {
            //Transform into registries
            
            ArrayList<Registry> list = new ArrayList<>();
            for (Observation o : observations) {
                Camera cam = o.getCamera();
                String type = o.getObservated().getType();
                String id = o.getObservated().getIdentifier();
                Timestamp time = fromMillis(currentTimeMillis());
                Registry r = new Registry(cam, type, id, time);
                list.add(r);
            }
            silo.addRegistries(list);
            
            response = ReportResponse.newBuilder().setResponseStatus(Status.OK).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void controlPing(ControlPingRequest request, StreamObserver<ControlPingResponse> responseObserver) {
        String inputText = request.getInputText();

        if (inputText == null || inputText.isBlank()) 
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty!").asRuntimeException());

        ControlPingResponse response = ControlPingResponse.newBuilder().setStatus("Hello " + inputText + "!").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void controlClear(ControlClearRequest request, StreamObserver<ControlClearResponse> responseObserver) {
        Status status = silo.clear() ? Status.OK : Status.NOK; //change accordingly
        
        ControlClearResponse response = ControlClearResponse.newBuilder().setStatus(status).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void controlInit(ControlInitRequest request, StreamObserver<ControlInitResponse> responseObserver) {
        List<Observation> observations =  request.getObservationList();
        ArrayList<Registry> registries = new ArrayList<Registry>();

        for(Observation o : observations){
            Registry r = new Registry(o.getCamera(),
                o.getObservated().getType(),
                o.getObservated().getIdentifier(),
                o.getTime());
            registries.add(r);
        }

        silo.addRegistries(registries);

        ControlInitResponse response = ControlInitResponse.newBuilder().setResponseStatus(Status.OK).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {
        
        String identifier = request.getIdentity().getIdentifier();

        Registry mostRecentRegistry = silo.getMostRecentRegistry(identifier);

        Observable observable = Observable.newBuilder()
            .setType(mostRecentRegistry.getType())
            .setIdentifier(mostRecentRegistry.getIdentifier())
            .build();

        Observation observation = Observation.newBuilder()
            .setObservated(observable)
            .setTime(mostRecentRegistry.getTime())
            .setCamera(mostRecentRegistry.getCamera())
            .build();

        
        TrackResponse response = TrackResponse.newBuilder().setObservation(observation).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
        String type =  request.getIdentity().getType();
        String identifier = request.getIdentity().getIdentifier();

        TrackMatchResponse response = null;
        //TODO  find the most recent observations of each object found
        ArrayList<Observation> observations = null;
        int index = 0;

        for(Observation observation : observations){
            response = TrackMatchResponse.newBuilder().setObservations(index, observation).build();
            index++;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
        String type =  request.getIdentity().getType();
        String identifier = request.getIdentity().getIdentifier();

        TraceResponse response = null;
        //TODO  find the observations for the object sorted from most recent to oldest
        ArrayList<Observation> observations = null;
        int index = 0;

        for(Observation observation : observations){
            response = TraceResponse.newBuilder().setObservations(index, observation).build();
            index++;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}