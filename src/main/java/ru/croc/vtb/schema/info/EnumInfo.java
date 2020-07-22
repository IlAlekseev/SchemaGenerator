package ru.croc.vtb.schema.info;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EnumInfo implements IClassInfo {

    private String name;

    private List<String> symbols = new ArrayList<>();
}
