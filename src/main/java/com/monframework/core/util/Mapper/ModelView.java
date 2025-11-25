package com.monframework.core.util.Mapper;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Modèle simple qui encapsule une route et permet de récupérer la vue
 * renvoyée par la méthode du contrôleur.
 */
public class ModelView {
    private final RouteMapping route;
    private String view; // chemin de la vue, optionnel si route utilisé
    private final Map<String, Object> model = new HashMap<>();

    public ModelView(RouteMapping route) {
        this.route = route;
    }

    public ModelView(String view) {
        this.route = null;
        this.view = view;
    }

    public RouteMapping getRoute() {
        return route;
    }

    public String getViewPath() {
        return view;
    }

    public ModelView setViewPath(String view) {
        this.view = view;
        return this;
    }

    public ModelView addObject(String name, Object value) {
        model.put(name, value);
        return this;
    }

    public ModelView setValue(String name, Object value) {
        model.put(name, value);
        return this;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    /**
     * Appelle la méthode associée à la route et retourne la chaîne
     * qui représente la vue (chemin à forward).
     * @return chemin de la vue (ex: "/contact.jsp")
     * @throws Exception si l'invocation échoue ou si le retour n'est pas un String
     */
    public String getView() throws Exception {
        if (view != null && !view.isEmpty()) {
            return view;
        }
        if (route == null) {
            throw new Exception("Aucune route associée et aucune vue définie");
        }
        String result = route.callMethod();
        if (result == null) {
            throw new Exception("La méthode du contrôleur a retourné null");
        }
        this.view = result;
        return result;
    }

    /**
     * Variante qui effectue directement le forward vers la vue retournée
     * par la méthode du contrôleur.
     *
     * @throws Exception si l'invocation échoue, si la vue est vide,
     *                   ou si le forward échoue
     */
    public void getView(HttpServletRequest request, HttpServletResponse response, java.util.Map<String,String> pathVars) throws Exception {
        String viewPath;
        if (this.view != null && !this.view.isEmpty()) {
            viewPath = this.view;
        } else if (this.route != null) {
            // appeler la méthode du contrôleur en passant request/response et Model si possible
            RouteMapping.InvokeResult res = this.route.callMethodWithModel(request, response, pathVars);
            viewPath = res.getView();
            this.view = viewPath;
            // injecter les attributs fournis par le contrôleur via Model
            if (res.getModel() != null) {
                for (Map.Entry<String, Object> e : res.getModel().asMap().entrySet()) {
                    request.setAttribute(e.getKey(), e.getValue());
                }
            }
        } else {
            viewPath = getView(); // fallback
        }
        // injecter le modèle local
        for (Map.Entry<String, Object> e : model.entrySet()) {
            request.setAttribute(e.getKey(), e.getValue());
        }
        // Remarque: les contrôleurs peuvent aussi utiliser request.setAttribute directement
        if (viewPath == null || viewPath.isEmpty()) {
            throw new Exception("La vue retournée est vide");
        }
        if (!viewPath.startsWith("/")) {
            viewPath = "/" + viewPath;
        }
        RequestDispatcher rd = request.getRequestDispatcher(viewPath);
        rd.forward(request, response);
    }
}
