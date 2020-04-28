package pt.tecnico.sauron.silo.client.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.Message;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.Status.Code;
import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.grpc.SauronGrpc;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;


// TODO maybe this class can also manage versions (vector clock)
public class MessageStrategy {
    private final ZKNaming zkNaming;
    private final String path;
    private final String instanceNumber;
    private SauronGrpc.SauronBlockingStub stub;
    private ManagedChannel channel;

    public MessageStrategy(ZKNaming zkNaming, String path, String instanceNumber) throws ZKNamingException {
        this.zkNaming = zkNaming;
        this.path = path;
        this.instanceNumber = instanceNumber;

        channel = ManagedChannelBuilder.forTarget(getPossibleAddresses().get(0).getURI()).usePlaintext().build();
        stub = SauronGrpc.newBlockingStub(channel);
    }


    private List<ZKRecord> getPossibleAddresses() throws ZKNamingException {
         // Initialize stub
         List<ZKRecord> lst;
         if (!instanceNumber.equals("0")) {
             // try only one
             lst = new ArrayList<>();
             lst.add(zkNaming.lookup(path + "/" + instanceNumber));
         } else {
             // try multiple in random order
             lst = new ArrayList<>(zkNaming.listRecords(path));
             Collections.shuffle(lst);
         }
         return lst;
    }

    /**
     * This method has the basic logic for checking all the servers and handling
     * errors If all the servers are unreachable, throws unrecheable too
     * 
     * @param msg
     * @param instanceNumber
     * @param zkNaming
     * @param path
     * @return
     * @throws ZKNamingException
     * @throws StatusRuntimeException
     */
    public Message execute(Request req) throws ZKNamingException, StatusRuntimeException, UnavailableException {

        try {
            return req.call(stub);
        } catch (final StatusRuntimeException e) {
            // If host unreachable just advance. If any other error, throw
            if (e.getStatus().getCode() == Code.UNAVAILABLE) 
                System.out.println("Target is unreachable. Retrying...");
            else
                throw e;
        }

        //Couldn't connect
        //Needs to retry new host
        channel.shutdown(); //close previous channel

        //Get new hosts to try
        List<ZKRecord> lst = getPossibleAddresses();
        
        for (ZKRecord r : lst) {
            //Update connection information
            channel = ManagedChannelBuilder.forTarget(r.getURI()).usePlaintext().build();
            stub = SauronGrpc.newBlockingStub(channel);

            try {
                return req.call(stub);
            } catch (final StatusRuntimeException e) {
                // If host unreachable just advance. If any other error, throw
                if (e.getStatus().getCode() == Code.UNAVAILABLE)
                    System.out.println("Target " + r.getURI() + " is unreachable. Retrying...");
                else
                    throw e;
            }

            //Did not connect, try next one
            channel.shutdown();
        }

        // What? No return yet? Alright then, catch this...
        throw new UnavailableException();
    }

    public void close() {
        channel.shutdown();
    }

}