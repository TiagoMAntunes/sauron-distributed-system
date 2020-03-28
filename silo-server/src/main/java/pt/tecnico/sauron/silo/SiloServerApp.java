package pt.tecnico.sauron.silo;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class SiloServerApp {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println(SiloServerApp.class.getSimpleName());

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final int port = Integer.parseInt(args[0]);
		final BindableService impl = new SiloServerImpl();
		
		//Create a new server
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		//Start the server
		server.start();

		// Server threads are running in the background.
		System.out.println("Server started");


		//Do not exit until termination
		server.awaitTermination();

	}
	
}
