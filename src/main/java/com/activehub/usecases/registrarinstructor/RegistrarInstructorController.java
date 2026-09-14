package com.activehub.usecases.registrarinstructor;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth/registro/instructor")
public class RegistrarInstructorController {

    private final RegistrarInstructorService registrarInstructorService;

    public RegistrarInstructorController(RegistrarInstructorService registrarInstructorService) {
        this.registrarInstructorService = registrarInstructorService;
    }

    /**
     * El alta viaja como multipart: los datos en la parte "datos" (JSON) y la documentacion
     * en la parte "documentos". Van juntos a proposito — la cuenta no puede crearse sin la
     * documentacion (E1A-HU04 criterio 9, RN-12).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RegistrarInstructorResponse> registrar(
            @Valid @RequestPart("datos") RegistrarInstructorRequest datos,
            @RequestPart("documentos") List<MultipartFile> documentos
    ) {
        RegistrarInstructorResponse response = registrarInstructorService.registrar(datos, documentos);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
