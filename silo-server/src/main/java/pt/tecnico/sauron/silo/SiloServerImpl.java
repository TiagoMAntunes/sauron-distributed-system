package pt.tecnico.sauron.silo;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static io.grpc.Status.FAILED_PRECONDITION;
import static io.grpc.Status.INVALID_ARGUMENT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.type.LatLng;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.CameraDomain;
import pt.tecnico.sauron.silo.domain.Registry;
import pt.tecnico.sauron.silo.domain.RegistryFactory;
import pt.tecnico.sauron.silo.domain.SiloServer;
import pt.tecnico.sauron.silo.domain.VectorClockDomain;
import pt.tecnico.sauron.silo.domain.exceptions.IncorrectDataException;
import pt.tecnico.sauron.silo.domain.exceptions.InvalidTypeException;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.tecnico.sauron.silo.grpc.Silo.GetNonAppliedLogsRequest;
import pt.tecnico.sauron.silo.grpc.Silo.GetNonAppliedLogsResponse;
import pt.tecnico.sauron.silo.grpc.Silo.GetReplicaTimestampRequest;
import pt.tecnico.sauron.silo.grpc.Silo.GetReplicaTimestampResponse;
import pt.tecnico.sauron.silo.grpc.Silo.GossipRequest;
import pt.tecnico.sauron.silo.grpc.Silo.GossipResponse;
import pt.tecnico.sauron.silo.grpc.Silo.LogElement;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.tecnico.sauron.silo.grpc.Silo.VectorClock;
import pt.tecnico.sauron.silo.grpc.Silo.LogElement.ObservationLog;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;


public class SiloServerImpl extends SauronGrpc.SauronImplBase {

    private final SiloServer silo;
    private static final RegistryFactory registryFactory = new RegistryFactory();
    private VectorClockDomain replicaTS;  // VectorClockDomain is synchronized
    private final int replicaIndex;
    private TreeSet<LogLocalElement> log; // Requires external synchronization

    public SiloServerImpl (int nReplicas, int whichReplica) {
        this.silo =  new SiloServer(new VectorClockDomain(nReplicas));
        this.replicaTS = new VectorClockDomain(nReplicas);
        this.replicaIndex = whichReplica - 1;
        this.log = new TreeSet<>((LogLocalElement a, LogLocalElement b) -> {
            VectorClockDomain na = new VectorClockDomain(a.element().getTs().getUpdatesList());
            VectorClockDomain nb = new VectorClockDomain(b.element().getTs().getUpdatesList());
            if (na.sameAs(nb)) return 0;
            else if (na.isMoreRecent(nb)) return 1;
            return -1;
        });
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
            return;
        } else if (camName.length() < 3 || camName.length() > 15 ) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Camera name must be between 3 and 15 characters in length").asRuntimeException());
        } else if (!(camCoords.getLatitude() >= - 90 && camCoords.getLatitude() <= 90.0 && camCoords.getLongitude() >= -180 && camCoords.getLongitude() <= 180)) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Coordinates are invalid. Should be in degrees and latitude should be in range [-90.0, +90.0] and longitude in [-180.0, +180.0]").asRuntimeException());
        } else {
            this.replicaTS.incUpdate(replicaIndex);
            // Add cam join request 
            synchronized (this.log) {
                this.log.add(new LogLocalElement(LogElement
                                                    .newBuilder()
                                                    .setCamera(request.getCamera())
                                                    .setTs(VectorClock.newBuilder()
                                                    .addAllUpdates(
                                                        this.replicaTS.getList())
                                                        .build())
                                                    .setOrigin(replicaIndex)
                                                    .build())); 
            }
            response = CamJoinResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();   

            applyAvailableUpdates(); //New modification so run
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

            //TODO Confirmed that observation has not been already reported

            //Validate data 
            for (Observation o : observations) {
                CameraDomain cam = silo.getCamera(camName);
                String type = o.getObservated().getType();
                String id = o.getObservated().getIdentifier();
                Date time = new Date();
                try {
                    registryFactory.build(cam, type, id, time); 
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
            }

            //Creates new modification timestamp based on prev and increment on replicas timeline
            VectorClockDomain modTS;
            this.replicaTS.incUpdate(this.replicaIndex);
            if (prevVec.getUpdatesList().size() == 0) {
                modTS = new VectorClockDomain(this.replicaTS.getList().size());
            } else {
                modTS = new VectorClockDomain(prevVec.getUpdatesList());
            }
            modTS.setUpdate(this.replicaIndex, this.replicaTS.getUpdate(this.replicaIndex));

            //Add observations to update log
            List<Observation> new_obs = observations.stream()
                            .map(obs -> Observation.newBuilder()
                                        .setCamera(cameraFromDomain(silo.getCamera(camName)))
                                        .setObservated(obs.getObservated())
                                        .setTime(obs.getTime())
                                        .build())
                            .collect(Collectors.toList());

            //Add request to log
            LogLocalElement el = new LogLocalElement(LogElement.newBuilder()
                                                            .setObservation(
                                                                ObservationLog
                                                                    .newBuilder()
                                                                    .addAllObservation(new_obs)    
                                                                    .build()
                                                            )
                                                            .setTs(VectorClock.newBuilder()
                                                            .addAllUpdates(
                                                                modTS.getList())
                                                                .build())
                                                            .setOrigin(replicaIndex)
                                                            .build());
            synchronized(this.log) {
                this.log.add(el);
            }
            //Send modification ts as response to update client
            VectorClock newVectorClock = VectorClock.newBuilder().addAllUpdates(modTS.getList()).build();
            response = ReportResponse.newBuilder().setNew(newVectorClock).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            //Apply new changes since we got a modification
            applyAvailableUpdates();
        }
    }

    // Verify if server is OK
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

        //Create required cameras
        for (Observation o : observations) {
            //Create cameras if needed
            if (!silo.cameraExists(o.getCamera().getName())) {
                silo.addCamera(new CameraDomain(o.getCamera().getName(), o.getCamera().getCoords().getLatitude(), o.getCamera().getCoords().getLongitude()), replicaIndex);
                this.replicaTS.incUpdate(replicaIndex); // New modification (camera)
            }
            // Add cam join request 
            synchronized(this.log) {
                this.log.add(new LogLocalElement(LogElement
                                                        .newBuilder()
                                                        .setCamera(o.getCamera())
                                                        .setTs(VectorClock.newBuilder()
                                                        .addAllUpdates(
                                                            this.replicaTS.getList())
                                                            .build())
                                                        .setOrigin(replicaIndex)
                                                        .build())); 
            }
            this.replicaTS.incUpdate(replicaIndex); // Increment replica clock
        
        }

        //Creates new modification timestamp based on prev and increment on replicas timeline
        VectorClockDomain modTS;
        
        if (prevVec.getUpdatesList().size() == 0) {
            modTS = new VectorClockDomain(this.replicaTS.getList().size());
        } else {
            modTS = new VectorClockDomain(prevVec.getUpdatesList());
        }
        modTS.setUpdate(this.replicaIndex, this.replicaTS.getUpdate(this.replicaIndex));

        //Add observations to update log
        List<Observation> new_obs = observations.stream()
                        .map(obs -> Observation.newBuilder()
                                    .setCamera(cameraFromDomain(silo.getCamera(obs.getCamera().getName())))
                                    .setObservated(obs.getObservated())
                                    .setTime(obs.getTime())
                                    .build())
                        .collect(Collectors.toList());

       //Add request to log
       LogLocalElement el = new LogLocalElement(LogElement.newBuilder()
                                                .setObservation(
                                                    ObservationLog
                                                        .newBuilder()
                                                        .addAllObservation(new_obs)    
                                                        .build()
                                                )
                                                .setTs(VectorClock.newBuilder()
                                                    .addAllUpdates(
                                                        modTS.getList())
                                                        .build())
                                                    .setOrigin(replicaIndex)
                                                .build());
        synchronized(this.log) {
            this.log.add(el);
        }
        this.replicaTS.incUpdate(replicaIndex); // Increment replica clock

        //Apply new changes since we got a modification
        applyAvailableUpdates();
        
        //Send modification ts as response to update client
        VectorClock newVectorClock = VectorClock.newBuilder().addAllUpdates(modTS.getList()).build();
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
        List<LogElement> logs = req.getUpdatesList();
        for (LogElement o : logs) { //For each modification
            //Check if exists and if not, adds to log
            synchronized(this.log) {
                if (!log.contains(new LogLocalElement(o)))  { //Log does not exist yet
                    log.add(new LogLocalElement(o));
                    replicaTS.incUpdate(o.getOrigin()); //Number of received updates
                } 
            }
        }

        //Send gossip OK message
        GossipResponse response = GossipResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        applyAvailableUpdates();
    }

    private void applyAvailableUpdates() {
        //Apply available updates
        synchronized (this.log) { //External lock to avoid invalid data TODO Is this really necessary?
            //Gets current value of silo
            VectorClockDomain value = silo.getClock();

            for (LogLocalElement l : log) {
                //Get prev from client request
                LogElement e = l.element();
                VectorClockDomain prev = new VectorClockDomain(e.getTs().getUpdatesList());
                if (!l.isApplied() && value.isMoreRecent(prev, e.getOrigin())) { //Can execute update
                    
                    //Decide message type
                    switch(e.getContentCase()) {
                        case OBSERVATION:
                            //Create the registry and insert it into silo
                            List<Registry> registries = registriesFromObservations(e.getObservation().getObservationList());
                            silo.addRegistries(registries, e.getOrigin());
                            break;
                        case CAMERA:
                            //Register camera
                            silo.addCamera(cameraDomainFromCamera(e.getCamera()), e.getOrigin());
                            break;
                        case CONTENT_NOT_SET:
                            System.out.println("ERROR: LogElement not set found");
                            break;
                    }

                    l.applied();
                }
                //if (!replicaTS.isMoreRecent(silo.getClock()))    System.out.println(e);
            }
            System.out.println("-- AFTER UPDATES -- ");
            System.out.println("Server ts: " + replicaTS);
            System.out.println("Silo ts: " + silo.getClock());
        }
    }

    public void doGossip(ZKNaming zkNaming, String path) {
        System.out.println("In replica " + replicaIndex);

        try {
            Collection<ZKRecord> available = zkNaming.listRecords(path);
            
            //For every replica
            for (ZKRecord record : available) {
                String recordPath = record.getPath();
                int replicaID = Integer.parseInt(recordPath.substring(recordPath.length()-1)) - 1; //Replica ids are -1

                if (replicaID != replicaIndex) { //Avoid sending to itself
                    //Build channel
                    String target = record.getURI();
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                    SauronGrpc.SauronBlockingStub stub = SauronGrpc.newBlockingStub(channel);

                    //Ask for destiny's TS to check which entries should be sent
                    VectorClockDomain incomingReplicaTS = getReplicaTS(stub);
                    ArrayList<ArrayList<Integer>> toSend = this.replicaTS.moreRecentIndexes(incomingReplicaTS);
                    ArrayList<Integer> indexesToSend = toSend.get(0);
                    ArrayList<Integer> replicaValues = toSend.get(1);

                    //Build request
                    VectorClock ts = VectorClock.newBuilder().addAllUpdates(getClock()).build(); 
                    GossipRequest req;
                    synchronized(this.log) {

                        //Filter log to get only the updates that refer to the proper indexes
                        req = GossipRequest.newBuilder().setTs(ts).setIncomingReplicaIndex(replicaIndex).addAllUpdates(this.log.stream()
                            .filter(el -> indexesToSend.contains(el.element().getOrigin())) // Filter for indexes of interest
                            .filter(el -> el.element().getTs().getUpdatesList().get(el.element().getOrigin()) > // Filter for logs whose timestamp at the index of interest
                                    replicaValues.get(indexesToSend.indexOf(el.element().getOrigin()))) // is bigger than the replicas timestamp at that index
                            .map(el -> el.element()).collect(Collectors.toList())).build(); 
                        
                    }
                    //Send request and handle answer
                    try {
                        //Response is empty == ok
                        stub.gossip(req);
                    } catch (StatusRuntimeException e) {

                        //If host unreachable just advance. If any other error, throw as it is unexpected
                        if (e.getStatus().getCode().equals(Status.UNAVAILABLE.getCode())) 
                            System.out.println("Target " + target + " is unreachable.");
                        else throw e;
                    
                    } finally {
                        //Shutdown channel and proceed
                        channel.shutdown();
                    
                    } // try

                } // if

            } // for
        } catch (ZKNamingException e) {
            System.out.println("Problem with gossip " + e.getMessage());
        }
    }

    public VectorClockDomain getReplicaTS(SauronGrpc.SauronBlockingStub stub) {
        GetReplicaTimestampRequest req = GetReplicaTimestampRequest.newBuilder().build();
        GetReplicaTimestampResponse res = stub.getReplicaTimestamp(req);
        VectorClock replicaTS = res.getCurrentTS();
        return new VectorClockDomain(replicaTS.getUpdatesList());
    }

    @Override
    public void getReplicaTimestamp(GetReplicaTimestampRequest req,StreamObserver<GetReplicaTimestampResponse> responseObserver ) {
        VectorClock currentTS = VectorClock.newBuilder().addAllUpdates(this.replicaTS.getList()).build();
        GetReplicaTimestampResponse response = GetReplicaTimestampResponse.newBuilder().setCurrentTS(currentTS).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    /**
     * This service is only for debugging
     */
    @Override
    public void getNonAppliedLogs(GetNonAppliedLogsRequest req, StreamObserver<GetNonAppliedLogsResponse> responseObserver) {
        GetNonAppliedLogsResponse response;
        synchronized (this.log) {
            response = GetNonAppliedLogsResponse.newBuilder()
                                                .addAllElements(
                                                    this.log.stream().filter(log -> !log.isApplied()).map(el -> el.element()).collect(Collectors.toList())
                                                )
                                                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void handleShare(List<Integer> versions) {
        System.out.println("Previous version: " + this.replicaTS);
        this.replicaTS.merge(new VectorClockDomain(versions));
        System.out.println("New version: " + this.replicaTS);
	}

	public Iterable<Integer> getClock() {
		return this.replicaTS.getList();
    }
    
    public List<Registry> registriesFromObservations(List<Observation> obs) {
        return obs.stream().map(o -> registryFromObservation(o)).collect(Collectors.toList());
    }

    /**
     * This function is to be used for log updates only
     * @param o
     * @return
     */
    public Registry registryFromObservation(Observation o) {
        CameraDomain cam = silo.getCamera(o.getCamera().getName());
        String type = o.getObservated().getType();
        String id = o.getObservated().getIdentifier();
        Date time = new Date(getTime(o));
        try {
            return registryFactory.build(cam, type, id, time);
        } catch (InvalidTypeException | IncorrectDataException e) {
            System.out.printf("ERROR: %s > %s%n", e.getClass(), e.getMessage());
            return null;
        }
    }

    public CameraDomain cameraDomainFromCamera(Camera c) {
        return new CameraDomain(c.getName(), c.getCoords().getLatitude(), c.getCoords().getLongitude());
    }

    /**
     * Gets an observation's time in miliseconds
     */
    private long getTime(Observation o) {
        return (o.getTime().getSeconds()*1000 + o.getTime().getNanos()/1000000);
    }


    /**
     * Creates a camera from domain camera name
     */
    private Camera cameraFromDomain(CameraDomain camera) {
        LatLng coords = LatLng.newBuilder().setLatitude(camera.getLatitude()).setLongitude(camera.getLongitude()).build();
        return Camera.newBuilder().setName(camera.getName()).setCoords(coords).build();
    }

    private static class LogLocalElement {
        private final LogElement element;
        private boolean applied;

        public LogLocalElement(LogElement element) {
            this.element = element;
            this.applied = false;
        }

        public void applied() {
            this.applied = true;
        }

        public boolean isApplied() { return applied; }

        public LogElement element() { return this.element; }
    }

}