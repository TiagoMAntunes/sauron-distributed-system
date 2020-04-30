package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.client.messages.CamInfoMessage;
import pt.tecnico.sauron.silo.client.messages.CamJoinMessage;
import pt.tecnico.sauron.silo.client.messages.ControlClearMessage;
import pt.tecnico.sauron.silo.client.messages.ControlInitMessage;
import pt.tecnico.sauron.silo.client.messages.ControlPingMessage;
import pt.tecnico.sauron.silo.client.messages.MessageStrategy;
import pt.tecnico.sauron.silo.client.messages.ReportMessage;
import pt.tecnico.sauron.silo.client.messages.TraceMessage;
import pt.tecnico.sauron.silo.client.messages.TrackMatchMessage;
import pt.tecnico.sauron.silo.client.messages.TrackMessage;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.tecnico.sauron.silo.grpc.Silo.VectorClock;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;

//For cache
import com.google.protobuf.Message;
import pt.tecnico.sauron.silo.client.messages.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SiloServerFrontend implements AutoCloseable {
    private static final String PATH = "/grpc/sauron/silo"; // TODO This is hard-coded, should it be?
    private final ZKNaming zkNaming;
    ArrayList<Integer> timestamp;
    private MessageStrategy requestManager;
    private Cache cache;

    public SiloServerFrontend(String host, String port) throws UnavailableException {
        this(host, port, "0");
    }

    public SiloServerFrontend(String host, String port, String instanceNumber) throws UnavailableException {
        this.cache = new Cache(50); //TODO change the max
        zkNaming = new ZKNaming(host, port);
        try {
            requestManager = new MessageStrategy(zkNaming, PATH, instanceNumber);
        } catch (ZKNamingException e) {
            throw new UnavailableException();
        }
        timestamp = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) timestamp.add(0);
    }

    public ControlPingResponse controlPing(ControlPingRequest r) throws ZKNamingException, UnavailableException {
        return (ControlPingResponse) requestManager.execute((new ControlPingMessage(r)));
    }

    public ControlClearResponse controlClear(ControlClearRequest r) throws ZKNamingException, UnavailableException {
        return (ControlClearResponse) requestManager.execute((new ControlClearMessage(r)));
    }

    public CamJoinResponse camJoin(CamJoinRequest r) throws ZKNamingException, UnavailableException {
        VectorClock vector = VectorClock.newBuilder().addAllUpdates(this.timestamp).build();
        CamJoinRequest req = CamJoinRequest.newBuilder().setCamera(r.getCamera())
                        .setPrev(vector).build();

        CamJoinResponse response = (CamJoinResponse) requestManager.execute((new CamJoinMessage(req)));

        this.timestamp = new ArrayList<>(response.getNew().getUpdatesList());
        
        return response;
    }

    public CamInfoResponse camInfo(CamInfoRequest r) throws ZKNamingException, UnavailableException {
        return (CamInfoResponse) requestManager.execute((new CamInfoMessage(r)));
    }

    public ControlInitResponse controlInit(ControlInitRequest r) throws ZKNamingException, UnavailableException {

        // Creates VectorClock from the timestamp
        // Create new request and sent it with the VectorClock
        VectorClock vector = VectorClock.newBuilder().addAllUpdates(this.timestamp).build();
        ControlInitRequest req = ControlInitRequest.newBuilder().addAllObservation(r.getObservationList())
                .setPrev(vector).build();

        ControlInitResponse res = (ControlInitResponse) requestManager.execute((new ControlInitMessage(req)));

        // Update timestamp
        this.timestamp = new ArrayList<>(res.getNew().getUpdatesList());

        return res;
    }

    public TrackResponse track(TrackRequest r) throws ZKNamingException, UnavailableException {
        //TODO test if I need to go to cache
        TrackMessage reqMessage = new TrackMessage(r);
        
        TrackResponse res = (TrackResponse) requestManager.execute(reqMessage);

        this.cache.insertReqRes(reqMessage, res);

        return res ;
        
    }

    public TrackMatchResponse trackMatch(TrackMatchRequest r) throws ZKNamingException, UnavailableException {
        TrackMatchMessage reqMessage = new TrackMatchMessage(r);
        TrackMatchResponse res =  (TrackMatchResponse) requestManager.execute(reqMessage);

        this.cache.insertReqRes(reqMessage, res);

        return res;
    }

    public TraceResponse trace(TraceRequest r) throws ZKNamingException, UnavailableException {
        TraceMessage reqMessage = new TraceMessage(r);

        TraceResponse res = (TraceResponse) requestManager.execute(reqMessage);
        this.cache.insertReqRes(reqMessage, res);

        return res;
    }

    public ReportResponse reports(ReportRequest r, CamJoinRequest jr) throws ZKNamingException, UnavailableException {
        // Creates VectorClock from the timestamp
        // Create new request and sent it with the VectorClock

        VectorClock vector = VectorClock.newBuilder().addAllUpdates(this.timestamp).build();
        ReportRequest req = ReportRequest.newBuilder().setPrev(vector).setCameraName(r.getCameraName())
                .addAllObservations(r.getObservationsList()).build();

                
        ReportResponse res = (ReportResponse) requestManager.execute(new ReportMessage(req, jr));
        
        // Update timestamp
        this.timestamp = new ArrayList<>(res.getNew().getUpdatesList());
        return res;
    }

    @Override
    public final void close() {
        // Nothing needs to be closed anymore
        requestManager.close();
    }

    private class Cache {
        //TODO do I need to use two
        private Map<Request, Message> cache = new HashMap<>();
        private Map<Integer, Request> orderCache = new HashMap<>();
        private int currentSize;
        private int maxSize;

        private Message testMessage;
        private boolean test;
        public Cache(int max) {
            this.maxSize = max;
            this.currentSize = 0;
        }

        public void insertReqRes(Request req, Message res) {
           
            if (!inCache(req)) {
                System.out.println("Not cache");
                cache.put(req, res);
                this.currentSize++;
                System.out.print(this);
            } else {
                System.out.println("Already in cache");
            }
            
        }

        public boolean inCache(Request req) {
            return this.cache.containsKey(req);
        }

        @Override
        public String toString() {
            String res = String.format("---- CACHE ----\nSize: %d;\nCache: %s;\n ---- ENDCACHE ----",this.currentSize,this.cache);
            return res;
        }

    }    
}