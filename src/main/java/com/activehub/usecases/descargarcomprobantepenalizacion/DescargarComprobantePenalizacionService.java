package com.activehub.usecases.descargarcomprobantepenalizacion;

import com.activehub.domain.penalizacion.ComprobantePenalizacion;
import com.activehub.domain.penalizacion.ComprobantePenalizacionRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.storage.AlmacenamientoArchivos;
import com.activehub.shared.storage.AlmacenamientoException;
import com.activehub.shared.storage.CarpetaArchivos;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El administrador abre el comprobante de pago de una multa para revisarlo (V36). El archivo
 * es privado: se sirve por aca, con permiso, y nunca por una URL publica del almacenamiento.
 */
@Service
public class DescargarComprobantePenalizacionService {

    private final ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    private final AlmacenamientoArchivos almacenamiento;

    public DescargarComprobantePenalizacionService(
            ComprobantePenalizacionRepository comprobantePenalizacionRepository,
            AlmacenamientoArchivos almacenamiento) {
        this.comprobantePenalizacionRepository = comprobantePenalizacionRepository;
        this.almacenamiento = almacenamiento;
    }

    @Transactional(readOnly = true)
    public ComprobanteDescarga descargar(UUID comprobanteId) {
        ComprobantePenalizacion comprobante = comprobantePenalizacionRepository.findById(comprobanteId)
                .orElseThrow(() -> new NoEncontradoException("Comprobante no encontrado."));
        try {
            byte[] contenido = almacenamiento.leer(
                    CarpetaArchivos.COMPROBANTES_PENALIZACION, comprobante.getRutaArchivo());
            return new ComprobanteDescarga(comprobante.getNombreArchivo(), comprobante.getTipoContenido(), contenido);
        } catch (AlmacenamientoException e) {
            throw new NoEncontradoException("No pudimos leer el archivo.");
        }
    }
}
