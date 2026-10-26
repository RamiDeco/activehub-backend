package com.activehub.usecases.listarmisdocumentos;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ListarMisDocumentosController {

    private final ListarMisDocumentosService listarMisDocumentosService;

    public ListarMisDocumentosController(ListarMisDocumentosService listarMisDocumentosService) {
        this.listarMisDocumentosService = listarMisDocumentosService;
    }

    @GetMapping("/api/instructor/documentos")
    // RN-16: es "Mis datos", no un módulo con permiso (igual que subirdocumento). El servicio
    // resuelve el PerfilInstructor del que llama; quien no tenga uno recibe 404.
    public List<ListarMisDocumentosResponse> listar(Authentication authentication) {
        return listarMisDocumentosService.listar((UUID) authentication.getPrincipal());
    }
}
