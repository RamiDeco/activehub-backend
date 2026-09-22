package com.activehub.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final EmailVerificadoFilter emailVerificadoFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(
            JwtService jwtService,
            tools.jackson.databind.ObjectMapper objectMapper,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        this.emailVerificadoFilter = new EmailVerificadoFilter(jwtService, objectMapper);
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/registro/alumno",
                                "/api/auth/registro/instructor",
                                "/api/auth/login",
                                // "¿Olvidaste tu contraseña?": las pide quien justamente no
                                // puede iniciar sesión, así que exigir token las haría
                                // inalcanzables. Ninguna de las dos revela si la cuenta
                                // existe — ver SolicitarRecuperacionPasswordService.
                                "/api/auth/recuperar-password",
                                "/api/auth/recuperar-password/confirmar",
                                // "Continuar con Google" pasa por acá justamente para NO tener
                                // sesión todavía. La credencial es el ID token, que se verifica
                                // contra las claves públicas de Google (GoogleIdTokenVerifier).
                                "/api/auth/google",
                                // El client id es público por diseño (viaja en el HTML de
                                // cualquier sitio con Google Sign-In) y la pantalla de login lo
                                // necesita antes de que haya sesión.
                                "/api/auth/google/config"
                        ).permitAll()
                        // "Reportar un problema" de /ayuda, que es una pantalla pública. Quien
                        // necesita soporte muchas veces es justamente alguien que no pudo
                        // registrarse o entrar: exigirle sesión lo dejaría sin forma de avisar.
                        // El service acepta `autorId` en null y guarda el reporte como anónimo.
                        .requestMatchers(HttpMethod.POST, "/api/soporte/reportes").permitAll()
                        // El catálogo público. Las tres listas de taxonomía van juntas
                        // porque el frontend las pide en un mismo Promise.all al arrancar,
                        // antes de que haya sesión: si una sola queda autenticada, la
                        // promesa entera se rechaza y la landing muestra el estado de error
                        // en vez del catálogo. Pasó exactamente eso al sumar los niveles.
                        .requestMatchers(HttpMethod.GET,
                                "/api/categorias",
                                "/api/tipos-actividad",
                                "/api/niveles-intensidad",
                                "/api/actividades",
                                "/api/actividades/**",
                                "/api/fotos/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Después del de autenticación: primero se sabe quién es, después si puede
                // hacer algo más que confirmar su correo.
                .addFilterAfter(emailVerificadoFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
