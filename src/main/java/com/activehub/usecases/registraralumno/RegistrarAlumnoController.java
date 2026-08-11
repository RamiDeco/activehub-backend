package com.activehub.usecases.registraralumno;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/registro/alumno")
public class RegistrarAlumnoController {

    private final RegistrarAlumnoService registrarAlumnoService;

    public RegistrarAlumnoController(RegistrarAlumnoService registrarAlumnoService) {
        this.registrarAlumnoService = registrarAlumnoService;
    }

    @PostMapping
    public ResponseEntity<RegistrarAlumnoResponse> registrar(@Valid @RequestBody RegistrarAlumnoRequest request) {
        RegistrarAlumnoResponse response = registrarAlumnoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
