package com.activehub.usecases.resolverdenuncia;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ResolverDenunciaRequest(
        @NotBlank(message = "La acción es obligatoria") String accion,
        // E3A-HU11 criterio 7: el detalle que el denunciante ve junto a la resolución. Opcional.
        @Size(max = 1000, message = "El detalle no puede superar los 1000 caracteres") @SinHtml String detalle,
        /**
         * Solo para SUSPENDER: multa que acompaña a la suspensión. Puede ser cero (o venir
         * null), y en ese caso la sanción es solo la suspensión, sin penalización económica.
         */
        @DecimalMin(value = "0", message = "El monto no puede ser negativo") BigDecimal montoMulta,
        /**
         * Solo para SUSPENDER: cuántos días dura. El mínimo de negocio lo valida el Service
         * contra {@code VentanaPenalizacion.MINIMO_DIAS_SUSPENSION}.
         */
        Integer diasSuspension
) {
}
