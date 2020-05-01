package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.VectorClock;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class CamJoinMessage implements Request {

    private CamJoinRequest req;

    public CamJoinMessage(CamJoinRequest req) {
        this.req = req;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException { 
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(req.getCamera()).setPrev(VectorClock.newBuilder().addAllUpdates(timestamp.getList()).build()).build();
        CamJoinResponse response = stub.camJoin(request);
        timestamp.update(response.getNew().getUpdatesList());
        return response;
    }
}
