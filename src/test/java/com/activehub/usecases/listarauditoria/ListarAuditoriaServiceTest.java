package com.activehub.usecases.listarauditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditLog;
import com.activehub.shared.audit.AuditLogRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.activehub.domain.permiso.Permiso;
import com.activehub.domain.permiso.PermisoRepository;
import com.activehub.domain.usuario.RolRepository;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ListarAuditoriaServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PermisoRepository permisoRepository;
    @Mock
    private RolRepository rolRepository;

    private ListarAuditoriaService service;

    @BeforeEach
    void setUp() {
        service = new ListarAuditoriaService(
                auditLogRepository, usuarioRepository, permisoRepository, rolRepository);
        // Los dos catalogos que usa la descripcion legible. Vacios alcanzan para los casos de
        // este test; el que los ejercita es DescripcionAuditoriaTest.
        org.mockito.Mockito.lenient().when(permisoRepository.findAllByOrderByOrdenAsc()).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(rolRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void listar_conActor_resuelveNombreYRol() {
        UUID actorId = UUID.randomUUID();
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre(RolNombre.ADMIN.name());
        Usuario actor = new Usuario();
        actor.setNombre("Ana");
        actor.setApellido("Lopez");
        actor.setRol(rolAdmin);
        ReflectionTestUtils.setField(actor, "id", actorId);

        AuditLog log = new AuditLog(actorId, AuditAccion.LOGIN_OK, "Usuario", actorId, null);

        when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(log));
        when(usuarioRepository.findAllById(List.of(actorId))).thenReturn(List.of(actor));

        List<ListarAuditoriaResponse> resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).actorNombre()).isEqualTo("Ana Lopez");
        assertThat(resultado.get(0).actorRol()).isEqualTo("ADMIN");
        assertThat(resultado.get(0).accion()).isEqualTo("LOGIN_OK");
    }

    @Test
    void listar_sinActor_marcaSistema() {
        AuditLog log = new AuditLog(null, AuditAccion.CLASE_FINALIZADA, "Clase", UUID.randomUUID(), null);

        when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(log));
        when(usuarioRepository.findAllById(List.of())).thenReturn(List.of());

        List<ListarAuditoriaResponse> resultado = service.listar();

        assertThat(resultado.get(0).actorId()).isNull();
        assertThat(resultado.get(0).actorNombre()).isEqualTo("Sistema");
        assertThat(resultado.get(0).actorRol()).isNull();
    }
}
