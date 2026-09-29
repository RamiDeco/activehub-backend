package com.activehub.usecases.iniciarsesiongoogle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.AuthProveedor;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.security.GoogleIdTokenVerifier;
import com.activehub.shared.security.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Los tres desenlaces de "Continuar con Google": es lo que hace que el mismo endpoint sirva
 * para el login y para el alta sin inventar cuentas a medio llenar.
 */
@ExtendWith(MockitoExtension.class)
class IniciarSesionGoogleServiceTest {

    private static final GoogleIdTokenVerifier.DatosGoogle DATOS =
            new GoogleIdTokenVerifier.DatosGoogle("sub-123", "ana@gmail.com", "Ana", "Gomez");

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PerfilAlumnoRepository perfilAlumnoRepository;
    @Mock private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuditService auditService;

    private IniciarSesionGoogleService service;

    @BeforeEach
    void setUp() {
        service = new IniciarSesionGoogleService(
                usuarioRepository, rolRepository, perfilAlumnoRepository, googleIdTokenVerifier,
                passwordEncoder, jwtService, auditService,
                Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC));
        when(googleIdTokenVerifier.verificar("id-token")).thenReturn(DATOS);
    }

    private Usuario cuentaVerificada() {
        Rol rol = new Rol();
        rol.setNombre(RolNombre.ALUMNO.name());
        Usuario u = new Usuario();
        u.setNombre("Ana");
        u.setApellido("Gomez");
        u.setEmail("ana@gmail.com");
        u.setRol(rol);
        u.setEmailVerificado(true);
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    /**
     * El boton del LOGIN manda sin rol. Que no cree nada es el punto: quien aprieta "Iniciar
     * sesion con Google" espera entrar a su cuenta, no que le aparezca una nueva vacia.
     */
    @Test
    void sinRolYSinCuenta_noCreaNada_devuelveSinCuenta() {
        when(usuarioRepository.findAllByEmailConRol("ana@gmail.com")).thenReturn(List.of());

        var respuesta = service.ingresar(new IniciarSesionGoogleRequest("id-token", null));

        assertThat(respuesta.modo()).isEqualTo(IniciarSesionGoogleResponse.MODO_SIN_CUENTA);
        assertThat(respuesta.identidad().email()).isEqualTo("ana@gmail.com");
        assertThat(respuesta.token()).isNull();
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    /**
     * Un instructor no puede darse de alta sin documentacion (RN-12) y Google no la trae: lo
     * unico que aporta es la identidad, para precargar el formulario.
     */
    @Test
    void rolInstructorSinCuenta_noCreaNada_devuelveLaIdentidad() {
        when(usuarioRepository.findAllByEmailConRol("ana@gmail.com")).thenReturn(List.of());

        var respuesta = service.ingresar(new IniciarSesionGoogleRequest("id-token", "INSTRUCTOR"));

        assertThat(respuesta.modo()).isEqualTo(IniciarSesionGoogleResponse.MODO_COMPLETAR_INSTRUCTOR);
        assertThat(respuesta.identidad().idToken()).isEqualTo("id-token");
        assertThat(respuesta.identidad().nombre()).isEqualTo("Ana");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void rolAlumnoSinCuenta_laCrea_verificadaYComoGoogle() {
        when(usuarioRepository.findAllByEmailConRol("ana@gmail.com")).thenReturn(List.of());
        Rol rolAlumno = new Rol();
        rolAlumno.setNombre(RolNombre.ALUMNO.name());
        when(rolRepository.findByNombre(RolNombre.ALUMNO)).thenReturn(Optional.of(rolAlumno));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-aleatorio");
        when(usuarioRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });
        when(jwtService.emitir(any(), anyString(), anyString(), anyBoolean())).thenReturn("token-jwt");

        var respuesta = service.ingresar(new IniciarSesionGoogleRequest("id-token", "ALUMNO"));

        assertThat(respuesta.modo()).isEqualTo(IniciarSesionGoogleResponse.MODO_SESION);
        assertThat(respuesta.cuentaNueva()).isTrue();
        // Google ya verifico el correo, asi que no se manda ningun codigo y la direccion queda
        // reservada en el acto (V26).
        assertThat(respuesta.usuario().emailVerificado()).isTrue();
        assertThat(respuesta.usuario().authProveedor()).isEqualTo(AuthProveedor.GOOGLE.name());
        // Sin telefono ni fecha de nacimiento: Google no los da. De eso se encarga la pantalla
        // "Termina tu registro" del frontend.
        assertThat(respuesta.usuario().telefono()).isNull();
        assertThat(respuesta.usuario().fechaNacimiento()).isNull();
    }

    /** Con cuenta verificada se entra siempre, sin importar el rol pedido ni que no venga. */
    @Test
    void conCuentaVerificada_entra_aunqueVengaSinRol() {
        when(usuarioRepository.findAllByEmailConRol("ana@gmail.com")).thenReturn(List.of(cuentaVerificada()));
        when(jwtService.emitir(any(), anyString(), anyString(), anyBoolean())).thenReturn("token-jwt");

        var respuesta = service.ingresar(new IniciarSesionGoogleRequest("id-token", null));

        assertThat(respuesta.modo()).isEqualTo(IniciarSesionGoogleResponse.MODO_SESION);
        assertThat(respuesta.cuentaNueva()).isFalse();
        assertThat(respuesta.token()).isEqualTo("token-jwt");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    /**
     * Las cuentas SIN verificar con ese correo se ignoran: no probaron nada, y por eso no
     * reservan la direccion (V26). Como instructor, igual manda a completar el formulario.
     */
    @Test
    void conCuentaSinVerificar_laIgnora() {
        Usuario sinVerificar = cuentaVerificada();
        sinVerificar.setEmailVerificado(false);
        when(usuarioRepository.findAllByEmailConRol("ana@gmail.com")).thenReturn(List.of(sinVerificar));

        var respuesta = service.ingresar(new IniciarSesionGoogleRequest("id-token", "INSTRUCTOR"));

        assertThat(respuesta.modo()).isEqualTo(IniciarSesionGoogleResponse.MODO_COMPLETAR_INSTRUCTOR);
    }
}
