package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SiloClientApp {
	
	public static void main(String[] args) throws ZKNamingException, UnavailableException {
		System.out.println(SiloClientApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final String host = args[0];
		final String port = args[1];

		
		try (SiloServerFrontend frontend = new SiloServerFrontend(host, port, 100)) {
			ControlPingRequest req = ControlPingRequest.newBuilder().setInputText("friend").build();
			ControlPingResponse res = frontend.controlPing(req);
			System.out.println(res.getStatus());
		} catch (StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
		} catch (UnavailableException e) {
			System.out.println("No server available.");
		}
	}
	
}
