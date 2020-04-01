package pt.tecnico.sauron.silo;

import pt.tecnico.sauron.silo.domain.CameraDomain;
import pt.tecnico.sauron.silo.grpc.Silo.Camera;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlInitResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.Silo.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.Silo.Status;
import pt.tecnico.sauron.silo.grpc.Silo.Observation;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.domain.SiloServer;
import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.domain.Registry;

import static io.grpc.Status.INVALID_ARGUMENT;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Timestamp;

import io.grpc.stub.StreamObserver;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;
import com.google.type.LatLng;


public class SiloServerImpl extends SauronGrpc.SauronImplBase {

    private final SiloServer silo = new SiloServer();

    @Override
    public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
        String camName = request.getCamera().getName();
        LatLng camCoords = request.getCamera().getCoords();


        CamJoinResponse response;
        if (camName == null) {
            response = CamJoinResponse.newBuilder().setResponseStatus(Status.INVALID_ARG).build();
        } else if (camName.equals("") || silo.cameraExists(camName)) {
            response = CamJoinResponse.newBuilder().setResponseStatus(Status.INVALID_ARG).build();
        } else if (camName.length() < 3 || camName.length() > 15 ) {
            response = CamJoinResponse.newBuilder().setResponseStatus(Status.INVALID_ARG).build();
        } else if (camCoords.getLatitude() == 0.0 || camCoords.getLongitude() == 0.0) {
            response = CamJoinResponse.newBuilder().setResponseStatus(Status.NULL_COORDS).build();
        } else {
            CameraDomain newCam = new CameraDomain(camName, camCoords);
            silo.addCamera(camName, newCam);
            response = CamJoinResponse.newBuilder().setResponseStatus(Status.OK).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {
        String camName = request.getName();
        CamInfoResponse response;
        if (camName == null) {
            response = CamInfoResponse.newBuilder().setResponseStatus(Status.INVALID_ARG).build();
        } else if (camName.equals("") || !silo.cameraExists(camName)) {
            response = CamInfoResponse.newBuilder().setResponseStatus(Status.INVALID_ARG).build();
        } else {
            CameraDomain camDom = silo.getCamera(camName);
            Camera cam = Camera.newBuilder().setCoords(camDom.getCoords()).setName(camDom.getName()).build();
            response = CamInfoResponse.newBuilder().setCamera(cam).setResponseStatus(Status.OK).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        
        String camName = request.getCameraName();
        List<Observation> observations = request.getObservationsList();
        
        ReportResponse response;
        if (camName == null) {
            response = ReportResponse.newBuilder().setResponseStatus(Status.INVALID_ARG).build();
        } else if (camName.equals("") || !silo.cameraExists(camName)) {
            response = ReportResponse.newBuilder().setResponseStatus(Status.INVALID_ARG).build();
        } else if (observations == null) {
            response = ReportResponse.newBuilder().setResponseStatus(Status.NULL_OBS).build();
        } else if (observations.isEmpty()) {
            response = ReportResponse.newBuilder().setResponseStatus(Status.INVALID_ARG).build();
        } else {
            //Transform into registries
            
            ArrayList<Registry> list = new ArrayList<>();
            for (Observation o : observations) {
                Camera cam = o.getCamera();
                String type = o.getObservated().getType();
                String id = o.getObservated().getIdentifier();
                Timestamp time = fromMillis(currentTimeMillis());
                Registry r = new Registry(cam, type, id, time); //TODO validate created entity
                list.add(r);
            }
            silo.addRegistries(list);
            
            response = ReportResponse.newBuilder().setResponseStatus(Status.OK).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void controlPing(ControlPingRequest request, StreamObserver<ControlPingResponse> responseObserver) {
        String inputText = request.getInputText();

        if (inputText == null || inputText.isBlank()) 
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty!").asRuntimeException());

        ControlPingResponse response = ControlPingResponse.newBuilder().setStatus("Hello " + inputText + "!").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void controlClear(ControlClearRequest request, StreamObserver<ControlClearResponse> responseObserver) {
        Status status = silo.clear() ? Status.OK : Status.NOK; //change accordingly
        
        ControlClearResponse response = ControlClearResponse.newBuilder().setStatus(status).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void controlInit(ControlInitRequest request, StreamObserver<ControlInitResponse> responseObserver) {
        List<Observation> observations =  request.getObservationList();
        
        for(Observation o : observations){
            Registry r = new Registry(o.getCamera(),
                o.getObservated().getType(),
                o.getObservated().getIdentifier(),
                o.getTime());
            silo.inputRegistry(o.getObservated().getIdentifier(), r);
        }

        ControlInitResponse response = ControlInitResponse.newBuilder().setResponseStatus(Status.OK).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}