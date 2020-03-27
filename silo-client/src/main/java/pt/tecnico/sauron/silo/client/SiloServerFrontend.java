package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearResponse;

public class SiloServerFrontend {

    private final String target;
    final ManagedChannel channel;
    SauronGrpc.SauronBlockingStub stub;

    public SiloServerFrontend(String host, int port) {
        target = host + ":" + port;
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = SauronGrpc.newBlockingStub(channel);
    }

    public ControlPingResponse controlPing(ControlPingRequest r) {
        return stub.controlPing(r);
    }

    public ControlClearResponse controlClear(ControlClearRequest r) {
        return stub.controlClear(r);
    }

}