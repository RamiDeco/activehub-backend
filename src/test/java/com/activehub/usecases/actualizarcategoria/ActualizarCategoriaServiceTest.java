package com.activehub.usecases.actualizarcategoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CategoriaDuplicadaException;
import com.activehub.shared.error.NoEncontradoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActualizarCategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private AuditService auditService;

    private ActualizarCategoriaService service;
    private UUID categoriaId;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        service = new ActualizarCategoriaService(categoriaRepository, auditService);

        categoriaId = UUID.randomUUID();
        categoria = new Categoria();
        categoria.setNombre("Bienestar");
        ReflectionTestUtils.setField(categoria, "id", categoriaId);
    }

    @Test
    void actualizar_datosValidos_actualizaNombre() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findByNombreIgnoreCaseAndDeletedFalse("Running")).thenReturn(Optional.empty());

        ActualizarCategoriaResponse response = service.actualizar(
                categoriaId, new ActualizarCategoriaRequest("Running"), UUID.randomUUID());

        assertThat(response.nombre()).isEqualTo("Running");
        assertThat(categoria.getNombre()).isEqualTo("Running");
    }

    @Test
    void actualizar_nombreIgualAlPropio_noLanzaDuplicado() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findByNombreIgnoreCaseAndDeletedFalse("Bienestar")).thenReturn(Optional.of(categoria));

        ActualizarCategoriaResponse response = service.actualizar(
                categoriaId, new ActualizarCategoriaRequest("Bienestar"), UUID.randomUUID());

        assertThat(response.nombre()).isEqualTo("Bienestar");
    }

    @Test
    void actualizar_nombreDeOtraCategoria_lanzaDuplicado() {
        Categoria otra = new Categoria();
        otra.setNombre("Running");
        ReflectionTestUtils.setField(otra, "id", UUID.randomUUID());

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findByNombreIgnoreCaseAndDeletedFalse("Running")).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> service.actualizar(categoriaId, new ActualizarCategoriaRequest("Running"), UUID.randomUUID()))
                .isInstanceOf(CategoriaDuplicadaException.class);
    }

    @Test
    void actualizar_categoriaInexistente_lanzaNoEncontrado() {
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(categoriaId, new ActualizarCategoriaRequest("Running"), UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
