package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class TrackMatchMessage extends MessageStrategy {

    private TrackMatchRequest req;

    public TrackMatchMessage(TrackMatchRequest req) {
        this.req = req;
    }

    protected Message call(Message msg, SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return null;
    }
}
