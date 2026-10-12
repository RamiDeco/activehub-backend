package com.activehub.domain.soporte;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoReporteSoporteConverter implements AttributeConverter<EstadoReporteSoporte, String> {

    @Override
    public String convertToDatabaseColumn(EstadoReporteSoporte attribute) {
        return attribute == null ? null : attribute.getEtiqueta();
    }

    @Override
    public EstadoReporteSoporte convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EstadoReporteSoporte.fromEtiqueta(dbData);
    }
}
