package com.monframework.core.util.Mapper.ParmeterUtil;

/**
 * Classe utilitaire pour convertir des chaînes de caractères vers différents types Java.
 */
public class TypeConverter {
    
    /**
     * Convertit une chaîne vers un type simple supporté (String, wrappers numériques, boolean).
     * 
     * @param raw La valeur String à convertir
     * @param targetType Le type cible
     * @return La valeur convertie, ou null si la conversion échoue ou si le type n'est pas supporté
     */
    public static Object convertStringToType(String raw, Class<?> targetType) {
        if (targetType.equals(String.class)) {
            return raw;
        }
        
        try {
            if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return Long.valueOf(raw);
            }
            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return Integer.valueOf(raw);
            }
            if (targetType.equals(Short.class) || targetType.equals(short.class)) {
                return Short.valueOf(raw);
            }
            if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
                return Byte.valueOf(raw);
            }
            if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return Double.valueOf(raw);
            }
            if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return Float.valueOf(raw);
            }
            if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
                return Boolean.valueOf(raw);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        
        // Type non supporté
        return null;
    }

    /**
     * Convertit un tableau de String vers un tableau du composant target (ex: String[] -> Integer[] si compType=Integer)
     */
    public static Object convertStringArrayToArray(String[] raw, Class<?> compType) {
        if (raw == null) return null;
        Object array = java.lang.reflect.Array.newInstance(compType, raw.length);
        for (int i = 0; i < raw.length; i++) {
            if (compType.equals(String.class)) {
                java.lang.reflect.Array.set(array, i, raw[i]);
            } else {
                Object conv = convertStringToType(raw[i], compType);
                java.lang.reflect.Array.set(array, i, conv);
            }
        }
        return array;
    }
}
