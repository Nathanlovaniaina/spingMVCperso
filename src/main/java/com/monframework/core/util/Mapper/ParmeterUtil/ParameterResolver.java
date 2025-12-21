package com.monframework.core.util.Mapper.ParmeterUtil;

import com.monframework.core.util.Annotation.PathVariable;
import com.monframework.core.util.Annotation.RequestParam;
import com.monframework.core.util.Mapper.Model;
import com.monframework.core.util.FileUpload.FileUploadHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitaire pour résoudre les paramètres de méthode des contrôleurs.
 * Gère l'injection de Model, HttpServletRequest, HttpServletResponse, 
 * Map<String,Object>, @PathVariable, @RequestParam, etc.
 */
public class ParameterResolver {
    
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Model model;
    private final Map<String, String> pathVars;
    
    public ParameterResolver(HttpServletRequest request, HttpServletResponse response, 
                            Model model, Map<String, String> pathVars) {
        this.request = request;
        this.response = response;
        this.model = model;
        this.pathVars = pathVars;
    }
    
    /**
     * Résout la valeur d'un paramètre de méthode en fonction de son type et de ses annotations.
     * 
     * @param param Le paramètre à résoudre
     * @return La valeur résolue, ou null si le paramètre ne peut pas être résolu
     * @throws ParameterResolutionException si le paramètre est obligatoire mais ne peut pas être résolu
     */
    public Object resolveParameter(Parameter param) throws ParameterResolutionException {
        Class<?> paramType = param.getType();
        String paramName = param.getName();
        
        // Cas 1: Model
        if (paramType.equals(Model.class)) {
            return model;
        }
        
        // Cas 2: HttpServletRequest
        if (paramType.equals(HttpServletRequest.class)) {
            return request;
        }
        
        // Cas 3: HttpServletResponse
        if (paramType.equals(HttpServletResponse.class)) {
            return response;
        }
        
        // Cas 4: Map<String,Object> - Injection de tous les paramètres du formulaire
        // Vérifier que le paramètre est bien Map<String,Object> (pas Map<Integer,Integer> ou raw Map)
        if (Map.class.isAssignableFrom(paramType)) {
            java.lang.reflect.Type ptype = param.getParameterizedType();
            if (ptype instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.Type[] args = ((java.lang.reflect.ParameterizedType) ptype).getActualTypeArguments();
                if (args != null && args.length == 2) {
                    boolean keyIsString = args[0] instanceof Class && ((Class<?>) args[0]).equals(String.class);
                    boolean valIsObject = args[1] instanceof Class && ((Class<?>) args[1]).equals(Object.class);
                    boolean valIsByteArray = args[1] instanceof Class && ((Class<?>) args[1]).equals(byte[].class);
                    
                    if (keyIsString && valIsObject) {
                        return resolveFormValuesMap();
                    }
                    
                    // Cas 4b: Map<String, byte[]> - Injection des fichiers uploadés
                    if (keyIsString && valIsByteArray) {
                        return resolveUploadedFilesMap();
                    }
                }
            }
            // si le Map n'a pas les bons generics, on ne l'interprète pas comme form map
        }

        // Cas 5: Si le paramètre est un POJO (classe custom) => tenter de le peupler
        if (!paramType.isPrimitive()
                && !paramType.equals(String.class)
                && !paramType.isArray()
                && !Map.class.isAssignableFrom(paramType)
                && !HttpServletRequest.class.isAssignableFrom(paramType)
                && !HttpServletResponse.class.isAssignableFrom(paramType)
                && !Model.class.isAssignableFrom(paramType)) {

            // Ne pas peupler si le param est annoté @PathVariable ou @RequestParam (handled later)
            if (param.getAnnotation(com.monframework.core.util.Annotation.PathVariable.class) == null
                && param.getAnnotation(com.monframework.core.util.Annotation.RequestParam.class) == null) {
                // Populate using parameter name as prefix: supports inputs like "paramName.field"
                Object bean = BeanMapper.populateWithPrefix(paramType, request, pathVars, paramName);
                if (bean != null) return bean;
            }
        }
        
        // Cas 6: Paramètres avec annotations ou simples
        return resolveAnnotatedOrSimpleParameter(param, paramType, paramName);
    }
    
    /**
     * Crée une Map contenant tous les paramètres de la requête HTTP.
     * Supporte les checkboxes (valeurs multiples).
     */
    private Map<String, Object> resolveFormValuesMap() {
        Map<String, Object> formValues = new HashMap<>();
        if (request != null) {
            java.util.Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String[] values = request.getParameterValues(paramName);
                if (values != null) {
                    if (values.length == 1) {
                        // Une seule valeur : ajouter directement la String
                        formValues.put(paramName, values[0]);
                    } else {
                        // Plusieurs valeurs (checkbox) : ajouter le tableau
                        formValues.put(paramName, values);
                    }
                }
            }
        }
        return formValues;
    }
    
    /**
     * Crée une Map contenant tous les fichiers uploadés.
     * Clé: nom du fichier, Valeur: contenu du fichier en bytes
     */
    private Map<String, byte[]> resolveUploadedFilesMap() {
        if (request != null) {
            return FileUploadHandler.extractUploadedFiles(request);
        }
        return new HashMap<>();
    }
    
    /**
     * Résout un paramètre avec annotations (@PathVariable, @RequestParam) ou simple.
     */
    private Object resolveAnnotatedOrSimpleParameter(Parameter param, Class<?> paramType, String paramName) 
            throws ParameterResolutionException {
        
        PathVariable pathVariableAnnotation = param.getAnnotation(PathVariable.class);
        RequestParam requestParamAnnotation = param.getAnnotation(RequestParam.class);
        
        String paramNameToLookup = paramName;
        String defaultValue = null;
        boolean isPathVariable = false;
        
        // Déterminer le nom du paramètre et la valeur par défaut
        if (pathVariableAnnotation != null) {
            isPathVariable = true;
            String annotationValue = pathVariableAnnotation.value();
            if (annotationValue != null && !annotationValue.isEmpty()) {
                paramNameToLookup = annotationValue;
            }
        } else if (requestParamAnnotation != null) {
            String annotationValue = requestParamAnnotation.value();
            if (annotationValue != null && !annotationValue.isEmpty()) {
                paramNameToLookup = annotationValue;
            }
            String annotationDefault = requestParamAnnotation.defaultValue();
            if (annotationDefault != null && !annotationDefault.isEmpty()) {
                defaultValue = annotationDefault;
            }
        }
        
        // Chercher la valeur
        String rawValue = findParameterValue(paramNameToLookup, isPathVariable);
        
        // Utiliser la valeur par défaut si nécessaire
        if (rawValue == null && defaultValue != null) {
            rawValue = defaultValue;
        }
        
        // Convertir et retourner
        return convertValue(rawValue, paramType, paramName);
    }
    
    /**
     * Cherche la valeur d'un paramètre dans pathVars ou dans les paramètres de requête.
     */
    private String findParameterValue(String paramName, boolean isPathVariable) {
        if (isPathVariable) {
            // Pour @PathVariable, chercher uniquement dans pathVars
            return (pathVars != null && pathVars.containsKey(paramName)) ? pathVars.get(paramName) : null;
        } else {
            // Pour @RequestParam ou sans annotation, chercher d'abord pathVars puis request params
            if (pathVars != null && pathVars.containsKey(paramName)) {
                return pathVars.get(paramName);
            } else if (request != null) {
                return request.getParameter(paramName);
            }
            return null;
        }
    }
    
    /**
     * Convertit une valeur String vers le type cible.
     * 
     * @throws ParameterResolutionException si la conversion échoue ou si un primitif est null
     */
    private Object convertValue(String rawValue, Class<?> targetType, String paramName) 
            throws ParameterResolutionException {
        
        if (rawValue != null) {
            Object converted = TypeConverter.convertStringToType(rawValue, targetType);
            if (converted == null) {
                throw new ParameterResolutionException(
                    "Impossible de convertir la valeur '" + rawValue + "' vers le type " + targetType.getName()
                );
            }
            return converted;
        } else {
            // Paramètre non fourni
            if (targetType.isPrimitive()) {
                throw new ParameterResolutionException(
                    "Le paramètre '" + paramName + "' de type primitif " + targetType.getName() + 
                    " est requis mais n'a pas été fourni"
                );
            }
            // Pour les wrappers et String, retourner null
            return null;
        }
    }
    
    /**
     * Exception levée lorsqu'un paramètre ne peut pas être résolu.
     */
    public static class ParameterResolutionException extends Exception {
        public ParameterResolutionException(String message) {
            super(message);
        }
    }
}
