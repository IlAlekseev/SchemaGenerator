package ru.croc.vtb.schema.info;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class EnumInfo implements IClassInfo {
    private String name;
    private List<String> symbols = new ArrayList<>();
}
