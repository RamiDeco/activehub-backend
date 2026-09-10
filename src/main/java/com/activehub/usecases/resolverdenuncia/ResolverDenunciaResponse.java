package com.activehub.usecases.resolverdenuncia;

import java.util.UUID;

public record ResolverDenunciaResponse(UUID id, String estado, String resolucion) {
}
