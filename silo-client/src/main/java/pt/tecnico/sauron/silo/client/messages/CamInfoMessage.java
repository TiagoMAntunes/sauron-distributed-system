package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import com.google.protobuf.Message;

public class CamInfoMessage extends MessageStrategy {

    private CamInfoRequest req;

    public CamInfoMessage(CamInfoRequest req) {
        this.req = req;
    }

    protected Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return stub.camInfo(req);
    }
}
