package com.activehub.usecases.listarpenalizaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.penalizacion.ComprobantePenalizacion;
import com.activehub.domain.penalizacion.ComprobantePenalizacionRepository;
import com.activehub.domain.penalizacion.EstadoComprobantePenalizacion;
import com.activehub.domain.penalizacion.EstadoPagoPenalizacion;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.usuario.Usuario;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * E4Ad-HU06 criterio 1: la tabla de Penalizaciones muestra, entre otras cosas, si la sanción
 * está vigente. La vigencia se calcula acá y no se persiste, así que el reloj es lo único
 * que la mueve — por eso el servicio recibe un {@link Clock} inyectado.
 */
@ExtendWith(MockitoExtension.class)
class ListarPenalizacionesServiceTest {

    private static final ZoneId ZONA_AR = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock
    private PenalizacionRepository penalizacionRepository;
    @Mock
    private ComprobantePenalizacionRepository comprobantePenalizacionRepository;

    private ListarPenalizacionesService service;

    /** Mediodía del 10/09/2026 en hora argentina, para que el día local no dependa del huso. */
    private static Clock relojEn(LocalDate dia) {
        return Clock.fixed(dia.atTime(12, 0).atZone(ZONA_AR).toInstant(), ZONA_AR);
    }

    @BeforeEach
    void setUp() {
        service = new ListarPenalizacionesService(
                penalizacionRepository, comprobantePenalizacionRepository, relojEn(LocalDate.of(2026, 9, 10)));
    }

    private static Usuario usuario() {
        Usuario u = new Usuario();
        u.setNombre("Bruno");
        u.setApellido("Salas");
        u.setEmail("bruno@test.com");
        u.setCantidadPenalizaciones(2);
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    private static Penalizacion suspension(LocalDate inicio, LocalDate fin) {
        Penalizacion p = new Penalizacion();
        p.setUsuario(usuario());
        p.setTipo(TipoPenalizacion.SUSPENSION_TEMPORAL);
        p.setMotivo("Inasistencias reiteradas");
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(p, "createdAt", Instant.now());
        return p;
    }

    private static Penalizacion economica() {
        Penalizacion p = new Penalizacion();
        p.setUsuario(usuario());
        p.setTipo(TipoPenalizacion.ECONOMICA);
        p.setMotivo("Cancelación tardía");
        p.setMonto(new BigDecimal("1500"));
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(p, "createdAt", Instant.now());
        return p;
    }

    @Test
    void listar_suspensionEnCurso_quedaVigente() {
        when(penalizacionRepository.findAllConDetalle())
                .thenReturn(List.of(suspension(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20))));

        assertThat(service.listar().get(0).vigente()).isTrue();
    }

    @Test
    void listar_suspensionQueTerminaHoy_todaviaEstaVigente() {
        // El último día cuenta: la suspensión vence al final del día, no al empezarlo.
        when(penalizacionRepository.findAllConDetalle())
                .thenReturn(List.of(suspension(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10))));

        assertThat(service.listar().get(0).vigente()).isTrue();
    }

    @Test
    void listar_suspensionYaVencida_noEstaVigente() {
        when(penalizacionRepository.findAllConDetalle())
                .thenReturn(List.of(suspension(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 9))));

        assertThat(service.listar().get(0).vigente()).isFalse();
    }

    @Test
    void listar_suspensionQueEmpiezaMasAdelante_todaviaNoEstaVigente() {
        when(penalizacionRepository.findAllConDetalle())
                .thenReturn(List.of(suspension(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20))));

        assertThat(service.listar().get(0).vigente()).isFalse();
    }

    @Test
    void listar_penalizacionEconomica_nuncaEsVigenteYConservaElMonto() {
        when(penalizacionRepository.findAllConDetalle()).thenReturn(List.of(economica()));

        ListarPenalizacionesResponse fila = service.listar().get(0);

        assertThat(fila.vigente()).isFalse();
        assertThat(fila.monto()).isEqualByComparingTo("1500");
        assertThat(fila.tipo()).isEqualTo(TipoPenalizacion.ECONOMICA.getEtiqueta());
    }

    @Test
    void listar_armaElNombreCompletoYElAcumuladoDelUsuario() {
        when(penalizacionRepository.findAllConDetalle()).thenReturn(List.of(economica()));

        ListarPenalizacionesResponse fila = service.listar().get(0);

        assertThat(fila.usuarioNombre()).isEqualTo("Bruno Salas");
        assertThat(fila.cantidadPenalizacionesUsuario()).isEqualTo(2);
    }

    @Test
    void listar_sinPenalizaciones_devuelveListaVacia() {
        when(penalizacionRepository.findAllConDetalle()).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }

    /** V36: una suspension levantada por un administrador ya no esta vigente aunque no haya vencido. */
    @Test
    void listar_suspensionLevantada_noEstaVigenteYTraeElMotivo() {
        Penalizacion p = suspension(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20));
        p.setLevantadaAt(Instant.parse("2026-09-05T15:00:00Z"));
        p.setMotivoLevantamiento("Se aclaró la situación");
        when(penalizacionRepository.findAllConDetalle()).thenReturn(List.of(p));

        ListarPenalizacionesResponse fila = service.listar().get(0);

        assertThat(fila.vigente()).isFalse();
        assertThat(fila.motivoLevantamiento()).isEqualTo("Se aclaró la situación");
    }

    /** V36: la multa trae el estado del pago y sus comprobantes, del mas reciente al mas viejo. */
    @Test
    void listar_multa_traeElEstadoDelPagoYSusComprobantes() {
        Penalizacion multa = economica();
        multa.setEstadoPago(EstadoPagoPenalizacion.EN_REVISION);
        ComprobantePenalizacion comprobante = new ComprobantePenalizacion();
        comprobante.setPenalizacion(multa);
        comprobante.setNombreArchivo("transferencia.pdf");
        comprobante.setEstado(EstadoComprobantePenalizacion.EN_REVISION);
        ReflectionTestUtils.setField(comprobante, "id", UUID.randomUUID());
        when(penalizacionRepository.findAllConDetalle()).thenReturn(List.of(multa));
        when(comprobantePenalizacionRepository.findDePenalizaciones(List.of(multa.getId())))
                .thenReturn(List.of(comprobante));

        ListarPenalizacionesResponse fila = service.listar().get(0);

        assertThat(fila.estadoPago()).isEqualTo("EN_REVISION");
        assertThat(fila.comprobantes()).singleElement()
                .satisfies(c -> {
                    assertThat(c.nombreArchivo()).isEqualTo("transferencia.pdf");
                    assertThat(c.estado()).isEqualTo("EN_REVISION");
                });
    }

    /** Una multa anterior a V36 no tiene estado de pago: no se paga por la plataforma. */
    @Test
    void listar_multaAnteriorAV36_viajaSinEstadoDePago() {
        when(penalizacionRepository.findAllConDetalle()).thenReturn(List.of(economica()));

        assertThat(service.listar().get(0).estadoPago()).isNull();
    }
}
