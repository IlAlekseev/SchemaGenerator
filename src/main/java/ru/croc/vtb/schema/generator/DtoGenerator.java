package ru.croc.vtb.schema.generator;

import ru.croc.vtb.schema.info.IClassInfo;

import java.util.Collection;

public class DtoGenerator {
    private String packageName;
    private String schemaPath;

    public DtoGenerator(final String schemaPath, final String packageName) {
        this.schemaPath = schemaPath;
        this.packageName = packageName;
    }

    public void generate() {
        final Collection<IClassInfo> classes = new SchemaReader(schemaPath).resolveClasses();
        new ClassGenerator(packageName).generateClasses(classes);
    }

}
