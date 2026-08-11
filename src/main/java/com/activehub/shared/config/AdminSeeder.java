package com.activehub.shared.config;

import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedEmail;
    private final String seedPassword;

    public AdminSeeder(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-seed.email}") String seedEmail,
            @Value("${app.admin-seed.password}") String seedPassword
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedEmail == null || seedEmail.isBlank() || seedPassword == null || seedPassword.isBlank()) {
            log.info("ADMIN_SEED_EMAIL/ADMIN_SEED_PASSWORD no configurados, no se siembra ningun admin inicial.");
            return;
        }

        if (usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse(seedEmail)) {
            log.info("El admin sembrado ({}) ya existe, no se crea de nuevo.", seedEmail);
            return;
        }

        var rolAdmin = rolRepository.findByNombre(RolNombre.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no encontrado, revisar la migracion V2."));

        Usuario admin = new Usuario();
        admin.setNombre("Admin");
        admin.setApellido("ActiveHub");
        admin.setEmail(seedEmail.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(seedPassword));
        admin.setRol(rolAdmin);
        admin.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(admin);

        log.info("Admin inicial sembrado: {}", seedEmail);
    }
}
