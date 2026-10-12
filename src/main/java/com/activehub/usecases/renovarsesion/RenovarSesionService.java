package com.activehub.usecases.renovarsesion;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.UsuarioSuspendidoException;
import com.activehub.shared.security.JwtService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E1A-HU02 criterio 3: la sesion expira automaticamente por inactividad.
 *
 * <p>No hay entidad {@code Sesion} y no hace falta: con JWT stateless, la ventana de
 * inactividad <b>es</b> el TTL del token ({@code app.jwt.expiration-min}). El frontend llama
 * a este endpoint mientras el usuario esta usando la aplicacion, asi que el token se renueva
 * solo si hay actividad; si el usuario se va, el ultimo token emitido vence y la proxima
 * request cae en 401. Antes el TTL era de 2 horas fijas y no se renovaba: la sesion vencia
 * por antiguedad, estuviera el usuario usando la app o no, que es justo lo contrario.
 *
 * <p>Se revalida el estado del usuario en cada renovacion: una cuenta suspendida o dada de
 * baja no puede seguir estirando su sesion con el token que ya tenia.
 */
@Service
public class RenovarSesionService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public RenovarSesionService(UsuarioRepository usuarioRepository, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public RenovarSesionResponse renovar(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO) {
            throw new UsuarioSuspendidoException();
        }

        String token = jwtService.emitir(usuario.getId(), usuario.getEmail(), usuario.getRol().getNombre(), usuario.isEmailVerificado());
        return new RenovarSesionResponse(token, jwtService.getExpiracionMinutos());
    }
}
