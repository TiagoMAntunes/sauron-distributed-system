package pt.tecnico.sauron.silo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.type.LatLng;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.type.LatLng;

import pt.tecnico.sauron.silo.client.BaseIT;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Status;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;

public class CamInfoIT extends BaseIT {

    static final String NAME = "test";
    static final String INEXISTENT_NAME = "inexistent";
    static final LatLng COORDS = LatLng.newBuilder().setLatitude(50).setLongitude(50).build();
    static final Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();

    // initialization and clean-up for each test

    @BeforeEach
	public void setUp() {
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinResponse response = frontend.camJoin(request);
    }
	
	@AfterEach
	public void tearDown() {
        frontend.controlClear(ControlClearRequest.newBuilder().build());
	}
		
	// tests 
	
	@Test
	public void camInfoTest() {
        CamInfoRequest request = CamInfoRequest.newBuilder().setName(NAME).build();
        CamInfoResponse response = frontend.camInfo(request);
        assertEquals(camera, response.getCamera());
    }
    
    @Test
    public void inexistentCameraTest() {
        CamInfoRequest request = CamInfoRequest.newBuilder().setName(INEXISTENT_NAME).build();
        CamInfoResponse response = frontend.camInfo(request);

        //TODO maybe return Status.INVALID_CAM?
        assertEquals(null, response.getCamera());
    }

}