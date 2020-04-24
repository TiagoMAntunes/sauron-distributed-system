package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class TrackMessage extends MessageStrategy {

    private TrackRequest req;

    public TrackMessage(TrackRequest req) {
        this.req = req;
    }

    protected Message call(Message msg, SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return null;
    }
}