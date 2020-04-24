package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class ReportMessage extends MessageStrategy {

    private ReportRequest req;

    public ReportMessage(ReportRequest req) {
        this.req = req;
    }

    protected Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return stub.report(req);
    }
}
