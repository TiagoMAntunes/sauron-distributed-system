package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.client.messages.CamInfoMessage;
import pt.tecnico.sauron.silo.client.messages.CamJoinMessage;
import pt.tecnico.sauron.silo.client.messages.ControlClearMessage;
import pt.tecnico.sauron.silo.client.messages.ControlInitMessage;
import pt.tecnico.sauron.silo.client.messages.ControlPingMessage;
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
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;

import java.lang.AutoCloseable;
import java.util.ArrayList;

public class SiloServerFrontend implements AutoCloseable {
    private static final String path = "/grpc/sauron/silo"; // TODO This is hard-coded, should it be?
    private final ZKNaming zkNaming;
    private final String instanceNumber;
    ArrayList<Integer> timestamp = new ArrayList<>();

    public SiloServerFrontend(String host, String port) {
        zkNaming = new ZKNaming(host, port);
        instanceNumber = "0"; // No instance specified
    }

    public SiloServerFrontend(String host, String port, String instanceNumber) {
        zkNaming = new ZKNaming(host, port);
        this.instanceNumber = instanceNumber;
    }

    public ControlPingResponse controlPing(ControlPingRequest r) throws ZKNamingException, UnavailableException {
        return (ControlPingResponse) (new ControlPingMessage(r)).execute(instanceNumber, zkNaming, path);
    }

    public ControlClearResponse controlClear(ControlClearRequest r) throws ZKNamingException, UnavailableException {
        return (ControlClearResponse) (new ControlClearMessage(r)).execute(instanceNumber, zkNaming, path);
    }

    public CamJoinResponse camJoin(CamJoinRequest r) throws ZKNamingException, UnavailableException {
        return (CamJoinResponse) (new CamJoinMessage(r)).execute(instanceNumber, zkNaming, path);        
    }

    public CamInfoResponse camInfo(CamInfoRequest r) throws ZKNamingException, UnavailableException {
        return (CamInfoResponse) (new CamInfoMessage(r)).execute(instanceNumber, zkNaming, path);
    }

    public ControlInitResponse controlInit(ControlInitRequest r) throws ZKNamingException, UnavailableException {

        // Creates VectorClock from the timestamp
        // Create new request and sent it with the VectorClock
        VectorClock vector = VectorClock.newBuilder().addAllUpdates(this.timestamp).build();
        ControlInitRequest req = ControlInitRequest.newBuilder().addAllObservation(r.getObservationList())
                .setPrev(vector).build(); // TODO Is it really necessary for init to register changes?

        
        ControlInitResponse res = (ControlInitResponse) (new ControlInitMessage(r)).execute(instanceNumber, zkNaming, path);
        
        // Update timestamp
        System.out.println("B4:" + this.timestamp);
        this.timestamp = new ArrayList<>(res.getNew().getUpdatesList());
        System.out.println("After:" + this.timestamp);

        return res;

    }

    public TrackResponse track(TrackRequest r) throws ZKNamingException, UnavailableException {
        return (TrackResponse) (new TrackMessage(r)).execute(instanceNumber, zkNaming, path);
    }

    public TrackMatchResponse trackMatch(TrackMatchRequest r) throws ZKNamingException, UnavailableException {
        return (TrackMatchResponse) (new TrackMatchMessage(r)).execute(instanceNumber, zkNaming, path);
    }

    public TraceResponse trace(TraceRequest r) throws ZKNamingException, UnavailableException {
        return (TraceResponse) (new TraceMessage(r)).execute(instanceNumber, zkNaming, path);
    }

    public ReportResponse reports(ReportRequest r, CamJoinRequest jr) throws ZKNamingException, UnavailableException {
        // Creates VectorClock from the timestamp
        // Create new request and sent it with the VectorClock
    
        VectorClock vector = VectorClock.newBuilder().addAllUpdates(this.timestamp).build();
        ReportRequest req = ReportRequest.newBuilder().setPrev(vector).setCameraName(r.getCameraName())
                .addAllObservations(r.getObservationsList()).build();

        ReportResponse res =  (ReportResponse) (new ReportMessage(req,jr)).execute(instanceNumber, zkNaming, path);

        // Update timestamp
        this.timestamp = new ArrayList<>(res.getNew().getUpdatesList());
        return res;
    }

    @Override
    public final void close() {
        // Nothing needs to be closed anymore
    }

}