package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class TrackMatchMessage implements Request {

    private TrackMatchRequest req;

    public TrackMatchMessage(TrackMatchRequest req) {
        this.req = req;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return stub.trackMatch(req);
    }
}
