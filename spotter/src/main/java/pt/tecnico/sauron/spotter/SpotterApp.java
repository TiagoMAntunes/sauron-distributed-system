package pt.tecnico.sauron.spotter;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import pt.tecnico.sauron.silo.client.SiloServerFrontend;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;


public class SpotterApp {
	
	public static void main(String[] args) {
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 2) {
			System.out.printf("Usage: java %s <address> <port>%n", SpotterApp.class.getName());
			System.exit(0);
		}

		final String host = args[0];
		final int port = Integer.valueOf(args[1]);

		try (SiloServerFrontend frontend = new SiloServerFrontend(host, port); Scanner sc = new Scanner(System.in)) {
			boolean end = false;
			while(!end) {

				System.out.print("$ ");

				String line[] = sc.nextLine().split(" ");

				switch(line[0]) {
					case "spot":
						spotHandler(frontend, line);
						break;
					case "trail":
						trailHandler(frontend, line);
						break;
					case "ping":
					case "clear":
					case "init":
					case "help":
						System.out.println("Available commands: ");
						System.out.println("spot <type> <identifier> - Find the last observation of an object or person with the specified ID");
						System.out.println("trail <type> <identifier> - Find the path followed by an object or person with the specified ID");
						System.out.println("ping <name> - Checks if the server is responding by saying hi");
						System.out.println("clear - Resets the server to default status");
						System.out.println("init - to be done"); //TODO
						System.out.println("help - Displays this menu");
						System.out.println("exit - Exits the program");
						break;
					case "exit":
						end = true;
						break;
					default:
						System.out.printf("Unknown command '%s' Write help to get the list of available commands%n", line[0]);
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Closing...");
		}

	}

	private static void spotHandler(SiloServerFrontend frontend, String[] line) {
		if (line.length < 3) {
			System.out.println("Invalid number of arguments!");
			return;
		}

		//To allow different types and ids, we dont verify them here but only in the server

		String type = line[1];
		String id = line[2];
		boolean partial = id.contains("*");
		
		List<Observation> observations;
		Observable identity = Observable.newBuilder().setType(type).setIdentifier(id).build();

		if (partial) {
			//Id is partial
			TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(identity).build();
			TrackMatchResponse response = frontend.trackMatch(request);
			observations = response.getObservationsList();
		} else {
			TrackRequest request = TrackRequest.newBuilder().setIdentity(identity).build();
			TrackResponse response = frontend.track(request);
			observations = new ArrayList<Observation>();
			observations.add((response.getObservation()));
		}

		Collections.sort(observations, Comparators.OBSERVATION_ID);

		printObservations(System.out, observations);
	}

	private static void trailHandler(SiloServerFrontend frontend, String[] line) {
		if (line.length < 3) {
			System.out.println("Invalid number of arguments!");
			return;
		}

		String type = line[1];
		String id = line[2];

		Observable identity = Observable.newBuilder().setType(type).setIdentifier(id).build();
		TraceRequest request = TraceRequest.newBuilder().setIdentity(identity).build();
		TraceResponse response = frontend.trace(request);

		List<Observation> observations = response.getObservationsList();

		printObservations(System.out, observations);
		
	}

	private static void printObservations(PrintStream out, Iterable<Observation> observations) {
		for (Observation o : observations) {
			out.printf("%s,%s,%s,%s,%s,%s%n", 
				o.getObservated().getType(), 
				o.getObservated().getIdentifier(),
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(o.getTime()), 
				o.getCamera().getName(),
				o.getCamera().getCoords().getLatitude(),
				o.getCamera().getCoords().getLongitude()
			);
		}
	}

	

}
