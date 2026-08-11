package com.activehub.domain.inscripcion;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoInscripcionConverter implements AttributeConverter<EstadoInscripcion, String> {

    @Override
    public String convertToDatabaseColumn(EstadoInscripcion attribute) {
        return attribute == null ? null : attribute.getEtiqueta();
    }

    @Override
    public EstadoInscripcion convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EstadoInscripcion.fromEtiqueta(dbData);
    }
}
