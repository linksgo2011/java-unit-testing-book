package cn.printf.demos.loggable;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HelloServiceTest {
    @Test
    public void should_get_hello_xxx() {
        assertEquals("Hello zhangsan", new HelloService().hello("zhangsan"));
    }
}
