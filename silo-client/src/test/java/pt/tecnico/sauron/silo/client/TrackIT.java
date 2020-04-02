package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static io.grpc.Status.Code.FAILED_PRECONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.type.LatLng;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;

public class TrackIT extends BaseIT {

    private final String TYPE = "CAR";
    private final String ID = "AA00AA";
    private final String INV_ID = "AA01AA";
    
    private final String CAM_NAME = "Alameda";
	private final LatLng CAM_COORDS = LatLng.newBuilder().setLatitude(1).setLongitude(1).build();
	private final Camera CAMERA = Camera.newBuilder().setName(CAM_NAME).setCoords(CAM_COORDS).build();
    
    private final Observable OBSERVABLE = Observable.newBuilder().setType(TYPE).setIdentifier(ID).build();
	private final Observation OBSERVATION = Observation.newBuilder()
				.setObservated(OBSERVABLE)
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
        frontend.controlInit(ControlInitRequest.newBuilder().addObservation(OBSERVATION).build());
        TrackRequest request = TrackRequest.newBuilder().setIdentity(OBSERVABLE).build();
        TrackResponse response = frontend.track(request);
        assertNotEquals(null, response, "Response shouldn't be null");
    }

    @Test
    public void emptyResponse() {
        //server has no data
        TrackRequest request = TrackRequest.newBuilder().setIdentity(OBSERVABLE).build();
        
        assertEquals(
            FAILED_PRECONDITION,
            assertThrows(StatusRuntimeException.class, () -> frontend.track(request)).getStatus().getCode()
            );
    }

    @Test
    public void okResponse() {
        //load data first
        frontend.controlInit(ControlInitRequest.newBuilder().addObservation(OBSERVATION).build());

        TrackRequest request = TrackRequest.newBuilder().setIdentity(OBSERVABLE).build();
        TrackResponse response = frontend.track(request);
        Observation o = response.getObservation();

        assertEquals(OBSERVATION, o);
    }

    @Test
    public void noMatch() {
        //load data first
        frontend.controlInit(ControlInitRequest.newBuilder().addObservation(OBSERVATION).build());

        Observable inv_obs = Observable.newBuilder().setType(TYPE).setIdentifier(INV_ID).build();
        TrackRequest request = TrackRequest.newBuilder().setIdentity(inv_obs).build();

        assertEquals(
            FAILED_PRECONDITION,
            assertThrows(
                StatusRuntimeException.class, () -> frontend.track(request)).getStatus().getCode()
            );
    }
    
    @Test
	public void nullObservation() {
		TrackRequest request = TrackRequest.newBuilder().build();
		
        assertEquals(
            INVALID_ARGUMENT,
            assertThrows(
                StatusRuntimeException.class, () -> frontend.track(request)).getStatus().getCode()
            );
	}

	@Test
	public void emptyType() {
		Observable observation = Observable.newBuilder().setIdentifier(ID).build();
		TrackRequest request = TrackRequest.newBuilder().setIdentity(observation).build();

        assertEquals(
            INVALID_ARGUMENT,
            assertThrows(
                StatusRuntimeException.class, () -> frontend.track(request)).getStatus().getCode()
            );
	}

	@Test
	public void emptyId() {
		Observable part_obs = Observable.newBuilder().setType(TYPE).build();
		TrackRequest request = TrackRequest.newBuilder().setIdentity(part_obs).build();
		
        assertEquals(
            INVALID_ARGUMENT,
            assertThrows(
                StatusRuntimeException.class, () -> frontend.track(request)).getStatus().getCode()
            );
	}

}