package com.activehub.usecases.solicitarrecuperacionpassword;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Público: lo pide justamente quien no puede entrar. Sin permiso de módulo detrás, como el
 * resto de los endpoints de {@code /api/auth}.
 */
@RestController
@RequestMapping("/api/auth/recuperar-password")
public class SolicitarRecuperacionPasswordController {

    private final SolicitarRecuperacionPasswordService solicitarRecuperacionPasswordService;

    public SolicitarRecuperacionPasswordController(
            SolicitarRecuperacionPasswordService solicitarRecuperacionPasswordService) {
        this.solicitarRecuperacionPasswordService = solicitarRecuperacionPasswordService;
    }

    @PostMapping
    public SolicitarRecuperacionPasswordResponse solicitar(
            @Valid @RequestBody SolicitarRecuperacionPasswordRequest request) {
        return solicitarRecuperacionPasswordService.solicitar(request);
    }
}
