package com.activehub.domain.actividad;

// Nombres de constante no-convencionales (Title-Case) a proposito: son identificadores
// Java validos y @Enumerated(STRING) + .name() da el string exacto que espera el frontend
// (activehub-frontend/src/lib/types.ts EstadoClase), sin necesitar un converter.
public enum EstadoClase {
    Programada,
    Habilitada,
    Cancelada,
    Finalizada
}
