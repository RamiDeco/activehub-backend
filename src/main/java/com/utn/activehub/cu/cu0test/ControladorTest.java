package com.utn.activehub.cu.cu0test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorTest {

    @GetMapping("/")
    public String helloWorld() {
        return "Hola ActiveHubPibes! esto no fue generado por claude";
    }

}
