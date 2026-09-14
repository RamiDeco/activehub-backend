package com.activehub.usecases.listarmisdenuncias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.denuncia.EstadoDenuncia;
import com.activehub.domain.denuncia.ResolucionDenuncia;
import com.activehub.domain.resenia.Resenia;
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
    private UUID denuncianteId;

    @BeforeEach
    void setUp() {
        service = new ListarMisDenunciasService(denunciaRepository);
        denuncianteId = UUID.randomUUID();
    }

    private Clase claseDeYoga() {
        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga");
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());
        return clase;
    }

    @Test
    void listar_devuelveDenunciasDeClase() {
        Denuncia denuncia = new Denuncia();
        denuncia.setClase(claseDeYoga());
        denuncia.setMotivo("No se presentó");
        ReflectionTestUtils.setField(denuncia, "id", UUID.randomUUID());

        when(denunciaRepository.findByDenuncianteIdConDetalle(denuncianteId)).thenReturn(List.of(denuncia));

        List<ListarMisDenunciasResponse> resultado = service.listar(denuncianteId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).tipo()).isEqualTo("CLASE");
        assertThat(resultado.get(0).actividadNombre()).isEqualTo("Yoga");
        assertThat(resultado.get(0).estado()).isEqualTo("Pendiente");
        assertThat(resultado.get(0).resolucion()).isNull();
    }

    @Test
    void listar_devuelveLaResolucionCuandoYaSeCerro() {
        Denuncia denuncia = new Denuncia();
        denuncia.setClase(claseDeYoga());
        denuncia.setMotivo("No se presentó");
        denuncia.setEstado(EstadoDenuncia.RESUELTA);
        denuncia.setResolucion(ResolucionDenuncia.REINTEGRAR);
        denuncia.setDetalle("Se reintegró el pago.");
        ReflectionTestUtils.setField(denuncia, "id", UUID.randomUUID());

        when(denunciaRepository.findByDenuncianteIdConDetalle(denuncianteId)).thenReturn(List.of(denuncia));

        var r = service.listar(denuncianteId).get(0);

        assertThat(r.estado()).isEqualTo("Resuelta");
        assertThat(r.resolucion()).isEqualTo("REINTEGRAR");
        assertThat(r.detalle()).isEqualTo("Se reintegró el pago.");
    }

    @Test
    void listar_denunciaDeResenia_tomaLaClaseDeLaResenia() {
        // El instructor también ve las suyas: la denuncia no tiene `clase`, cuelga de la reseña.
        Resenia resenia = new Resenia();
        resenia.setClase(claseDeYoga());
        ReflectionTestUtils.setField(resenia, "id", UUID.randomUUID());

        Denuncia denuncia = new Denuncia();
        denuncia.setResenia(resenia);
        denuncia.setMotivo("Comentario agresivo");
        ReflectionTestUtils.setField(denuncia, "id", UUID.randomUUID());

        when(denunciaRepository.findByDenuncianteIdConDetalle(denuncianteId)).thenReturn(List.of(denuncia));

        var r = service.listar(denuncianteId).get(0);

        assertThat(r.tipo()).isEqualTo("RESENIA");
        assertThat(r.actividadNombre()).isEqualTo("Yoga");
    }
}
