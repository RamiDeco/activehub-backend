package com.activehub.usecases.listarmissanciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.penalizacion.ComprobantePenalizacion;
import com.activehub.domain.penalizacion.ComprobantePenalizacionRepository;
import com.activehub.domain.penalizacion.EstadoComprobantePenalizacion;
import com.activehub.domain.penalizacion.EstadoPagoPenalizacion;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.time.Zonas;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** V36: lo que ve el instructor en "Pago de penalización". */
@ExtendWith(MockitoExtension.class)
class ListarMisSancionesServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");

    @Mock private PenalizacionRepository penalizacionRepository;
    @Mock private ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    @Mock private PenalizacionVigenteGuard penalizacionVigenteGuard;

    private final UUID usuarioId = UUID.randomUUID();

    private ListarMisSancionesService service(String alias, String cbu, String titular) {
        return new ListarMisSancionesService(
                penalizacionRepository, comprobantePenalizacionRepository, penalizacionVigenteGuard,
                Clock.fixed(AHORA, Zonas.AR), alias, cbu, titular);
    }

    private Penalizacion multa() {
        Penalizacion p = new Penalizacion();
        p.setTipo(TipoPenalizacion.ECONOMICA);
        p.setMotivo("No se presentó");
        p.setMonto(new BigDecimal("5000"));
        p.setEstadoPago(EstadoPagoPenalizacion.PENDIENTE_PAGO);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        return p;
    }

    @Test
    void listar_devuelveLaMultaConSuEstadoSusComprobantesYLosDatosParaTransferir() {
        Penalizacion multa = multa();
        ComprobantePenalizacion invalido = new ComprobantePenalizacion();
        invalido.setPenalizacion(multa);
        invalido.setNombreArchivo("foto.jpg");
        invalido.setEstado(EstadoComprobantePenalizacion.INVALIDO);
        invalido.setMotivoInvalidez("No se lee el monto");
        when(penalizacionRepository.findDeUsuario(usuarioId)).thenReturn(List.of(multa));
        when(comprobantePenalizacionRepository.findDePenalizaciones(List.of(multa.getId()))).thenReturn(List.of(invalido));
        when(penalizacionVigenteGuard.tieneMultaPendiente(usuarioId)).thenReturn(true);

        ListarMisSancionesResponse response = service("activehub.multas", "0000003100000000000001", "ActiveHub").listar(usuarioId);

        assertThat(response.multaPendiente()).isTrue();
        assertThat(response.transferencia().alias()).isEqualTo("activehub.multas");
        assertThat(response.transferencia().titular()).isEqualTo("ActiveHub");
        ListarMisSancionesResponse.Sancion sancion = response.sanciones().get(0);
        assertThat(sancion.estadoPago()).isEqualTo("PENDIENTE_PAGO");
        assertThat(sancion.monto()).isEqualByComparingTo("5000");
        assertThat(sancion.comprobantes()).singleElement()
                .satisfies(c -> assertThat(c.motivoInvalidez()).isEqualTo("No se lee el monto"));
    }

    /** Sin la configuración cargada, la pantalla le dice que consulte con la administración. */
    @Test
    void listar_sinDatosDeTransferenciaCargados_viajanEnNull() {
        when(penalizacionRepository.findDeUsuario(usuarioId)).thenReturn(List.of());

        ListarMisSancionesResponse response = service("", "  ", "").listar(usuarioId);

        assertThat(response.transferencia().alias()).isNull();
        assertThat(response.transferencia().cbu()).isNull();
        assertThat(response.sanciones()).isEmpty();
    }

    @Test
    void listar_suspension_esVigenteSoloSiRigeHoyYNoFueLevantada() {
        Penalizacion enCurso = new Penalizacion();
        enCurso.setTipo(TipoPenalizacion.SUSPENSION_TEMPORAL);
        enCurso.setFechaInicio(LocalDate.of(2026, 10, 20));
        enCurso.setFechaFin(LocalDate.of(2026, 11, 20));
        Penalizacion levantada = new Penalizacion();
        levantada.setTipo(TipoPenalizacion.SUSPENSION_TEMPORAL);
        levantada.setFechaInicio(LocalDate.of(2026, 10, 20));
        levantada.setFechaFin(LocalDate.of(2026, 11, 20));
        levantada.setLevantadaAt(Instant.parse("2026-10-25T12:00:00Z"));
        ReflectionTestUtils.setField(enCurso, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(levantada, "id", UUID.randomUUID());
        when(penalizacionRepository.findDeUsuario(usuarioId)).thenReturn(List.of(enCurso, levantada));

        List<ListarMisSancionesResponse.Sancion> sanciones = service("", "", "").listar(usuarioId).sanciones();

        assertThat(sanciones.get(0).vigente()).isTrue();
        assertThat(sanciones.get(1).vigente()).isFalse();
    }
}
