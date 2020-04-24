package pt.tecnico.sauron.silo.client.control;

import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.BaseIT;
import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlPingResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class PingIT extends BaseIT {
	
	// initialization and clean-up for each test
	
	@BeforeEach
	public void setUp() {
    
    }
	
	@AfterEach
	public void tearDown() {
        
		
	}
		
	// tests 
	
	@Test
	public void pingOkTest() throws ZKNamingException, UnavailableException {
        ControlPingRequest request = ControlPingRequest.newBuilder().setInputText("friend").build();
        ControlPingResponse response = frontend.controlPing(request);
        assertEquals("Hello friend!", response.getStatus());
    }
    
    @Test
    public void emptyStringTest() {
        ControlPingRequest request = ControlPingRequest.newBuilder().setInputText("").build();
        assertEquals(
            INVALID_ARGUMENT,
            assertThrows(
                StatusRuntimeException.class, () -> frontend.controlPing(request)).getStatus().getCode()
            );
    }

}