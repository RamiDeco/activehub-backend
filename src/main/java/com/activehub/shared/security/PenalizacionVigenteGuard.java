package com.activehub.shared.security;

import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.time.Zonas;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Una Suspension temporal vigente le corta al instructor la operacion, no la sesion.
 *
 * <p><b>Por que no alcanzaba con {@code EstadoUsuario.SUSPENDIDO}.</b> Aplicar la penalizacion
 * ponia la cuenta en SUSPENDIDO, y SUSPENDIDO es la condicion que {@code iniciarsesion} y
 * {@code renovarsesion} usan para rechazar el login. Resultado: al penalizado se le cerraba la
 * puerta de entrada, no podia ver su propia sancion, ni sus clases canceladas, ni sus datos.
 * Decision del usuario: <b>el penalizado inicia sesion, pero no puede operar</b>. Asi que
 * SUSPENDIDO vuelve a significar solo lo que dice {@code actualizarestadousuario} —
 * suspension administrativa de la cuenta — y la penalizacion se hace valer con esta guarda,
 * que mira la vigencia real de la sancion.
 *
 * <p>Se pregunta por fecha de calendario en zona horaria del negocio ({@link Zonas#AR}), no por
 * instante: la vigencia se cargo como dos fechas y "hasta el 20" incluye todo el dia 20.
 *
 * <p>Vive en {@code shared/security} por el mismo motivo que {@link InstructorVerificadoGuard}:
 * es una regla transversal a todos los slices del instructor, no un usecase.
 */
@Component
public class PenalizacionVigenteGuard {

    private final PenalizacionRepository penalizacionRepository;
    private final Clock clock;

    public PenalizacionVigenteGuard(PenalizacionRepository penalizacionRepository, Clock clock) {
        this.penalizacionRepository = penalizacionRepository;
        this.clock = clock;
    }

    /**
     * @param accion se interpola en el mensaje: "No podes {accion} mientras dure la
     *               suspension." Ej: "crear clases", "publicar actividades".
     */
    public void exigirSinSuspensionVigente(UUID usuarioId, String accion) {
        LocalDate hoy = LocalDate.now(clock.withZone(Zonas.AR));
        penalizacionRepository
                .findSuspensionVigente(usuarioId, TipoPenalizacion.SUSPENSION_TEMPORAL, hoy)
                .ifPresent(penalizacion -> {
                    throw new SinPermisoException(
                            "Tenés una suspensión vigente hasta el " + penalizacion.getFechaFin()
                                    + ". No podés " + accion + " hasta que termine.");
                });
    }
}
