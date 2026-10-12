package com.activehub.usecases.verfotoperfil;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.AlmacenamientoException;
import com.activehub.shared.storage.CarpetaArchivos;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerFotoPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final AlmacenamientoArchivos almacenamiento;

    public VerFotoPerfilService(UsuarioRepository usuarioRepository, AlmacenamientoArchivos almacenamiento) {
        this.usuarioRepository = usuarioRepository;
        this.almacenamiento = almacenamiento;
    }

    @Transactional(readOnly = true)
    public FotoDescarga ver(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        if (usuario.getFotoPath() == null) {
            throw new NoEncontradoException("El usuario no tiene foto de perfil.");
        }

        try {
            byte[] contenido = almacenamiento.leer(CarpetaArchivos.FOTOS_PERFIL, usuario.getFotoPath());
            return new FotoDescarga(tipoContenido(usuario.getFotoPath()), contenido);
        } catch (AlmacenamientoException e) {
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
