package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.type.LatLng;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.grpc.Silo;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.Observable;



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

    private final CamJoinRequest CAM_REQUEST = CamJoinRequest.newBuilder().
                setCamera(CAMERA)
                .build();
    // one-time initialization and clean-up

    // initialization and clean-up for each test

    @BeforeEach
    public void setUp() throws ZKNamingException, UnavailableException {
        //Set up a valid camera for each test
        CamJoinRequest camReq = CamJoinRequest.newBuilder().
                setCamera(CAMERA).
                build();

        frontend.camJoin(camReq);
    }

    @AfterEach
    public void tearDown() throws ZKNamingException, UnavailableException {
        //Clean server state after each test
        frontend.controlClear(Silo.ControlClearRequest.newBuilder().build());
        frontend.reset();
    }

    // tests

    @Test
    public void reportOkTest() throws ZKNamingException, UnavailableException {
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName(VALID_CAM_NAME).
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();

        frontend.reports(request, CAM_REQUEST);

    }

    @Test
    public void reportNonExistantCameraTest() {
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName(INVALID_CAM_NAME).
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();


        assertEquals(
            INVALID_ARGUMENT,
            assertThrows(StatusRuntimeException.class, () -> frontend.reports(request, CAM_REQUEST)).getStatus().getCode());
    }

    @Test
    public void reportEmptyCameraNameTest() {
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName("").
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.reports(request, CAM_REQUEST)).getStatus().getCode());
    }

    @Test
    public void reportNoCameraNameTest() {
        ReportRequest request = ReportRequest.newBuilder().
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.reports(request, CAM_REQUEST)).getStatus().getCode());
    }

    @Test
    public void reportEmptySetOfObservationsTest() {
        //Set is empty because no observation is added
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName(VALID_CAM_NAME).
                build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.reports(request, CAM_REQUEST)).getStatus().getCode());
    }

     @Test
    public void reportInvalidObservationData() {
        //Invalid observation consisting on having an ID that doesn't match type
        ReportRequest request = ReportRequest.newBuilder().
                setCameraName(VALID_CAM_NAME).addObservations(INVALID_OBSERVATION).
                build();

        assertEquals(
                INVALID_ARGUMENT,
                assertThrows(StatusRuntimeException.class, () -> frontend.reports(request, CAM_REQUEST)).getStatus().getCode());
    } 
}
