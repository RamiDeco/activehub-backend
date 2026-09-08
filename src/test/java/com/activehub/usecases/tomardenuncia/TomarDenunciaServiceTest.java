package com.activehub.usecases.tomardenuncia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.denuncia.EstadoDenuncia;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TomarDenunciaServiceTest {

    @Mock private DenunciaRepository denunciaRepository;
    @Mock private AuditService auditService;

    @InjectMocks private TomarDenunciaService service;

    private UUID denunciaId;
    private UUID adminId;
    private Denuncia denuncia;

    @BeforeEach
    void setUp() {
        denunciaId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        denuncia = new Denuncia();
        ReflectionTestUtils.setField(denuncia, "id", denunciaId);
    }

    @Test
    void tomar_denunciaPendiente_pasaAEnAuditoriaYAudita() {
        denuncia.setEstado(EstadoDenuncia.PENDIENTE);
        when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.of(denuncia));

        TomarDenunciaResponse response = service.tomar(denunciaId, adminId);

        assertThat(denuncia.getEstado()).isEqualTo(EstadoDenuncia.EN_AUDITORIA);
        assertThat(response.estado()).isEqualTo("En Auditoría");
        verify(denunciaRepository).save(denuncia);
        verify(auditService).registrar(
                eq(adminId), eq(AuditAccion.DENUNCIA_EN_AUDITORIA), eq("Denuncia"), eq(denunciaId), any());
    }

    @Test
    void tomar_denunciaYaEnAuditoria_esIdempotenteYNoReaudita() {
        // El admin puede abrir la misma fila varias veces.
        denuncia.setEstado(EstadoDenuncia.EN_AUDITORIA);
        when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.of(denuncia));

        TomarDenunciaResponse response = service.tomar(denunciaId, adminId);

        assertThat(response.estado()).isEqualTo("En Auditoría");
        verify(denunciaRepository, never()).save(any());
        verify(auditService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void tomar_denunciaResuelta_lanzaValidacion() {
        // El estado solo avanza: no se vuelve de Resuelta a En Auditoría.
        denuncia.setEstado(EstadoDenuncia.RESUELTA);
        when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.of(denuncia));

        assertThatThrownBy(() -> service.tomar(denunciaId, adminId))
                .isInstanceOf(ValidacionException.class);

        verify(denunciaRepository, never()).save(any());
    }

    @Test
    void tomar_denunciaInexistente_lanzaNoEncontrado() {
        when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.tomar(denunciaId, adminId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
