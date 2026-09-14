package com.activehub.usecases.verfotoperfil;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.error.NoEncontradoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerFotoPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final String directorioAlmacenamiento;

    public VerFotoPerfilService(
            UsuarioRepository usuarioRepository,
            @Value("${app.storage.fotos-perfil-dir}") String directorioAlmacenamiento
    ) {
        this.usuarioRepository = usuarioRepository;
        this.directorioAlmacenamiento = directorioAlmacenamiento;
    }

    @Transactional(readOnly = true)
    public FotoDescarga ver(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        if (usuario.getFotoPath() == null) {
            throw new NoEncontradoException("El usuario no tiene foto de perfil.");
        }

        try {
            byte[] contenido = Files.readAllBytes(Path.of(directorioAlmacenamiento).resolve(usuario.getFotoPath()));
            return new FotoDescarga(tipoContenido(usuario.getFotoPath()), contenido);
        } catch (IOException e) {
            throw new NoEncontradoException("No pudimos leer la foto.");
        }
    }

    private String tipoContenido(String rutaArchivo) {
        String ruta = rutaArchivo.toLowerCase();
        if (ruta.endsWith(".png")) {
            return "image/png";
        }
        return "image/jpeg";
    }
}
