package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class ReportMessage extends MessageStrategy {

    private ReportRequest req;
    private CamJoinRequest camReq;
    public ReportMessage(ReportRequest req, CamJoinRequest camReq) {
        this.req = req;
        this.camReq = camReq;
    }

    protected Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        stub.camJoin(camReq); //Must guarantee camera is registered
        return stub.report(req);
    }
}
