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
    private CamJoinRequest camReq;
    public ReportMessage(ReportRequest req, CamJoinRequest camReq) {
        this.req = req;
        this.camReq = camReq;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException { 
        ReportRequest request = ReportRequest.newBuilder().setCameraName(req.getCameraName()).addAllObservations(req.getObservationsList()).setPrev(VectorClock.newBuilder().addAllUpdates(req.getPrev().getUpdatesList()).build()).build();
        ReportResponse response = stub.report(request);
        System.out.println("Received new timestamp: " + response.getNew().getUpdatesList());
        timestamp.update(response.getNew().getUpdatesList());
        return response;
    }
}
