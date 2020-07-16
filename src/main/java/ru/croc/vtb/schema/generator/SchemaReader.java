package ru.croc.vtb.schema.generator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import ru.croc.vtb.schema.info.ClassInfo;
import ru.croc.vtb.schema.info.EnumInfo;
import ru.croc.vtb.schema.info.FieldInfo;
import ru.croc.vtb.schema.info.IClassInfo;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;

public class SchemaReader {
    private String schemaPath;

    public SchemaReader(final String schemaPath) {
        this.schemaPath = schemaPath;
    }

    @SuppressWarnings("unchecked")
    public Collection<IClassInfo> resolveClasses() {
        JSONParser parser = new JSONParser();

        final Collection<IClassInfo> classes = new ArrayList<>();

        try (FileReader fileReader = new FileReader(schemaPath)) {
            Object obj = parser.parse(fileReader);

            final JSONArray array = (JSONArray) obj;

            array.forEach(objectJson -> {
                final JSONObject object = (JSONObject) objectJson;

                final String type = (String) object.get("type");
                final String name = (String) object.get("name");

                if (type.equals("enum")) {
                    final EnumInfo enumInfo = new EnumInfo();
                    enumInfo.setName(name);
                    final JSONArray symbols = (JSONArray) object.get("symbols");
                    symbols.forEach(objectSymbol -> {
                        enumInfo.getSymbols().add((String) objectSymbol);
                    });
                    classes.add(enumInfo);
                } else {
                    final ClassInfo classInfo = new ClassInfo();
                    classInfo.setName(name);
                    classInfo.setType((String) object.get("type"));
                    classInfo.setLogicalType((String) object.get("logicalType"));

                    final JSONArray fields = (JSONArray) object.get("fields");
                    fields.forEach(fieldJson -> {
                        final JSONObject field = (JSONObject) fieldJson;

                        final FieldInfo fieldInfo = new FieldInfo();
                        fieldInfo.setName((String) field.get("name"));
                        fieldInfo.setDefinition(field.toJSONString());

                        final Object typeObject = field.get("type");
                        processType(object, field, fieldInfo, typeObject);

                        classInfo.getFields().add(fieldInfo);
                    });

                    final JSONArray relations = (JSONArray) object.get("relations");
                    if (relations != null) {
                        relations.forEach(relationJson -> {
                            final JSONObject relation = (JSONObject) relationJson;

                            final FieldInfo fieldInfo = new FieldInfo();

                            fieldInfo.setName((String) relation.get("name"));
                            fieldInfo.setDefinition(relation.toJSONString());
                            fieldInfo.setCollection(relation.get("count").equals("many"));
                            fieldInfo.setType((String) relation.get("to"));
                            fieldInfo.setRelationFieldFrom((String) relation.get("from_fields"));
                            fieldInfo.setRelationFieldTo((String) relation.get("to_fields"));

                            if (!"individualUUID".equals(fieldInfo.getRelationFieldFrom()) ||
                                    !"uuid".equals(fieldInfo.getRelationFieldTo())) {
                                classInfo.getFields().add(fieldInfo);
                            }
                        });
                    }

                    classes.add(classInfo);
                }
            });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return classes;
    }

    private void processType(final JSONObject object, final JSONObject field, final FieldInfo fieldInfo, final Object typeObject) {
        if (typeObject instanceof String) {
            fieldInfo.setType((String) typeObject);
        } else if (typeObject instanceof JSONArray) {
            final JSONArray typeInfo = (JSONArray) typeObject;
            if (typeInfo.size() != 2 || !typeInfo.get(0).equals("null")) {
                throw new IllegalStateException("Invalid type " + typeObject + "\r\nFor field " + field.get("name") + "\r\nIn object " + object.get("name"));
            }
            fieldInfo.setOptional(true);
            processType(object, field, fieldInfo, typeInfo.get(1));
        } else {
            final JSONObject typeInfo = (JSONObject) typeObject;
            final String type = (String) typeInfo.get("type");
            if (type.equals("array")) {
                fieldInfo.setCollection(true);
                processType(object, field, fieldInfo, typeInfo.get("items"));
            } else {
                fieldInfo.setType((String) typeInfo.get("logicalType"));
            }
        }
    }
}
