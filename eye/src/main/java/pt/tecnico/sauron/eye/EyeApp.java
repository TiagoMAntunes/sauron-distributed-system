package pt.tecnico.sauron.eye;

import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import static java.lang.System.currentTimeMillis;

import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.client.SiloServerFrontend;

import com.google.type.LatLng;
import static com.google.protobuf.util.Timestamps.fromMillis;

import io.grpc.StatusRuntimeException;
import io.grpc.Status.Code;

public class EyeApp {

	public static void main(String[] args) {
		System.out.println(EyeApp.class.getSimpleName());
		if(args.length > 5) {
			System.out.println("Too many arguments;");
			System.out.println("Please use:\n	'host port cameraName latitude longitude'");
			System.exit(0);
		}
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		} 

		final String host = args[0];
		final int port = Integer.parseInt(args[1]);
		final String camName = args[2];
		final double lat = Double.parseDouble(args[3]);
		final double lon = Double.parseDouble(args[4]);

		SiloServerFrontend frontend = new SiloServerFrontend(host, port);

		CamInfoRequest request = CamInfoRequest.newBuilder().
				setName(camName).build();

		try{frontend.camInfo(request);}
		catch(StatusRuntimeException e) {
			LatLng camCoords = LatLng.newBuilder().
					setLatitude(lat).
					setLongitude(lon).build();

			Camera newCam = Camera.newBuilder().
					setName(camName).
					setCoords(camCoords).build();
			CamJoinRequest camJoinReq = CamJoinRequest.newBuilder().
					setCamera(newCam).build();

			frontend.camJoin(camJoinReq);
		}

		helpInputMessage();
		System.out.print("$ ");

		Scanner sc = new Scanner(System.in);
		ArrayList<Observation> observations = new ArrayList<>();

		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.equals("")) {
				if(observations.size()>0) {
					try {
						sendObservations(observations, frontend, camName);
					} catch (StatusRuntimeException e) {
						System.out.println(e.getStatus().getDescription());
						if (e.getStatus().getCode() == Code.UNAVAILABLE) {
								System.out.println("The hostname is unavailable. Exiting...");
								System.exit(0);
						}
					}
					observations = new ArrayList<>();
				}
			} else if (line.charAt(0) == '#') {
				// Ignores comments
			}else {
				String[] values = line.split(",");
				if (values.length != 2) {
					helpInputMessage();
				} else {
					String obsType = values[0];
					String obsId = values[1];

					if (obsType.equals("zzz") && isLong(obsId)) {
						try {Thread.sleep(Long.parseLong(obsId)); }
						catch (InterruptedException e) {
							//TODO Alternative: Throw exception
							e.printStackTrace();
						}
					} else {
						
						Observable entity = Observable.newBuilder().
								setType(obsType).
								setIdentifier(obsId).build();

						Observation obs = Observation.newBuilder().
								setObservated(entity).
								setTime(fromMillis(currentTimeMillis())).build();

						observations.add(obs);
					}
				}
				
			}
			System.out.println();
			System.out.print("$ ");
		}
		System.out.print("Closing eyelids...");
		sc.close();

		if(observations.size()>0) {
			sendObservations(observations, frontend, camName);
		}
		
		System.out.println("Report sent. Sauron will be pleased for your aid in ending the Age of Men");
		System.exit(0);
	}

	static void sendObservations(List<Observation> observations, SiloServerFrontend frontend, String camName) {
		frontend.reports(ReportRequest.newBuilder().setCameraName(camName).addAllObservations(observations).build());
	}

	//TODO Do this without using exceptions
	static boolean isLong(String string) {
		if (string == null) {
			return false;
		}
		try {
			Long.parseLong(string);
		} catch (NumberFormatException e) {return false;}
		return true;
	}

	static void helpInputMessage() {
		System.out.println();
		System.out.println("How to use the Eye of Sauron:");
		System.out.println("	> To make a comment start the message with '#' (it won't transmit anything to the server);");
		System.out.println("	> To add an observation: 'objTYPE,objID' that is the object type followed by a comma ',' and the object id;");
		System.out.println("	> To make the Eye sleep use 'zzz,timeMilliseconds' that is three characters 'z' followed by a comma and the the time you desire in milliseconds");
		System.out.println("	> To send the observations submit an empty line;");
		System.out.println("	> To close the Eye use Ctrl-D, it will automatically send the observations you added and haven't submited.");
		System.out.println();
	}
}
