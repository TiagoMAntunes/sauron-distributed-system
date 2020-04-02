package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import com.google.type.LatLng;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;

import io.grpc.StatusRuntimeException;

import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;

public class TrackMatchIT extends BaseIT {
	
	private final String CAR_TYPE = "CAR";
    private final String PERSON_TYPE = "PERSON";
    private final String CAR_ID = "AA00AA";
	private final String PERSON_ID = "14388236";
		
	private final String CAM_NAME = "Alameda";
	private final LatLng CAM_COORDS = LatLng.newBuilder().setLatitude(1).setLongitude(1).build();
	private final Camera CAMERA = Camera.newBuilder().setName(CAM_NAME).setCoords(CAM_COORDS).build();
    
    private final Observable CAR_OBSERVABLE = Observable.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).build();
	private final Observation CAR_OBSERVATION = Observation.newBuilder()
				.setObservated(CAR_OBSERVABLE)
				.setTime(fromMillis(currentTimeMillis()))
				.setCamera(CAMERA)
				.build();

	
	private final String CAR_PARTIAL_ID = "AA0*";
	private final String CAR_INV_PARTIAL_ID = "AA01*";
	
    @BeforeEach
	public void setUp() {
    
    }
	
	@AfterEach
	public void tearDown() {
        //Clean the server state after each test
        frontend.controlClear(ControlClearRequest.newBuilder().build());

	}

    @Test
    public void nonNullResponse() {
		TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(CAR_OBSERVABLE).build();
		TrackMatchResponse response = frontend.trackMatch(request);
		
		assertNotEquals(null, response, "Response shouldn't be null");
		
    }

    @Test
    public void emptyResponse() {
		//server is empty
		TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(CAR_OBSERVABLE).build();
		TrackMatchResponse response = frontend.trackMatch(request);
		
		assertEquals(0, response.getObservationsCount());
    }

    @Test
    public void oneObservation() {
        //load data first
		frontend.controlInit(ControlInitRequest.newBuilder().addObservation(CAR_OBSERVATION).build());

		Observable part_obs = Observable.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_PARTIAL_ID).build();
		TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(part_obs).build();
		TrackMatchResponse response = frontend.trackMatch(request);
		
		assertEquals(1, response.getObservationsCount());
		assertEquals(CAR_OBSERVATION, response.getObservationsList().get(0));
	}

	@Test
    public void idPerfectMatch() {
        //load data first
		frontend.controlInit(ControlInitRequest.newBuilder().addObservation(CAR_OBSERVATION).build());

		TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(CAR_OBSERVABLE).build();
		TrackMatchResponse response = frontend.trackMatch(request);
		
		List<Observation> observations = response.getObservationsList();
		assertEquals(1, observations.size());
		assertEquals(CAR_OBSERVATION, observations.get(0));
	}
	
	@Test
	public void multipleObservations() {
		//Load data first
		List<Observation> values = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
			Observable o = Observable.newBuilder().setType(CAR_TYPE).setIdentifier("AA0" + String.valueOf(i) + "AA").build();
			values.add(Observation.newBuilder().setObservated(o).setCamera(CAMERA).setTime(fromMillis(currentTimeMillis())).build());
		}
            
		frontend.controlInit(ControlInitRequest.newBuilder().addAllObservation(values).build());
		
		Observable part_obs = Observable.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_PARTIAL_ID).build();
		TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(part_obs).build();
		TrackMatchResponse response = frontend.trackMatch(request);

		assertEquals(10, response.getObservationsCount());
	}

	@Test
	public void multipleObservationsSameId() {
		//Load data first
		List<Observation> values = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
			Observable o = Observable.newBuilder().setType(CAR_TYPE).setIdentifier("AA1" + String.valueOf(i) + "AA").build();
			values.add(Observation.newBuilder().setObservated(o).setCamera(CAMERA).setTime(fromMillis(currentTimeMillis())).build());
		}
		for (int i = 0; i < 5; i++){
			Observable o = Observable.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).build();
			values.add(Observation.newBuilder().setObservated(o).setCamera(CAMERA).setTime(fromMillis(currentTimeMillis())).build());
		}
		frontend.controlInit(ControlInitRequest.newBuilder().addAllObservation(values).build());
		
		Observable part_obs = Observable.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_PARTIAL_ID).build();
		TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(part_obs).build();
		TrackMatchResponse response = frontend.trackMatch(request);

		assertEquals(1, response.getObservationsCount());
	}

	@Test
	public void noMatch() {
		//Load data first
		List<Observation> values = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Observable o = Observable.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).build();
			values.add(Observation.newBuilder().setObservated(o).setCamera(CAMERA).setTime(fromMillis(currentTimeMillis())).build());
		}
		frontend.controlInit(ControlInitRequest.newBuilder().addAllObservation(values).build());
		
		Observable part_obs = Observable.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_INV_PARTIAL_ID).build();
		TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(part_obs).build();
		TrackMatchResponse response = frontend.trackMatch(request);

		assertEquals(0, response.getObservationsCount());
	}

	@Test
	public void nullObservation() {
		TrackMatchRequest request = TrackMatchRequest.newBuilder().build();

		//assertEquals(Status.INVALID_ARG, response.getResponseStatus());
		assertEquals(
            INVALID_ARGUMENT,
            assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request)).getStatus().getCode()
            );
	}

	@Test
	public void nullType() {
		Observable observation = Observable.newBuilder().setIdentifier(CAR_ID).build();
		TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(observation).build();

		//assertEquals(Status.INVALID_ARG, response.getResponseStatus());
		assertEquals(
            INVALID_ARGUMENT,
            assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request)).getStatus().getCode()
            );
	}

	@Test
	public void nullId() {
		Observable part_obs = Observable.newBuilder().setType(CAR_TYPE).build();
		TrackMatchRequest request = TrackMatchRequest.newBuilder().setIdentity(part_obs).build();

		//assertEquals(Status.INVALID_ARG, response.getResponseStatus());
		assertEquals(
            INVALID_ARGUMENT,
            assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request)).getStatus().getCode()
            );
	}
}