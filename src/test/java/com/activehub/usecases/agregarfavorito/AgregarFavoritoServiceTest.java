package com.activehub.usecases.agregarfavorito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.favorito.ActividadFavorita;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
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
class AgregarFavoritoServiceTest {

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private FavoritoRepository favoritoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditService auditService;

    private AgregarFavoritoService service;
    private UUID actividadId;
    private UUID alumnoId;

    @BeforeEach
    void setUp() {
        service = new AgregarFavoritoService(actividadRepository, favoritoRepository, usuarioRepository, auditService);
        actividadId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
    }

    @Test
    void agregar_actividadExistente_creaFavorito() {
        Actividad actividad = new Actividad();
        ReflectionTestUtils.setField(actividad, "id", actividadId);
        Usuario alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        when(favoritoRepository.findByUsuarioIdAndActividadId(alumnoId, actividadId)).thenReturn(Optional.empty());
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        when(usuarioRepository.getReferenceById(alumnoId)).thenReturn(alumno);

        service.agregar(actividadId, alumnoId);

        verify(favoritoRepository).save(any(ActividadFavorita.class));
    }

    @Test
    void agregar_yaFavorito_esIdempotenteYNoDuplica() {
        ActividadFavorita existente = new ActividadFavorita();
        when(favoritoRepository.findByUsuarioIdAndActividadId(alumnoId, actividadId)).thenReturn(Optional.of(existente));

        service.agregar(actividadId, alumnoId);

        verify(favoritoRepository, never()).save(any());
        verify(actividadRepository, never()).findById(any());
    }

    @Test
    void agregar_actividadInexistente_lanzaNoEncontrado() {
        when(favoritoRepository.findByUsuarioIdAndActividadId(alumnoId, actividadId)).thenReturn(Optional.empty());
        when(actividadRepository.findById(actividadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agregar(actividadId, alumnoId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
