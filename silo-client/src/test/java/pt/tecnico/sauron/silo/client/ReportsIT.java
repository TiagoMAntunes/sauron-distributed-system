package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.*;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.google.protobuf.util.Timestamps.fromMillis;

import pt.tecnico.sauron.silo.grpc.Silo;
import pt.tecnico.sauron.silo.grpc.Silo.Status;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;


import pt.tecnico.sauron.silo.grpc.Silo.Status;

import java.util.ArrayList;
import java.util.List;

public class ReportsIT extends BaseIT {

    private final String VALID_CAM_NAME = "ValidCam";
    private final String INVALID_CAM_NAME = "InvalidCam";
    private final double CAM_LATITUDE = 37.14360;
    private final double CAM_LONGITUDE = -115.482399;

    private final String CAR_TYPE = "CAR";
    private final String CAR_ID = "AA00AA";
    private final Observation VALID_CAR_OBSERVATION = Observation.newBuilder().
            setType(CAR_TYPE).
            setIdentifier(CAR_ID).
            setTime(fromMillis(currentTimeMillis())).
            build();

    private final String PERSON_TYPE = "PERSON";
    private final String PERSON_ID = "14388236";
    private final Observation VALID_PERSON_OBSERVATION = Observation.newBuilder().
            setType(PERSON_TYPE).
            setIdentifier(PERSON_ID).
            setTime(fromMillis(currentTimeMillis())).
            build();

    private final Observation NULL_OBSERVATION = null;

    private final Observation INVALID_OBSERVATION = Observation.newBuilder().
            setType(PERSON_TYPE).
            setIdentifier(CAR_ID).
            setTime(fromMillis(currentTimeMillis())).
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
                setName(VALID_CAM_NAME).
                setLat(CAM_LATITUDE).
                setLon(CAM_LONGITUDE).
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
                setName(VALID_CAM_NAME).
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();

        ReportResponse response = frontend.reports(request);

        assertEquals(Status.OK, response.getResponseStatus());
    }

    @Test
    public void reportNonExistantCameraTest() {
        ReportRequest request = ReportRequest.newBuilder().
                setName(INVALID_CAM_NAME).
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();

        ReportResponse response = frontend.reports(request);

        assertEquals(Status.NOK, response.getResponseStatus());
    }

    @Test
    public void reportNoCameraNameTest() {
        ReportRequest request = ReportRequest.newBuilder().
                setName("").
                addObservations(VALID_CAR_OBSERVATION).addObservations(VALID_PERSON_OBSERVATION).
                build();

        ReportResponse response = frontend.reports(request);

        assertEquals(Status.NOK, response.getResponseStatus());
    }

    @Test
    public void reportNullSetOfObservationsTest() {
        ReportRequest request = ReportRequest.newBuilder().
                setName(VALID_CAM_NAME).
                addObservations(NULL_OBSERVATION).
                build();

        ReportResponse response = frontend.reports(request);

        assertEquals(Status.NOK, response.getResponseStatus());
    }

    @Test
    public void reportEmptySetOfObservationsTest() {
        //Set is empty because no observation is added
        ReportRequest request = ReportRequest.newBuilder().
                setName(VALID_CAM_NAME).
                build();

        ReportResponse response = frontend.reports(request);

        assertEquals(Status.NOK, response.getResponseStatus());
    }

    @Test
    public void reportInvalidObservationData() {
        //Invalid observation consisting on having an ID that doesn't match type
        ReportRequest request = ReportRequest.newBuilder().
                setName(VALID_CAM_NAME).addObservations(INVALID_OBSERVATION).
                build();

        ReportResponse response = frontend.reports(request);

        assertEquals(Status.NOK, response.getResponseStatus());
    }
}
