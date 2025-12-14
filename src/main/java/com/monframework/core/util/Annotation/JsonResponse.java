package com.monframework.core.util.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour indiquer qu'une méthode de contrôleur doit retourner une réponse JSON.
 * La méthode peut retourner n'importe quel objet (POJO, List, String, etc.) et il sera 
 * automatiquement converti en une réponse JSON structurée avec status, message, code et data.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonResponse {
    /**
     * Message par défaut à inclure dans la réponse JSON.
     * Peut être surchargé dynamiquement.
     */
    String message() default "Success";
    
    /**
     * Code HTTP par défaut pour la réponse.
     */
    int code() default 200;
}
