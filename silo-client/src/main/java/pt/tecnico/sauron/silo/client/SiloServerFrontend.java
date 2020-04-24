package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
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
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;

import java.lang.AutoCloseable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SiloServerFrontend implements AutoCloseable {
    private static final String path = "/grpc/sauron/silo"; // TODO This is hard-coded, should it be?
    private final ZKNaming zkNaming;
    private final String instanceNumber;
    ArrayList<Integer> timestamp = new ArrayList<>();

    ManagedChannel channel;
    SauronGrpc.SauronBlockingStub stub;

    /**
     * This function generates a stub to communicate with a specific server
     * instance. If instanceNumber is not defined, it will communicate with a random
     * one.
     * 
     * @return A blocking stub to be used in that moment and then closed
     * @throws ZKNamingException
     */
    private SauronGrpc.SauronBlockingStub getStub() throws ZKNamingException {
        ZKRecord record;

        if (!instanceNumber.equals("0")) // No specified prefered instance
            record = zkNaming.lookup(path + "/" + instanceNumber);
        else {
            // Select one at random
            Collection<ZKRecord> available = zkNaming.listRecords(path);
            record = available.stream().skip((int) (available.size() * Math.random())).findFirst().get(); // this code
                                                                                                          // selects a
                                                                                                          // random
                                                                                                          // option
        }

        String target = record.getURI();
        System.out.println(target);
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        return SauronGrpc.newBlockingStub(channel);
    }

    // TODO In case it fails to connect try again
    public SiloServerFrontend(String host, String port) {
        zkNaming = new ZKNaming(host, port);
        instanceNumber = "0"; // No instance specified
    }

    public SiloServerFrontend(String host, String port, String instanceNumber) {
        zkNaming = new ZKNaming(host, port);
        this.instanceNumber = instanceNumber;
    }

    public ControlPingResponse controlPing(ControlPingRequest r) throws ZKNamingException {
        stub = getStub();
        try {
            ControlPingResponse response = stub.controlPing(r);
        } finally {
            channel.shutdown();
        }
        return response;
    }

    public ControlClearResponse controlClear(ControlClearRequest r) throws ZKNamingException {
        stub = getStub();
        try {
            return stub.controlClear(r);
        } finally {
            channel.shutdown();
        }
    }

    public CamJoinResponse camJoin(CamJoinRequest r) throws ZKNamingException {
        stub = getStub();
        try {
            return stub.camJoin(r);
        } finally {
            channel.shutdown();
        }
    }

    public CamInfoResponse camInfo(CamInfoRequest r) throws ZKNamingException {
        stub = getStub();
        try { 
            return stub.camInfo(r);
        } finally {
            channel.shutdown();
        }
    }

    public ControlInitResponse controlInit(ControlInitRequest r) throws ZKNamingException {

        // Creates VectorClock from the timestamp
        // Create new request and sent it with the VectorClock
        VectorClock vector = VectorClock.newBuilder().addAllUpdates(this.timestamp).build();
        ControlInitRequest req = ControlInitRequest.newBuilder().addAllObservation(r.getObservationList())
                .setPrev(vector).build(); //TODO Is it really necessary for init to register changes?
        
        stub = getStub();
        ControlInitResponse res;
        try {
            res = stub.controlInit(req);
        } finally {
            channel.shutdown();
        }

        // Update timestamp
        System.out.println("B4:" + this.timestamp);
        this.timestamp = new ArrayList<>(res.getNew().getUpdatesList());
        System.out.println("After:" + this.timestamp);

        return res;

    }

    public TrackResponse track(TrackRequest r) throws ZKNamingException {
        stub = getStub();
        TrackResponse response;
        try { 
            response = stub.track(r);
        } finally {
            channel.shutdown();
        }
        return response;
    }

    public TrackMatchResponse trackMatch(TrackMatchRequest r) throws ZKNamingException {
        stub = getStub();
        try {
            return stub.trackMatch(r);
        } finally {
            channel.shutdown();
        }
    }

    public TraceResponse trace(TraceRequest r) throws ZKNamingException {
        stub = getStub();
        try { 
            return stub.trace(r);
        } finally {
            channel.shutdown();
        }
    }

    public ReportResponse reports(ReportRequest r) throws ZKNamingException {
        // Creates VectorClock from the timestamp
        // Create new request and sent it with the VectorClock
        VectorClock vector = VectorClock.newBuilder().addAllUpdates(this.timestamp).build();
        ReportRequest req = ReportRequest.newBuilder().setPrev(vector).setCameraName(r.getCameraName())
                .addAllObservations(r.getObservationsList()).build();
        
        stub = getStub();
        ReportResponse res;
        try { 
            res = stub.report(req);
        } finally {
            channel.shutdown();
        }

        // Update timestamp
        System.out.println("B4:" + this.timestamp);
        this.timestamp = new ArrayList<>(res.getNew().getUpdatesList());
        System.out.println("After:" + this.timestamp);
        return res;
    }

    @Override
    public final void close() {
        channel.shutdown();
    }

}