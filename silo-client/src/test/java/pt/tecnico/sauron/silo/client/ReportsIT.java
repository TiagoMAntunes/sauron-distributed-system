package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.type.LatLng;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.grpc.StatusRuntimeException;

import pt.tecnico.sauron.silo.grpc.Silo;
import pt.tecnico.sauron.silo.grpc.Silo.Status;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;
import pt.tecnico.sauron.silo.grpc.Silo.Status;



public class ReportsIT extends BaseIT {

    private final String VALID_CAM_NAME = "ValidCam";
    private final String INVALID_CAM_NAME = "InvalidCam";
    private final double CAM_LATITUDE = 37.14360;
    private final double CAM_LONGITUDE = -115.482399;
    private final LatLng CAM_COORDS = LatLng.newBuilder().
            setLatitude(CAM_LATITUDE).
            setLongitude(CAM_LONGITUDE).
            build();

    private final String CAR_TYPE = "CAR";
    private final String CAR_ID = "AA00AA";
    private final Observable CAR_OBSERVABLE = Observable.newBuilder().
            setType(CAR_TYPE).
            setIdentifier(CAR_ID).
            build();

    private final Observation VALID_CAR_OBSERVATION = Observation.newBuilder().
            setObservated(CAR_OBSERVABLE).
            setTime(fromMillis(currentTimeMillis())).
            build();

    private final String PERSON_TYPE = "PERSON";
    private final String PERSON_ID = "14388236";
    private final Observable PERSON_OBSERVABLE = Observable.newBuilder().
            setType(PERSON_TYPE).
            setIdentifier(PERSON_ID).
            build();

    private final Observation VALID_PERSON_OBSERVATION = Observation.newBuilder().
            setObservated(PERSON_OBSERVABLE).
            setTime(fromMillis(currentTimeMillis())).
            build();

    private final Observation NULL_OBSERVATION = null;


    private final Observable BAD_OBSERVABLE = Observable.newBuilder().
            setType(PERSON_TYPE).
            setIdentifier(CAR_ID).
            build();

    private final Observation INVALID_OBSERVATION = Observation.newBuilder().
            setObservated(BAD_OBSERVABLE).
            setTime(fromMillis(currentTimeMillis())).
            build();

    private final Camera CAMERA = Camera.newBuilder().
            setName(VALID_CAM_NAME).
            setCoords(CAM_COORDS).
            build();
    // one-time initialization and clean-up

    @BeforeAll
    public static void oneTimeSetUp(){
        //No need for general setup
    }

    @AfterAll
    public static void oneTimeTearDown() {
        //No need for general tear down
    }

    // initialization and clean-up for each test

    @BeforeEach
    public void setUp() {
        //Set up a valid camera for each test
        CamJoinRequest camReq = CamJoinRequest.newBuilder().
                setCamera(CAMERA).
                build();

        CamJoinResponse camRes = frontend.camJoin(camReq);
    }

    @AfterEach
    public void tearDown() {
        //Clean server state after each test
        frontend.controlClear(Silo.ControlClearRequest.newBuilder().build());
    }

    // tests

    @Test
    public void reportOkTest() {
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName(VALID_CAM_NAME).
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();

        ReportResponse response = frontend.reports(request);

        assertEquals(Status.OK, response.getResponseStatus());
    }

    @Test
    public void reportNonExistantCameraTest() {
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName(INVALID_CAM_NAME).
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();


        assertEquals(
            INVALID_ARGUMENT,
            assertThrows(StatusRuntimeException.class, () -> frontend.reports(request)).getStatus().getCode());
    }

    @Test
    public void reportEmptyCameraNameTest() {
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName("").
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.reports(request)).getStatus().getCode());
    }

    @Test
    public void reportNoCameraNameTest() {
        ReportRequest request = ReportRequest.newBuilder().
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.reports(request)).getStatus().getCode());
    }

    @Test
    public void reportEmptySetOfObservationsTest() {
        //Set is empty because no observation is added
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName(VALID_CAM_NAME).
                build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.reports(request)).getStatus().getCode());
    }

    /* @Test
    public void reportInvalidObservationData() {
        //Invalid observation consisting on having an ID that doesn't match type
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName(VALID_CAM_NAME).addObservations(INVALID_OBSERVATION).
                build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.reports(request)).getStatus().getCode());
    } */
}
