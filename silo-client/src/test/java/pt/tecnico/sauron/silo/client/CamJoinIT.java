package pt.tecnico.sauron.silo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.tecnico.sauron.silo.client.BaseIT;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Status;

public class CamJoinIT extends BaseIT {

    static final String NAME = "test";
    static final String SMALL_NAME = "12";
    static final String BIG_NAME = "abcdefghijlmnopq";
    static final double LON = 1;
    static final double LAT = 1;
    static final double NULL_LON = null;
    static final double NULL_LAT = null;
	
	// initialization and clean-up for each test
	
	@BeforeEach
	public void setUp() {
    
    }
	
	@AfterEach
	public void tearDown() {
        frontend.controlClear(ControlClearRequest.newBuilder().build());
	}
		
	// tests 
	
	@Test
	public void camJoinTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(NAME).setLon(LON).setLat(LAT).build();
        CamJoinResponse response = frontend.camJoin(request);
        assertEquals(Status.OK, response.getResponseStatus());
    }
    
    @Test
    public void duplicateNameTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(NAME).setLon(LON).setLat(LAT).build();
        CamJoinRequest request_duplicate = CamJoinRequest.newBuilder().setName(NAME).setLon(LON).setLat(LAT).build();
        CamJoinResponse response = frontend.camJoin(request);
        CamJoinResponse response_duplicate = frontend.camJoin(request_duplicate);

        assertEquals(Status.INVALID_ARG, response_duplicate.getResponseStatus());
    }

    @Test
    public void smallNameTest() {
        //Verifies if the name is less than 3 characters long
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(SMALL_NAME).setLon(LON).setLat(LAT).build();
        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.INVALID_ARG, response.getResponseStatus());
    }

    @Test
    public void bigNameTest() {
        //Verifies if the name is more than 15 characters long
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(BIG_NAME).setLon(LON).setLat(LAT).build();
        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.INVALID_ARG, response.getResponseStatus());
    }

    @Test
    public void nullLonTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(NAME).setLon(NULL_LON).setLat(LAT).build();

        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.NULL_COORDS, response.getResponseStatus());
    }

    @Test
    public void nullLatTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(NAME).setLon(LON).setLat(NULL_LAT).build();

        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.NULL_COORDS, response.getResponseStatus());
    }

    @Test
    public void nullCoordsTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(NAME).setLon(NULL_LON).setLat(NULL_LAT).build();

        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.NULL_COORDS, response.getResponseStatus());
    }
}