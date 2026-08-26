package com.travelmate.backend;

import com.travelmate.TravelMateApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = TravelMateApplication.class,
        properties = "JWT_SECRET=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
