package com.activehub.domain.actividad;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructor;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ActividadSpecifications {

    private ActividadSpecifications() {
    }

    /**
     * Las vocales acentuadas y la ñ tal como pueden estar guardadas, y su equivalente plano.
     * Las dos cadenas tienen que quedar del mismo largo: {@code translate()} mapea posición a
     * posición y lo que sobre en la primera se elimina del texto en vez de reemplazarse.
     */
    private static final String ACENTOS = "áàäâãéèëêíìïîóòöôõúùüûñçÁÀÄÂÃÉÈËÊÍÌÏÎÓÒÖÔÕÚÙÜÛÑÇ";
    private static final String SIN_ACENTOS = "aaaaaeeeeiiiiooooouuuuncAAAAAEEEEIIIIOOOOOUUUUNC";

    /**
     * Busca por nombre <b>ignorando tildes y mayúsculas</b>: "natacion" tiene que encontrar
     * "Natación". Comparar con {@code lower()} a secas no alcanzaba — para SQL 'ó' y 'o' son
     * caracteres distintos —, y en castellano casi toda palabra larga lleva tilde, así que
     * escribir sin acento (lo normal al tipear rápido) no traía nada.
     *
     * <p>Se usa {@code translate()} y no la extensión {@code unaccent}: no hace falta instalar
     * nada en la base, que es de Supabase y no conviene tocarle las extensiones desde una
     * migración. El espejo del lado del cliente es {@code lib/texto.ts}.
     */
    public static Specification<Actividad> conTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String like = "%" + sinAcentos(texto.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.like(
                cb.function("translate", String.class,
                        cb.lower(root.get("nombre")), cb.literal(ACENTOS), cb.literal(SIN_ACENTOS)),
                like);
    }

    /** El mismo aplanado que hace {@code translate()} en la base, para el término buscado. */
    private static String sinAcentos(String texto) {
        StringBuilder salida = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            int i = ACENTOS.indexOf(c);
            salida.append(i >= 0 ? SIN_ACENTOS.charAt(i) : c);
        }
        return salida.toString();
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
