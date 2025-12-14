package com.monframework.core.util.Formatter;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * Classe utilitaire pour générer des réponses JSON structurées.
 * Format de réponse standardisé avec status, message, code et data.
 */
public class JsonResponseBuilder {

    /**
     * Génère une réponse JSON à partir d'un objet de données.
     * 
     * @param data L'objet à sérialiser (peut être un POJO, List, Array, String, etc.)
     * @param status Le statut de la réponse ("success" ou "error")
     * @param message Le message descriptif
     * @param code Le code HTTP
     * @return La chaîne JSON formatée
     */
    public static String buildJsonResponse(Object data, String status, String message, int code) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"status\":\"").append(escapeJson(status)).append("\",");
        json.append("\"message\":\"").append(escapeJson(message)).append("\",");
        json.append("\"code\":").append(code);
        
        if (data != null) {
            // Vérifier si data est une collection ou un tableau
            if (data instanceof Collection) {
                Collection<?> collection = (Collection<?>) data;
                json.append(",\"count\":").append(collection.size());
                json.append(",\"data\":");
                json.append(serializeCollection(collection));
            } else if (data.getClass().isArray()) {
                int length = Array.getLength(data);
                json.append(",\"count\":").append(length);
                json.append(",\"data\":");
                json.append(serializeArray(data, length));
            } else {
                // Objet unique
                json.append(",\"data\":");
                json.append(serializeObject(data));
            }
        }
        
        json.append("}");
        return json.toString();
    }

    /**
     * Sérialise une collection en JSON.
     */
    private static String serializeCollection(Collection<?> collection) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Object item : collection) {
            if (!first) json.append(",");
            json.append(serializeObject(item));
            first = false;
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Sérialise un tableau en JSON.
     */
    private static String serializeArray(Object array, int length) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            if (i > 0) json.append(",");
            Object item = Array.get(array, i);
            json.append(serializeObject(item));
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Sérialise un objet en JSON (simple ou complexe).
     */
    private static String serializeObject(Object obj) {
        if (obj == null) {
            return "null";
        }

        Class<?> clazz = obj.getClass();

        // Types primitifs et wrappers
        if (clazz.equals(String.class)) {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
        if (clazz.equals(Integer.class) || clazz.equals(Long.class) || 
            clazz.equals(Short.class) || clazz.equals(Byte.class) ||
            clazz.equals(Double.class) || clazz.equals(Float.class)) {
            return obj.toString();
        }
        if (clazz.equals(Boolean.class)) {
            return obj.toString().toLowerCase();
        }

        // Collection imbriquée
        if (obj instanceof Collection) {
            return serializeCollection((Collection<?>) obj);
        }

        // Tableau imbriqué
        if (clazz.isArray()) {
            return serializeArray(obj, Array.getLength(obj));
        }

        // Map
        if (obj instanceof Map) {
            return serializeMap((Map<?, ?>) obj);
        }

        // POJO complexe - utiliser la réflexion pour extraire les champs
        return serializePojo(obj);
    }

    /**
     * Sérialise un POJO en JSON en utilisant la réflexion.
     */
    private static String serializePojo(Object obj) {
        StringBuilder json = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;

        for (Field field : fields) {
            // Ignorer les champs statiques ou transient
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                
                if (!first) json.append(",");
                json.append("\"").append(field.getName()).append("\":");
                json.append(serializeObject(value));
                first = false;
            } catch (IllegalAccessException e) {
                // Ignorer les champs inaccessibles
            }
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Sérialise une Map en JSON.
     */
    private static String serializeMap(Map<?, ?> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(escapeJson(entry.getKey().toString())).append("\":");
            json.append(serializeObject(entry.getValue()));
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Échappe les caractères spéciaux pour JSON.
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Génère une réponse JSON de succès avec code 200.
     */
    public static String success(Object data, String message) {
        return buildJsonResponse(data, "success", message, 200);
    }

    /**
     * Génère une réponse JSON de succès avec code personnalisé.
     */
    public static String success(Object data, String message, int code) {
        return buildJsonResponse(data, "success", message, code);
    }

    /**
     * Génère une réponse JSON d'erreur avec code 400.
     */
    public static String error(String message) {
        return buildJsonResponse(null, "error", message, 400);
    }

    /**
     * Génère une réponse JSON d'erreur avec code personnalisé.
     */
    public static String error(String message, int code) {
        return buildJsonResponse(null, "error", message, code);
    }
}
