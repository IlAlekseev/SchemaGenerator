package ru.croc.vtb.schema.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import ru.croc.vtb.profile.common.dto.schema.IndividualDto;
import ru.croc.vtb.profile.common.dto.schema.extend.FieldExtension;
import ru.croc.vtb.profile.common.dto.schema.extend.FieldExtensionConfig;
import ru.croc.vtb.schema.info.ClassInfo;
import ru.croc.vtb.schema.info.FieldInfo;

public class CustomFieldGenerator {

    private final Map<String, FieldExtensionConfig> fieldExtensions = new HashMap<>();

    public void init() throws Exception {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(FieldExtension.class));
        final Set<BeanDefinition> results = scanner.findCandidateComponents(IndividualDto.class.getPackage().getName());
        for (final BeanDefinition beanDefinition : results) {
            final String className = beanDefinition.getBeanClassName();
            final Class<?> fieldClass = Class.forName(className);
            final FieldExtension extension = fieldClass.getAnnotation(FieldExtension.class);
            for (final FieldExtensionConfig config : extension.config()) {
                addFieldEx(config);
            }
        }
    }

    private void addFieldEx(FieldExtensionConfig config) {
        fieldExtensions.put(config.target().getSimpleName() + "_" + config.fieldName(), config);
    }

    public boolean isSkipField(ClassInfo classInfo, FieldInfo fieldInfo) {
        final FieldExtensionConfig fieldExtension =
                fieldExtensions.get(classInfo.getName() + "Dto" + "_" + fieldInfo.getName());
        return fieldExtension != null && fieldExtension.skipField();
    }

    public void resolveActualFieldType(ClassInfo classInfo, FieldInfo fieldInfo) {
        final FieldExtensionConfig fieldExtension =
                fieldExtensions.get(classInfo.getName() + "Dto" + "_" + fieldInfo.getName());
        if (fieldExtension != null) {
            fieldInfo.setType(fieldExtension.fieldClass().getName());
        }
    }
}
