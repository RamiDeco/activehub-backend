package com.activehub.usecases.listarinscripcionesadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.usuario.Usuario;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarInscripcionesAdminServiceTest {

    @Mock
    private InscripcionRepository inscripcionRepository;

    private ListarInscripcionesAdminService service;

    @BeforeEach
    void setUp() {
        service = new ListarInscripcionesAdminService(inscripcionRepository);
    }

    @Test
    void listar_conPago_mapeaDetalle() {
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", UUID.randomUUID());

        Actividad actividad = new Actividad();
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());
        clase.setActividad(actividad);

        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMetodo(MetodoPago.MERCADO_PAGO);
        pago.setMonto(new BigDecimal("4500"));
        ReflectionTestUtils.setField(pago, "id", UUID.randomUUID());

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setAlumno(alumno);
        inscripcion.setClase(clase);
        inscripcion.setEstado(EstadoInscripcion.INSCRIPTO);
        inscripcion.setPago(pago);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());

        when(inscripcionRepository.findAllConDetalle()).thenReturn(List.of(inscripcion));

        List<ListarInscripcionesAdminResponse> resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).estado()).isEqualTo("Inscripto");
        assertThat(resultado.get(0).actividadId()).isEqualTo(actividad.getId());
        assertThat(resultado.get(0).pago().estado()).isEqualTo("Retenido");
        assertThat(resultado.get(0).pago().monto()).isEqualByComparingTo("4500");
    }

    @Test
    void listar_sinPago_pagoEsNull() {
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", UUID.randomUUID());
        Actividad actividad = new Actividad();
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());
        Clase clase = new Clase();
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());
        clase.setActividad(actividad);

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setAlumno(alumno);
        inscripcion.setClase(clase);
        inscripcion.setEstado(EstadoInscripcion.PRE_INSCRIPCION);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());

        when(inscripcionRepository.findAllConDetalle()).thenReturn(List.of(inscripcion));

        List<ListarInscripcionesAdminResponse> resultado = service.listar();

        assertThat(resultado.get(0).pago()).isNull();
    }
}
