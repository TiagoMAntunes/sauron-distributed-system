package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class ControlPingMessage extends MessageStrategy {

    private ControlPingRequest req;

    public ControlPingMessage(ControlPingRequest req) {
        this.req = req;
    }

    protected Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return stub.controlPing(req);
    }
}
