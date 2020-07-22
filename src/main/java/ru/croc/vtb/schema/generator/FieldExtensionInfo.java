package ru.croc.vtb.schema.generator;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.croc.vtb.profile.common.dto.schema.SchemaDto;

@Data
@AllArgsConstructor
public class FieldExtensionInfo {

    private final Class<? extends SchemaDto> target;

    private final String fieldName;

    private final Class<?> fieldClass;
}
