package com.monframework.core.util.Annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation imitation de Spring's @Controller.
 *
 * Usage:
 * @ControleurAnnotation
 * public class MonControleur { ... }
 *
 * Optionnellement on peut fournir une valeur (par ex. un nom ou un chemin) :
 * @ControleurAnnotation("monControleur")
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ControleurAnnotation {
    /**
     * Valeur optionnelle (par ex. nom de bean ou chemin). Par défaut chaine vide.
     */
    String value() default "";
}
