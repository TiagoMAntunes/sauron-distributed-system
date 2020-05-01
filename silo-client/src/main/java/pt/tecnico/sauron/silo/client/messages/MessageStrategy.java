package pt.tecnico.sauron.silo.client.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

public class MessageStrategy {
    private final ZKNaming zkNaming;
    private final String path;
    private final String instanceNumber;
    private SauronGrpc.SauronBlockingStub stub;
    private ManagedChannel channel;
    private Clock timestamp;
    private Cache cache;

    public MessageStrategy(ZKNaming zkNaming, String path, String instanceNumber) throws ZKNamingException, UnavailableException {
        this.zkNaming = zkNaming;
        this.path = path;
        this.instanceNumber = instanceNumber;
        this.timestamp = new Clock(9); // TODO should this be hard-coded?
        this.cache = new Cache(3); // TODO Change this value
        try {
            channel = ManagedChannelBuilder.forTarget(getPossibleAddresses().get(0).getURI()).usePlaintext().build();
        } catch (IndexOutOfBoundsException e) {
            System.out.println("No servers available!");
            throw new UnavailableException();
        }
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
        System.out.println("Prev timestamp: " + timestamp.getList());

        int tries = 5; // 5 times looks like a good number

        do {
            try {
                Message res =  req.call(stub, timestamp);
                System.out.println(" New timestamp: " + timestamp.getList());
                return handleCache(req, res);
            } catch (final StatusRuntimeException e) {
                // If host unreachable just advance. If any other error, throw
                if (e.getStatus().getCode() == Code.UNAVAILABLE) {
                    System.out.println("Target is unreachable. Retrying...");
                    tries--;
                    try {
                        Thread.sleep( (long) (new Random().nextInt(200)) * (6-tries)); //backoff
                    } catch (InterruptedException ex) {
                        // Why is this happening?
                        System.out.println(ex.getMessage());
                    }
                } else
                    throw e;
            }
        } while (tries > 0);

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
                Message res =  req.call(stub, timestamp);
                System.out.println("Received response with timestamp: " + timestamp.getList());
                return handleCache(req, res);
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

    /**
     * This function handles cache management.
     * It decides wheter to get an element from cache or just return it normally
     * @param req
     * @param res
     */
    public Message handleCache(Request req, Message res) {
        if (this.timestamp.shouldCache()) {
            return this.cache.getValue(req, res);
        } else {
            this.cache.insertReqRes(req, res); // insert the element in case it is new
            return res;
        }
    }

    public void close() {
        channel.shutdown();
    }

    public void reset() {
        this.timestamp.reset();
        this.cache.reset();
    }

}