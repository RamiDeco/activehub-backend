package com.activehub.usecases.resolverdenuncia;

import jakarta.validation.constraints.NotBlank;

public record ResolverDenunciaRequest(
        @NotBlank(message = "La acción es obligatoria") String accion
) {
}
