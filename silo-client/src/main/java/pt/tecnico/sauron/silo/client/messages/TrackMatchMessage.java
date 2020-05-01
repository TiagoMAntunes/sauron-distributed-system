package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.Silo.VectorClock;
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

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException { 
        TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(req.getIdentity()).setPrev(VectorClock.newBuilder().addAllUpdates(timestamp.getList()).build()).build();
        TrackMatchResponse response = stub.trackMatch(request);
        if (!(new Clock(response.getNew().getUpdatesList()).isMoreRecent(timestamp))) {
            timestamp.cache(); // This checks if the view is or not old
        }
        timestamp.update(response.getNew().getUpdatesList()); //Always update
        return response;
    }

    //Used in cache to compare keys
    @Override
    public boolean equals(Object o) {
        if (! (o instanceof TrackMatchMessage)) return false;
        TrackMatchMessage d = (TrackMatchMessage) o;
        return this.identity.getType().equals(d.identity.getType()) && this.identity.getIdentifier().equals(d.identity.getIdentifier());
    }

    @Override
    public int hashCode() {
        return this.identity.getType().hashCode() + this.identity.getIdentifier().hashCode();
    }
}
