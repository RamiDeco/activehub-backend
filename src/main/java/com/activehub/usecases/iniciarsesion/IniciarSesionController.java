package com.activehub.usecases.iniciarsesion;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/login")
public class IniciarSesionController {

    private final IniciarSesionService iniciarSesionService;

    public IniciarSesionController(IniciarSesionService iniciarSesionService) {
        this.iniciarSesionService = iniciarSesionService;
    }

    @PostMapping
    public ResponseEntity<IniciarSesionResponse> login(@Valid @RequestBody IniciarSesionRequest request) {
        return ResponseEntity.ok(iniciarSesionService.login(request));
    }
}
