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
import java.util.Collection;

public class SiloServerFrontend implements AutoCloseable {
    private static final String path = "/grpc/sauron/silo"; //This is hard-coded, should it be?
    private final String target;
    final ManagedChannel channel;
    SauronGrpc.SauronBlockingStub stub;

    public SiloServerFrontend(String host, String port) throws ZKNamingException {
        ZKNaming zkNaming = new ZKNaming(host, port);
        Collection<ZKRecord> available = zkNaming.listRecords(path);
        
        ZKRecord record = available.stream().skip((int) (available.size() * Math.random())).findFirst().get(); //this code selects a random option
        target = record.getURI();
        
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = SauronGrpc.newBlockingStub(channel);
    }

    public SiloServerFrontend(String host, String port, String instanceNumber) throws ZKNamingException {
        ZKNaming zkNaming = new ZKNaming(host, port);
        ZKRecord record = zkNaming.lookup(path + "/" + instanceNumber);
        target = record.getURI();

        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = SauronGrpc.newBlockingStub(channel);
    }

    public ControlPingResponse controlPing(ControlPingRequest r) {
        return stub.controlPing(r);
    }

    public ControlClearResponse controlClear(ControlClearRequest r) {
        return stub.controlClear(r);
    }
    
    public CamJoinResponse camJoin(CamJoinRequest r) {
		    return stub.camJoin(r);
    }

    public CamInfoResponse camInfo(CamInfoRequest r) {
		return stub.camInfo(r);
	}

    
    public ControlInitResponse controlInit(ControlInitRequest r) {
        return stub.controlInit(r);
    }

    public TrackResponse track(TrackRequest r) {
        return stub.track(r);
    }

    public TrackMatchResponse trackMatch(TrackMatchRequest r) {
        return stub.trackMatch(r);
    }

    public TraceResponse trace(TraceRequest r) {
        return stub.trace(r);
    }

    public ReportResponse reports(ReportRequest r) {
        return stub.report(r);
    }

    @Override
    public final void close() {
        channel.shutdown();
    }

}