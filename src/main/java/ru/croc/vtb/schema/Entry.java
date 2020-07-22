package ru.croc.vtb.schema;

import ru.croc.vtb.schema.generator.DtoGenerator;

public class Entry {

    public static final String DEFAULT_PACKAGE_NAME = "ru.croc.vtb.profile.common.dto.schema";

    public static final String DEFAULT_SCHEMA_PATH = "./schema.avsc";

    public static void main(String[] args) throws Exception {
        new DtoGenerator(args.length > 0 ? args[0] : DEFAULT_SCHEMA_PATH,
                args.length > 1 ? args[1] : DEFAULT_PACKAGE_NAME).generate();
    }
}
