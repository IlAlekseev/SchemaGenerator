package ru.croc.vtb.schema.generator;

import java.util.Collection;

import ru.croc.vtb.schema.info.IClassInfo;

public class DtoGenerator {

    private final String packageName;

    private final String schemaPath;

    public DtoGenerator(final String schemaPath, final String packageName) {
        this.schemaPath = schemaPath;
        this.packageName = packageName;
    }

    public void generate() throws Exception {
        final Collection<IClassInfo> classes = new SchemaReader(schemaPath).resolveClasses();
        new ClassGenerator(packageName).generateClasses(classes);
    }

}
