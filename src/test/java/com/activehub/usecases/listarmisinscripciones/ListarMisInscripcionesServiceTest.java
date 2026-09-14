package com.activehub.usecases.listarmisinscripciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.activehub.shared.error.ValidacionException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * "Mis clases" y "Mis pagos" del alumno (E3A-HU06 / E3A-HU07). El filtro por estado llega
 * como la ETIQUETA ("Pago pendiente"), no como el nombre del enum: es lo que muestran las
 * pestañas de la pantalla.
 */
@ExtendWith(MockitoExtension.class)
class ListarMisInscripcionesServiceTest {

    @Mock
    private InscripcionRepository inscripcionRepository;

    private ListarMisInscripcionesService service;
    private UUID alumnoId;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        service = new ListarMisInscripcionesService(inscripcionRepository);
        alumnoId = UUID.randomUUID();

        actividad = new Actividad();
        actividad.setNombre("Running en grupo");
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());
    }

    private Inscripcion inscripcionCon(EstadoInscripcion estado, Pago pago) {
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        Clase clase = new Clase();
        clase.setActividad(actividad);
        clase.setEstado(EstadoClase.Programada);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setAlumno(alumno);
        inscripcion.setClase(clase);
        inscripcion.setEstado(estado);
        inscripcion.setPago(pago);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());
        return inscripcion;
    }

    private static Pago pagoRetenido() {
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMetodo(MetodoPago.MERCADO_PAGO);
        pago.setMonto(new BigDecimal("4500"));
        ReflectionTestUtils.setField(pago, "id", UUID.randomUUID());
        return pago;
    }

    @Test
    void listar_sinFiltro_consultaConEstadoNuloYMapeaLaActividad() {
        when(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null))
                .thenReturn(List.of(inscripcionCon(EstadoInscripcion.INSCRIPTO, pagoRetenido())));

        List<ListarMisInscripcionesResponse> resultado = service.listar(alumnoId, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).actividadNombre()).isEqualTo("Running en grupo");
        assertThat(resultado.get(0).alumnoId()).isEqualTo(alumnoId);
        assertThat(resultado.get(0).estado()).isEqualTo(EstadoInscripcion.INSCRIPTO.getEtiqueta());
    }

    @Test
    void listar_conEtiquetaDeEstado_laTraduceAlEnumAntesDeConsultar() {
        when(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, EstadoInscripcion.INSCRIPTO))
                .thenReturn(List.of(inscripcionCon(EstadoInscripcion.INSCRIPTO, pagoRetenido())));

        assertThat(service.listar(alumnoId, EstadoInscripcion.INSCRIPTO.getEtiqueta())).hasSize(1);
    }

    @Test
    void listar_conPago_devuelveMontoMetodoYEstado() {
        when(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null))
                .thenReturn(List.of(inscripcionCon(EstadoInscripcion.INSCRIPTO, pagoRetenido())));

        ListarMisInscripcionesResponse fila = service.listar(alumnoId, null).get(0);

        assertThat(fila.pagoId()).isNotNull();
        assertThat(fila.pago().estado()).isEqualTo("Retenido");
        assertThat(fila.pago().monto()).isEqualByComparingTo("4500");
        assertThat(fila.pago().metodo()).isEqualTo(MetodoPago.MERCADO_PAGO.getEtiqueta());
    }

    @Test
    void listar_preinscripcionSinPago_devuelvePagoNulo() {
        // RN-01: la preinscripción no genera pago, así que la pantalla tiene que tolerar null.
        when(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null))
                .thenReturn(List.of(inscripcionCon(EstadoInscripcion.PRE_INSCRIPCION, null)));

        ListarMisInscripcionesResponse fila = service.listar(alumnoId, null).get(0);

        assertThat(fila.pago()).isNull();
        assertThat(fila.pagoId()).isNull();
    }

    @Test
    void listar_estadoInexistente_lanzaValidacion() {
        assertThatThrownBy(() -> service.listar(alumnoId, "Cualquier cosa"))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Estado de inscripción inválido");
    }

    @Test
    void listar_sinInscripciones_devuelveListaVacia() {
        when(inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null)).thenReturn(List.of());

        assertThat(service.listar(alumnoId, null)).isEmpty();
    }
}
