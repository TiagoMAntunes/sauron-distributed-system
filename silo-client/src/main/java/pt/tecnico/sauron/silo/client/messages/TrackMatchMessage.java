package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class TrackMatchMessage implements Request {

    private TrackMatchRequest req;
    //For the cache in Frontend
    private Observable identity;

    public TrackMatchMessage(TrackMatchRequest req) {
        this.req = req;
        this.identity = req.getIdentity();
    }

    public Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return stub.trackMatch(req);
    }

    //Used in cache to compare keys
    @Override
    public boolean equals(Object o) {
        if (! (o instanceof TrackMatchMessage)) return false;
        TrackMatchMessage d = (TrackMatchMessage) o;
        System.out.println("Comparing");
        System.out.println("Type: " +  this.identity.getType().equals(d.identity.getType()));
        System.out.println("Identifier: " +this.identity.getIdentifier().equals(d.identity.getIdentifier()) );
        return this.identity.getType().equals(d.identity.getType()) && this.identity.getIdentifier().equals(d.identity.getIdentifier());
    }

    @Override
    public int hashCode() {
        return this.identity.getType().hashCode() + this.identity.getIdentifier().hashCode();
    }
}
