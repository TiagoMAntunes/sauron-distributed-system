package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class ControlInitMessage extends MessageStrategy {

    private ControlInitRequest req;

    public ControlInitMessage(ControlInitRequest req) {
        this.req = req;
    }

    protected Message call(Message msg, SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return null;
    }
}
