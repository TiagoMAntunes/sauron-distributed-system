package pt.tecnico.sauron.eye;

import java.util.Scanner;
import java.util.ArrayList;
import static java.lang.System.currentTimeMillis;

import com.google.type.LatLng;
import static com.google.protobuf.util.Timestamps.fromMillis;

import pt.tecnico.sauron.silo.grpc.Silo.Status;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.client.SiloServerFrontend;



public class EyeApp {

	public static void main(String[] args) {
		System.out.println(EyeApp.class.getSimpleName());
		
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

		CamInfoRequest request = CamInfoRequest.newBuilder().setName(camName).build();
		CamInfoResponse response = frontend.camInfo(request);

		if(response.getResponseStatus() == Status.INVALID_CAM) {
			LatLng camCoords = LatLng.newBuilder().
								setLatitude(lat).
								setLongitude(lon).
								build();

			Camera newCam = Camera.newBuilder().
									setName(camName).
									setCoords(camCoords).
									build();
			CamJoinRequest camJoinReq = CamJoinRequest.newBuilder().
											setCamera(newCam).
											build();
			frontend.camJoin(camJoinReq);
		}

		System.out.print("$ ");

		Scanner sc = new Scanner(System.in);
		ArrayList<Observation> observations = new ArrayList<>();

		while((sc.hasNextLine())) {
			String line = sc.nextLine();
			String[] values = line.split(",");
			String obsType = values[0];
			String obsId = values[1];

			Observable entity = Observable.newBuilder().
									setType(obsType).
									setIdentifier(obsId).
									build();
			Observation obs = Observation.newBuilder().
								setObservated(entity).
								setTime(fromMillis(currentTimeMillis())).
								build();

			observations.add(obs);

		}
		System.out.print("Closing eyelids...");

		for (Observation o : observations ) {

			//TODO Alternative to consider: make a single report with multiple observations
			ReportRequest reportReq = ReportRequest.newBuilder().
										setCameraName(camName).
										addObservations(o).
										build();

			frontend.reports(reportReq);
		}

		System.out.println("Report sent. Sauron will be pleased for your aid to ending the Age of Men");

	}



}
