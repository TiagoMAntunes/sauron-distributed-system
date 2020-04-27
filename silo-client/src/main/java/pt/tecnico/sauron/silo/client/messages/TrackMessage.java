package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class TrackMessage implements Request {

    private TrackRequest req;

    public TrackMessage(TrackRequest req) {
        this.req = req;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return stub.track(req);
    }
}
