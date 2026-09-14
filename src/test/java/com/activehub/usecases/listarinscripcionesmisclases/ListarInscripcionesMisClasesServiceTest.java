package com.activehub.usecases.listarinscripcionesmisclases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.usuario.Usuario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarInscripcionesMisClasesServiceTest {

    @Mock private InscripcionRepository inscripcionRepository;

    @InjectMocks private ListarInscripcionesMisClasesService service;

    private Inscripcion inscripcion(EstadoInscripcion estado, Pago pago) {
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", UUID.randomUUID());

        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga");
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setEstado(EstadoClase.Finalizada);
        clase.setFechaHora(Instant.parse("2026-06-01T10:00:00Z"));
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Usuario alumno = new Usuario();
        alumno.setNombre("Juan");
        alumno.setApellido("Pérez");
        ReflectionTestUtils.setField(alumno, "id", UUID.randomUUID());

        Inscripcion i = new Inscripcion();
        i.setClase(clase);
        i.setAlumno(alumno);
        i.setEstado(estado);
        ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(i, "createdAt", Instant.parse("2026-05-28T09:00:00Z"));
        if (pago != null) {
            ReflectionTestUtils.setField(i, "pago", pago);
        }
        return i;
    }

    private Pago pago(EstadoPago estado, String monto) {
        Pago p = new Pago();
        p.setEstado(estado);
        p.setMetodo(MetodoPago.MERCADO_PAGO);
        p.setMonto(new BigDecimal(monto));
        return p;
    }

    @Test
    void listar_mapeaAlumnoEstadoYPago() {
        UUID instructorId = UUID.randomUUID();
        when(inscripcionRepository.findByInstructorIdConDetalle(instructorId))
                .thenReturn(List.of(inscripcion(EstadoInscripcion.INSCRIPTO, pago(EstadoPago.Liberado, "3500"))));

        List<ListarInscripcionesMisClasesResponse> resultado = service.listar(instructorId);

        assertThat(resultado).hasSize(1);
        var r = resultado.get(0);
        assertThat(r.alumnoNombre()).isEqualTo("Juan Pérez");
        assertThat(r.estado()).isEqualTo("Inscripto");
        assertThat(r.pagoEstado()).isEqualTo("Liberado");
        assertThat(r.pagoMonto()).isEqualByComparingTo("3500");
        assertThat(r.actividadNombre()).isEqualTo("Yoga");
    }

    @Test
    void listar_inscripcionSinPago_devuelvePagoNulo() {
        UUID instructorId = UUID.randomUUID();
        when(inscripcionRepository.findByInstructorIdConDetalle(instructorId))
                .thenReturn(List.of(inscripcion(EstadoInscripcion.PRE_INSCRIPCION, null)));

        var r = service.listar(instructorId).get(0);

        assertThat(r.estado()).isEqualTo("PreInscripción");
        assertThat(r.pagoEstado()).isNull();
        assertThat(r.pagoMonto()).isNull();
    }

    @Test
    void listar_sinInscripciones_devuelveListaVacia() {
        UUID instructorId = UUID.randomUUID();
        when(inscripcionRepository.findByInstructorIdConDetalle(instructorId)).thenReturn(List.of());

        assertThat(service.listar(instructorId)).isEmpty();
    }
}
