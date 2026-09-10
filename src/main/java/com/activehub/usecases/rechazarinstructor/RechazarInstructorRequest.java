package com.activehub.usecases.rechazarinstructor;

import com.activehub.shared.error.SinHtml;
public record RechazarInstructorRequest(@SinHtml String motivo) {
}
