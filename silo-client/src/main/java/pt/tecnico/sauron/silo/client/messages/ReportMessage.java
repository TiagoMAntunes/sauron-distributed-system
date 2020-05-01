package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.VectorClock;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class ReportMessage implements Request {

    private ReportRequest req;
    private CamJoinRequest camReq; // TODO: recover from camera not propagated fault
    public ReportMessage(ReportRequest req, CamJoinRequest camReq) {
        this.req = req;
        this.camReq = camReq;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException { 
        ReportRequest request = ReportRequest.newBuilder().setCameraName(req.getCameraName()).addAllObservations(req.getObservationsList()).setPrev(VectorClock.newBuilder().addAllUpdates(timestamp.getList()).build()).build();
        ReportResponse response = stub.report(request);
        timestamp.update(response.getNew().getUpdatesList());
        return response;
    }
}
