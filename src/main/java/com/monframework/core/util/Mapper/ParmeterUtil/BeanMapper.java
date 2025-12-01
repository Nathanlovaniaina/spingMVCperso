package com.monframework.core.util.Mapper.ParmeterUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Popule un POJO à partir des paramètres HTTP (request + pathVars).
 * - cherche param par nom de champ
 * - supporte valeurs uniques et multiples (checkbox -> String[])
 */
public class BeanMapper {

    public static <T> T populate(Class<T> type, HttpServletRequest request, Map<String,String> pathVars) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();

            Map<String, String[]> paramMap = request != null ? request.getParameterMap() : null;

            for (Field field : type.getDeclaredFields()) {
                String name = field.getName();

                // Priorité : pathVars -> request parameters
                String[] values = null;
                if (pathVars != null && pathVars.containsKey(name)) {
                    values = new String[] { pathVars.get(name) };
                } else if (paramMap != null && paramMap.containsKey(name)) {
                    values = paramMap.get(name);
                }

                if (values == null) continue; // rien à remplir

                Class<?> fieldType = field.getType();
                Object toSet = null;

                // Tableau
                if (fieldType.isArray()) {
                    Class<?> compType = fieldType.getComponentType();
                    toSet = TypeConverter.convertStringArrayToArray(values, compType);
                }
                // List -> on assigne List<String> si plusieurs valeurs (simple)
                else if (List.class.isAssignableFrom(fieldType)) {
                    // simple approach: list of strings
                    List<String> list = new ArrayList<>(Arrays.asList(values));
                    toSet = list;
                }
                // simple type (String, primitive, wrapper, boolean, number)
                else {
                    // si plusieurs valeurs et champ non-array -> garder la première
                    String raw = values.length > 0 ? values[0] : null;
                    if (raw != null) {
                        Object converted = TypeConverter.convertStringToType(raw, fieldType);
                        toSet = converted;
                    }
                }

                if (toSet == null) continue;

                // essayer setter setX(...) sinon field accessible
                String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                try {
                    Method setter = findSetter(type, setterName, toSet.getClass(), fieldType);
                    if (setter != null) {
                        setter.setAccessible(true);
                        setter.invoke(instance, toSet);
                        continue;
                    }
                } catch (Throwable ignored) { }

                // fallback: écriture directe dans le champ
                try {
                    field.setAccessible(true);
                    // conversion si nécessaire pour primitives (auto-unboxing géré par reflection)
                    field.set(instance, toSet);
                } catch (Throwable ignored) { }
            }

            return instance;
        } catch (Exception e) {
            // en cas d'erreur, retourner null pour indiquer l'impossibilité de peupler
            return null;
        }
    }

    private static Method findSetter(Class<?> type, String setterName, Class<?> valueClass, Class<?> declaredFieldType) {
        // chercher setter exact (valeurClass peut être un tableau ou ArrayList)
        for (Method m : type.getMethods()) {
            if (!m.getName().equals(setterName)) continue;
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1) continue;
            // accepter si param assignable depuis la valeur qu'on veut passer
            if (params[0].isAssignableFrom(valueClass) || params[0].isAssignableFrom(declaredFieldType)) {
                return m;
            }
        }
        return null;
    }
}
