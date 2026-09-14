package com.activehub.usecases.registrarinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.DocumentoInstructor;
import com.activehub.domain.usuario.DocumentoInstructorRepository;
import com.activehub.domain.usuario.PerfilInstructor;
import com.activehub.domain.usuario.PerfilInstructorRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.DniEnUsoException;
import com.activehub.shared.error.EmailEnUsoException;
import com.activehub.shared.error.ValidacionException;
import com.activehub.shared.security.JwtService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * E1A-HU04 criterio 9 / RN-12: el alta es todo-o-nada. Lo que se prueba acá es justamente
 * que la validación de la documentación ocurra <b>antes</b> de tocar la base, para que no
 * quede una cuenta creada cuando los archivos no sirven.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrarInstructorServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PerfilInstructorRepository perfilInstructorRepository;
    @Mock private DocumentoInstructorRepository documentoInstructorRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuditService auditService;

    private RegistrarInstructorService service;
    private Path directorio;

    @BeforeEach
    void setUp() throws IOException {
        directorio = Files.createTempDirectory("ah-docs-instructor-test");
        service = new RegistrarInstructorService(
                usuarioRepository, rolRepository, perfilInstructorRepository, documentoInstructorRepository,
                passwordEncoder, jwtService, auditService, directorio.toString());

        Rol rolInstructor = new Rol();
        rolInstructor.setNombre(RolNombre.INSTRUCTOR.name());
        when(rolRepository.findByNombre(RolNombre.INSTRUCTOR)).thenReturn(Optional.of(rolInstructor));

        when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
        when(jwtService.emitir(any(), anyString(), eq(RolNombre.INSTRUCTOR.name()))).thenReturn("token-jwt");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(u, "createdAt", Instant.now());
            return u;
        });
        when(documentoInstructorRepository.save(any(DocumentoInstructor.class))).thenAnswer(inv -> {
            DocumentoInstructor d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
            return d;
        });
    }

    @AfterEach
    void limpiar() throws IOException {
        try (var archivos = Files.walk(directorio)) {
            archivos.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // directorio temporal
                }
            });
        }
    }

    private RegistrarInstructorRequest request() {
        return new RegistrarInstructorRequest(
                "Mateo", "Ríos", "mateo@email.com", "2611234567", null, "Password1",
                LocalDate.of(1990, 4, 2), "Running", 6, "Entrenador de running", true);
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("documentos", "certificado.pdf", "application/pdf", new byte[]{1, 2, 3});
    }

    private int archivosEnDisco() {
        var listado = directorio.toFile().listFiles();
        return listado == null ? 0 : listado.length;
    }

    @Test
    void registrar_conDocumentacion_creaUsuarioPerfilYDocumentos() {
        RegistrarInstructorResponse response = service.registrar(request(), List.of(pdf()));

        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.usuario().email()).isEqualTo("mateo@email.com");
        assertThat(response.usuario().rol()).isEqualTo("INSTRUCTOR");

        verify(usuarioRepository).saveAndFlush(any(Usuario.class));
        verify(perfilInstructorRepository).save(any(PerfilInstructor.class));
        verify(documentoInstructorRepository).save(any(DocumentoInstructor.class));
        assertThat(archivosEnDisco()).isEqualTo(1);
    }

    @Test
    void registrar_perfilNaceEnPendiente() {
        // RN-12: el instructor no puede publicar hasta que un admin lo valide.
        service.registrar(request(), List.of(pdf()));

        ArgumentCaptor<PerfilInstructor> captor = ArgumentCaptor.forClass(PerfilInstructor.class);
        verify(perfilInstructorRepository).save(captor.capture());
        assertThat(captor.getValue().getEstadoVerificacion().name()).isEqualTo("PENDIENTE");
    }

    @Test
    void registrar_guardaElMetadatoDeCadaDocumento() {
        MockMultipartFile jpg = new MockMultipartFile("documentos", "dni.jpg", "image/jpeg", new byte[]{9, 9});
        service.registrar(request(), List.of(pdf(), jpg));

        ArgumentCaptor<DocumentoInstructor> captor = ArgumentCaptor.forClass(DocumentoInstructor.class);
        verify(documentoInstructorRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        var guardados = captor.getAllValues();
        assertThat(guardados).extracting(DocumentoInstructor::getNombreArchivo)
                .containsExactly("certificado.pdf", "dni.jpg");
        assertThat(guardados).extracting(DocumentoInstructor::getTipoDocumento)
                .containsExactly("application/pdf", "image/jpeg");
        // El nombre en disco es un UUID, no el original: dos usuarios pueden subir "dni.jpg".
        assertThat(guardados.get(0).getRutaArchivo()).isNotEqualTo("certificado.pdf").endsWith(".pdf");
        assertThat(archivosEnDisco()).isEqualTo(2);
    }

    @Test
    void registrar_sinDocumentos_noCreaLaCuenta() {
        assertThatThrownBy(() -> service.registrar(request(), List.of()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("al menos un documento");

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_documentosNulo_noCreaLaCuenta() {
        assertThatThrownBy(() -> service.registrar(request(), null))
                .isInstanceOf(ValidacionException.class);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_archivoVacio_noCreaLaCuenta() {
        MultipartFile vacio = new MockMultipartFile("documentos", "vacio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.registrar(request(), List.of(vacio)))
                .isInstanceOf(ValidacionException.class);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_formatoNoPermitido_noCreaLaCuentaNiEscribeEnDisco() {
        // La validación tiene que correr ANTES de tocar la base: si corriera después,
        // quedaría el Usuario creado sin documentación, que es lo que RN-12 prohíbe.
        MultipartFile exe = new MockMultipartFile("documentos", "virus.exe", "application/x-msdownload", new byte[]{1});

        assertThatThrownBy(() -> service.registrar(request(), List.of(exe)))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("PDF, JPG o PNG");

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(documentoInstructorRepository, never()).save(any());
        assertThat(archivosEnDisco()).isZero();
    }

    @Test
    void registrar_archivoDemasiadoGrande_noCreaLaCuenta() {
        // El límite es 5 MB, igual que lo que promete la pantalla de registro.
        MultipartFile pesado =
                new MockMultipartFile("documentos", "grande.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.registrar(request(), List.of(pesado)))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("tamaño máximo");

        verify(usuarioRepository, never()).saveAndFlush(any());
        assertThat(archivosEnDisco()).isZero();
    }

    @Test
    void registrar_emailEnUso_noEscribeArchivos() {
        when(usuarioRepository.existsByEmailIgnoreCaseAndDeletedFalse("mateo@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request(), List.of(pdf())))
                .isInstanceOf(EmailEnUsoException.class);

        verify(usuarioRepository, never()).saveAndFlush(any());
        assertThat(archivosEnDisco()).isZero();
    }

    @Test
    void registrar_dniEnUso_noCreaLaCuenta() {
        var conDni = new RegistrarInstructorRequest(
                "Mateo", "Ríos", "mateo@email.com", "2611234567", "30123456", "Password1",
                LocalDate.of(1990, 4, 2), "Running", 6, "Entrenador", true);
        when(usuarioRepository.existsByDniAndDeletedFalse("30123456")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(conDni, List.of(pdf())))
                .isInstanceOf(DniEnUsoException.class);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_normalizaElEmailAMinusculas() {
        var mayus = new RegistrarInstructorRequest(
                "Mateo", "Ríos", "  MATEO@Email.COM ", "2611234567", null, "Password1",
                LocalDate.of(1990, 4, 2), "Running", 6, "Entrenador", true);

        RegistrarInstructorResponse response = service.registrar(mayus, List.of(pdf()));

        // El login busca con lower(): guardarlo con mayúsculas dejaba al instructor sin poder entrar.
        assertThat(response.usuario().email()).isEqualTo("mateo@email.com");
    }

    @Test
    void registrar_auditaElAltaYCadaDocumento() {
        service.registrar(request(), List.of(pdf()));

        verify(auditService).registrar(any(), eq(AuditAccion.REGISTRO_INSTRUCTOR), eq("Usuario"), any(), any());
        verify(auditService).registrar(
                any(), eq(AuditAccion.DOCUMENTO_INSTRUCTOR_SUBIDO), eq("DocumentoInstructor"), any(), any());
    }
}
