package com.jeevan.TradingApp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HomeController {
    @GetMapping("/home")
    public String Home(){
        return "welcome to TradingApp";
    }
    @GetMapping("/api")
    public String secure(){
        return "this is secure endpoint";
    }
}
