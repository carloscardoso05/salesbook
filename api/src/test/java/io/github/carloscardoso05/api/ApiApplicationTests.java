package io.github.carloscardoso05.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testFails() {
        throws new RuntimeException("This test is designed to fail");
    }
}
