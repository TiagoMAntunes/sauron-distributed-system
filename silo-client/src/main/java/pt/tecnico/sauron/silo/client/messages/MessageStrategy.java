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

public abstract class MessageStrategy {

    /**
     * This method is what defines the specific message logic and must be
     * reimplemented
     * 
     * @param msg
     * @param stub
     * @return
     * @throws ZKNamingException
     */
    protected abstract Message call(SauronGrpc.SauronBlockingStub stub) throws ZKNamingException;

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
    public Message execute(final String instanceNumber, final ZKNaming zkNaming, final String path)
            throws ZKNamingException, StatusRuntimeException, UnavailableException {

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

        // Check every record, in a non-specific way
        for (final ZKRecord r : lst) {
            final String target = r.getURI();
            final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            final SauronGrpc.SauronBlockingStub stub = SauronGrpc.newBlockingStub(channel);

            Message result = null;
            try {
                result = call(stub);
            } catch (final StatusRuntimeException e) {
                // If host unreachable just advance. If any other error, throw
                if (e.getStatus().getCode() == Code.UNAVAILABLE) {
                    System.out.println("Target " + target + " is unreachable.");
                    continue;
                }
                else
                    throw e;
            } finally {
                channel.shutdown();
            }
            // completed successfully
            return result;

            // TODO maybe change timestamp here?
        }

        // What? No return yet? Alright then, catch this...
        throw new UnavailableException();
    }

}