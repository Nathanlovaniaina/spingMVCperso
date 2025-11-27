package com.monframework.core.util.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PathVariable {
    /**
     * Le nom de la variable de chemin.
     * Si vide, utilise le nom du paramètre de la méthode.
     */
    String value() default "";
}
