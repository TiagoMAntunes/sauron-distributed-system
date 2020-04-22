package pt.tecnico.sauron.silo;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

/**
 * The type Silo server app.
 */
public class SiloServerApp {

	/**
	 * The entry point of application.
	 *
	 * @param args the input arguments
	 * @throws IOException          the io exception
	 * @throws InterruptedException the interrupted exception
	 * @throws ZKNamingException    the zk naming exception
	 */
	public static void main(String[] args) throws IOException, InterruptedException, ZKNamingException {
		System.out.println(SiloServerApp.class.getSimpleName());

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String host = args[2];
		final int port = Integer.parseInt(args[3]);
		final String path = args[4];
		final BindableService impl = new SiloServerImpl();
		
		//Create a new server
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		ZKNaming zkNaming = null;
		try {
			zkNaming = new ZKNaming(zooHost, zooPort);
			zkNaming.rebind(path, host, String.valueOf(port));
			//Start the server
			server.start();

			// Server threads are running in the background.
			System.out.println("Server started");


			//Do not exit until termination
			server.awaitTermination();	
		} finally {
			if (zkNaming != null) {
				//remove
				zkNaming.unbind(path, host, String.valueOf(port));
			}
		}

	}
	
}
