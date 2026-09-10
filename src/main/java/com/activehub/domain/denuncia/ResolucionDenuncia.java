package com.activehub.domain.denuncia;

/**
 * Como se cerro una denuncia. Vive en el dominio (y no en el slice `resolverdenuncia`, que es
 * de donde salio) porque ahora se persiste en {@code denuncia.resolucion}: el alumno tiene que
 * poder ver el resultado de la suya (E3A-HU11 criterios 2 y 7), no solo que paso a "Resuelta".
 */
public enum ResolucionDenuncia {
    REINTEGRAR,
    SUSPENDER,
    PENALIZAR,
    DESESTIMAR,
    /** Solo para denuncias de resenia (E2I-HU11): la oculta del listado publico. */
    OCULTAR_RESENIA
}
