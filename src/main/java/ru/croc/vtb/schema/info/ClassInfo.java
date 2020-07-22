package ru.croc.vtb.schema.info;

import java.util.ArrayList;
import java.util.Collection;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClassInfo implements IClassInfo {

    private String name;

    private String type;

    private String logicalType;

    private Collection<FieldInfo> fields = new ArrayList<>();
}
