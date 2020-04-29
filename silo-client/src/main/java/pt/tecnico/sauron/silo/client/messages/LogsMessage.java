package pt.tecnico.sauron.silo.client.messages;

import com.google.protobuf.Message;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.GetNonAppliedLogsRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class LogsMessage implements Request {

    GetNonAppliedLogsRequest req;

    public LogsMessage(GetNonAppliedLogsRequest req) {
        this.req = req;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException {
        return stub.getNonAppliedLogs(req);
    }

}