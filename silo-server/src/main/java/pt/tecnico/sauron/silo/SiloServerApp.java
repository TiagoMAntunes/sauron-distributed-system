package pt.tecnico.sauron.silo;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.GossipRequest;
import pt.tecnico.sauron.silo.grpc.Silo.GossipResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

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
		final int whichReplica = Integer.parseInt(path.substring(path.length()-1, path.length())); // Which replica is it
		int interval;
		if (args.length >= 7)
			interval = Integer.parseInt(args[6]) * 1000; //ms
		else 
			interval = 30000;
		
		SiloServerImpl silo = new SiloServerImpl(nReplicas,whichReplica); //Passes number of replicas and which replica it is
		final BindableService impl = silo;
		
		//Create a new server
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		ZKNaming zkNaming = null;
	
		zkNaming = new ZKNaming(zooHost, zooPort);
		zkNaming.rebind(path, host, String.valueOf(port));
		//Start the server
		server.start();

		// Server threads are running in the background.
		System.out.println("Server started");

		//Start gossip at every interval ms
		Timer timer = new Timer();
		timer.schedule(new GossipRun(silo, zooHost, zooPort, whichReplica), 1000, interval); //TODO introduced delay to allow for other silos to connect
		
		//Handle end of the server
		Runtime.getRuntime().addShutdownHook(new HandleEnd(zkNaming, path, host, String.valueOf(port)));

		//Do not exit until termination
		server.awaitTermination();	
	

	}

	static class GossipRun extends TimerTask {
		private final SiloServerImpl silo;
		private int whichReplica;
		private ZKNaming zkNaming;
		private final String path = "/grpc/sauron/silo"; // TODO This is hard-coded, should it be?

		public GossipRun(SiloServerImpl s, String host, String port, int rep) {
			silo = s;
			zkNaming = new ZKNaming(host, port);;
			whichReplica = rep;
		}

		public void run(){
			silo.doGossip(whichReplica, zkNaming, path);
		}	
	}

	static class HandleEnd extends Thread {
		private final ZKNaming zk;
		private final String path, host, port;

		public HandleEnd(ZKNaming zk, String path, String host, String port) {
			this.zk = zk;
			this.path = path;
			this.host = host;
			this.port = port;
		}
		public void run() {
			System.out.println("Server closing...");
			try {
				if (zk != null)
					zk.unbind(path, host, port);
			}
			catch(ZKNamingException e) {
				System.out.printf("There was a problem unbinding the server at %s %s:%s%n", path, host, port);	
			}
		}
	}

}
