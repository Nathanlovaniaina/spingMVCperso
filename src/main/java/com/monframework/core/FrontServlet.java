package com.monframework.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.monframework.core.util.Mapper.RouteMapping;
import com.monframework.core.util.Mapper.ModelView;



@WebServlet(name = "FrontServlet", urlPatterns = {"/"}, loadOnStartup = 1)
public class FrontServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            scanAndCollectRoutes(getServletContext());
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des contrôleurs et routes", e);
        }
    }
    
    private void scanAndCollectRoutes(ServletContext ctx) {
        try {
            String real = ctx.getRealPath("WEB-INF/classes");
            if (real == null) {
                System.err.println("WARNING: WEB-INF/classes path is null - déploiement non explosé");
                return;
            }
            
            System.out.println("[DEBUG] Scanning for controllers in: " + real);
            Path classesRoot = Paths.get(real);

            // Utiliser le ClassLoader du contexte de la servlet
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            System.out.println("[DEBUG] Using ClassLoader: " + contextClassLoader.getClass().getName());

            // Scanner et collecter les routes via RouteMapping
            List<RouteMapping> routeMappings = RouteMapping.scanFromClassesRoot(classesRoot, contextClassLoader);
            
            System.out.println("[DEBUG] Found " + routeMappings.size() + " route mappings");
            for (RouteMapping rm : routeMappings) {
                System.out.println("[DEBUG]   -> " + rm);
            }
            
            // Stocker la liste dans le ServletContext
            ctx.setAttribute("route.mappings", Collections.unmodifiableList(routeMappings));
            
        } catch (Exception e) {
            System.err.println("ERROR during route scanning:");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        service(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        service(request, response);
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Définir l'encodage UTF-8 pour les paramètres avant toute lecture
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        String resourcePath = requestURI.substring(contextPath.length());
        
        // Vérifier d'abord si c'est une ressource statique
        try {
            java.net.URL resource = getServletContext().getResource(resourcePath);
            if (resource != null) {
                RequestDispatcher defaultServlet = getServletContext().getNamedDispatcher("default");
                if (defaultServlet != null) {
                    defaultServlet.forward(request, response);
                    return;
                }
            }
        } catch (Exception e) {
            // Continuer si ce n'est pas une ressource statique
        }
        
        // Récupérer la liste des routes depuis le ServletContext
        @SuppressWarnings("unchecked")
        List<RouteMapping> routeMappings = (List<RouteMapping>) getServletContext().getAttribute("route.mappings");
        
        if (routeMappings == null) {
            routeMappings = Collections.emptyList();
        }
        
        // Chercher une route correspondante (support des path variables)
        RouteMapping matchedRoute = null;
        java.util.Map<String,String> pathVars = null;
        for (RouteMapping rm : routeMappings) {
            java.util.Map<String,String> vars = rm.match(resourcePath);
            if (vars != null) {
                matchedRoute = rm;
                pathVars = vars;
                break;
            }
        }
        
        if (matchedRoute != null) {
            // Route trouvée ! Afficher les informations
            showMatchedRoute(request, response, resourcePath, matchedRoute, pathVars);
        } else {
            // Aucune route trouvée, afficher la page par défaut
            showFrameworkPage(request, response, resourcePath, routeMappings);
        }
    }
    
    /**
     * Utilise ModelView pour effectuer directement le forward.
     */
    private void showMatchedRoute(HttpServletRequest request, HttpServletResponse response,
                                  String requestedPath, RouteMapping route, java.util.Map<String,String> pathVars)
            throws IOException {
        try {
            // Toujours utiliser ModelView basé sur la route; le contrôleur retourne un String
            ModelView mv = new ModelView(route);
            mv.getView(request, response, pathVars);
        } catch (Exception e) {
            response.setContentType("text/plain; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("Erreur lors de la résolution de la vue");
            out.println("URL: " + requestedPath);
            out.println("Classe: " + route.getClassName());
            out.println("Méthode: " + route.getMethodName() + "()");
            out.println();
            out.println("Exception: " + e.getClass().getName());
            out.println("Message: " + e.getMessage());
            System.err.println("Erreur lors de la résolution de la vue pour " + route.getMethodName() + ":");
            e.printStackTrace();
        }
    }
    
    private void showFrameworkPage(HttpServletRequest request, HttpServletResponse response, 
                                 String requestedPath, List<RouteMapping> routeMappings) 
            throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("Route non trouvée");
        out.println("URL demandée: " + requestedPath);
        out.println("Routes disponibles: " + routeMappings.size());
        if (routeMappings.isEmpty()) {
            out.println("(aucune)");
        } else {
            for (RouteMapping rm : routeMappings) {
                // Minimal: fullUrl -> class#method
                out.println(rm.getFullUrl() + " -> " + rm.getClassName() + "#" + rm.getMethodName());
            }
        }
    }
}