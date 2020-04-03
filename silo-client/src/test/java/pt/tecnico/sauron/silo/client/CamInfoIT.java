package pt.tecnico.sauron.silo.client;

import static io.grpc.Status.Code.FAILED_PRECONDITION;
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
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;

public class CamInfoIT extends BaseIT {

    static final String NAME = "test";
    static final String INV_NAME = "inexistent";
    static final LatLng COORDS = LatLng.newBuilder().setLatitude(50).setLongitude(50).build();
    static final Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();

    // initialization and clean-up for each test

    @BeforeEach
	public void setUp() {
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        frontend.camJoin(request);
    }
	
	@AfterEach
	public void tearDown() {
        frontend.controlClear(ControlClearRequest.newBuilder().build());
	}
		
	// tests

    @Test
    public void nonNullResponse() {
        CamInfoRequest request = Silo.CamInfoRequest.newBuilder().setName(NAME).build();
        CamInfoResponse response = frontend.camInfo(request);
        assertNotEquals(null, response, "Response shouldn't be null");
    }

	@Test
	public void okResponse() {
        CamInfoRequest request = CamInfoRequest.newBuilder().setName(NAME).build();
        CamInfoResponse response = frontend.camInfo(request);

        assertEquals(camera, response.getCamera());
    }

    @Test
    public void noMatch() {
        //load data first
        CamInfoRequest request = CamInfoRequest.newBuilder().setName(INV_NAME).build();

        assertEquals(
                FAILED_PRECONDITION,
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.camInfo(request)).getStatus().getCode()
        );
    }

    @Test
    public void emptyName() {
        CamInfoRequest request = CamInfoRequest.newBuilder().build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(
                        StatusRuntimeException.class, () -> frontend.camInfo(request)).getStatus().getCode()
        );
    }




}