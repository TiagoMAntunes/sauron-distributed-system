package pt.tecnico.sauron.silo.client.messages;

import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.ReportRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ReportResponse;
import pt.tecnico.sauron.silo.grpc.Silo.VectorClock;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.ArrayList;
import java.util.Collections;

import com.google.protobuf.Message;

public class ReportMessage implements Request {

    private ReportRequest req;
    private CamJoinRequest camReq; // TODO: recover from camera not propagated fault
    public ReportMessage(ReportRequest req, CamJoinRequest camReq) {
        this.req = req;
        this.camReq = camReq;
    }

    public Message call(SauronGrpc.SauronBlockingStub stub, Clock timestamp) throws ZKNamingException { 
        ReportRequest request = ReportRequest.newBuilder().setCameraName(req.getCameraName()).addAllObservations(req.getObservationsList()).setPrev(VectorClock.newBuilder().addAllUpdates(timestamp.getList()).build()).build();
        ReportResponse response ;
        try {
            response = stub.report(request);
        } catch (RuntimeException e) {
            //This happens because replica crashed before communicating the join
            //Create a new camjoin
            ArrayList<Integer> newLst = new ArrayList<>(Collections.nCopies(timestamp.getList().size(), 0));
            CamJoinRequest newCamReq = CamJoinRequest.newBuilder().setCamera(camReq.getCamera()).setPrev(VectorClock.newBuilder().addAllUpdates(newLst).build()).build();
            CamJoinResponse camRes = stub.camJoin(newCamReq);
            //Add new join timestamp to client
            timestamp.reset();
            timestamp.update(camRes.getNew().getUpdatesList());
            //Make report
            request = ReportRequest.newBuilder().setCameraName(req.getCameraName()).addAllObservations(req.getObservationsList()).setPrev(VectorClock.newBuilder().addAllUpdates(timestamp.getList()).build()).build();
            response = stub.report(request);
        }
        timestamp.update(response.getNew().getUpdatesList());
        return response;
    }
}
