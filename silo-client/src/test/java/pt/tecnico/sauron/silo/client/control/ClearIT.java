package pt.tecnico.sauron.silo.client.control;

import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.BaseIT;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearResponse;
import pt.tecnico.sauron.silo.grpc.Silo.Status;

public class ClearIT extends BaseIT {
	
	// initialization and clean-up for each test
	
	@BeforeEach
	public void setUp() {
    
    }
	
	@AfterEach
	public void tearDown() {
        
		
	}
		
	// tests 
	
	@Test
	public void clearOkTest() {
        ControlClearRequest request = ControlClearRequest.newBuilder().build();
        frontend.controlClear(request);
    }

}