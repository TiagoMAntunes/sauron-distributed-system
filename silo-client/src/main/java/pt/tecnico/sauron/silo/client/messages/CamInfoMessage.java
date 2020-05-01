package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.VectorClock;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class CamInfoMessage implements Request {

    private CamInfoRequest req;

    public CamInfoMessage(CamInfoRequest req) {
        this.req = req;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException { 
        CamInfoRequest request = CamInfoRequest.newBuilder().setPrev(VectorClock.newBuilder().addAllUpdates(req.getPrev().getUpdatesList()).build()).build();
        CamInfoResponse response = stub.camInfo(request);
        System.out.println("Received new timestamp: " + response.getNew().getUpdatesList());
        timestamp.update(response.getNew().getUpdatesList());
        return response;
    }
}
