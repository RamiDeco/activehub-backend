package com.activehub.domain.actividad;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ActividadSpecifications {

    private ActividadSpecifications() {
    }

    public static Specification<Actividad> conTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String like = "%" + texto.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("nombre")), like);
    }

    public static Specification<Actividad> conCategoria(UUID categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tipoActividad").get("categoria").get("id"), categoriaId);
    }

    public static Specification<Actividad> conTipo(UUID tipoActividadId) {
        if (tipoActividadId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tipoActividad").get("id"), tipoActividadId);
    }

    public static Specification<Actividad> conNivel(NivelIntensidad nivel) {
        if (nivel == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("nivelIntensidad"), nivel);
    }

    public static Specification<Actividad> precioMenorIgual(BigDecimal precioMax) {
        if (precioMax == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("precio"), precioMax);
    }

    public static Specification<Actividad> conInstructor(UUID instructorId) {
        if (instructorId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("instructor").get("id"), instructorId);
    }
}
