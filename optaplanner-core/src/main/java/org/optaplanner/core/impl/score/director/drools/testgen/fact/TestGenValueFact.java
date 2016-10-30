/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.optaplanner.core.impl.score.director.drools.testgen.fact;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.optaplanner.core.impl.domain.common.ReflectionHelper;
import org.optaplanner.core.impl.domain.common.accessor.BeanPropertyMemberAccessor;

public class TestGenValueFact implements TestGenFact {

    private final Object instance;
    private final String variableName;
    private final List<TestGenFactField> fields = new ArrayList<>();
    private final List<TestGenFact> dependencies = new ArrayList<>();
    private final List<Class<?>> imports = new ArrayList<>();

    public TestGenValueFact(int id, Object instance) {
        this.instance = instance;
        this.variableName = instance.getClass().getSimpleName().substring(0, 1).toLowerCase()
                + instance.getClass().getSimpleName().substring(1) + "_" + id;
    }

    @Override
    public void setUp(Map<Object, TestGenFact> existingInstances) {
        dependencies.clear();
        imports.clear();
        imports.add(instance.getClass());
        Class<?> clazz = instance.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                setUpField(field, existingInstances);
            }
            clazz = clazz.getSuperclass();
        }
    }

    private void setUpField(Field field, Map<Object, TestGenFact> existingInstances) {
        String fieldName = field.getName();
        Method setter = ReflectionHelper.getSetterMethod(instance.getClass(), field.getType(), fieldName);
        Method getter = ReflectionHelper.getGetterMethod(instance.getClass(), fieldName);
        if (setter != null && getter != null) {
            BeanPropertyMemberAccessor accessor = new BeanPropertyMemberAccessor(getter);
            Object value = accessor.executeGetter(instance);
            if (value != null) {
                if (field.getType().equals(String.class)) {
                    fields.add(new TestGenFactField(this, accessor, new TestGenStringValueProvider((String) value)));
                } else if (field.getType().isPrimitive()) {
                    fields.add(new TestGenFactField(this, accessor, new TestGenPrimitiveValueProvider(value)));
                } else if (field.getType().isEnum()) {
                    fields.add(new TestGenFactField(this, accessor, new TestGenEnumValueProvider((Enum) value)));
                    imports.add(field.getType());
                } else if (existingInstances.containsKey(value)) {
                    String id = existingInstances.get(value).toString();
                    fields.add(new TestGenFactField(this, accessor, new TestGenExistingInstanceValueProvider(value, id)));
                    dependencies.add(existingInstances.get(value));
                } else if (field.getType().equals(List.class)) {
                    String id = variableName + "_" + field.getName();
                    Type[] typeArgs = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                    TestGenListValueProvider listValueProvider = new TestGenListValueProvider((List) value, id, typeArgs[0], existingInstances);
                    fields.add(new TestGenFactField(this, accessor, listValueProvider));
                    dependencies.addAll(listValueProvider.getFacts());
                    imports.addAll(listValueProvider.getImports());
                } else if (field.getType().equals(Map.class)) {
                    String id = variableName + "_" + field.getName();
                    Type[] typeArgs = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                    TestGenMapValueProvider mapValueProvider = new TestGenMapValueProvider((Map) value, id, typeArgs, existingInstances);
                    fields.add(new TestGenFactField(this, accessor, mapValueProvider));
                    dependencies.addAll(mapValueProvider.getFacts());
                    imports.addAll(mapValueProvider.getImports());
                } else {
                    Method parseMethod = getParseMethod(field);
                    if (parseMethod != null) {
                        fields.add(new TestGenFactField(this, accessor, new TestGenParsedValueProvider(parseMethod, value)));
                        imports.add(field.getType());
                    } else {
                        throw new IllegalStateException("Unsupported type: " + field.getType());
                    }
                }
            } else {
                fields.add(new TestGenFactField(this, accessor, new TestGenNullValueProvider()));
            }
        }
    }

    private static Method getParseMethod(Field f) {
        for (Method m : f.getType().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())
                    && f.getType().equals(m.getReturnType())
                    && m.getParameters().length == 1
                    && m.getParameters()[0].getType().equals(String.class)
                    && (m.getName().startsWith("parse") || m.getName().startsWith("valueOf"))) {
                return m;
            }
        }
        return null;
    }

    @Override
    public Object getInstance() {
        return instance;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public List<TestGenFactField> getFields() {
        return fields;
    }

    @Override
    public List<TestGenFact> getDependencies() {
        return dependencies;
    }

    @Override
    public List<Class<?>> getImports() {
        return imports;
    }

    @Override
    public void reset() {
        fields.forEach(TestGenFactField::reset);
    }

    @Override
    public void printInitialization(StringBuilder sb) {
        if (instance.getClass().isEnum()) {
            sb.append(String.format("    private final %s %s = %s.%s;%n",
                    instance.getClass().getSimpleName(), variableName, instance.getClass().getSimpleName(),
                    ((Enum) instance).name()));
        } else {
            sb.append(String.format("    private final %s %s = new %s();%n",
                    instance.getClass().getSimpleName(), variableName, instance.getClass().getSimpleName()));
        }
    }

    @Override
    public void printSetup(StringBuilder sb) {
        sb.append(String.format("        //%s%n", instance));
        fields.forEach(f -> f.print(sb));
    }

    @Override
    public String toString() {
        return variableName;
    }

}
