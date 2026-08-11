package com.activehub.usecases.registrarinstructor;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/registro/instructor")
public class RegistrarInstructorController {

    private final RegistrarInstructorService registrarInstructorService;

    public RegistrarInstructorController(RegistrarInstructorService registrarInstructorService) {
        this.registrarInstructorService = registrarInstructorService;
    }

    @PostMapping
    public ResponseEntity<RegistrarInstructorResponse> registrar(@Valid @RequestBody RegistrarInstructorRequest request) {
        RegistrarInstructorResponse response = registrarInstructorService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
