package com.monframework.core.util.Mapper;

/**
 * Modèle simple qui encapsule une route et permet de récupérer la vue
 * renvoyée par la méthode du contrôleur.
 */
public class ModelView {
    private final RouteMapping route;

    public ModelView(RouteMapping route) {
        this.route = route;
    }

    public RouteMapping getRoute() {
        return route;
    }

    /**
     * Appelle la méthode associée à la route et retourne la chaîne
     * qui représente la vue (chemin à forward).
     * @return chemin de la vue (ex: "/contact.jsp")
     * @throws Exception si l'invocation échoue ou si le retour n'est pas un String
     */
    public String getView() throws Exception {
        String result = route.callMethod();
        if (result == null) {
            throw new Exception("La méthode du contrôleur a retourné null");
        }
        return result;
    }
}
