package com.activehub.usecases.inscribirse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.inscripcion.PagoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.SinCuposDisponiblesException;
import com.activehub.shared.notificacion.NotificacionService;
import com.activehub.shared.payments.PaymentGateway;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RN de cupos: el último lugar no puede venderse dos veces.
 *
 * <p>La garantía real vive en el UPDATE condicional de {@code ClaseRepository.ocuparCupo}
 * ({@code WHERE cuposOcupados < cuposMax}), que es atómico en la base. Lo que se prueba acá
 * es la otra mitad, la que sí depende de nuestro código: que el Service **no** decida el cupo
 * en Java (un read-then-write sobre {@code clase.getCuposOcupados()} sería una condición de
 * carrera aunque la query fuera atómica) y que respete el "0 filas afectadas" como negativa.
 *
 * <p>Por eso el mock de {@code ocuparCupo} reproduce la semántica de la base con un
 * {@link AtomicInteger}: N pedidos simultáneos contra M cupos tienen que terminar en
 * exactamente M inscripciones, ni una más.
 *
 * <p>La atomicidad de Postgres en sí no se prueba con mocks —para eso hace falta la base
 * levantada, y la suite corre sin Postgres a propósito.
 */
class InscribirseConcurrenciaTest {

    private static final Instant AHORA = Instant.parse("2026-08-01T00:00:00Z");
    private static final int CUPOS_MAX = 5;
    private static final int PEDIDOS_SIMULTANEOS = 40;

    private ClaseRepository claseRepository;
    private InscripcionRepository inscripcionRepository;
    private PagoRepository pagoRepository;
    private UsuarioRepository usuarioRepository;
    private PaymentGateway paymentGateway;
    private AuditService auditService;
    private NotificacionService notificacionService;
    private InstructorVerificadoGuard instructorVerificadoGuard;

    private InscribirseService service;
    private UUID claseId;
    private AtomicInteger cuposOcupadosEnLaBase;

    @BeforeEach
    void setUp() {
        claseRepository = mock(ClaseRepository.class);
        inscripcionRepository = mock(InscripcionRepository.class);
        pagoRepository = mock(PagoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        paymentGateway = mock(PaymentGateway.class);
        auditService = mock(AuditService.class);
        notificacionService = mock(NotificacionService.class);
        instructorVerificadoGuard = mock(InstructorVerificadoGuard.class);

        service = new InscribirseService(
                claseRepository, inscripcionRepository, pagoRepository, usuarioRepository, paymentGateway,
                auditService, notificacionService, instructorVerificadoGuard, Clock.fixed(AHORA, ZoneOffset.UTC));

        claseId = UUID.randomUUID();

        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", UUID.randomUUID());

        Actividad actividad = new Actividad();
        actividad.setNombre("Running");
        actividad.setPrecio(new BigDecimal("4500"));
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setEstado(EstadoClase.Programada);
        clase.setFechaHora(AHORA.plus(Duration.ofDays(2)));
        clase.setCuposMax(CUPOS_MAX);
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", claseId);

        cuposOcupadosEnLaBase = new AtomicInteger(0);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(
                any(), any(), org.mockito.ArgumentMatchers.eq(EstadoInscripcion.CANCELADA)))
                .thenReturn(Optional.empty());
        // Semántica del UPDATE condicional: incrementa y devuelve 1 fila solo si quedaba lugar.
        when(claseRepository.ocuparCupo(claseId)).thenAnswer(inv ->
                cuposOcupadosEnLaBase.getAndUpdate(n -> n < CUPOS_MAX ? n + 1 : n) < CUPOS_MAX ? 1 : 0);
        when(usuarioRepository.getReferenceById(any())).thenReturn(new Usuario());
        when(inscripcionRepository.saveAndFlush(any(Inscripcion.class))).thenAnswer(inv -> {
            Inscripcion i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(i, "createdAt", AHORA);
            return i;
        });
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });
    }

    @Test
    void inscribirse_cuarentaPedidosSimultaneosContraCincoCupos_soloEntranCinco() throws Exception {
        var arranquenTodosJuntos = new CountDownLatch(1);
        var terminaron = new CountDownLatch(PEDIDOS_SIMULTANEOS);
        var inscriptos = new AtomicInteger();
        var rechazadosPorFaltaDeCupo = new AtomicInteger();
        var otrosErrores = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(PEDIDOS_SIMULTANEOS);
        try {
            for (int i = 0; i < PEDIDOS_SIMULTANEOS; i++) {
                pool.execute(() -> {
                    try {
                        arranquenTodosJuntos.await();
                        service.inscribirse(claseId, new InscribirseRequest("Efectivo"), UUID.randomUUID());
                        inscriptos.incrementAndGet();
                    } catch (SinCuposDisponiblesException e) {
                        rechazadosPorFaltaDeCupo.incrementAndGet();
                    } catch (Exception e) {
                        otrosErrores.incrementAndGet();
                    } finally {
                        terminaron.countDown();
                    }
                });
            }
            arranquenTodosJuntos.countDown();
            assertThat(terminaron.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(otrosErrores.get()).isZero();
        assertThat(inscriptos.get()).isEqualTo(CUPOS_MAX);
        assertThat(rechazadosPorFaltaDeCupo.get()).isEqualTo(PEDIDOS_SIMULTANEOS - CUPOS_MAX);
        assertThat(cuposOcupadosEnLaBase.get()).isEqualTo(CUPOS_MAX);

        // Ni una inscripción ni un pago de más: el rechazo ocurre antes de escribir nada.
        verify(inscripcionRepository, times(CUPOS_MAX)).saveAndFlush(any(Inscripcion.class));
        verify(pagoRepository, times(CUPOS_MAX)).save(any(Pago.class));
    }

    @Test
    void inscribirse_conLaClaseYaLlena_noEscribeNada() {
        cuposOcupadosEnLaBase.set(CUPOS_MAX);

        List<UUID> alumnos = List.of(UUID.randomUUID(), UUID.randomUUID());
        for (UUID alumno : alumnos) {
            try {
                service.inscribirse(claseId, new InscribirseRequest("Efectivo"), alumno);
                throw new AssertionError("Tenía que rechazar por falta de cupo.");
            } catch (SinCuposDisponiblesException esperado) {
                // El camino esperado: 409 SIN_CUPOS_DISPONIBLES.
            }
        }

        verify(inscripcionRepository, org.mockito.Mockito.never()).saveAndFlush(any(Inscripcion.class));
        verify(pagoRepository, org.mockito.Mockito.never()).save(any(Pago.class));
        assertThat(cuposOcupadosEnLaBase.get()).isEqualTo(CUPOS_MAX);
    }
}
