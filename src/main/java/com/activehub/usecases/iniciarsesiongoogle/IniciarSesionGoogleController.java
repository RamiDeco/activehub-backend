package com.activehub.usecases.iniciarsesiongoogle;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/google")
public class IniciarSesionGoogleController {

    private final IniciarSesionGoogleService iniciarSesionGoogleService;

    public IniciarSesionGoogleController(IniciarSesionGoogleService iniciarSesionGoogleService) {
        this.iniciarSesionGoogleService = iniciarSesionGoogleService;
    }

    @PostMapping
    public IniciarSesionGoogleResponse ingresar(@Valid @RequestBody IniciarSesionGoogleRequest request) {
        return iniciarSesionGoogleService.ingresar(request);
    }

    /**
     * El client id es público por diseño (viaja en el HTML de cualquier sitio que use Google
     * Sign-In). Se sirve desde acá para que el frontend no necesite su propia variable de
     * entorno ni quede desincronizado del backend: si el servidor no tiene Google
     * configurado, `habilitado` viene en false y la pantalla no muestra el botón en vez de
     * ofrecer algo que va a fallar.
     */
    @GetMapping("/config")
    public GoogleConfigResponse config() {
        return iniciarSesionGoogleService.config();
    }
}
