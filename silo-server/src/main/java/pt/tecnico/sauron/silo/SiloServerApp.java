package pt.tecnico.sauron.silo;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SiloServerApp {
	
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
		final int nReplicas = Integer.parseInt(args[5]);
		int interval;
		if (args.length >= 7)
			interval = Integer.parseInt(args[6]) * 1000; //ms
		else 
			interval = 30000;
		
		SiloServerImpl silo = new SiloServerImpl(nReplicas,Integer.parseInt(path.substring(path.length()-1, path.length()))); //Passes number of replicas and which replica it is
		final BindableService impl = silo;
		
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

			//Start gossip at every interval ms
			Timer timer = new Timer();
			timer.schedule(new GossipRun(silo), 0, interval);
			
			//Do not exit until termination
			server.awaitTermination();	
		} finally {
			if (zkNaming != null) {
				//remove
				zkNaming.unbind(path, host, String.valueOf(port));
			}
		}

	}

	static class GossipRun extends TimerTask {
		private final SiloServerImpl silo;

		public GossipRun(SiloServerImpl s) {
			silo = s;
		}

		public void run() {
			silo.gossip();
		}
	}

}
