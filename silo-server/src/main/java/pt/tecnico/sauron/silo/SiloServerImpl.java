package pt.tecnico.sauron.silo;

import pt.tecnico.sauron.silo.domain.CameraDomain;
import pt.tecnico.sauron.silo.domain.exceptions.IncorrectDataException;
import pt.tecnico.sauron.silo.domain.exceptions.InvalidTypeException;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.tecnico.sauron.silo.grpc.Silo.GossipRequest;
import pt.tecnico.sauron.silo.grpc.Silo.GossipResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.tecnico.sauron.silo.grpc.Silo.VectorClock;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.domain.SiloServer;
import pt.tecnico.sauron.silo.domain.VectorClockDomain;
import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.domain.Registry;
import pt.tecnico.sauron.silo.domain.RegistryFactory;

import static io.grpc.Status.FAILED_PRECONDITION;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.ALREADY_EXISTS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import static com.google.protobuf.util.Timestamps.fromMillis;
import com.google.type.LatLng;


public class SiloServerImpl extends SauronGrpc.SauronImplBase {

    private final SiloServer silo;
    private static final RegistryFactory registryFactory = new RegistryFactory();

    public SiloServerImpl (int nReplicas, int whichReplica) {
        this.silo =  new SiloServer(nReplicas,whichReplica);
    }
    //Add camera to server
    @Override
    public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
        String camName = request.getCamera().getName();
        LatLng camCoords = request.getCamera().getCoords();

        //Verify that request has been properly constructed
        CamJoinResponse response;
        if (camName == null || camName.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Camera must not be null or empty").asRuntimeException());
        } else if (  silo.cameraExists(camName)) {
            //Everything is ok, just accept and go on
            responseObserver.onNext(CamJoinResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } else if (camName.length() < 3 || camName.length() > 15 ) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Camera name must be between 3 and 15 characters in length").asRuntimeException());
        } else if (!(camCoords.getLatitude() >= - 90 && camCoords.getLatitude() <= 90.0 && camCoords.getLongitude() >= -180 && camCoords.getLongitude() <= 180)) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Coordinates are invalid. Should be in degrees and latitude should be in range [-90.0, +90.0] and longitude in [-180.0, +180.0]").asRuntimeException());
        } else {
            //Create server side representation of camera and add it
            CameraDomain newCam = new CameraDomain(camName, camCoords.getLatitude(), camCoords.getLongitude());
            silo.addCamera(newCam);
            response = CamJoinResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();   
        }
    }

    //Return camera coords based on name
    @Override
    public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {
        String camName = request.getName();
        CamInfoResponse response = CamInfoResponse.newBuilder().build();

        //Verify that request has been properly constructed
        if (camName == null || camName.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Camera must not be null or empty").asRuntimeException());
        } else if (!silo.cameraExists(camName)) {
            responseObserver.onError(FAILED_PRECONDITION.withDescription("Camera must already exist").asRuntimeException());
        }  else {
            //Build coordinates from the camera server side representation
            CameraDomain camDom = silo.getCamera(camName);
            LatLng coords = LatLng.newBuilder().setLatitude(camDom.getLatitude()).setLongitude(camDom.getLongitude()).build();
            Camera cam = Camera.newBuilder().setCoords(coords).setName(camDom.getName()).build();
            response = CamInfoResponse.newBuilder().setCamera(cam).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

    }

    //Send observations
    @Override
    public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {

        String camName = request.getCameraName();
        List<Observation> observations = request.getObservationsList();
        ReportResponse response;
        VectorClock prevVec = request.getPrev(); //Get timestamp from request
        //Verify that request has been properly constructed
        if (camName == null || camName.equals("") ) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty or null").asRuntimeException());
        } else if ( !silo.cameraExists(camName)) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Camera must already exist").asRuntimeException());
        } else if (observations == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observations List must not be null").asRuntimeException());
        } else if (observations.isEmpty()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observations List must not be empty").asRuntimeException());
        } else {
            //Transform into registries
            ArrayList<Registry> list = new ArrayList<>();
            for (Observation o : observations) {
                CameraDomain cam = silo.getCamera(camName);
                String type = o.getObservated().getType();
                String id = o.getObservated().getIdentifier();
                Date time = new Date();
                Registry r = null;
                try {
                    r = registryFactory.build(cam, type, id, time); 
                } catch (InvalidTypeException e) {
                    responseObserver.onError(INVALID_ARGUMENT.withDescription("The type " + e.getType() + " is not available in the current system").asRuntimeException());
                    return;
                } catch (IncorrectDataException e) {
                    responseObserver.onError(INVALID_ARGUMENT.withDescription("The identifier " + e.getId() + " does not match type's " + e.getType() + " specification").asRuntimeException());
                    return;
                } catch (Exception e) {
                    System.out.println("Unhandled exception caught.");
                    e.printStackTrace();
                    System.out.println("Rethrowing...");
                    throw e;
                }
                list.add(r);
            }
            //Save the registries

            //Gets vector from SiloServer and sends it in the response
            VectorClockDomain newVec = silo.addRegistries(list, new VectorClockDomain(prevVec.getUpdatesList()));
            
            VectorClock newVectorClock = VectorClock.newBuilder().addAllUpdates(newVec.getList()).build();

            response = ReportResponse.newBuilder().setNew(newVectorClock).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    //Verify if server is OK
    @Override
    public void controlPing(ControlPingRequest request, StreamObserver<ControlPingResponse> responseObserver) {
        String inputText = request.getInputText();

        //Verify that request has been properly constructed        
        if (inputText == null || inputText.isBlank()) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty").asRuntimeException());
        } else {
            ControlPingResponse response = ControlPingResponse.newBuilder().setStatus("Hello " + inputText + "!").build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    //Clears testing
    @Override
    public void controlClear(ControlClearRequest request, StreamObserver<ControlClearResponse> responseObserver) {
        silo.clear();
        ControlClearResponse response = ControlClearResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    //Initializes conditions for testing
    @Override
    public void controlInit(ControlInitRequest request, StreamObserver<ControlInitResponse> responseObserver) {
        List<Observation> observations =  request.getObservationList();
        VectorClock prevVec = request.getPrev(); //Get timestamp from request
        ArrayList<Registry> list = new ArrayList<>();
            //For every observation that is provided        
            for (Observation o : observations) {

                //Create camera if there's none
                if(!silo.cameraExists(o.getCamera().getName())) {
                    CameraDomain newCam = new CameraDomain(o.getCamera().getName(), o.getCamera().getCoords().getLatitude(), o.getCamera().getCoords().getLongitude());
                    silo.addCamera(newCam);
                }
                //Create registries
                CameraDomain cam = silo.getCamera(o.getCamera().getName());
                String type = o.getObservated().getType();
                String id = o.getObservated().getIdentifier();
                Date time = new Date(o.getTime().getSeconds() * 1000 + o.getTime().getNanos() / 1000000);
                Registry r = null;
                try {
                    r = registryFactory.build(cam, type, id, time); 
                } catch (InvalidTypeException e) {
                    responseObserver.onError(INVALID_ARGUMENT.withDescription("The type " + e.getType() + " is not available in the current system").asRuntimeException());
                    return;
                } catch (IncorrectDataException e) {
                    responseObserver.onError(INVALID_ARGUMENT.withDescription("The identifier " + e.getId() + " does not match type's " + e.getType() + " specification").asRuntimeException());
                    return;
                } 
                list.add(r);
            }
        
        //Gets vector from SiloServer and sends it in the response
        VectorClockDomain newVec = silo.addRegistries(list, new VectorClockDomain(prevVec.getUpdatesList()));
        
        VectorClock newVectorClock = VectorClock.newBuilder().addAllUpdates(newVec.getList()).build();
        
        ControlInitResponse response = ControlInitResponse.newBuilder().setNew(newVectorClock).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    //Returns latest observation of an entity
    @Override
    public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {

        String identifier = request.getIdentity().getIdentifier();
        String type = request.getIdentity().getType();
        Registry mostRecentRegistry;
        Observation observation = null;
        TrackResponse response;

        //Verify that request has been properly constructed
        if (identifier == null || identifier.equals("") || type == null || type.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty or null").asRuntimeException());
        } else if (request.getIdentity() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observation must not be null").asRuntimeException());
        } else if(silo.noRegistries()){
            responseObserver.onError(FAILED_PRECONDITION.withDescription("Server has no data").asRuntimeException());
        } else {
            //Gets most recent entrance in registry
            mostRecentRegistry = silo.getMostRecentRegistry(type, identifier);
            if (mostRecentRegistry == null) {
                responseObserver.onError(FAILED_PRECONDITION.withDescription("The object has no reports").asRuntimeException());
                return;
            }

            //Creates observable entity from registry
            Observable observable = Observable.newBuilder()
                    .setType(mostRecentRegistry.getType())
                    .setIdentifier(mostRecentRegistry.getIdentifier())
                    .build();

            LatLng coords = LatLng.newBuilder().setLatitude(mostRecentRegistry.getCamera().getLatitude()).setLongitude(mostRecentRegistry.getCamera().getLongitude()).build();

            //Build observation from Registry and Observable
            observation = Observation.newBuilder()
                    .setObservated(observable)
                    .setTime(fromMillis(mostRecentRegistry.getTime().getTime()))
                    .setCamera(Camera.newBuilder().setCoords(coords).setName(mostRecentRegistry.getCamera().getName()).build())
                    .build();
            response = TrackResponse.newBuilder()
                    .setObservation(observation)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    //Returns list of observations that match the partial identifier provided
    @Override
    public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
        String partialIdentifier = request.getIdentity().getIdentifier();
        String type = request.getIdentity().getType();
        TrackMatchResponse response;
        ArrayList<Registry> registries;
        ArrayList<Observation> observations = new ArrayList<>();

        //Verify that request has been properly constructed
        if (partialIdentifier == null || partialIdentifier.equals("") || type == null || type.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty or null").asRuntimeException());
        } else if (request.getIdentity() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observation must not be null").asRuntimeException());
        } else {
            registries = silo.getAllRecentRegistries(type, partialIdentifier);

            //Builds observations from the registries
            for(Registry r : registries){
                Observable observable = Observable.newBuilder()
                        .setType(r.getType())
                        .setIdentifier(r.getIdentifier())
                        .build();

                LatLng coords = LatLng.newBuilder().setLatitude(r.getCamera().getLatitude()).setLongitude(r.getCamera().getLongitude()).build();

                Observation observation = Observation.newBuilder()
                        .setObservated(observable)
                        .setTime(fromMillis(r.getTime().getTime()))
                        .setCamera(Camera.newBuilder().setCoords(coords).setName(r.getCamera().getName()).build())
                        .build();

                observations.add(observation);
            }
            response = TrackMatchResponse.newBuilder()
                    .addAllObservations(observations)
                    .build();
            responseObserver.onNext(response);
                responseObserver.onCompleted();
        }
    }


    //Returns a list of observations refering to the identity
    @Override
    public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
        String type =  request.getIdentity().getType();
        String identifier = request.getIdentity().getIdentifier();

        TraceResponse response;
        List<Registry> registries;
        ArrayList<Observation> observations = new ArrayList<>();

        //Verify that request has been properly constructed
        if (identifier == null || identifier.equals("") || type == null || type.equals("")) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty or null").asRuntimeException());
        } else if (request.getIdentity() == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Observable must not be null").asRuntimeException());
        } else if(silo.noRegistries() || !silo.registryExists(type, identifier)){
            response = TraceResponse.newBuilder()
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        
        else {
            registries = silo.getRegistries(type, identifier);
            
            // Builds observations to return from the registries
            for (int i = registries.size() - 1; i >= 0; i--) {
                Registry r = registries.get(i);
                Observable observable = Observable.newBuilder()
                        .setType(r.getType())
                        .setIdentifier(r.getIdentifier())
                        .build();

                LatLng coords = LatLng.newBuilder().setLatitude(r.getCamera().getLatitude()).setLongitude(r.getCamera().getLongitude()).build();

                Observation observation = Observation.newBuilder()
                        .setObservated(observable)
                        .setTime(fromMillis(r.getTime().getTime()))
                        .setCamera(Camera.newBuilder().setCoords(coords).setName(r.getCamera().getName()).build())
                        .build();

                observations.add(observation);
            }

            response = TraceResponse.newBuilder()
                    .addAllObservations(observations)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void gossip(GossipRequest req, StreamObserver<GossipResponse> responseObserver) {
        System.out.println("Received request"); //TODO implement gossip
        GossipResponse response = GossipResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
	public void doGossip(int whichReplica, ZKNaming zkNaming, String path) {
        System.out.println("In replica " + whichReplica);

        //TODO send updates
        //TODO clear updates after sending
        //TODO confirm / fix error at beginning because it cant find the other server then one of them stop  sending even when it connects
                    
        try {
            Collection<ZKRecord> available = zkNaming.listRecords(path);
            
            //For every replica that is not this one
            available.forEach(record -> {
                String recPath = record.getPath();
                int recID = Integer.parseInt(recPath.substring(recPath.length()-1));

                if (recID != whichReplica) {
                    String target = record.getURI();
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                    SauronGrpc.SauronBlockingStub stub = SauronGrpc.newBlockingStub(channel);
                
                    GossipRequest req = GossipRequest.newBuilder().build(); 
                    try {
                        GossipResponse res = stub.gossip(req);
                        System.out.println("Received response");
                    } catch(StatusRuntimeException e) {

                        //If host unreachable just advance. If any other error, throw
                        if (e.getStatus().getCode().equals(Status.UNAVAILABLE.getCode()))
                            System.out.println("Target " + target + " is unreachable.");
                        else throw e;
                    } finally {
                        channel.shutdown();
                    } 
                }
            });
        } catch (ZKNamingException e) {
            System.out.println("Problem with gossip " + e.getMessage());
        }
	}

}