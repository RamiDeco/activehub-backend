package com.activehub.usecases.listarreportessoporte;

import com.activehub.domain.soporte.ReporteSoporteRepository;
import com.activehub.domain.usuario.Usuario;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarReportesSoporteService {

    private final ReporteSoporteRepository reporteSoporteRepository;

    public ListarReportesSoporteService(ReporteSoporteRepository reporteSoporteRepository) {
        this.reporteSoporteRepository = reporteSoporteRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarReportesSoporteResponse> listar() {
        return reporteSoporteRepository.findAllConDetalle().stream()
                .map(r -> new ListarReportesSoporteResponse(
                        r.getId(),
                        r.getEmail(),
                        r.getAsunto(),
                        r.getDetalle(),
                        r.getEstado().getEtiqueta(),
                        nombre(r.getUsuario()),
                        r.getUsuario() == null ? null : r.getUsuario().getId(),
                        r.getRespuesta(),
                        nombre(r.getCerradoPor()),
                        r.getCerradoAt(),
                        r.getCreatedAt()))
                .toList();
    }

    /** Null y no "Anónimo": la etiqueta es decision de la pantalla, no del backend. */
    private static String nombre(Usuario usuario) {
        return usuario == null ? null : usuario.getNombre() + " " + usuario.getApellido();
    }
}
