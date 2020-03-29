package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.Timestamp;
import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;

import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.TrackRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TrackResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;

public class TrackIT extends BaseIT {

    private final String CAR_TYPE = "CAR";
    private final String CAR_ID = "AA00AA";
    private final String CAR_INV_ID = "AA01AA";
    private final Observation CAR_OBSERVATION = Observation.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).setTime(fromMillis(currentTimeMillis())).build();
    

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
        TrackRequest request = TrackRequest.newBuilder().setIdentifier(CAR_ID).setType(CAR_TYPE).build();
        TrackResponse response = frontend.track(request);
        assertNotEquals(null, response, "Response shouldn't be null");
    }

    @Test
    public void emptyResponse() {
        //server has no data
        TrackRequest request = TrackRequest.newBuilder().setIdentifier(CAR_ID).setType(CAR_TYPE).build();
        TrackResponse response = frontend.track(request);
        assertEquals(null, response.getObservation());
    }

    @Test
    public void okResponse() {
        //load data first
        frontend.controlInit(ControlInitRequest.newBuilder().addObservation(CAR_OBSERVATION).build());

        TrackRequest request = TrackRequest.newBuilder().setIdentifier(CAR_ID).setType(CAR_TYPE).build();
        TrackResponse response = frontend.track(request);
        Observation o = response.getObservation();

        assertEquals(CAR_OBSERVATION, o);
    }

    @Test
    public void noMatch() {
        //load data first
        frontend.controlInit(ControlInitRequest.newBuilder().addObservation(CAR_OBSERVATION).build());

        TrackRequest request = TrackRequest.newBuilder().setIdentifier(CAR_INV_ID).setType(CAR_TYPE).build();
        TrackResponse response = frontend.track(request);
        Observation o = response.getObservation();

        assertNotEquals(CAR_OBSERVATION, o);
    }

}