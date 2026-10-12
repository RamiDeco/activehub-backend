package com.activehub.usecases.finalizarclasesvencidas;

import com.activehub.shared.error.PagoException;
import com.activehub.shared.notificacion.Destino;
import com.activehub.shared.notificacion.NotificacionMensajes;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.notificacion.TipoNotificacion;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import com.activehub.domain.inscripcion.VentanaPagos;
import com.activehub.domain.permiso.ConfiguracionRolRepository;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.domain.actividad.Actividad;
import com.activehub.shared.payments.OperacionesPagoMercadoPago;
import com.activehub.domain.inscripcion.PagoRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FinalizarClasesVencidasServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-20T00:00:00Z");

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private OperacionesPagoMercadoPago operacionesPago;
    @Mock
    private AuditService auditService;
    @Mock
    private ConfiguracionRolRepository configuracionRolRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacionService notificacionService;

    private FinalizarClasesVencidasService service;
    private UUID claseId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        service = new FinalizarClasesVencidasService(
                claseRepository, inscripcionRepository, pagoRepository, operacionesPago, auditService, clock,
                configuracionRolRepository, usuarioRepository, notificacionService);

        claseId = UUID.randomUUID();
        clase = new Clase();
        clase.setEstado(EstadoClase.Programada);
        clase.setFechaHora(AHORA.minus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(clase, "id", claseId);
    }

    private Inscripcion inscripcionCon(EstadoInscripcion estado, Pago pago) {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setEstado(estado);
        inscripcion.setPago(pago);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());
        return inscripcion;
    }

    @Test
    void finalizar_claseVencidaProgramada_pasaAFinalizada() {
        when(claseRepository.findByEstadoInAndFechaHoraBefore(List.of(EstadoClase.Programada, EstadoClase.Habilitada), AHORA))
                .thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA)).thenReturn(List.of());

        service.finalizar();

        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Finalizada);
        verify(auditService).registrar(isNull(), eq(AuditAccion.CLASE_FINALIZADA), eq("Clase"), eq(claseId), isNull());
    }

    @Test
    void finalizar_claseVencidaHabilitada_pasaAFinalizada() {
        clase.setEstado(EstadoClase.Habilitada);
        when(claseRepository.findByEstadoInAndFechaHoraBefore(List.of(EstadoClase.Programada, EstadoClase.Habilitada), AHORA))
                .thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA)).thenReturn(List.of());

        service.finalizar();

        assertThat(clase.getEstado()).isEqualTo(EstadoClase.Finalizada);
    }

    @Test
    void finalizar_preInscripcion_pasaACancelada() {
        Inscripcion preInscripcion = inscripcionCon(EstadoInscripcion.PRE_INSCRIPCION, null);

        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), eq(AHORA))).thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(preInscripcion));

        service.finalizar();

        assertThat(preInscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
    }

    @Test
    void finalizar_pagoPendienteEfectivo_pasaACanceladaSinTocarElPago() {
        Pago efectivo = new Pago();
        efectivo.setEstado(EstadoPago.Efectivo);
        efectivo.setMetodo(MetodoPago.EFECTIVO);
        efectivo.setMonto(new BigDecimal("4500"));
        Inscripcion pagoPendiente = inscripcionCon(EstadoInscripcion.PAGO_PENDIENTE, efectivo);

        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), eq(AHORA))).thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(pagoPendiente));

        service.finalizar();

        assertThat(pagoPendiente.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        assertThat(efectivo.getEstado()).isEqualTo(EstadoPago.Efectivo);
    }

    @Test
    void finalizar_inscripto_quedaIntactoSinTocarElPagoRetenido() {
        Pago retenido = new Pago();
        retenido.setEstado(EstadoPago.Retenido);
        retenido.setMetodo(MetodoPago.MERCADO_PAGO);
        retenido.setMonto(new BigDecimal("4500"));
        Inscripcion inscripto = inscripcionCon(EstadoInscripcion.INSCRIPTO, retenido);

        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), eq(AHORA))).thenReturn(List.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoNot(claseId, EstadoInscripcion.CANCELADA))
                .thenReturn(List.of(inscripto));

        service.finalizar();

        assertThat(inscripto.getEstado()).isEqualTo(EstadoInscripcion.INSCRIPTO);
        assertThat(retenido.getEstado()).isEqualTo(EstadoPago.Retenido);
    }

    @Test
    void finalizar_sinClasesVencidas_noHaceNada() {
        when(claseRepository.findByEstadoInAndFechaHoraBefore(any(), eq(AHORA))).thenReturn(List.of());

        service.finalizar();

        verify(auditService, org.mockito.Mockito.never()).registrar(any(), any(), any(), any(), any());
    }

    private Pago pagoMercadoPagoDeClaseFinalizada(UUID instructorId) {
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);
        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);
        clase.setActividad(actividad);
        clase.setEstado(EstadoClase.Finalizada);

        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMpOrderId("ORD-" + UUID.randomUUID());
        ReflectionTestUtils.setField(pago, "id", UUID.randomUUID());
        pago.setInscripcion(inscripcionCon(EstadoInscripcion.INSCRIPTO, pago));
        return pago;
    }

    @Test
    void capturarPagosPendientes_capturaConElInstructorDeLaClase() {
        UUID instructorId = UUID.randomUUID();
        Pago pago = pagoMercadoPagoDeClaseFinalizada(instructorId);
        when(pagoRepository.findSinCapturarDeClasesEn(EstadoPago.Retenido, EstadoClase.Finalizada))
                .thenReturn(List.of(pago));

        int capturados = service.capturarPagosPendientes();

        assertThat(capturados).isEqualTo(1);
        verify(operacionesPago).capturar(pago, instructorId);
    }

    @Test
    void capturarPagosPendientes_siFallaUno_siguenLosDemas() {
        UUID instructorId = UUID.randomUUID();
        Pago falla = pagoMercadoPagoDeClaseFinalizada(instructorId);
        Pago anda = pagoMercadoPagoDeClaseFinalizada(instructorId);
        when(pagoRepository.findSinCapturarDeClasesEn(EstadoPago.Retenido, EstadoClase.Finalizada))
                .thenReturn(List.of(falla, anda));
        doThrow(new PagoException("MP caido")).when(operacionesPago).capturar(falla, instructorId);

        int capturados = service.capturarPagosPendientes();

        assertThat(capturados).isEqualTo(1);
        verify(operacionesPago).capturar(anda, instructorId);
    }

    @Test
    void capturarPagosPendientes_siFalla_sumaUnIntentoAuditaYSigueRetenido() {
        UUID instructorId = UUID.randomUUID();
        Pago pago = pagoMercadoPagoDeClaseFinalizada(instructorId);
        pago.setIntentosCaptura(2);
        when(pagoRepository.findSinCapturarDeClasesEn(EstadoPago.Retenido, EstadoClase.Finalizada))
                .thenReturn(List.of(pago));
        doThrow(new PagoException("MP caido")).when(operacionesPago).capturar(pago, instructorId);

        service.capturarPagosPendientes();

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.Retenido);
        assertThat(pago.getCapturadoAt()).isNull();
        assertThat(pago.getIntentosCaptura()).isEqualTo(3);
        verify(pagoRepository).save(pago);
        verify(auditService).registrar(isNull(), eq(AuditAccion.PAGO_CAPTURA_FALLIDA), eq("Pago"), eq(pago.getId()),
                eq("intento 3 · PagoException"));
    }

    @Test
    void capturarPagosPendientes_autorizacionPorVencer_seCapturaAunqueLaClaseNoHayaFinalizado() {
        UUID instructorId = UUID.randomUUID();
        Pago pago = pagoMercadoPagoDeClaseFinalizada(instructorId);
        clase.setEstado(EstadoClase.Habilitada);
        Instant vence = AHORA.plus(Duration.ofHours(10));
        pago.setAutorizadoHasta(vence);

        // El limite es AHORA + 24 h: con Clock.fixed se puede afirmar exacto.
        when(pagoRepository.findSinCapturarPorVencer(EstadoPago.Retenido, AHORA.plus(Duration.ofHours(24)),
                List.of(EstadoClase.Programada, EstadoClase.Habilitada)))
                .thenReturn(List.of(pago));
        doAnswer(inv -> {
            pago.setCapturadoAt(AHORA);
            pago.setAutorizadoHasta(null);
            return null;
        }).when(operacionesPago).capturar(pago, instructorId);

        int capturados = service.capturarPagosPendientes();

        assertThat(capturados).isEqualTo(1);
        verify(operacionesPago).capturar(pago, instructorId);
        verify(auditService).registrar(isNull(), eq(AuditAccion.PAGO_CAPTURA_ANTICIPADA), eq("Pago"), eq(pago.getId()),
                eq("vencia " + vence));
    }

    @Test
    void capturarPagosPendientes_porVencerPeroYaVencida_noSeAuditaComoAnticipada() {
        // capturar() la pasa a Cancelado sin capturadoAt: no hubo cobro que registrar.
        UUID instructorId = UUID.randomUUID();
        Pago pago = pagoMercadoPagoDeClaseFinalizada(instructorId);
        clase.setEstado(EstadoClase.Habilitada);
        pago.setAutorizadoHasta(AHORA.minus(Duration.ofMinutes(1)));
        when(pagoRepository.findSinCapturarPorVencer(any(), any(), any())).thenReturn(List.of(pago));
        doAnswer(inv -> {
            pago.setEstado(EstadoPago.Cancelado);
            return null;
        }).when(operacionesPago).capturar(pago, instructorId);

        service.capturarPagosPendientes();

        verify(auditService, never()).registrar(any(), eq(AuditAccion.PAGO_CAPTURA_ANTICIPADA), any(), any(), any());
    }

    private Pago pagoQueLlegaAlTope(UUID instructorId) {
        Pago pago = pagoMercadoPagoDeClaseFinalizada(instructorId);
        clase.getActividad().setNombre("Yoga");
        // Uno menos que el tope: este intento fallido es el que llega.
        pago.setIntentosCaptura(VentanaPagos.MAX_INTENTOS_CAPTURA - 1);
        when(pagoRepository.findSinCapturarDeClasesEn(EstadoPago.Retenido, EstadoClase.Finalizada))
                .thenReturn(List.of(pago));
        doThrow(new PagoException("MP caido")).when(operacionesPago).capturar(pago, instructorId);
        return pago;
    }

    @Test
    void capturarPagosPendientes_alLlegarAlTopeDeIntentos_avisaALaAdministracion() {
        Pago pago = pagoQueLlegaAlTope(UUID.randomUUID());
        UUID rolAdmin = UUID.randomUUID();
        UUID admin1 = UUID.randomUUID();
        UUID admin2 = UUID.randomUUID();
        when(configuracionRolRepository.rolesConPermiso("auditoria.ver")).thenReturn(List.of(rolAdmin));
        when(usuarioRepository.findIdsActivosPorRol(List.of(rolAdmin))).thenReturn(List.of(admin1, admin2));

        service.capturarPagosPendientes();

        String esperado = "No se pudo cobrar en Mercado Pago un pago de la clase de Yoga del "
                + NotificacionMensajes.formatFechaHora(clase.getFechaHora())
                + " después de 6 intentos. Revisá que el instructor tenga su cuenta de Mercado Pago vinculada.";
        verify(notificacionService).notificar(admin1, TipoNotificacion.CAPTURA_PAGO_FALLIDA, esperado,
                pago.getId(), Destino.ninguno());
        verify(notificacionService).notificar(admin2, TipoNotificacion.CAPTURA_PAGO_FALLIDA, esperado,
                pago.getId(), Destino.ninguno());
    }

    @Test
    void capturarPagosPendientes_despuesDelTope_noVuelveAAvisar() {
        Pago pago = pagoQueLlegaAlTope(UUID.randomUUID());
        // Ya se aviso en el intento anterior: este es el septimo.
        pago.setIntentosCaptura(VentanaPagos.MAX_INTENTOS_CAPTURA);

        service.capturarPagosPendientes();

        assertThat(pago.getIntentosCaptura()).isEqualTo(VentanaPagos.MAX_INTENTOS_CAPTURA + 1);
        verify(configuracionRolRepository, never()).rolesConPermiso(any());
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }

    @Test
    void capturarPagosPendientes_sinRolesConElPermiso_noConsultaUsuariosNiNotifica() {
        pagoQueLlegaAlTope(UUID.randomUUID());
        when(configuracionRolRepository.rolesConPermiso("auditoria.ver")).thenReturn(List.of());

        service.capturarPagosPendientes();

        verify(usuarioRepository, never()).findIdsActivosPorRol(any());
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }

    @Test
    void capturarPagosPendientes_siFallaElAviso_siguenLosDemasPagos() {
        UUID instructorId = UUID.randomUUID();
        Pago llegaAlTope = pagoQueLlegaAlTope(instructorId);
        Pago otro = pagoMercadoPagoDeClaseFinalizada(instructorId);
        when(pagoRepository.findSinCapturarDeClasesEn(EstadoPago.Retenido, EstadoClase.Finalizada))
                .thenReturn(List.of(llegaAlTope, otro));
        when(configuracionRolRepository.rolesConPermiso("auditoria.ver")).thenThrow(new RuntimeException("base caida"));

        int capturados = service.capturarPagosPendientes();

        assertThat(capturados).isEqualTo(1);
        verify(operacionesPago).capturar(otro, instructorId);
    }
}
