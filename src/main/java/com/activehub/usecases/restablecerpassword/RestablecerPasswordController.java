package com.activehub.usecases.restablecerpassword;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Público, por la misma razón que el paso anterior: acá todavía no hay sesión. */
@RestController
@RequestMapping("/api/auth/recuperar-password/confirmar")
public class RestablecerPasswordController {

    private final RestablecerPasswordService restablecerPasswordService;

    public RestablecerPasswordController(RestablecerPasswordService restablecerPasswordService) {
        this.restablecerPasswordService = restablecerPasswordService;
    }

    @PostMapping
    public RestablecerPasswordResponse restablecer(@Valid @RequestBody RestablecerPasswordRequest request) {
        return restablecerPasswordService.restablecer(request);
    }
}
