package pt.tecnico.sauron.spotter;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.google.type.LatLng;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.SiloServerFrontend;
import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.tecnico.sauron.silo.grpc.Silo.GetNonAppliedLogsRequest;
import pt.tecnico.sauron.silo.grpc.Silo.GetNonAppliedLogsResponse;
import pt.tecnico.sauron.silo.grpc.Silo.LogElement;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;


public class SpotterApp {
	
	public static void main(String[] args) throws ZKNamingException, UnavailableException {
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length < 2 || args.length > 3) {
			System.out.printf("Usage: java %s <address> <port> [instance]%n", SpotterApp.class.getName());
			System.exit(0);
		}

		final String host = args[0];
		final String port = args[1];

		String instance = args.length ==3 ? args[2] : "0";

		try (SiloServerFrontend frontend = new SiloServerFrontend(host, port, instance); Scanner sc = new Scanner(System.in)) {
			boolean end = false;
			while(!end) {

				System.out.print("$ ");

				String line[] = sc.nextLine().split(" ");
				try {
					switch(line[0]) {
						case "spot":
							spotHandler(frontend, line);
							break;
						case "trail":
							trailHandler(frontend, line);
							break;
						case "ping":
							pingHandler(frontend, line);
							break;
						case "clear":
							clearHandler(frontend, line);
							break;
						case "init":
							initHandler(frontend, line);
							break;
						case "logs":
							logsHandler(frontend, line);
							break;
						case "help":
							System.out.println("Available commands: ");
							System.out.println("spot <type> <identifier> - Find the last observation of an object or person with the specified ID");
							System.out.println("trail <type> <identifier> - Find the path followed by an object or person with the specified ID");
							System.out.println("ping [name] - Checks if the server is responding by saying hi");
							System.out.println("clear - Resets the server to default status");
							System.out.println("init <amount> [<type> <identifier> <camera name> <latitude> <longitude>]");
							System.out.println("help - Displays this menu");
							System.out.println("exit - Exits the program");
							break;
						case "exit":
							end = true;
							break;
						default:
							System.out.printf("Unknown command '%s' Write help to get the list of available commands%n", line[0]);
					}
				} catch(StatusRuntimeException e) {
					System.out.println(e.getStatus().getDescription());
					if (e.getStatus().getCode() == Code.UNAVAILABLE) {
							System.out.println("The hostname is unavailable. Exiting...");
							System.exit(0);
					}
				}
			}
		} 
		catch (NoSuchElementException e) {
			System.out.println("Input has been closed.");
		}
		catch (Exception e) {
			System.out.println("Uncatched exception. Throwing...");
			e.printStackTrace();
			throw e;
		} finally {
			System.out.println("Closing...");
		}

	}

	private static void spotHandler(SiloServerFrontend frontend, String[] line) throws ZKNamingException, UnavailableException {
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
			observations = new ArrayList<>(response.getObservationsList());
		} else {
			TrackRequest request = TrackRequest.newBuilder().setIdentity(identity).build();
			TrackResponse response = frontend.track(request);
			observations = new ArrayList<Observation>();
			observations.add((response.getObservation()));
		}

		Collections.sort(observations, Comparators.OBSERVATION_ID);

		printObservations(System.out, observations);
	}

	private static void trailHandler(SiloServerFrontend frontend, String[] line) throws ZKNamingException, UnavailableException {
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
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date(o.getTime().getSeconds()*1000 + o.getTime().getNanos()/1000000)), 
				o.getCamera().getName(),
				o.getCamera().getCoords().getLatitude(),
				o.getCamera().getCoords().getLongitude()
			);
		}
	}

	private static void pingHandler(SiloServerFrontend frontend, String[] line) throws ZKNamingException, UnavailableException {
		String name;
		if (line.length < 2) {
			System.out.println("Assuming default name value");
			name = "friend";
		} else
			name = line[1];

		ControlPingRequest request = ControlPingRequest.newBuilder().setInputText(name).build();
		ControlPingResponse response = frontend.controlPing(request);

		System.out.println(response.getStatus());
	}

	private static void clearHandler(SiloServerFrontend frontend, String[] line) throws ZKNamingException, UnavailableException {
		ControlClearRequest request = ControlClearRequest.getDefaultInstance();
		frontend.controlClear(request);
		System.out.println("System has been cleared");
	}

	private static void initHandler(SiloServerFrontend frontend, String[] line) throws ZKNamingException, UnavailableException {
		if (line.length < 2 || Integer.parseInt(line[1]) * 5 + 2 > line.length) {
			System.out.println("Invalid number of arguments!");
			return;
		}

		int amount = Integer.parseInt(line[1]);
		ArrayList<Observation> observations = new ArrayList<>();
		for (int i = 2; i < amount * 5 + 2; i+=5) {
			Observation observation = Observation.newBuilder().
					setCamera(Camera.newBuilder().
									setCoords(LatLng.newBuilder().setLatitude(Double.parseDouble(line[i+3])).setLongitude(Double.parseDouble(line[i+4])).build()).
									setName(line[i+2]).
									build()).
					setObservated(Observable.newBuilder().
											setIdentifier(line[i+1]).
											setType(line[i]).
											build()).
					setTime(fromMillis(currentTimeMillis()))
					.build();
			observations.add(observation);
		}

		ControlInitRequest request = ControlInitRequest.newBuilder().addAllObservation(observations).build();
		frontend.controlInit(request);
	}
	
	private static void logsHandler(SiloServerFrontend frontend, String[] line) throws ZKNamingException, UnavailableException {
		GetNonAppliedLogsRequest req = GetNonAppliedLogsRequest.getDefaultInstance();
		GetNonAppliedLogsResponse res = frontend.logs(req);
		
		for (LogElement l : res.getElementsList()) {
			System.out.println(l);
		}

	}

}
