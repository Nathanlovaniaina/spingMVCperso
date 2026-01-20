package com.monframework.core.util.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour injecter une Map<String, Object> représentant les données de session.
 * Cette Map permet de manipuler les attributs de la session HTTP de manière simplifiée.
 * 
 * Exemple d'utilisation:
 * <pre>
 * {@literal @}HandleURL("/session")
 * public String manageSession({@literal @}Session Map<String, Object> session) {
 *     // Ajouter une valeur
 *     session.put("username", "John");
 *     
 *     // Récupérer une valeur
 *     String username = (String) session.get("username");
 *     
 *     // Modifier une valeur
 *     session.put("username", "Jane");
 *     
 *     // Supprimer une valeur
 *     session.remove("username");
 *     
 *     return "session-view";
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Session {
}
