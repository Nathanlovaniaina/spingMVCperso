package com.monframework.core.util.Mapper.ParmeterUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

                Class<?> fieldType = field.getType();
                Object toSet = null;

                // Tableau
                if (fieldType.isArray()) {
                    Class<?> compType = fieldType.getComponentType();
                    toSet = TypeConverter.convertStringArrayToArray(values, compType);
                }
                // List -> on assigne List<String> si plusieurs valeurs (simple)
                else if (List.class.isAssignableFrom(fieldType)) {
                    // Tenter de détecter le type générique de la liste
                    Class<?> elemClass = null;
                    Type gtype = field.getGenericType();
                    if (gtype instanceof ParameterizedType) {
                        Type[] args = ((ParameterizedType) gtype).getActualTypeArguments();
                        if (args != null && args.length == 1 && args[0] instanceof Class) {
                            elemClass = (Class<?>) args[0];
                        }
                    }

                    if (elemClass != null && !elemClass.equals(String.class) && !elemClass.isPrimitive()) {
                        // Liste d'objets complexes : chercher des clés indexées comme name + "[i].field"
                        // Ex: hobbies[0].nom  (dans la variante sans préfixe)
                        java.util.Set<Integer> indices = new java.util.TreeSet<>();
                        if (paramMap != null) {
                            String basePrefix = name; // without object prefix
                            Pattern pat = Pattern.compile("^" + Pattern.quote(basePrefix) + "\\[(\\d+)\\]\\.(.+)$");
                            for (String k : paramMap.keySet()) {
                                Matcher m = pat.matcher(k);
                                if (m.find()) {
                                    try {
                                        int idx = Integer.parseInt(m.group(1));
                                        indices.add(idx);
                                    } catch (NumberFormatException ignored) { }
                                }
                            }
                        }

                        List<Object> list = new ArrayList<>();
                        for (Integer idx : indices) {
                            String base = name + "[" + idx + "]";
                            Object elem = populateBeanFromParamMap(elemClass, paramMap, base);
                            if (elem != null) list.add(elem);
                        }
                        toSet = list;
                    } else {
                        // simple approach: list of strings
                        List<String> list = new ArrayList<>(Arrays.asList(values));
                        toSet = list;
                    }
                }
                // simple type (String, primitive, wrapper, boolean, number)
                else {
                    // si plusieurs valeurs et champ non-array -> garder la première
                    String raw = (values != null && values.length > 0) ? values[0] : null;
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

    private static Object populateBeanFromParamMap(Class<?> elemClass, Map<String,String[]> paramMap, String base) {
        try {
            Object instance = elemClass.getDeclaredConstructor().newInstance();

            for (Field field : elemClass.getDeclaredFields()) {
                String fname = field.getName();
                String key = base + "." + fname;
                String[] values = paramMap != null ? paramMap.get(key) : null;
                if (values == null) continue;

                Class<?> fieldType = field.getType();
                Object toSet = null;

                if (fieldType.isArray()) {
                    Class<?> compType = fieldType.getComponentType();
                    toSet = TypeConverter.convertStringArrayToArray(values, compType);
                } else if (List.class.isAssignableFrom(fieldType)) {
                    List<String> list = new ArrayList<>(Arrays.asList(values));
                    toSet = list;
                } else {
                    String raw = values.length > 0 ? values[0] : null;
                    if (raw != null) {
                        Object converted = TypeConverter.convertStringToType(raw, fieldType);
                        toSet = converted;
                    }
                }

                if (toSet == null) continue;

                String setterName = "set" + Character.toUpperCase(fname.charAt(0)) + fname.substring(1);
                try {
                    Method setter = findSetter(elemClass, setterName, toSet.getClass(), fieldType);
                    if (setter != null) {
                        setter.setAccessible(true);
                        setter.invoke(instance, toSet);
                        continue;
                    }
                } catch (Throwable ignored) { }

                try {
                    field.setAccessible(true);
                    field.set(instance, toSet);
                } catch (Throwable ignored) { }
            }

            return instance;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Variante de populate qui recherche les paramètres en utilisant un préfixe.
     * Ex: prefix = "emptest" -> cherchera "emptest.id", "emptest.nom", etc.
     * Si aucune valeur préfixée n'est trouvée, retombe sur le nom simple (compatibilité).
     */
    public static <T> T populateWithPrefix(Class<T> type, HttpServletRequest request, Map<String,String> pathVars, String prefix) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();

            Map<String, String[]> paramMap = request != null ? request.getParameterMap() : null;

            for (Field field : type.getDeclaredFields()) {
                String name = field.getName();
                String prefixed = prefix == null || prefix.isEmpty() ? name : (prefix + "." + name);

                // Priorité : pathVars[prefixed] -> request[prefixed] -> pathVars[name] -> request[name]
                String[] values = null;
                if (pathVars != null && pathVars.containsKey(prefixed)) {
                    values = new String[] { pathVars.get(prefixed) };
                } else if (paramMap != null && paramMap.containsKey(prefixed)) {
                    values = paramMap.get(prefixed);
                } else if (pathVars != null && pathVars.containsKey(name)) {
                    values = new String[] { pathVars.get(name) };
                } else if (paramMap != null && paramMap.containsKey(name)) {
                    values = paramMap.get(name);
                }

                Class<?> fieldType = field.getType();
                Object toSet = null;

                // Tableau
                if (fieldType.isArray()) {
                    Class<?> compType = fieldType.getComponentType();
                    toSet = TypeConverter.convertStringArrayToArray(values, compType);
                }
                // List -> on assigne List<String> si plusieurs valeurs (simple)
                else if (List.class.isAssignableFrom(fieldType)) {
                    // Tenter de détecter le type générique de la liste
                    Class<?> elemClass = null;
                    Type gtype = field.getGenericType();
                    if (gtype instanceof ParameterizedType) {
                        Type[] args = ((ParameterizedType) gtype).getActualTypeArguments();
                        if (args != null && args.length == 1 && args[0] instanceof Class) {
                            elemClass = (Class<?>) args[0];
                        }
                    }

                    if (elemClass != null && !elemClass.equals(String.class) && !elemClass.isPrimitive()) {
                        // Liste d'objets complexes avec préfixe: chercher des clés indexées comme prefixed + "[i].field"
                        java.util.Set<Integer> indices = new java.util.TreeSet<>();
                        if (paramMap != null) {
                            Pattern pat = Pattern.compile("^" + Pattern.quote(prefixed) + "\\[(\\d+)\\]\\.(.+)$");
                            for (String k : paramMap.keySet()) {
                                Matcher m = pat.matcher(k);
                                if (m.find()) {
                                    try {
                                        int idx = Integer.parseInt(m.group(1));
                                        indices.add(idx);
                                    } catch (NumberFormatException ignored) { }
                                }
                            }
                        }

                        List<Object> list = new ArrayList<>();
                        for (Integer idx : indices) {
                            String base = prefixed + "[" + idx + "]";
                            Object elem = populateBeanFromParamMap(elemClass, paramMap, base);
                            if (elem != null) list.add(elem);
                        }
                        toSet = list;
                    } else {
                        // simple approach: list of strings
                        if (values != null) {
                            List<String> list = new ArrayList<>(Arrays.asList(values));
                            toSet = list;
                        }
                    }
                }
                // simple type (String, primitive, wrapper, boolean, number)
                else {
                    // si plusieurs valeurs et champ non-array -> garder la première
                    String raw = (values != null && values.length > 0) ? values[0] : null;
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
