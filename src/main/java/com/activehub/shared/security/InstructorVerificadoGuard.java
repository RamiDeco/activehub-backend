package com.activehub.shared.security;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * RN-16: un instructor no verificado no puede publicar actividades ni gestionar clases, y
 * el control se hace en el backend, no ocultando botones.
 *
 * <p>Vive en {@code shared/security} y no en un usecase: es una regla transversal que
 * necesitan todos los slices del instructor. La convencion de no inyectar el Service de un
 * usecase dentro de otro sigue valiendo; esto no es un usecase, es una guarda de seguridad.
 *
 * <p>Hasta que existio esta clase la validacion estaba copiada en solo 3 de los 11 casos de
 * uso del instructor, asi que un perfil Pendiente o Rechazado podia cancelar clases,
 * confirmar cobros o ver el roster llamando directo a la API.
 */
@Component
public class InstructorVerificadoGuard {

    private final PerfilInstructorRepository perfilInstructorRepository;

    public InstructorVerificadoGuard(PerfilInstructorRepository perfilInstructorRepository) {
        this.perfilInstructorRepository = perfilInstructorRepository;
    }

    /**
     * @param accion se interpola en el mensaje de error: "No podés {accion} hasta que un
     *               administrador lo valide." Ej: "cancelar clases", "confirmar cobros".
     */
    public void exigirVerificado(UUID instructorId, String accion) {
        if (!estaVerificado(instructorId)) {
            throw new SinPermisoException(
                    "Tu perfil de instructor todavía no fue aprobado. No podés " + accion
                            + " hasta que un administrador lo valide.");
        }
    }

    public boolean estaVerificado(UUID instructorId) {
        return perfilInstructorRepository.findByUsuarioId(instructorId)
                .map(p -> p.getEstadoVerificacion() == EstadoVerificacion.APROBADO)
                .orElse(false);
    }

    /**
     * Version orientada al alumno: la oferta de un instructor no verificado no se puede
     * consumir. El mensaje no menciona el estado del instructor — no es asunto del alumno.
     */
    public void exigirOfertaVigente(UUID instructorId) {
        if (!estaVerificado(instructorId)) {
            throw new ValidacionException("Esta actividad no está disponible en este momento.");
        }
    }
}
