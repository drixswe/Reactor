package ink.reactor.dataparser.type.block.properties;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import ink.reactor.dataparser.type.block.BlockFacing;
import ink.reactor.dataparser.type.block.BlockParserUtils;
import ink.reactor.fission.classes.enums.JavaEnum;
import ink.reactor.fission.classes.enums.JavaEnumObject;
import ink.reactor.fission.field.JavaFieldNames;
import ink.reactor.fission.method.JavaMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
@Getter
public class BlockProperties {
    private final Map<String, JavaEnum> properties = new HashMap<>();
    private final String packageName;

    public void createPropertyClasses(final JSONObject blockProperties) {
        final Set<Map.Entry<String, Object>> entries = blockProperties.entrySet();

        for (final Map.Entry<String, Object> entry : entries) {
            final JSONArray values = (JSONArray) entry.getValue();
            JavaEnum javaEnum = this.properties.get(entry.getKey());

            if (javaEnum != null) {
                fillWithAbsentValues(javaEnum, values);
                continue;
            }

            if (!isValidPropertyEntry(values)) {
                continue;
            }

            javaEnum = new JavaEnum(packageName, "Block" + BlockParserUtils.toClassName(entry.getKey()));
            for (final Object value : values) {
                javaEnum.addEnumObjects(new JavaEnumObject(value.toString().toUpperCase()));
            }
            this.properties.put(entry.getKey(), javaEnum);
        }
    }

    private boolean isValidPropertyEntry(final JSONArray values) {
        for (final Object value : values) {
            if (BlockParserUtils.getValueClass(value.toString()) != String.class) {
                return false;
            }
        }
        return true;
    }

    public void addMethodParameters(final JavaMethod method, final JSONObject properties) {
        final Set<Map.Entry<String, Object>> entries = properties.entrySet();

        for (final Map.Entry<String, Object> entry : entries) {
            final String fieldName = (entry.getKey().equals("short")) ? "shortNumber" : JavaFieldNames.toFieldLocalName(entry.getKey()); // Piston has a property with this name
            final Class<?> valueClass = BlockParserUtils.getValueClass(entry.getValue().toString()); // json property entry is always a string, so don't matter the call to .toString()

            if (valueClass != String.class) {
                method.addParameterFinal(valueClass, fieldName);
                continue;
            }

            final JavaEnum javaEnum = this.properties.get(entry.getKey());
            if (javaEnum != null) {
                method.addParameterFinal(javaEnum.getClassName(), fieldName);
                continue;
            }
            method.addParameterFinal(valueClass, fieldName);
        }
    }

    private void fillWithAbsentValues(final JavaEnum javaEnum, final JSONArray values) {
        Collection<JavaEnumObject> enumObjects = javaEnum.getEnumObjects();
        valuesLoop : for (final Object value : values) {
            final String valueName = value.toString().toUpperCase();

            for (final JavaEnumObject enumObject : enumObjects) {
                if (enumObject.getName().equals(valueName)) {
                    continue valuesLoop;
                }
            }

            enumObjects.add(new JavaEnumObject(valueName));
        }
    }
}
