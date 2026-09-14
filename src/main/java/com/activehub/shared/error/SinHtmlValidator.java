package com.activehub.shared.error;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class SinHtmlValidator implements ConstraintValidator<SinHtml, String> {

    /**
     * Una etiqueta de apertura o cierre: {@code <script>}, {@code </b>}, {@code <img …>}.
     * Deliberadamente NO rechaza un {@code <} suelto: "cupo < 10" es texto legítimo y
     * bloquearlo genera falsos positivos que después alguien "arregla" sacando la validación.
     */
    private static final Pattern ETIQUETA = Pattern.compile("</?[a-zA-Z][^>]*>");

    /** Payloads que no necesitan una etiqueta: `javascript:alert(1)` en un campo de enlace. */
    private static final Pattern PROTOCOLO_PELIGROSO =
            Pattern.compile("(?i)\\b(javascript|vbscript|data)\\s*:");

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        // El campo vacío u obligatorio es problema de @NotBlank: acá solo se mira el contenido.
        if (valor == null || valor.isBlank()) {
            return true;
        }
        return !ETIQUETA.matcher(valor).find() && !PROTOCOLO_PELIGROSO.matcher(valor).find();
    }
}
