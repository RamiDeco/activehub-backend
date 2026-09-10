package com.activehub.shared.error;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validación anti-XSS de las reglas de seguridad de la sección 4: el texto libre que escribe
 * un usuario y que después se le muestra a otro no puede traer marcado.
 *
 * <p><b>Por qué rechazar y no sanear.</b> Sanear (borrar las etiquetas y guardar el resto)
 * cambia en silencio lo que la persona escribió; para campos como el motivo de una denuncia
 * o la descripción de una actividad, eso es peor que devolver un 400 y que lo corrija. Y
 * como se rechaza en el borde, lo que queda guardado está limpio: no hay que acordarse de
 * escapar en cada pantalla que lo lea.
 *
 * <p>React ya escapa por defecto al renderizar, así que esto es la segunda línea, no la
 * única. Sirve igual el día que ese texto salga por otro lado (un mail, un PDF, un export)
 * donde no hay nadie escapando.
 */
@Documented
@Constraint(validatedBy = SinHtmlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SinHtml {

    String message() default "No se permiten etiquetas HTML ni scripts en este campo.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
