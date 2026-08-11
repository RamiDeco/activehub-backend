package com.activehub.domain.actividad;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NivelIntensidadConverter implements AttributeConverter<NivelIntensidad, String> {

    @Override
    public String convertToDatabaseColumn(NivelIntensidad attribute) {
        return attribute == null ? null : attribute.getEtiqueta();
    }

    @Override
    public NivelIntensidad convertToEntityAttribute(String dbData) {
        return dbData == null ? null : NivelIntensidad.fromEtiqueta(dbData);
    }
}
