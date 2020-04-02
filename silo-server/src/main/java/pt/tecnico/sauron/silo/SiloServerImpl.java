package pt.tecnico.sauron.silo;

import pt.tecnico.sauron.silo.domain.CameraDomain;
import pt.tecnico.sauron.silo.domain.exceptions.IncorrectDataException;
import pt.tecnico.sauron.silo.domain.exceptions.InvalidTypeException;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
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
import pt.tecnico.sauron.silo.domain.RegistryFactory;

import static io.grpc.Status.FAILED_PRECONDITION;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.ALREADY_EXISTS;

import java.util.ArrayList;
import java.util.List;

import io.grpc.stub.StreamObserver;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;
import com.google.type.LatLng;

import com.google.protobuf.Timestamp;


public class SiloServerImpl extends SauronGrpc.SauronImplBase {

    private final SiloServer silo = new SiloServer();
    private static final RegistryFactory registryFactory = new RegistryFactory();

    @Override
    public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
        String camName = request.getCamera().getName();
        LatLng camCoords = request.getCamera().getCoords();


        CamJoinResponse response;
        if (camName == null || camName.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Camera must not be null or empty").asRuntimeException());
        } else if (  silo.cameraExists(camName)) {
            responseObserver.onError(ALREADY_EXISTS.withDescription("Camera already exists").asRuntimeException());
        } else if (camName.length() < 3 || camName.length() > 15 ) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Camera name must be between 3 and 15 characters in length").asRuntimeException());
        } else if (camCoords.getLatitude() == 0.0 || camCoords.getLongitude() == 0.0) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Coordinates of camera must both be above '0.0'").asRuntimeException());
        } else {
            CameraDomain newCam = new CameraDomain(camName, camCoords);
            silo.addCamera(newCam);
            
        }
        response = CamJoinResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {
        String camName = request.getName();
        CamInfoResponse response = CamInfoResponse.newBuilder().build();
        if (camName == null || camName.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Camera must not be null or empty").asRuntimeException());
        } else if (!silo.cameraExists(camName)) {
            responseObserver.onError(FAILED_PRECONDITION.withDescription("Camera must already exist").asRuntimeException());
        }  else {
            CameraDomain camDom = silo.getCamera(camName);
            Camera cam = Camera.newBuilder().setCoords(camDom.getCoords()).setName(camDom.getName()).build();
            response = CamInfoResponse.newBuilder().setCamera(cam).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {

        String camName = request.getCameraName();
        List<Observation> observations = request.getObservationsList();

        ReportResponse response;
        if (camName == null || camName.equals("") ) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty or null!").asRuntimeException());
        } else if ( !silo.cameraExists(camName)) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Camera must already exist!").asRuntimeException());
        } else if (observations == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observations List must not be null!").asRuntimeException());
        } else if (observations.isEmpty()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observations List must not be empty!").asRuntimeException());
        } else {
            //Transform into registries

            ArrayList<Registry> list = new ArrayList<>();
            for (Observation o : observations) {
                Camera cam = o.getCamera();
                String type = o.getObservated().getType().toLowerCase();
                String id = o.getObservated().getIdentifier().toLowerCase();
                Timestamp time = fromMillis(currentTimeMillis());
                Registry r = null;
                try {
                    r = registryFactory.build(cam, type, id, time); 
                } catch (InvalidTypeException e) {
                    responseObserver.onError(INVALID_ARGUMENT.withDescription("The type " + e.getType() + " is not available in the current system.").asRuntimeException());
                } catch (IncorrectDataException e) {
                    responseObserver.onError(INVALID_ARGUMENT.withDescription("The identifier " + e.getId() + " does not match type's " + e.getType() + " specification").asRuntimeException());
                } catch (Exception e) {
                    System.out.println("Unhandled exception caught.");
                    e.printStackTrace();
                    System.out.println("Rethrowing...");
                    throw e;
                }
                list.add(r);
            }
            silo.addRegistries(list);

        }

        response = ReportResponse.newBuilder().build();
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
        String type = request.getIdentity().getType();
        Registry mostRecentRegistry;
        Observation observation = null;
        TrackResponse response;

        if (identifier == null || identifier.equals("") || type == null || type.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty or null!").asRuntimeException());
        } else if (request.getIdentity() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observation must not be null!").asRuntimeException());
        } else if(silo.noRegistries()){
            responseObserver.onError(FAILED_PRECONDITION.withDescription("Server has no data!").asRuntimeException());
        } else if (!silo.registryExists(identifier)) {
            responseObserver.onError(FAILED_PRECONDITION.withDescription("Identifier must already exist!").asRuntimeException());
        } else {
            mostRecentRegistry = silo.getMostRecentRegistry(identifier);
            Observable observable = Observable.newBuilder()
                    .setType(mostRecentRegistry.getType())
                    .setIdentifier(mostRecentRegistry.getIdentifier())
                    .build();

            observation = Observation.newBuilder()
                    .setObservated(observable)
                    .setTime(mostRecentRegistry.getTime())
                    .setCamera(mostRecentRegistry.getCamera())
                    .build();
            response = TrackResponse.newBuilder()
                    .setObservation(observation)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
        String partialIdentifier = request.getIdentity().getIdentifier();
        String type = request.getIdentity().getType();
        Registry mostRecentRegistry;
        TrackMatchResponse response;
        ArrayList<Registry> registries;
        ArrayList<Observation> observations = new ArrayList<>();

        if (partialIdentifier == null || partialIdentifier.equals("") || type == null || type.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty or null!").asRuntimeException());
        } else if (request.getIdentity() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observation must not be null!").asRuntimeException());
        } else {
            registries = silo.getAllRecentRegistries(partialIdentifier);

            if (registries.size() > 0) {

                for(Registry r : registries){
                    Observable observable = Observable.newBuilder()
                            .setType(r.getType())
                            .setIdentifier(r.getIdentifier())
                            .build();

                    Observation observation = Observation.newBuilder()
                            .setObservated(observable)
                            .setTime(r.getTime())
                            .setCamera(r.getCamera())
                            .build();

                    observations.add(observation);
                }
                response = TrackMatchResponse.newBuilder()
                        .addAllObservations(observations)
                        .build();
            } else
                //could not find registry
                response = TrackMatchResponse.newBuilder()
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }
    }

    @Override
    public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
        String type =  request.getIdentity().getType();
        String identifier = request.getIdentity().getIdentifier();

        TraceResponse response;
        ArrayList<Registry> registries;
        ArrayList<Observation> observations = new ArrayList<>();

        if (identifier == null || identifier.equals("") || type == null || type.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty or null!").asRuntimeException());
        } else if (request.getIdentity() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observable must not be null!").asRuntimeException());
        } else if(silo.noRegistries() || !silo.registryExists(identifier)){
            response = TraceResponse.newBuilder()
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        else {
            registries = silo.getSortedRegistries(identifier);
            if (registries.size() > 0) {
                for (Registry r : registries) {
                    Observable observable = Observable.newBuilder()
                            .setType(r.getType())
                            .setIdentifier(r.getIdentifier())
                            .build();

                    Observation observation = Observation.newBuilder()
                            .setObservated(observable)
                            .setTime(r.getTime())
                            .setCamera(r.getCamera())
                            .build();

                    observations.add(observation);
                }

                response = TraceResponse.newBuilder()
                        .addAllObservations(observations)
                        .build();

            }
            else {
                //could not find registry
                response = TraceResponse.newBuilder()
                        .build();
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

}