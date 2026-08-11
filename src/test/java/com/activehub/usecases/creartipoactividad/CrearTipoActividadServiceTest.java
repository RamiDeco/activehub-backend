package com.activehub.usecases.creartipoactividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.actividad.TipoActividadRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.TipoActividadDuplicadoException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CrearTipoActividadServiceTest {

    @Mock
    private TipoActividadRepository tipoActividadRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private AuditService auditService;

    private CrearTipoActividadService service;
    private UUID categoriaId;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        service = new CrearTipoActividadService(tipoActividadRepository, categoriaRepository, auditService);

        categoriaId = UUID.randomUUID();
        categoria = new Categoria();
        categoria.setNombre("Bienestar");
        ReflectionTestUtils.setField(categoria, "id", categoriaId);
    }

    @Test
    void crear_datosValidos_creaTipoActividad() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.existsByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse("Yoga", categoriaId))
                .thenReturn(false);
        when(tipoActividadRepository.saveAndFlush(any(TipoActividad.class))).thenAnswer(inv -> {
            TipoActividad t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(t, "createdAt", Instant.now());
            return t;
        });

        CrearTipoActividadResponse response = service.crear(new CrearTipoActividadRequest("Yoga", categoriaId), UUID.randomUUID());

        assertThat(response.nombre()).isEqualTo("Yoga");
        assertThat(response.categoriaId()).isEqualTo(categoriaId);
    }

    @Test
    void crear_nombreDuplicadoEnCategoria_lanzaDuplicado() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(tipoActividadRepository.existsByNombreIgnoreCaseAndCategoriaIdAndDeletedFalse("Yoga", categoriaId))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CrearTipoActividadRequest("Yoga", categoriaId), UUID.randomUUID()))
                .isInstanceOf(TipoActividadDuplicadoException.class);
    }

    @Test
    void crear_categoriaInexistente_lanzaNoEncontrado() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new CrearTipoActividadRequest("Yoga", categoriaId), UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
