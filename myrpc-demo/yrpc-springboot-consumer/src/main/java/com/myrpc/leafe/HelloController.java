package com.myrpc.leafe;

import com.myrpc.leafe.bootatrap.annotaion.MyrpcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @MyrpcService
    private GreetingService greetingService;
    @GetMapping("/hello")
    public String hello() {
        return greetingService.hello("leafe");
    }
}
