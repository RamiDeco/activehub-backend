package com.activehub.shared.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
 * La suspension temporal corta la OPERACION, no la sesion.
 *
 * <p>Antes la penalizacion ponia la cuenta en {@code SUSPENDIDO}, que es la condicion con la
 * que {@code iniciarsesion} rechaza: al penalizado se le cerraba el login y no podia ni ver su
 * propia sancion. Decision del usuario: entra, pero no publica ni modifica oferta.
 */
@ExtendWith(MockitoExtension.class)
class PenalizacionVigenteGuardTest {

    /** Un mediodia de Argentina, para que la fecha local no dependa del offset. */
    private static final Instant AHORA = Instant.parse("2026-06-10T15:00:00Z");

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

    @Test
    void sinSuspensionVigente_dejaPasar() {
        UUID id = UUID.randomUUID();
        when(penalizacionRepository.findSuspensionVigente(
                eq(id), eq(TipoPenalizacion.SUSPENSION_TEMPORAL), eq(LocalDate.of(2026, 6, 10))))
                .thenReturn(Optional.empty());

        assertThatCode(() -> guard().exigirSinSuspensionVigente(id, "crear clases"))
                .doesNotThrowAnyException();
    }

    @Test
    void conSuspensionVigente_lanzaSinPermisoConLaFechaDeFin() {
        UUID id = UUID.randomUUID();
        when(penalizacionRepository.findSuspensionVigente(
                eq(id), eq(TipoPenalizacion.SUSPENSION_TEMPORAL), eq(LocalDate.of(2026, 6, 10))))
                .thenReturn(Optional.of(suspensionHasta(LocalDate.of(2026, 6, 30))));

        assertThatThrownBy(() -> guard().exigirSinSuspensionVigente(id, "crear clases"))
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
        when(penalizacionRepository.findSuspensionVigente(
                eq(id), eq(TipoPenalizacion.SUSPENSION_TEMPORAL), eq(LocalDate.of(2026, 6, 10))))
                .thenReturn(Optional.empty());

        guard().exigirSinSuspensionVigente(id, "publicar actividades");
    }
}
