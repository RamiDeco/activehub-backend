package com.activehub.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.activehub.domain.penalizacion.EstadoPagoPenalizacion;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.time.Zonas;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La suspension temporal y la multa sin pagar cortan la OPERACION, no la sesion.
 *
 * <p>Antes la penalizacion ponia la cuenta en {@code SUSPENDIDO}, que es la condicion con la
 * que {@code iniciarsesion} rechaza: al penalizado se le cerraba el login y no podia ni ver su
 * propia sancion. Decision del usuario: entra, pero no publica ni modifica oferta. Desde V36 la
 * multa tambien bloquea hasta que la administracion aprueba el comprobante.
 */
@ExtendWith(MockitoExtension.class)
class PenalizacionVigenteGuardTest {

    /** Un mediodia de Argentina, para que la fecha local no dependa del offset. */
    private static final Instant AHORA = Instant.parse("2026-06-10T15:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 6, 10);

    @Mock private PenalizacionRepository penalizacionRepository;

    private PenalizacionVigenteGuard guard() {
        return new PenalizacionVigenteGuard(penalizacionRepository, Clock.fixed(AHORA, Zonas.AR));
    }

    private Penalizacion suspensionHasta(LocalDate fin) {
        Penalizacion p = new Penalizacion();
        p.setTipo(TipoPenalizacion.SUSPENSION_TEMPORAL);
        p.setFechaInicio(fin.minusDays(20));
        p.setFechaFin(fin);
        return p;
    }

    private void sinSuspension(UUID id) {
        when(penalizacionRepository.findSuspensionVigente(eq(id), eq(TipoPenalizacion.SUSPENSION_TEMPORAL), eq(HOY)))
                .thenReturn(Optional.empty());
    }

    private void conMultaQueBloquea(UUID id, boolean bloquea) {
        Penalizacion multa = new Penalizacion();
        multa.setTipo(TipoPenalizacion.ECONOMICA);
        multa.setEstadoPago(EstadoPagoPenalizacion.PENDIENTE_PAGO);
        when(penalizacionRepository.findMultaQueBloquea(id, PenalizacionVigenteGuard.MULTA_QUE_BLOQUEA))
                .thenReturn(bloquea ? Optional.of(multa) : Optional.empty());
    }

    @Test
    void sinSancionesVigentes_dejaPasar() {
        UUID id = UUID.randomUUID();
        sinSuspension(id);
        conMultaQueBloquea(id, false);

        assertThatCode(() -> guard().exigirPuedeOperar(id, "crear clases")).doesNotThrowAnyException();
    }

    @Test
    void conSuspensionVigente_lanzaSinPermisoConLaFechaDeFin() {
        UUID id = UUID.randomUUID();
        when(penalizacionRepository.findSuspensionVigente(eq(id), eq(TipoPenalizacion.SUSPENSION_TEMPORAL), eq(HOY)))
                .thenReturn(Optional.of(suspensionHasta(LocalDate.of(2026, 6, 30))));

        assertThatThrownBy(() -> guard().exigirPuedeOperar(id, "crear clases"))
                .isInstanceOf(SinPermisoException.class)
                .hasMessageContaining("2026-06-30")
                .hasMessageContaining("crear clases");
    }

    @Test
    void preguntaPorLaFechaDeHoyEnZonaHorariaDelNegocio() {
        // 15:00 UTC del 10 de junio son las 12:00 del 10 de junio en Argentina. Si la guarda
        // preguntara en UTC o en la zona del servidor, un caso de borde (una suspension que
        // arranca o termina "hoy") se resolveria por el dia equivocado.
        UUID id = UUID.randomUUID();
        sinSuspension(id);
        conMultaQueBloquea(id, false);

        guard().exigirPuedeOperar(id, "publicar actividades");
    }

    /** V36: sin suspension, la multa sin pagar igual frena crear, editar o eliminar oferta. */
    @Test
    void conMultaSinPagar_exigirPuedeOperarLanzaSinPermiso() {
        UUID id = UUID.randomUUID();
        sinSuspension(id);
        conMultaQueBloquea(id, true);

        assertThatThrownBy(() -> guard().exigirPuedeOperar(id, "crear clases"))
                .isInstanceOf(SinPermisoException.class)
                .hasMessageContaining("multa pendiente de pago")
                .hasMessageContaining("crear clases");
    }

    @Test
    void exigirSinMultaPendiente_conMulta_lanzaYSinMulta_dejaPasar() {
        UUID conMulta = UUID.randomUUID();
        UUID sinMulta = UUID.randomUUID();
        conMultaQueBloquea(conMulta, true);
        conMultaQueBloquea(sinMulta, false);

        assertThatThrownBy(() -> guard().exigirSinMultaPendiente(conMulta, "cancelar clases"))
                .isInstanceOf(SinPermisoException.class);
        assertThatCode(() -> guard().exigirSinMultaPendiente(sinMulta, "cancelar clases"))
                .doesNotThrowAnyException();
    }

    /**
     * La multa bloquea pendiente de pago y tambien con el comprobante en revision: el instructor
     * queda bloqueado hasta que el administrador lo desbloquea. Pagada, o anterior a V36 (sin
     * estado), no bloquea.
     */
    @Test
    void bloqueanPendienteYEnRevision_noPagadaNiSinEstado() {
        assertThat(PenalizacionVigenteGuard.MULTA_QUE_BLOQUEA)
                .containsExactlyInAnyOrder(EstadoPagoPenalizacion.PENDIENTE_PAGO, EstadoPagoPenalizacion.EN_REVISION);
        assertThat(EstadoPagoPenalizacion.PAGADA.bloquea()).isFalse();
    }

    @Test
    void suspensionVigente_devuelveLaQueRigeHoy() {
        UUID id = UUID.randomUUID();
        when(penalizacionRepository.findSuspensionVigente(eq(id), eq(TipoPenalizacion.SUSPENSION_TEMPORAL), eq(HOY)))
                .thenReturn(Optional.of(suspensionHasta(LocalDate.of(2026, 6, 30))));

        assertThat(guard().suspensionVigente(id)).map(Penalizacion::getFechaFin).contains(LocalDate.of(2026, 6, 30));
    }
}
