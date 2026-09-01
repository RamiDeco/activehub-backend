package com.activehub.usecases.verfotoperfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.error.NoEncontradoException;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VerFotoPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @TempDir
    Path tempDir;

    private VerFotoPerfilService service;
    private UUID usuarioId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new VerFotoPerfilService(usuarioRepository, tempDir.toString());

        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
    }

    @Test
    void ver_fotoExistente_devuelveContenido() throws Exception {
        Files.write(tempDir.resolve("foto.png"), "contenido".getBytes());
        usuario.setFotoPath("foto.png");
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        FotoDescarga foto = service.ver(usuarioId);

        assertThat(foto.tipoContenido()).isEqualTo("image/png");
        assertThat(foto.contenido()).isEqualTo("contenido".getBytes());
    }

    @Test
    void ver_sinFoto_lanzaNoEncontrado() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.ver(usuarioId)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void ver_usuarioInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ver(usuarioId)).isInstanceOf(NoEncontradoException.class);
    }
}
