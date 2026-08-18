package com.activehub.domain.denuncia;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoDenunciaConverter implements AttributeConverter<EstadoDenuncia, String> {

    @Override
    public String convertToDatabaseColumn(EstadoDenuncia attribute) {
        return attribute == null ? null : attribute.getEtiqueta();
    }

    @Override
    public EstadoDenuncia convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EstadoDenuncia.fromEtiqueta(dbData);
    }
}
