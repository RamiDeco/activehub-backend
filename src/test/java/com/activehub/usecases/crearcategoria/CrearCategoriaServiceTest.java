package com.activehub.usecases.crearcategoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.CategoriaRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.CategoriaDuplicadaException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CrearCategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private AuditService auditService;

    private CrearCategoriaService service;

    @BeforeEach
    void setUp() {
        service = new CrearCategoriaService(categoriaRepository, auditService);
    }

    @Test
    void crear_datosValidos_creaCategoria() {
        when(categoriaRepository.existsByNombreIgnoreCaseAndDeletedFalse("Running")).thenReturn(false);
        when(categoriaRepository.saveAndFlush(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(c, "createdAt", Instant.now());
            return c;
        });

        CrearCategoriaResponse response = service.crear(new CrearCategoriaRequest("Running"), UUID.randomUUID());

        assertThat(response.nombre()).isEqualTo("Running");
        assertThat(response.id()).isNotNull();
    }

    @Test
    void crear_nombreDuplicado_lanzaDuplicado() {
        when(categoriaRepository.existsByNombreIgnoreCaseAndDeletedFalse("Running")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CrearCategoriaRequest("Running"), UUID.randomUUID()))
                .isInstanceOf(CategoriaDuplicadaException.class);
    }
}
