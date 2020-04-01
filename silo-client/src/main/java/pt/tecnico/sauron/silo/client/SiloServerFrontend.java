package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo;
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
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;


public class SiloServerFrontend {

    private final String target;
    final ManagedChannel channel;
    SauronGrpc.SauronBlockingStub stub;

    public SiloServerFrontend(String host, int port) {
        target = host + ":" + port;
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

    public final void close() {
        channel.shutdown();
    }

}