package pt.tecnico.sauron.silo.client;

import static io.grpc.Status.Code.ALREADY_EXISTS;
import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.*;

import com.google.type.LatLng;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.tecnico.sauron.silo.client.BaseIT;
import pt.tecnico.sauron.silo.grpc.Silo;
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
    static final LatLng COORDS_NO_LAT = LatLng.newBuilder().setLongitude(50).build();
    static final LatLng COORDS_NO_LON = LatLng.newBuilder().setLatitude(50).build();
	
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
    public void nonNullResponse() {
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinResponse response = frontend.camJoin(request);
        assertNotEquals(null, response, "Response shouldn't be null");
    }
	
	@Test
	public void okResponse() {
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinResponse response = frontend.camJoin(request);
    }
    
    @Test
    public void duplicateNameTest() {
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        Camera camera_duplicate = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinRequest request_duplicate = CamJoinRequest.newBuilder().setCamera(camera_duplicate).build();
        frontend.camJoin(request);

        assertEquals(
                ALREADY_EXISTS,
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.camJoin(request_duplicate)).getStatus().getCode()
        );
    }

    @Test
    public void emptyName() {
        Camera camera = Camera.newBuilder().setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode()
        );
    }

    @Test
    public void smallNameTest() {
        //Verifies if the name is less than 3 characters long
        Camera camera = Camera.newBuilder().setName(SMALL_NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinResponse response = frontend.camJoin(request);

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode()
        );
    }

    @Test
    public void bigNameTest() {
        //Verifies if the name is more than 15 characters long
        Camera camera = Camera.newBuilder().setName(BIG_NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        frontend.camJoin(request);

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode()
        );
    }

    @Test
    public void emptyLat() {
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS_NO_LAT).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode()
        );
    }

    @Test
    public void emptyLon() {
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS_NO_LON).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode()
        );
    }

    @Test
    public void emptyCoords() {
        Camera camera = Camera.newBuilder().setName(NAME).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode()
        );
    }
}