package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class ControlPingMessage implements Request {

    private ControlPingRequest req;

    public ControlPingMessage(ControlPingRequest req) {
        this.req = req;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException {
        return stub.controlPing(req);
    }
}
