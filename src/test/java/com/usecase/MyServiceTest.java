package com.usecase;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MyServiceTest {
    @Test
    void greet_returns_expected_message() {
        var s = new MyService();
        assertEquals("Hello, World!", s.greet("World"));
    }
}
