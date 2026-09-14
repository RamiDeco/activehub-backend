package com.activehub.usecases.denunciarresenia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.denuncia.DenunciaRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DenunciarReseniaServiceTest {

    @Mock private ReseniaRepository reseniaRepository;
    @Mock private DenunciaRepository denunciaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InstructorVerificadoGuard instructorVerificadoGuard;
    @Mock private AuditService auditService;

    private DenunciarReseniaService service;
    private UUID reseniaId;
    private UUID instructorId;
    private Resenia resenia;

    @BeforeEach
    void setUp() {
        service = new DenunciarReseniaService(
                reseniaRepository, denunciaRepository, usuarioRepository, instructorVerificadoGuard, auditService);

        instructorId = UUID.randomUUID();
        Usuario instructor = new Usuario();
        ReflectionTestUtils.setField(instructor, "id", instructorId);

        Actividad actividad = new Actividad();
        actividad.setNombre("Yoga");
        actividad.setInstructor(instructor);
        ReflectionTestUtils.setField(actividad, "id", UUID.randomUUID());

        Clase clase = new Clase();
        clase.setActividad(actividad);
        ReflectionTestUtils.setField(clase, "id", UUID.randomUUID());

        reseniaId = UUID.randomUUID();
        resenia = new Resenia();
        resenia.setClase(clase);
        resenia.setPuntaje(1);
        resenia.setComentario("Comentario agresivo");
        ReflectionTestUtils.setField(resenia, "id", reseniaId);
    }

    private void denunciaSeGuardaConId() {
        when(denunciaRepository.saveAndFlush(any(Denuncia.class))).thenAnswer(inv -> {
            Denuncia d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            return d;
        });
    }

    @Test
    void denunciar_creaLaDenunciaApuntandoALaResenia() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));
        when(usuarioRepository.getReferenceById(instructorId)).thenReturn(new Usuario());
        denunciaSeGuardaConId();

        DenunciarReseniaResponse response =
                service.denunciar(reseniaId, new DenunciarReseniaRequest("  Es una difamación  "), instructorId);

        assertThat(response.reseniaId()).isEqualTo(reseniaId);
        assertThat(response.estado()).isEqualTo("Pendiente");

        var captor = org.mockito.ArgumentCaptor.forClass(Denuncia.class);
        verify(denunciaRepository).saveAndFlush(captor.capture());
        Denuncia guardada = captor.getValue();
        assertThat(guardada.getResenia()).isSameAs(resenia);
        // Denuncia de reseña: `clase` queda nulo (antes clase_id era NOT NULL y esto no
        // se podía ni guardar).
        assertThat(guardada.getClase()).isNull();
        assertThat(guardada.getMotivo()).isEqualTo("Es una difamación");
    }

    @Test
    void denunciar_reseniaDeOtroInstructor_lanzaSinPermiso() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() ->
                service.denunciar(reseniaId, new DenunciarReseniaRequest("Motivo"), UUID.randomUUID()))
                .isInstanceOf(SinPermisoException.class);

        verify(denunciaRepository, never()).saveAndFlush(any());
    }

    @Test
    void denunciar_dosVecesLaMisma_lanzaValidacion() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));
        when(denunciaRepository.existsByReseniaIdAndDenuncianteId(reseniaId, instructorId)).thenReturn(true);

        assertThatThrownBy(() ->
                service.denunciar(reseniaId, new DenunciarReseniaRequest("Motivo"), instructorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Ya denunciaste");
    }

    @Test
    void denunciar_reseniaYaOculta_lanzaValidacion() {
        resenia.setOculta(true);
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() ->
                service.denunciar(reseniaId, new DenunciarReseniaRequest("Motivo"), instructorId))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void denunciar_reseniaInexistente_lanzaNoEncontrado() {
        when(reseniaRepository.findById(reseniaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.denunciar(reseniaId, new DenunciarReseniaRequest("Motivo"), instructorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
