package com.activehub.usecases.listarrosterclase;

import static org.mockito.ArgumentMatchers.any;
import java.math.BigDecimal;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Pago;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.List;
import java.util.Optional;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarRosterClaseServiceTest {

    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;

    @org.mockito.Mock private InstructorVerificadoGuard instructorVerificadoGuard;


    private ListarRosterClaseService service;
    private UUID instructorId;
    private UUID claseId;
    private Clase clase;

    @BeforeEach
    void setUp() {
        service = new ListarRosterClaseService(claseRepository, inscripcionRepository, instructorVerificadoGuard);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setInstructor(instructor);

        claseId = UUID.randomUUID();
        clase = new Clase();
        clase.setActividad(actividad);
        clase.setCuposMax(10);
        clase.setCuposOcupados(3);
        ReflectionTestUtils.setField(clase, "id", claseId);
    }

    private Inscripcion inscripcionCon(EstadoInscripcion estado) {
        Usuario alumno = new Usuario();
        alumno.setNombre("Ana");
        alumno.setApellido("Lopez");
        ReflectionTestUtils.setField(alumno, "id", UUID.randomUUID());

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setClase(clase);
        inscripcion.setAlumno(alumno);
        inscripcion.setEstado(estado);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());
        return inscripcion;
    }

    @Test
    void listar_incluyeCantidadDePreinscriptosSinListarlos() {
        Inscripcion inscripto = inscripcionCon(EstadoInscripcion.INSCRIPTO);
        Inscripcion pendiente = inscripcionCon(EstadoInscripcion.PAGO_PENDIENTE);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoInOrderByCreatedAtAsc(claseId, List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.PAGO_PENDIENTE)))
                .thenReturn(List.of(inscripto, pendiente));
        when(inscripcionRepository.countByClaseIdAndEstado(claseId, EstadoInscripcion.PRE_INSCRIPCION)).thenReturn(4L);

        ListarRosterClaseResponse response = service.listar(claseId, instructorId);

        assertThat(response.cantidadInscripto()).isEqualTo(1);
        assertThat(response.cantidadPagoPendiente()).isEqualTo(1);
        assertThat(response.cantidadPreInscripcion()).isEqualTo(4);
        assertThat(response.alumnos()).hasSize(2);
        assertThat(response.cuposLibres()).isEqualTo(7);
    }

    @Test
    void listar_incluyeElPagoDeCadaAlumnoYElNetoQueRecibeElInstructor() {
        // KAN-63: el instructor ve en que estado esta la plata de cada alumno y cuanto le queda.
        Inscripcion conMp = inscripcionCon(EstadoInscripcion.INSCRIPTO);
        Pago mp = new Pago();
        mp.setEstado(EstadoPago.Retenido);
        mp.setMetodo(MetodoPago.MERCADO_PAGO);
        mp.setMonto(new BigDecimal("5000"));
        mp.setComision(new BigDecimal("500"));
        mp.setCapturadoAt(java.time.Instant.parse("2026-10-10T12:00:00Z"));
        conMp.setPago(mp);
        Inscripcion conMontoCalculado = inscripcionCon(EstadoInscripcion.INSCRIPTO);
        Pago calculado = new Pago();
        calculado.setEstado(EstadoPago.Liberado);
        calculado.setMetodo(MetodoPago.MERCADO_PAGO);
        calculado.setMonto(new BigDecimal("5000"));
        calculado.setMontoInstructor(new BigDecimal("4700"));
        conMontoCalculado.setPago(calculado);
        Inscripcion sinPago = inscripcionCon(EstadoInscripcion.PAGO_PENDIENTE);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoInOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(conMp, conMontoCalculado, sinPago));

        var alumnos = service.listar(claseId, instructorId).alumnos();

        assertThat(alumnos.get(0).pagoEstado()).isEqualTo("Retenido");
        assertThat(alumnos.get(0).pagoMetodo()).isEqualTo("Mercado Pago");
        assertThat(alumnos.get(0).pagoCapturado()).isTrue();
        assertThat(alumnos.get(0).pagoNeto()).isEqualByComparingTo("4500");
        // Si el pago ya trae el monto del instructor calculado, manda ese.
        assertThat(alumnos.get(1).pagoNeto()).isEqualByComparingTo("4700");
        assertThat(alumnos.get(2).pagoEstado()).isNull();
        assertThat(alumnos.get(2).pagoNeto()).isNull();
        assertThat(alumnos.get(2).pagoCapturado()).isFalse();
    }

    @Test
    void listar_noDueño_lanzaSinPermiso() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> service.listar(claseId, UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    void listar_inexistente_lanzaNoEncontrado() {
        when(claseRepository.findById(claseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(claseId, instructorId))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void listar_claseCancelada_incluyeLasInscripcionesCanceladas() {
        // Desde el Historial el instructor abre una clase cancelada para ver quiénes estaban
        // anotados: la cascada de la cancelación ya las pasó todas a Cancelada.
        clase.setEstado(com.activehub.domain.actividad.EstadoClase.Cancelada);
        Inscripcion cancelada = inscripcionCon(EstadoInscripcion.CANCELADA);

        when(claseRepository.findById(claseId)).thenReturn(Optional.of(clase));
        when(inscripcionRepository.findByClaseIdAndEstadoInOrderByCreatedAtAsc(claseId,
                List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.PAGO_PENDIENTE, EstadoInscripcion.CANCELADA)))
                .thenReturn(List.of(cancelada));

        ListarRosterClaseResponse response = service.listar(claseId, instructorId);

        assertThat(response.alumnos()).hasSize(1);
        assertThat(response.alumnos().get(0).estado()).isEqualTo("Cancelada");
    }
}
