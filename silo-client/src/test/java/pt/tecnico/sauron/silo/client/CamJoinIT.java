package pt.tecnico.sauron.silo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.type.LatLng;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.tecnico.sauron.silo.client.BaseIT;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.Status;

public class CamJoinIT extends BaseIT {

    static final String NAME = "test";
    static final String SMALL_NAME = "12";
    static final String BIG_NAME = "abcdefghijlmnopq";
    static final double LON = 1;
    static final double LAT = 1;
    static final LatLng COORDS = LatLng.newBuilder().setLatitude(50).setLongitude(50).build();

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
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinResponse response = frontend.camJoin(request);
        assertEquals(Status.OK, response.getResponseStatus());
    }
    
    @Test
    public void duplicateNameTest() {
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        Camera camera_duplicate = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinRequest request_duplicate = CamJoinRequest.newBuilder().setCamera(camera_duplicate).build();
        CamJoinResponse response = frontend.camJoin(request);
        CamJoinResponse response_duplicate = frontend.camJoin(request_duplicate);

        assertEquals(Status.INVALID_ARG, response_duplicate.getResponseStatus());
    }

    @Test
    public void smallNameTest() {
        //Verifies if the name is less than 3 characters long
        Camera camera = Camera.newBuilder().setName(SMALL_NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.INVALID_ARG, response.getResponseStatus());
    }

    @Test
    public void bigNameTest() {
        //Verifies if the name is more than 15 characters long
        Camera camera = Camera.newBuilder().setName(BIG_NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.INVALID_ARG, response.getResponseStatus());
    }

    @Test
    public void nullLonTest() {
        LatLng bad_coords = LatLng.newBuilder().setLatitude(1).build();
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(bad_coords).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();

        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.NULL_COORDS, response.getResponseStatus());
    }

    @Test
    public void nullLatTest() {
        LatLng bad_coords = LatLng.newBuilder().setLongitude(1).build();
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(bad_coords).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        
        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.NULL_COORDS, response.getResponseStatus());
    }

    @Test
    public void nullCoordsTest() {
        LatLng bad_coords = LatLng.newBuilder().build();
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(bad_coords).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();

        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(Status.NULL_COORDS, response.getResponseStatus());
    }
}