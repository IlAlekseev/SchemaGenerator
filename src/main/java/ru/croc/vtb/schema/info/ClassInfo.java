package ru.croc.vtb.schema.info;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
public class ClassInfo implements IClassInfo {
    private String name;
    private String type;
    private String logicalType;

    private Collection<FieldInfo> fields = new ArrayList<>();
}
