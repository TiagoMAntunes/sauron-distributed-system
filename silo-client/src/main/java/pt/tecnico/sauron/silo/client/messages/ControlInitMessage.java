package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class ControlInitMessage implements Request {

    private ControlInitRequest req;

    public ControlInitMessage(ControlInitRequest req) {
        this.req = req;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException { Request request = Request.newBuilder().setPrev(VectorClock.newBuilder().addAllUpdates(req.getPrev().getUpdatesList()).build()).build()
        return stub.controlInit(req);
    }
}
