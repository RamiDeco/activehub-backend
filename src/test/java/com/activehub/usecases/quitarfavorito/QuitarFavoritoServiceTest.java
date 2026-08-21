package com.activehub.usecases.quitarfavorito;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.favorito.ActividadFavorita;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.shared.audit.AuditService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuitarFavoritoServiceTest {

    @Mock
    private FavoritoRepository favoritoRepository;
    @Mock
    private AuditService auditService;

    private QuitarFavoritoService service;
    private UUID actividadId;
    private UUID alumnoId;

    @BeforeEach
    void setUp() {
        service = new QuitarFavoritoService(favoritoRepository, auditService);
        actividadId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
    }

    @Test
    void quitar_favoritoExistente_loBorra() {
        ActividadFavorita favorito = new ActividadFavorita();
        when(favoritoRepository.findByUsuarioIdAndActividadId(alumnoId, actividadId)).thenReturn(Optional.of(favorito));

        service.quitar(actividadId, alumnoId);

        verify(favoritoRepository).delete(favorito);
    }

    @Test
    void quitar_noExistente_esIdempotente() {
        when(favoritoRepository.findByUsuarioIdAndActividadId(alumnoId, actividadId)).thenReturn(Optional.empty());

        service.quitar(actividadId, alumnoId);

        verify(favoritoRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
