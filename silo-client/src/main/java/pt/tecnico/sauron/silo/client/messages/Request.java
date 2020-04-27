package pt.tecnico.sauron.silo.client.messages;

import com.google.protobuf.Message;

import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public interface Request {

    public Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException;

}