package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import pt.tecnico.sauron.silo.client.exceptions.UnavailableException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;
import java.util.Properties;


public class BaseIT {

	private static final String TEST_PROP_FILE = "/test.properties";
	protected static Properties testProps;
	protected static SiloServerFrontend frontend;
	
	@BeforeAll
	public static void oneTimeSetup () throws IOException, ZKNamingException, UnavailableException {
		testProps = new Properties();
		
		try {
			testProps.load(BaseIT.class.getResourceAsStream(TEST_PROP_FILE));
			System.out.println("Test properties:");
			System.out.println(testProps);
		}catch (IOException e) {
			final String msg = String.format("Could not load properties file {}", TEST_PROP_FILE);
			System.out.println(msg);
			throw e;
		}

		final String host = testProps.getProperty("server.host");
		final String port = testProps.getProperty("server.port");
		final String instance = testProps.getProperty("instance");
		frontend = new SiloServerFrontend(host, port, instance, 0); // No cache
	}
	
	@AfterAll
	public static void cleanup() {
		frontend.close();
	}

}
