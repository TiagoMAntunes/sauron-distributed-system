package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class TraceMessage implements Request {

    private TraceRequest req;

    public TraceMessage(TraceRequest req) {
        this.req = req;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return stub.trace(req);
    }
}
