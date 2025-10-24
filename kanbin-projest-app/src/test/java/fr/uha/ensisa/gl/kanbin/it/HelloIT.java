package fr.uha.ensisa.gl.kanbin.it;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HelloIT{
	
	private static String port;
	
	@BeforeAll
	public static void findPort() {
		port = System.getProperty("servlet.port", "8080");
	}

	@Test
	public void hello() throws IOException {
		String testName = "testname";
		HttpURLConnection connection = (HttpURLConnection)new URL("http://localhost:" + port +"/hello?name="+testName).openConnection();
		{
			connection.connect();
			assertEquals(200, connection.getResponseCode());
			
			try (InputStream in = connection.getInputStream()) {
				String output = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
				assertTrue(output.contains(testName), "Sent name not found in page  with source \n" + output);
			}
		}
	}

}
