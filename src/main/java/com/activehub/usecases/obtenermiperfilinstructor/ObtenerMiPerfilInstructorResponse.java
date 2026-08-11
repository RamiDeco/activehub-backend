package com.activehub.usecases.obtenermiperfilinstructor;

public record ObtenerMiPerfilInstructorResponse(
        String especialidad,
        Integer aniosExperiencia,
        String descripcion,
        String estadoVerificacion,
        String motivoRechazo
) {
}
