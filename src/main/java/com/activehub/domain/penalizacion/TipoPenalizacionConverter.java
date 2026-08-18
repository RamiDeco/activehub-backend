package com.activehub.domain.penalizacion;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoPenalizacionConverter implements AttributeConverter<TipoPenalizacion, String> {

    @Override
    public String convertToDatabaseColumn(TipoPenalizacion attribute) {
        return attribute == null ? null : attribute.getEtiqueta();
    }

    @Override
    public TipoPenalizacion convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TipoPenalizacion.fromEtiqueta(dbData);
    }
}
