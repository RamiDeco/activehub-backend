package com.activehub.usecases.agregarfavorito;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.favorito.ActividadFavorita;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgregarFavoritoService {

    private final ActividadRepository actividadRepository;
    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public AgregarFavoritoService(
            ActividadRepository actividadRepository,
            FavoritoRepository favoritoRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService
    ) {
        this.actividadRepository = actividadRepository;
        this.favoritoRepository = favoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void agregar(UUID actividadId, UUID alumnoId) {
        if (favoritoRepository.findByUsuarioIdAndActividadId(alumnoId, actividadId).isPresent()) {
            return;
        }

        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));
        Usuario alumno = usuarioRepository.getReferenceById(alumnoId);

        favoritoRepository.save(new ActividadFavorita(alumno, actividad));

        auditService.registrar(alumnoId, AuditAccion.FAVORITO_AGREGADO, "Actividad", actividadId, null);
    }
}
