package com.monframework.core.util.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {
    /**
     * Le nom du paramètre dans la requête HTTP.
     * Si vide, utilise le nom du paramètre de la méthode.
     */
    String value() default "";
    
    /**
     * Valeur par défaut si le paramètre n'est pas présent dans la requête.
     */
    String defaultValue() default "";
}
