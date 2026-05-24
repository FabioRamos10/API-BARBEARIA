package com.example.barbearia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class BarbeariaApplicationTests {

	@Test
	void contextLoads() {
	}

}
