package pt.tecnico.sauron.silo.client.control;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.tecnico.sauron.silo.client.BaseIT;
import pt.tecnico.sauron.silo.grpc.Silo.ControlClearRequest;

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