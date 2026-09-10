package com.activehub.domain.actividad;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
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

    public static Specification<Actividad> conNivel(UUID nivelIntensidadId) {
        if (nivelIntensidadId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("nivelIntensidad").get("id"), nivelIntensidadId);
    }

    public static Specification<Actividad> precioMenorIgual(BigDecimal precioMax) {
        if (precioMax == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("precio"), precioMax);
    }

    /**
     * Solo actividades cuyo instructor tiene el perfil APROBADO.
     *
     * <p>RN-16: un instructor no verificado no puede tener oferta publicada. Sin esto, al
     * rechazar a un instructor que ya habia sido aprobado sus actividades seguian en el
     * catalogo publico y aceptando inscripciones: el rechazo no revocaba nada.
     */
    public static Specification<Actividad> deInstructorVerificado() {
        return (root, query, cb) -> {
            var sub = query.subquery(UUID.class);
            var perfil = sub.from(PerfilInstructor.class);
            sub.select(perfil.get("usuario").get("id"))
                    .where(cb.and(
                            cb.equal(perfil.get("usuario").get("id"), root.get("instructor").get("id")),
                            cb.equal(perfil.get("estadoVerificacion"), EstadoVerificacion.APROBADO)));
            return cb.exists(sub);
        };
    }

    public static Specification<Actividad> conInstructor(UUID instructorId) {
        if (instructorId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("instructor").get("id"), instructorId);
    }
}
