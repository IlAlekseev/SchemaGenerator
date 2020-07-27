package ru.croc.vtb.schema.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;
import com.sun.codemodel.JFieldVar;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.text.WordUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.CaseFormat;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JEnumConstant;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

import ru.croc.vtb.profile.common.dto.schema.DtoRelation;
import ru.croc.vtb.profile.common.dto.schema.DtoType;
import ru.croc.vtb.profile.common.dto.schema.IndividualRelationDto;
import ru.croc.vtb.profile.common.dto.schema.SchemaDto;
import ru.croc.vtb.profile.common.dto.schema.SchemaEnum;
import ru.croc.vtb.schema.info.ClassInfo;
import ru.croc.vtb.schema.info.EnumInfo;
import ru.croc.vtb.schema.info.FieldInfo;
import ru.croc.vtb.schema.info.IClassInfo;

public class ClassGenerator {

    public static final Map<String, Class<?>> TYPE_TO_CLASS_MAP = new HashMap<>();

    static {
        TYPE_TO_CLASS_MAP.put("string", String.class);
        TYPE_TO_CLASS_MAP.put("DateTime", Date.class);
        TYPE_TO_CLASS_MAP.put("int", Integer.class);
        TYPE_TO_CLASS_MAP.put("boolean", Boolean.class);
    }

    private final String packageName;

    public ClassGenerator(final String packageName) {
        this.packageName = packageName;
    }

    public void generateClasses(final Collection<IClassInfo> rootInfos) {
        final JCodeModel codeModel = new JCodeModel();
        final JPackage pack = codeModel._package(packageName);
        final Collection<String> individualRelations = new ArrayList<>();
        try {
            for (final IClassInfo rootInfo : rootInfos) {
                if (rootInfo instanceof ClassInfo) {
                    final ClassInfo classInfo = (ClassInfo) rootInfo;
                    if (classInfo.getName().contains("Proxy")) {
                        continue;
                    }
                    final JDefinedClass aClass;
                    if (shouldBeAbstract(classInfo)) {
                        aClass = pack._class(JMod.ABSTRACT | JMod.PUBLIC, formatClassName(classInfo.getName()));
                    } else {
                        aClass = pack._class(formatClassName(classInfo.getName()));
                        aClass.annotate(Builder.class);
                    }
                    aClass.annotate(AllArgsConstructor.class);
                    aClass.annotate(NoArgsConstructor.class);

                    final JAnnotationUse classAnnotation = aClass.annotate(DtoType.class);
                    classAnnotation.param("name", classInfo.getName());
                    classAnnotation.param("type", classInfo.getType());
                    classAnnotation.param("logicalType", classInfo.getLogicalType());

                    final boolean isIndividual = "Individual".equals(classInfo.getName());

                    final JAnnotationUse jsonIncludeAnnotation = aClass.annotate(JsonInclude.class);
                    jsonIncludeAnnotation.param("value", JsonInclude.Include.NON_NULL);

                    aClass._implements(SchemaDto.class);

                    final JDefinedClass fieldsClass = aClass._class(JMod.STATIC | JMod.PUBLIC, "Fields");

                    for (FieldInfo field : classInfo.getFields()) {
                        final Class<?> classValue = getClassValue(field.getType());
                        final JType valueType;
                        if (classValue != null) {
                            valueType = codeModel.ref(classValue);
                        } else {
                            valueType = codeModel.parseType(field.getType() + "Dto");
                        }
                        final JVar jVar;
                        if (field.isCollection()) {
                            JClass collectionClass = codeModel.ref(Collection.class).narrow(valueType);
                            JClass listClass = codeModel.ref(ArrayList.class).narrow(valueType);
                            jVar = aClass.field(JMod.PRIVATE, collectionClass, formatFieldName(field.getName()));
                            generateGetterAndSetter(jVar, aClass, codeModel);
                        } else {
                            jVar = aClass.field(JMod.PRIVATE, valueType, formatFieldName(field.getName()));
                            generateGetterAndSetter(jVar, aClass, codeModel);
                        }

                        final JVar fieldStatic = fieldsClass.field(JMod.STATIC | JMod.PUBLIC | JMod.FINAL,
                                String.class,
                                formatStaticFieldName(field.getName()));
                        fieldStatic.init(JExpr.lit(field.getName()));

                        final JAnnotationUse jsonAnnotation = jVar.annotate(JsonProperty.class);
                        jsonAnnotation.param("value", JExpr.ref(JExpr.ref("Fields"), fieldStatic.name()));

                        if (field.getRelationFieldFrom() != null || field.getRelationFieldTo() != null) {
                            final JAnnotationUse fieldAnnotation = jVar.annotate(DtoRelation.class);
                            if (field.getRelationFieldFrom() != null) {
                                fieldAnnotation.param("relationFieldFrom", field.getRelationFieldFrom());
                            }

                            if (field.getRelationFieldTo() != null) {
                                fieldAnnotation.param("relationFieldTo", field.getRelationFieldTo());
                            }

                            if (isIndividual) {
                                individualRelations.add(field.getType());
                            }
                        } else {
                            if (!field.isCollection()
                                    && !field.isOptional()
                                    && !field.getName().equals("uuid")
                                    && !field.getName().equals("individualUUID")) {
                                jsonAnnotation.param("required", true);
                            }
                        }
                    }
                } else if (rootInfo instanceof EnumInfo) {
                    final EnumInfo enumInfo = (EnumInfo) rootInfo;
                    JDefinedClass eClass = pack._enum(formatClassName(enumInfo.getName()));
                    eClass._implements(SchemaEnum.class);

                    final String valueVarName = "value";
                    final String valueGetName = "getValue";
                    final JFieldVar valueEnumField = eClass.field(JMod.PRIVATE, String.class, valueVarName);
                    final JMethod constructor = eClass.constructor(JMod.NONE);
                    constructor.param(valueEnumField.type(), valueVarName);
                    constructor.body().assign(JExpr._this().ref(valueVarName), JExpr.ref(valueVarName));

                    JMethod getter = eClass.method(JMod.PUBLIC, String.class, valueGetName);
                    getter.body()._return(valueEnumField);
                    getter.annotate(JsonValue.class);

                    JMethod toString = eClass.method(JMod.PUBLIC, String.class, "toString");
                    toString.body()._return(valueEnumField);
                    toString.annotate(Override.class);

                    for (int i = 0; i < enumInfo.getSymbols().size(); i++) {
                        final String symbol = enumInfo.getSymbols().get(i);
                        final JEnumConstant jEnumConstant = eClass.enumConstant(getValidEnumConstantName(symbol, i));
                        jEnumConstant.arg(JExpr.lit(symbol));
/*
                        final JAnnotationUse jsonAnnotation = jEnumConstant.annotate(JsonProperty.class);
                        jsonAnnotation.param("value", JExpr.lit(symbol));
*/
                    }
                }
            }

            individualRelations.forEach(type -> {
                JDefinedClass relationClass = pack._getClass(formatClassName(type));
                relationClass._implements(IndividualRelationDto.class);
            });

            final File outFile = new File("./out");
            if (!outFile.exists()) {
                outFile.mkdir();
            }
            codeModel.build(outFile);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean shouldBeAbstract(ClassInfo classInfo) {
        return classInfo.getName().equals("Metadata");
    }

    private String formatStaticFieldName(final String name) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
    }

    private String formatClassName(final String name) {
        String className = name;
        if (name.contains("_")) {
            className = WordUtils.capitalizeFully(name, '_').replaceAll("_", "");
        }
        return className + "Dto";
    }

    private String formatFieldName(final String name) {
        if (!name.contains("_")) {
            return name;
        }
        final String capitalized = WordUtils.capitalizeFully(name, '_').replaceAll("_", "");
        return name.charAt(0) + capitalized.substring(1);
    }

    private String getValidEnumConstantName(final String symbol, final int i) {
        String result = symbol;
        if (!Character.isLetter(result.charAt(0))) {
            result = "_" + result;
        }

        return toEng(result.toLowerCase()).replaceAll(" ", "_").toUpperCase();
    }

    private String toEng(String text) {
        char[] abcCyr = { 'а',
                'б',
                'в',
                'г',
                'д',
                'е',
                'ё',
                'ж',
                'з',
                'и',
                'й',
                'к',
                'л',
                'м',
                'н',
                'о',
                'п',
                'р',
                'с',
                'т',
                'у',
                'ф',
                'х',
                'ц',
                'ч',
                'ш',
                'щ',
                'ы',
                'э',
                'ю',
                'я',
                'ь',
                'ъ' };
        String[] abcLat = { "a",
                "b",
                "v",
                "g",
                "d",
                "e",
                "jo",
                "zh",
                "z",
                "i",
                "i",
                "k",
                "l",
                "m",
                "n",
                "o",
                "p",
                "r",
                "s",
                "t",
                "u",
                "f",
                "h",
                "ts",
                "ch",
                "sh",
                "sch",
                "i",
                "e",
                "ju",
                "ja",
                "",
                "" };

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            boolean found = false;
            for (int x = 0; x < abcCyr.length; x++) {
                if (text.charAt(i) == abcCyr[x]) {
                    builder.append(abcLat[x]);
                    found = true;
                    break;
                }
            }
            if (!found) {
                builder.append(text.charAt(i));
            }
        }
        return builder.toString();
    }

    private void generateGetterAndSetter(final JVar field, JDefinedClass jc, JCodeModel codeModel) {
        String methodName = field.name();
        methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);

        JMethod getter = jc.method(JMod.PUBLIC, field.type(), "get" + methodName);
        getter.body()._return(field);

        JMethod setter = jc.method(JMod.PUBLIC, codeModel.VOID, "set" + methodName);
        setter.param(field.type(), field.name());
        setter.body().assign(JExpr._this().ref(field.name()), JExpr.ref(field.name()));
    }

    private Class<?> getClassValue(final String type) {
        final Class<?> typeClass = TYPE_TO_CLASS_MAP.get(type);
        if (typeClass != null) {
            return typeClass;
        }
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
