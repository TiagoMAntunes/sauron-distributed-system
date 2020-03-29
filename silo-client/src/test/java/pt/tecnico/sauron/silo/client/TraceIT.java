package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import java.util.ArrayList;

import java.util.List;
import java.util.ArrayList;

import com.google.protobuf.Timestamp;
import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;

import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.TraceRequest;
import pt.tecnico.sauron.silo.grpc.Silo.TraceResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;

public class TraceIT extends BaseIT {

	private final String CAR_TYPE = "CAR";
    private final String CAR_ID = "AA00AA";
    private final String CAR_INV_ID = "AA01AA";
    private final Observation CAR_OBSERVATION = Observation.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).setTime(fromMillis(currentTimeMillis())).build();
    

    @BeforeEach
	public void setUp() {
    
    }
	
	@AfterEach
	public void tearDown() {
        
		
	}

	@Test
    public void nonNullResponse() {
		TraceRequest request = TraceRequest.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).build();
		TraceResponse response = frontend.trace(request);
		
		assertNotEquals(null, response, "Response shouldn't be null");
    }

    @Test
    public void emptyResponse() {
		//server is empty

		TraceRequest request = TraceRequest.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(0, response.getObservationsCount());
    }

    @Test
    public void oneObservation() {
		//load data first
		frontend.controlInit(ControlInitRequest.newBuilder().addObservation(CAR_OBSERVATION).build());
		
		TraceRequest request = TraceRequest.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(1, response.getObservationsCount());
		assertEquals(CAR_OBSERVATION, response.getObservationsList().get(0));
	}
	
	@Test
	public void multipleObservationsDifferentIds() {
		//Load data first
		List<Observation> values = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            values.add(Observation.newBuilder().setType(CAR_TYPE).setIdentifier("AA0" + String.valueOf(i) + "AA").setTime(fromMillis(currentTimeMillis())).build());
		frontend.controlInit(ControlInitRequest.newBuilder().addAllObservation(values).build());
		
		TraceRequest request = TraceRequest.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(1, response.getObservationsCount());
	}

	@Test
	public void multipleObservationsSameId() {
		//Load data first
		List<Observation> values = new ArrayList<>();
        for (int i = 0; i < 5; i++)
			values.add(Observation.newBuilder().setType(CAR_TYPE).setIdentifier("AA1" + String.valueOf(i) + "AA").setTime(fromMillis(currentTimeMillis())).build());
		for (int i = 0; i < 5; i++)
			values.add(Observation.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).setTime(fromMillis(currentTimeMillis())).build());
			
		frontend.controlInit(ControlInitRequest.newBuilder().addAllObservation(values).build());
		
		TraceRequest request = TraceRequest.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(5, response.getObservationsCount());
	}


	@Test
	public void noMatch() {
		//Load data first
		List<Observation> values = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            values.add(Observation.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_ID).setTime(fromMillis(currentTimeMillis())).build());
		frontend.controlInit(ControlInitRequest.newBuilder().addAllObservation(values).build());
		
		TraceRequest request = TraceRequest.newBuilder().setType(CAR_TYPE).setIdentifier(CAR_INV_ID).build();
		TraceResponse response = frontend.trace(request);

		assertEquals(0, response.getObservationsCount());
	}


}