package com.activehub.usecases.listarmisdenuncias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarMisDenunciasServiceTest {

    @Mock
    private DenunciaRepository denunciaRepository;

    private ListarMisDenunciasService service;
    private UUID alumnoId;

    @BeforeEach
    void setUp() {
        service = new ListarMisDenunciasService(denunciaRepository);
        alumnoId = UUID.randomUUID();
    }

    @Test
    void listar_devuelveDenunciasDelAlumno() {
        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga");
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        Denuncia denuncia = new Denuncia();
        denuncia.setClase(clase);
        denuncia.setMotivo("No se presentó");
        ReflectionTestUtils.setField(denuncia, "id", UUID.randomUUID());

        when(denunciaRepository.findByAlumnoIdConDetalle(alumnoId)).thenReturn(List.of(denuncia));

        List<ListarMisDenunciasResponse> resultado = service.listar(alumnoId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).actividadNombre()).isEqualTo("Yoga");
        assertThat(resultado.get(0).estado()).isEqualTo("Pendiente");
    }
}
