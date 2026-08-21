package com.activehub.usecases.quitarfavorito;

import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuitarFavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final AuditService auditService;

    public QuitarFavoritoService(FavoritoRepository favoritoRepository, AuditService auditService) {
        this.favoritoRepository = favoritoRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void quitar(UUID actividadId, UUID alumnoId) {
        favoritoRepository.findByUsuarioIdAndActividadId(alumnoId, actividadId)
                .ifPresent(favorito -> {
                    favoritoRepository.delete(favorito);
                    auditService.registrar(alumnoId, AuditAccion.FAVORITO_QUITADO, "Actividad", actividadId, null);
                });
    }
}
