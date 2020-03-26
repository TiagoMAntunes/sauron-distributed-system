package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;

public class SiloClientApp {
	
	public static void main(String[] args) {
		System.out.println(SiloClientApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final String host = args[0];
		final int port = Integer.parseInt(args[1]);

		SiloServerFrontend frontend = new SiloServerFrontend(host, port);
		try {
			ControlPingRequest req = ControlPingRequest.newBuilder().setInputText("friend").build();
			ControlPingResponse res = frontend.controlPing(req);
			System.out.println(res.getStatus());
		} catch (StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
		}

	}
	
}
