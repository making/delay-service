package am.ik.routeservice;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;

public class AppTest {
	private WebTestClient testClient;

	@Before
	public void setUp() throws Exception {
		RouterFunction<?> routes = App.routes();
		this.testClient = WebTestClient.bindToRouterFunction(routes).build();
	}

	@Test
	public void testGetConf() throws Exception {
		this.testClient.get().uri("/") //
				.exchange() //
				.expectStatus().isOk() //
				.expectBody(String.class)
				.isEqualTo("{\"type\":\"random\",\"delay\":10000}");
	}

	@Test
	public void testSetConfFixed() throws Exception {
		this.testClient.post().uri("/") //
				.contentType(MediaType.APPLICATION_JSON) //
				.syncBody("{\"type\":\"fixed\",\"delay\":5000}") // s
				.exchange() //
				.expectStatus().isOk() //
				.expectBody(String.class)
				.isEqualTo("{\"type\":\"fixed\",\"delay\":5000}");
	}

	@Test
	public void testSetConfRandom() throws Exception {
		this.testClient.post().uri("/") //
				.contentType(MediaType.APPLICATION_JSON) //
				.syncBody("{\"type\":\"random\",\"delay\":5000}") // s
				.exchange() //
				.expectStatus().isOk() //
				.expectBody(String.class)
				.isEqualTo("{\"type\":\"random\",\"delay\":5000}");
	}

	@Test
	public void testSetConfOverMax() throws Exception {
		this.testClient.post().uri("/") //
				.contentType(MediaType.APPLICATION_JSON) //
				.syncBody("{\"type\":\"random\",\"delay\":50000}") // s
				.exchange() //
				.expectStatus().isOk() //
				.expectBody(String.class)
				.isEqualTo("{\"type\":\"random\",\"delay\":10000}");
	}

	@Test
	public void testSetConfUnderMin() throws Exception {
		this.testClient.post().uri("/") //
				.contentType(MediaType.APPLICATION_JSON) //
				.syncBody("{\"type\":\"random\",\"delay\":0}") // s
				.exchange() //
				.expectStatus().isOk() //
				.expectBody(String.class)
				.isEqualTo("{\"type\":\"random\",\"delay\":10000}");
	}
}
