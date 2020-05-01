package pt.tecnico.sauron.silo.client;

import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.*;

import com.google.type.LatLng;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.tecnico.sauron.silo.client.BaseIT;
import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;

public class CamJoinIT extends BaseIT {

    static final String NAME = "test";
    static final String SMALL_NAME = "12";
    static final String BIG_NAME = "abcdefghijlmnopq";
    static final double LON = 1;
    static final double LAT = 1;
    static final LatLng COORDS = LatLng.newBuilder().setLatitude(50).setLongitude(50).build();
    static final LatLng COORDS_NO_LAT = LatLng.newBuilder().setLongitude(50).build();
    static final LatLng COORDS_NO_LON = LatLng.newBuilder().setLatitude(50).build();
    static final double BAD_LON = -200;
    static final double BAD_LAT = -200;

    // initialization and clean-up for each test

    @BeforeEach
    public void setUp() {

    }

    @AfterEach
    public void tearDown() throws ZKNamingException, UnavailableException {
        frontend.controlClear(ControlClearRequest.newBuilder().build());
        frontend.reset();
    }

    // tests

    @Test
    public void nonNullResponse() throws ZKNamingException, UnavailableException {
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinResponse response = frontend.camJoin(request);
        assertNotEquals(null, response, "Response shouldn't be null");
    }

    @Test
    public void okResponse() throws ZKNamingException, UnavailableException {
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        frontend.camJoin(request);
    }

    @Test
    public void duplicateNameTest() throws ZKNamingException, UnavailableException {
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        Camera camera_duplicate = Camera.newBuilder().setName(NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        CamJoinRequest request_duplicate = CamJoinRequest.newBuilder().setCamera(camera_duplicate).build();
        frontend.camJoin(request);

        //Should not throw anything
        frontend.camJoin(request_duplicate);
    }

    @Test
    public void emptyName() {
        Camera camera = Camera.newBuilder().setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();

        assertEquals(INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode());
    }

    @Test
    public void smallNameTest() {
        // Verifies if the name is less than 3 characters long
        Camera camera = Camera.newBuilder().setName(SMALL_NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();

        assertEquals(INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode());
    }

    @Test
    public void bigNameTest() {
        // Verifies if the name is more than 15 characters long
        Camera camera = Camera.newBuilder().setName(BIG_NAME).setCoords(COORDS).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();

        assertEquals(INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode());
    }

    @Test
    public void badLatitudeTest() {
        LatLng coords = LatLng.newBuilder().setLatitude(BAD_LAT).setLongitude(LON).build();
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(coords).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        assertEquals(INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode());
    }

    @Test
    public void badLongitudeTest() {
        LatLng coords = LatLng.newBuilder().setLatitude(LAT).setLongitude(BAD_LON).build();
        Camera camera = Camera.newBuilder().setName(NAME).setCoords(coords).build();
        CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(camera).build();
        assertEquals(INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request)).getStatus().getCode());
    }
}