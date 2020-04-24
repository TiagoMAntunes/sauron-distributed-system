package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class CamJoinMessage extends MessageStrategy {

    private CamJoinRequest req;

    public CamJoinMessage(CamJoinRequest req) {
        this.req = req;
    }

    protected Message call(Message msg, SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return null;
    }
}
