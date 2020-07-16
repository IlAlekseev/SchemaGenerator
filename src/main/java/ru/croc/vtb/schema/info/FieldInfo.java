package ru.croc.vtb.schema.info;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FieldInfo implements IClassInfo {
    private String name;
    private String type;
    private String definition;
    private boolean isOptional;
    private boolean isCollection;

    private String relationFieldFrom;
    private String relationFieldTo;
}
