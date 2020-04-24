package pt.tecnico.sauron.silo;

import java.io.IOException;
import java.util.Collection;
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
		try {
			zkNaming = new ZKNaming(zooHost, zooPort);
			zkNaming.rebind(path, host, String.valueOf(port));
			//Start the server
			server.start();

			// Server threads are running in the background.
			System.out.println("Server started");

			//Start gossip at every interval ms
			Timer timer = new Timer();
			timer.schedule(new GossipRun(silo, zooHost, zooPort, whichReplica), 1000, interval); //TODO introduced delay to allow for other silos to connect
			
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
		private int whichReplica;
		private ZKNaming zkNaming;
		private final String path = "/grpc/sauron/silo"; // TODO This is hard-coded, should it be?

		public GossipRun(SiloServerImpl s, String host, String port, int rep) {
			silo = s;
			zkNaming = new ZKNaming(host, port);;
			whichReplica = rep;
		}

		public void run(){
			System.out.println("In replica " + whichReplica);

			//TODO send updates
			//TODO clear updates after sending
			//TODO confirm / fix error at beginning because it cant find the other server then one of them stop  sending even when it connects
			    		
			try {
				Collection<ZKRecord> available = zkNaming.listRecords(path);
				//For every replica that is not this one
				available.forEach(record -> {
					String recPath = record.getPath();
					int recID = Integer.parseInt(recPath.substring(recPath.length()-1));
					if (recID != whichReplica) {
						String target = record.getURI();
						ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
						SauronGrpc.SauronBlockingStub stub = SauronGrpc.newBlockingStub(channel);
						GossipRequest req = GossipRequest.newBuilder().build(); 
						try {
							GossipResponse res = stub.gossip(req);
							System.out.println("Received response");
						} finally {
							channel.shutdown();
						} 
					}
				});
			} catch (ZKNamingException e) {
				System.out.println("Problem with gossip " + e.getMessage());
			}
		}	
	}

}
