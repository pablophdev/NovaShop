package com.pabloph.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"GATEWAY_USER=test-user",
		"GATEWAY_PASSWORD=test-password"
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
