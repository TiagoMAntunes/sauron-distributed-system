package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.tecnico.sauron.silo.grpc.Silo.VectorClock;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class TraceMessage implements Request {

    private TraceRequest req;

    //For the cache in Frontend
    private Observable identity;

    public TraceMessage(TraceRequest req) {
        this.req = req;
        this.identity = req.getIdentity();
    }

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException { 
        TraceRequest request = TraceRequest.newBuilder().setIdentity(req.getIdentity()).setPrev(VectorClock.newBuilder().addAllUpdates(timestamp.getList()).build()).build();
        TraceResponse response = stub.trace(request);
        timestamp.update(response.getNew().getUpdatesList());
        return response;
    }

    //Used in cache to compare keys
    @Override
    public boolean equals(Object o) {
        if (! (o instanceof TraceMessage)) return false;
        TraceMessage d = (TraceMessage) o;
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
