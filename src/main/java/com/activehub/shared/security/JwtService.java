package com.activehub.shared.security;

import com.activehub.domain.usuario.RolNombre;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMin;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-min}") long expirationMin
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMin = expirationMin;
        this.expirationMillis = expirationMin * 60_000L;
    }

    /**
     * Ventana de inactividad, en minutos. El frontend la usa para decidir cada cuánto renovar
     * el token en vez de tener el número duplicado y desincronizado del lado del cliente.
     */
    public long getExpiracionMinutos() {
        return expirationMin;
    }

    /**
     * El rol viaja como texto y no como enum: desde E4Ad-HU08 el admin puede crear roles
     * propios, y el filtro arma la authority `ROLE_<nombre>` con lo que venga en el claim.
     * Lo que decide el acceso son los permisos de `ConfiguracionRol` (RN-19), no el nombre.
     */
    public String emitir(UUID usuarioId, String email, String rol) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("email", email)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(key)
                .compact();
    }

    public Claims validarYObtenerClaims(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }
}
