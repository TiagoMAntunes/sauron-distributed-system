package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class ControlClearMessage implements Request {

    private ControlClearRequest req;

    public ControlClearMessage(ControlClearRequest req) {
        this.req = req;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException { Request request = Request.newBuilder().setPrev(VectorClock.newBuilder().addAllUpdates(req.getPrev().getUpdatesList()).build()).build()
        return stub.controlClear(req);
    }
}
