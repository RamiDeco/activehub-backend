package com.activehub.usecases.listarmispagos;

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
class ListarMisPagosServiceTest {

    @Mock private InscripcionRepository inscripcionRepository;

    @InjectMocks private ListarMisPagosService service;

    private Inscripcion inscripcion(EstadoInscripcion estado, EstadoClase estadoClase, Pago pago) {
        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga al aire libre");
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setEstado(estadoClase);
        clase.setFechaHora(Instant.parse("2026-06-01T10:00:00Z"));
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Usuario alumno = new Usuario();
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

    private Pago pago(EstadoPago estado, MetodoPago metodo, String monto) {
        Pago p = new Pago();
        p.setEstado(estado);
        p.setMetodo(metodo);
        p.setMonto(new BigDecimal(monto));
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(p, "createdAt", Instant.parse("2026-05-28T09:00:05Z"));
        return p;
    }

    @Test
    void listar_mapeaPagoClaseYActividad() {
        UUID alumnoId = UUID.randomUUID();
        when(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null))
                .thenReturn(List.of(inscripcion(
                        EstadoInscripcion.INSCRIPTO,
                        EstadoClase.Finalizada,
                        pago(EstadoPago.Liberado, MetodoPago.MERCADO_PAGO, "3500"))));

        List<ListarMisPagosResponse> resultado = service.listar(alumnoId);

        assertThat(resultado).hasSize(1);
        var r = resultado.get(0);
        assertThat(r.actividadNombre()).isEqualTo("Yoga al aire libre");
        // La pantalla "Mis pagos" pinta el badge con estos strings exactos.
        assertThat(r.estado()).isEqualTo("Liberado");
        assertThat(r.claseEstado()).isEqualTo("Finalizada");
        assertThat(r.inscripcionEstado()).isEqualTo("Inscripto");
        assertThat(r.metodo()).isEqualTo(MetodoPago.MERCADO_PAGO.getEtiqueta());
        assertThat(r.monto()).isEqualByComparingTo("3500");
        assertThat(r.createdAt()).isEqualTo(Instant.parse("2026-05-28T09:00:05Z"));
    }

    @Test
    void listar_incluyePagosDeInscripcionesCanceladas() {
        // El reintegro es justo lo que el alumno necesita ver: si se filtraran las
        // inscripciones canceladas, el pago Cancelado desaparecería del historial.
        UUID alumnoId = UUID.randomUUID();
        when(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null))
                .thenReturn(List.of(inscripcion(
                        EstadoInscripcion.CANCELADA,
                        EstadoClase.Cancelada,
                        pago(EstadoPago.Cancelado, MetodoPago.MERCADO_PAGO, "3500"))));

        var r = service.listar(alumnoId).get(0);

        assertThat(r.estado()).isEqualTo("Cancelado");
        assertThat(r.inscripcionEstado()).isEqualTo("Cancelada");
    }

    @Test
    void listar_ignoraInscripcionesSinPago() {
        UUID alumnoId = UUID.randomUUID();
        when(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null))
                .thenReturn(List.of(
                        inscripcion(EstadoInscripcion.PRE_INSCRIPCION, EstadoClase.Programada, null),
                        inscripcion(
                                EstadoInscripcion.INSCRIPTO,
                                EstadoClase.Habilitada,
                                pago(EstadoPago.Efectivo, MetodoPago.EFECTIVO, "2000"))));

        List<ListarMisPagosResponse> resultado = service.listar(alumnoId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).estado()).isEqualTo("Efectivo");
    }

    @Test
    void listar_sinInscripciones_devuelveListaVacia() {
        UUID alumnoId = UUID.randomUUID();
        when(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null)).thenReturn(List.of());

        assertThat(service.listar(alumnoId)).isEmpty();
    }
}
