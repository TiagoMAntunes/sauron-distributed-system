package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.client.messages.Cache;
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

import java.util.ArrayList;

public class SiloServerFrontend implements AutoCloseable {
    private static final String PATH = "/grpc/sauron/silo"; // TODO This is hard-coded, should it be?
    private final ZKNaming zkNaming;
    private MessageStrategy requestManager;
    private Cache cache;

    public SiloServerFrontend(String host, String port) throws UnavailableException {
        this(host, port, "0");
    }

    public SiloServerFrontend(String host, String port, String instanceNumber) throws UnavailableException {
        this.cache = new Cache(3); //TODO change the max
        zkNaming = new ZKNaming(host, port);
        try {
            requestManager = new MessageStrategy(zkNaming, PATH, instanceNumber);
        } catch (ZKNamingException e) {
            throw new UnavailableException();
        }
    }

    public ControlPingResponse controlPing(ControlPingRequest r) throws ZKNamingException, UnavailableException {
        return (ControlPingResponse) requestManager.execute((new ControlPingMessage(r)));
    }

    public ControlClearResponse controlClear(ControlClearRequest r) throws ZKNamingException, UnavailableException {
        return (ControlClearResponse) requestManager.execute((new ControlClearMessage(r)));
    }

    public CamJoinResponse camJoin(CamJoinRequest r) throws ZKNamingException, UnavailableException {
        return (CamJoinResponse) requestManager.execute((new CamJoinMessage(r)));
    }

    public CamInfoResponse camInfo(CamInfoRequest r) throws ZKNamingException, UnavailableException {
        return (CamInfoResponse) requestManager.execute((new CamInfoMessage(r)));
    }

    public ControlInitResponse controlInit(ControlInitRequest r) throws ZKNamingException, UnavailableException {
        return (ControlInitResponse) requestManager.execute((new ControlInitMessage(r)));
    }

    public TrackResponse track(TrackRequest r) throws ZKNamingException, UnavailableException {
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
        return (ReportResponse) requestManager.execute(new ReportMessage(r, jr));
    }

    @Override
    public final void close() {
        requestManager.close();
    }
}