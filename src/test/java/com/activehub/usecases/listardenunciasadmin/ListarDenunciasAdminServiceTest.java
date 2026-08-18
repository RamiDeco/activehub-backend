package com.activehub.usecases.listardenunciasadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.EstadoPago;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.MetodoPago;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.usuario.Usuario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarDenunciasAdminServiceTest {

    @Mock
    private DenunciaRepository denunciaRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;

    private ListarDenunciasAdminService service;

    private UUID claseId;
    private UUID alumnoId;
    private Denuncia denuncia;

    @BeforeEach
    void setUp() {
        service = new ListarDenunciasAdminService(denunciaRepository, inscripcionRepository);

        Usuario instructor = new Usuario();
        instructor.setNombre("Franco");
        instructor.setApellido("Gonzales");
        ReflectionTestUtils.setField(instructor, "id", UUID.randomUUID());

        Actividad actividad = new Actividad();
        actividad.setNombre("Meditación al aire libre");
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        claseId = UUID.randomUUID();
        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", claseId);

        Usuario alumno = new Usuario();
        alumno.setNombre("Ana");
        alumno.setApellido("Lopez");
        alumnoId = UUID.randomUUID();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        denuncia = new Denuncia();
        denuncia.setClase(clase);
        denuncia.setAlumno(alumno);
        denuncia.setMotivo("No se presentó");
        ReflectionTestUtils.setField(denuncia, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(denuncia, "createdAt", java.time.Instant.now());
    }

    @Test
    void listar_conPagoRetenido_incluyeElMonto() {
        when(denunciaRepository.findAllConDetalle()).thenReturn(List.of(denuncia));

        Pago pago = new Pago();
        pago.setEstado(EstadoPago.Retenido);
        pago.setMetodo(MetodoPago.MERCADO_PAGO);
        pago.setMonto(new BigDecimal("4500"));

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setPago(pago);

        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.of(inscripcion));

        List<ListarDenunciasAdminResponse> response = service.listar();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).instructor().nombre()).isEqualTo("Franco");
        assertThat(response.get(0).pago()).isNotNull();
        assertThat(response.get(0).pago().monto()).isEqualByComparingTo("4500");
    }

    @Test
    void listar_sinInscripcion_pagoEsNull() {
        when(denunciaRepository.findAllConDetalle()).thenReturn(List.of(denuncia));
        when(inscripcionRepository.findByClaseIdAndAlumnoIdAndEstadoNot(claseId, alumnoId, EstadoInscripcion.CANCELADA))
                .thenReturn(Optional.empty());

        List<ListarDenunciasAdminResponse> response = service.listar();

        assertThat(response.get(0).pago()).isNull();
    }
}
