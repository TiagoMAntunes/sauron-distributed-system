package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;

import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.Status;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;

public class TrackIT extends BaseIT {

    private final String CAR_TYPE = "CAR";
    private final String CAR_ID = "AA00AA";
    private final String CAR_INV_ID = "AA01AA";
    
    private final String CAM_NAME = "Alameda";
	private final LatLng CAM_COORDS = LatLng.newBuilder().setLatitude(1).setLongitude(1).build();
	private final Camera CAMERA = Camera.newBuilder().setName(CAM_NAME).setCoords(CAM_COORDS).build();
    
    private final Observable CAR_OBSERVABLE = Observable.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).build();
	private final Observation CAR_OBSERVATION = Observation.newBuilder()
				.setObservated(CAR_OBSERVABLE)
				.setTime(fromMillis(currentTimeMillis()))
				.setCamera(CAMERA)
				.build();


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
        TrackRequest request = TrackRequest.newBuilder().setIdentity(CAR_OBSERVABLE).build();
        TrackResponse response = frontend.track(request);
        assertNotEquals(null, response, "Response shouldn't be null");
    }

    @Test
    //TODO getObservation cant return null. Fix assertion
    public void emptyResponse() {
        //server has no data
        TrackRequest request = TrackRequest.newBuilder().setIdentity(CAR_OBSERVABLE).build();
        TrackResponse response = frontend.track(request);
        assertEquals(null, response.getObservation());
        assertEquals(Status.OK, response.getResponseStatus());
    }

    @Test
    public void okResponse() {
        //load data first
        frontend.controlInit(ControlInitRequest.newBuilder().addObservation(CAR_OBSERVATION).build());

        TrackRequest request = TrackRequest.newBuilder().setIdentity(CAR_OBSERVABLE).build();
        TrackResponse response = frontend.track(request);
        Observation o = response.getObservation();

        assertEquals(CAR_OBSERVATION, o);
        assertEquals(Status.OK, response.getResponseStatus());
    }

    @Test
    public void noMatch() {
        //load data first
        frontend.controlInit(ControlInitRequest.newBuilder().addObservation(CAR_OBSERVATION).build());

        Observable inv_obs = Observable.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_INV_ID).build();
        TrackRequest request = TrackRequest.newBuilder().setIdentity(inv_obs).build();
        TrackResponse response = frontend.track(request);
        Observation o = response.getObservation();

        assertNotEquals(CAR_OBSERVATION, o);
        assertEquals(Status.OK, response.getResponseStatus());
    }
    
    @Test
	public void nullObservation() {
		TrackRequest request = TrackRequest.newBuilder().build();
		TrackResponse response = frontend.track(request);

		assertEquals(Status.NULL_OBS, response.getResponseStatus());
	}

	@Test
	public void nullType() {
		Observable observation = Observable.newBuilder().setIdentifier(CAR_ID).build();
		TrackRequest request = TrackRequest.newBuilder().setIdentity(observation).build();
		TrackResponse response = frontend.track(request);

		assertEquals(Status.INVALID_ARG, response.getResponseStatus());
	}

	@Test
	public void nullId() {
		Observable part_obs = Observable.newBuilder().setType(CAR_TYPE).build();
		TrackRequest request = TrackRequest.newBuilder().setIdentity(part_obs).build();
		TrackResponse response = frontend.track(request);

		assertEquals(Status.INVALID_ARG, response.getResponseStatus());
	}

}