package com.activehub.shared.security;

import com.activehub.domain.penalizacion.EstadoPagoPenalizacion;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.time.Zonas;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Las penalizaciones vigentes le cortan al instructor la operacion, no la sesion. Son dos:
 *
 * <ul>
 *   <li><b>Suspension temporal vigente</b> (entre sus fechas y no levantada por un administrador):
 *       no puede crear, editar ni eliminar actividades ni clases ({@link #exigirPuedeOperar}).</li>
 *   <li><b>Multa sin pagar</b> (V36: pendiente de pago o con el comprobante en revision): eso y
 *       ademas todo lo demas que modifica su oferta ({@link #exigirSinMultaPendiente}). Lo unico
 *       que le queda es ver los inscriptos de sus clases y confirmar cobros en efectivo, para
 *       no perjudicar a los alumnos, y la pantalla donde paga la multa.</li>
 * </ul>
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

    /** Los estados de pago en los que una multa bloquea. Null (multas anteriores a V36) no bloquea. */
    public static final Set<EstadoPagoPenalizacion> MULTA_QUE_BLOQUEA =
            EnumSet.of(EstadoPagoPenalizacion.PENDIENTE_PAGO, EstadoPagoPenalizacion.EN_REVISION);

    private final PenalizacionRepository penalizacionRepository;
    private final Clock clock;

    public PenalizacionVigenteGuard(PenalizacionRepository penalizacionRepository, Clock clock) {
        this.penalizacionRepository = penalizacionRepository;
        this.clock = clock;
    }

    /**
     * Crear, editar o eliminar actividades y clases: lo frena una suspension vigente o una multa
     * sin pagar.
     *
     * @param accion se interpola en el mensaje: "No podes {accion} ...". Ej: "crear clases".
     */
    public void exigirPuedeOperar(UUID usuarioId, String accion) {
        suspensionVigente(usuarioId).ifPresent(penalizacion -> {
            throw new SinPermisoException(
                    "Tenés una suspensión vigente hasta el " + penalizacion.getFechaFin()
                            + ". No podés " + accion + " hasta que termine.");
        });
        exigirSinMultaPendiente(usuarioId, accion);
    }

    /** El resto de la operacion del instructor: solo la frena una multa sin pagar (V36). */
    public void exigirSinMultaPendiente(UUID usuarioId, String accion) {
        if (tieneMultaPendiente(usuarioId)) {
            throw new SinPermisoException(
                    "Tenés una multa pendiente de pago. No podés " + accion
                            + " hasta que la administración apruebe tu comprobante.");
        }
    }

    public boolean tieneMultaPendiente(UUID usuarioId) {
        return penalizacionRepository.findMultaQueBloquea(usuarioId, MULTA_QUE_BLOQUEA).isPresent();
    }

    /** La suspension que rige hoy, si la hay. Una levantada por un administrador ya no cuenta. */
    public Optional<Penalizacion> suspensionVigente(UUID usuarioId) {
        LocalDate hoy = LocalDate.now(clock.withZone(Zonas.AR));
        return penalizacionRepository.findSuspensionVigente(usuarioId, TipoPenalizacion.SUSPENSION_TEMPORAL, hoy);
    }
}
