package com.activehub.usecases.subirfotoperfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubirFotoPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditService auditService;

    @TempDir
    Path tempDir;

    private SubirFotoPerfilService service;
    private UUID usuarioId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new SubirFotoPerfilService(usuarioRepository, auditService, tempDir.toString());

        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    @Test
    void subir_fotoValida_actualizaUsuarioYAudita() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "perfil.jpg", "image/jpeg", "contenido".getBytes());
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        SubirFotoPerfilResponse response = service.subir(archivo, usuarioId);

        assertThat(response.usuarioId()).isEqualTo(usuarioId);
        assertThat(usuario.getFotoPath()).isNotNull();
        verify(usuarioRepository).save(usuario);
        verify(auditService).registrar(
                eq(usuarioId), eq(AuditAccion.FOTO_PERFIL_SUBIDA), eq("Usuario"), eq(usuarioId), isNull());
    }

    @Test
    void subir_fotoAnterior_laBorraDelDisco() throws Exception {
        Path anterior = tempDir.resolve("vieja.jpg");
        Files.write(anterior, "vieja".getBytes());
        usuario.setFotoPath("vieja.jpg");

        MockMultipartFile archivo = new MockMultipartFile("archivo", "nueva.jpg", "image/jpeg", "contenido".getBytes());
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        service.subir(archivo, usuarioId);

        assertThat(Files.exists(anterior)).isFalse();
    }

    @Test
    void subir_formatoNoPermitido_lanzaValidacion() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "video.mp4", "video/mp4", "x".getBytes());

        assertThatThrownBy(() -> service.subir(archivo, usuarioId)).isInstanceOf(ValidacionException.class);
    }

    @Test
    void subir_archivoVacio_lanzaValidacion() {
        MockMultipartFile vacio = new MockMultipartFile("archivo", "perfil.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.subir(vacio, usuarioId)).isInstanceOf(ValidacionException.class);
    }

    @Test
    void subir_usuarioInexistente_lanzaNoEncontrado() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "perfil.jpg", "image/jpeg", "contenido".getBytes());
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subir(archivo, usuarioId)).isInstanceOf(NoEncontradoException.class);
    }
}
