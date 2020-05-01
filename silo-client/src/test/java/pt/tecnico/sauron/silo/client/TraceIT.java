package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import java.util.ArrayList;

import com.google.type.LatLng;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;

public class TraceIT extends BaseIT {

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
	public void tearDown() throws ZKNamingException, UnavailableException {
		//Clean the server state after each test
		frontend.controlClear(ControlClearRequest.newBuilder().build());
		frontend.reset();
	}

	@Test
    public void nonNullResponse() throws ZKNamingException, UnavailableException {
		frontend.controlInit(ControlInitRequest.newBuilder().addObservation(OBSERVATION).build());
		TraceRequest request = TraceRequest.newBuilder().setIdentity(OBSERVABLE).build();
		TraceResponse response = frontend.trace(request);
		
		assertNotEquals(null, response, "Response shouldn't be null");
    }

    @Test
    public void emptyResponse() throws ZKNamingException, UnavailableException {
		//server is empty

		TraceRequest request = TraceRequest.newBuilder().setIdentity(OBSERVABLE).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(0, response.getObservationsCount());
    }

    @Test
    public void oneObservation() throws ZKNamingException, UnavailableException {
		//load data first
		frontend.controlInit(ControlInitRequest.newBuilder().addObservation(OBSERVATION).build());
		
		TraceRequest request = TraceRequest.newBuilder().setIdentity(OBSERVABLE).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(1, response.getObservationsCount());
		assertEquals(OBSERVATION, response.getObservationsList().get(0));
	}
	
	@Test
	public void multipleObservationsDifferentIds() throws ZKNamingException, UnavailableException {
		//Load data first
		List<Observation> values = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
			Observable o = Observable.newBuilder().setType(TYPE).setIdentifier("AA0" + String.valueOf(i) + "AA").build();
			values.add(Observation.newBuilder().setObservated(o).setCamera(CAMERA).setTime(fromMillis(currentTimeMillis())).build());
		}
		frontend.controlInit(ControlInitRequest.newBuilder().addAllObservation(values).build());
		
		TraceRequest request = TraceRequest.newBuilder().setIdentity(OBSERVABLE).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(1, response.getObservationsCount());
	}

	@Test
	public void multipleObservationsSameId() throws ZKNamingException, UnavailableException {
		//Load data first
		List<Observation> values = new ArrayList<>();
        for (int i = 0; i < 5; i++){
			Observable o = Observable.newBuilder().setType(TYPE).setIdentifier("AA1" + String.valueOf(i) + "AA").build();
			values.add(Observation.newBuilder().setObservated(o).setCamera(CAMERA).setTime(fromMillis(currentTimeMillis())).build());
		}
			
		for (int i = 0; i < 5; i++) {
			Observable o = Observable.newBuilder().setType(TYPE).setIdentifier(ID).build();
			values.add(Observation.newBuilder().setObservated(o).setCamera(CAMERA).setTime(fromMillis(currentTimeMillis())).build());
		}
			
		frontend.controlInit(ControlInitRequest.newBuilder().addAllObservation(values).build());
		
		TraceRequest request = TraceRequest.newBuilder().setIdentity(OBSERVABLE).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(5, response.getObservationsCount());
	}


	@Test
	public void noMatch() throws ZKNamingException, UnavailableException {
		//Load data first
		List<Observation> values = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
			Observable o = Observable.newBuilder().setType(TYPE).setIdentifier(INV_ID).build();
			values.add(Observation.newBuilder().setObservated(o).setCamera(CAMERA).setTime(fromMillis(currentTimeMillis())).build());
		}
        frontend.controlInit(ControlInitRequest.newBuilder().addAllObservation(values).build());
		
		TraceRequest request = TraceRequest.newBuilder().setIdentity(OBSERVABLE).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(0, response.getObservationsCount());
	}

	@Test
	public void nullObservation() {
		TraceRequest request = TraceRequest.newBuilder().build();
		
		assertEquals(
            INVALID_ARGUMENT,
            assertThrows(
                StatusRuntimeException.class, () -> frontend.trace(request)).getStatus().getCode()
            );
	}

	@Test
	public void emptyType() {
		Observable observation = Observable.newBuilder().setIdentifier(ID).build();
		TraceRequest request = TraceRequest.newBuilder().setIdentity(observation).build();
		
		assertEquals(
            INVALID_ARGUMENT,
            assertThrows(
                StatusRuntimeException.class, () -> frontend.trace(request)).getStatus().getCode()
            );

	}

	@Test
	public void emptyId() {
		Observable part_obs = Observable.newBuilder().setType(TYPE).build();
		TraceRequest request = TraceRequest.newBuilder().setIdentity(part_obs).build();
		
		assertEquals(
            INVALID_ARGUMENT,
            assertThrows(
                StatusRuntimeException.class, () -> frontend.trace(request)).getStatus().getCode()
            );
	}


}