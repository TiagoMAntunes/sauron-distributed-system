package pt.tecnico.sauron.silo.client.control;

import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.print.attribute.standard.MediaSize.NA;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.BaseIT;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.Silo.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Status;

public class CamJoinIT extends BaseIT {

    static final String NAME = "teste";
    static final double LON = 1;
    static final double LAT = 1;
	
	// initialization and clean-up for each test
	
	@BeforeEach
	public void setUp() {
    
    }
	
	@AfterEach
	public void tearDown() {
        
		
	}
		
	// tests 
	
	@Test
	public void camJoinTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(NAME).setLon(LON).setLat(LAT).build();
        CamJoinResponse response = frontend.camJoin(request);
        assertEquals(Status.OK, response.getResponseStatus());
    }
    
    @Test
    public void duplicateNameTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder().setName(NAME).setLon(LON).setLat(LAT).build();
        CamJoinRequest request_duplicate = CamJoinRequest.newBuilder().setName(NAME).setLon(LON).setLat(LAT).build();
        CamJoinResponse response = frontend.camJoin(request);
        CamJoinResponse response_duplicate = frontend.camJoin(request_duplicate);

        assertEquals(Status.NOK, response_duplicate.getResponseStatus());
    }

}