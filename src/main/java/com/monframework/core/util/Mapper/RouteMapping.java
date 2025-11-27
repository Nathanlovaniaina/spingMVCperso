package com.monframework.core.util.Mapper;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.monframework.core.util.Annotation.ControleurAnnotation;
import com.monframework.core.util.Annotation.HandleURL;
import com.monframework.core.util.Annotation.RequestParam;
import com.monframework.core.util.Annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RouteMapping {
    private final String className;
    private final String controllerValue;
    private final String urlValue;
    private final String methodName;

    public RouteMapping(String className, String controllerValue, String urlValue, String methodName) {
        this.className = className;
        this.controllerValue = controllerValue;
        this.urlValue = urlValue;
        this.methodName = methodName;
    }

    public String getClassName() { return className; }
    public String getControllerValue() { return controllerValue; }
    public String getUrlValue() { return urlValue; }
    public String getMethodName() { return methodName; }
    
    /**
     * Retourne l'URL complète en combinant controllerValue et urlValue
     */
    public String getFullUrl() {
        String controller = controllerValue == null || controllerValue.isEmpty() ? "" : controllerValue;
        String url = urlValue == null || urlValue.isEmpty() ? "" : urlValue;
        
        // Ajouter des slashes si nécessaire
        if (!controller.startsWith("/")) {
            controller = "/" + controller;
        }
        if (!url.isEmpty() && !url.startsWith("/")) {
            url = "/" + url;
        }
        
        return controller + url;
    }

    /**
     * Tente de matcher le chemin demandé avec ce route mapping. Si match,
     * retourne une map nom->valeur des variables de chemin; sinon retourne null.
     */
    public Map<String,String> match(String requestedPath) {
        String full = getFullUrl();
        // Construire un regex à partir du pattern contenant {var}
        // Ex: /test/view/{id} -> ^/test/view/(?<id>[^/]+)$
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < full.length()) {
            int open = full.indexOf('{', i);
            if (open == -1) {
                // append rest (escape regex meta)
                regex.append(Pattern.quote(full.substring(i)));
                break;
            }
            // append literal up to open
            if (open > i) {
                regex.append(Pattern.quote(full.substring(i, open)));
            }
            int close = full.indexOf('}', open);
            if (close == -1) {
                // malformed, treat literally
                regex.append(Pattern.quote(full.substring(open)));
                break;
            }
            String name = full.substring(open + 1, close);
            // named group
            regex.append("(?<").append(name).append(">[^/]+)");
            i = close + 1;
        }
        String finalRegex = "^" + regex.toString() + "$";
        Pattern p = Pattern.compile(finalRegex);
        Matcher m = p.matcher(requestedPath);
        if (!m.matches()) return null;
        // extract named groups (Java exposes via group names not directly; use group for each name)
        // We can parse names by scanning the regex for (?<name>
        Map<String,String> vars = new HashMap<>();
        // Simple approach: find occurrences of (?<name>
        Pattern namePat = Pattern.compile("\\(\\?<([a-zA-Z0-9_]+)>");
        Matcher nm = namePat.matcher(regex.toString());
        int idx = 1;
        while (nm.find()) {
            String varName = nm.group(1);
            String value = m.group(varName);
            vars.put(varName, value);
            idx++;
        }
        return vars;
    }

    @Override
    public String toString() {
        return "RouteMapping{" +
                "className='" + className + '\'' +
                ", controllerValue='" + controllerValue + '\'' +
                ", urlValue='" + urlValue + '\'' +
                ", methodName='" + methodName + '\'' +
                ", fullUrl='" + getFullUrl() + '\'' +
                '}';
    }
    
    /**
     * Appelle la méthode du contrôleur en utilisant la réflexion.
     * La méthode doit retourner un String, sinon une exception est levée.
     * 
     * @return Le résultat String retourné par la méthode
     * @throws Exception Si la méthode ne retourne pas un String ou si l'invocation échoue
     */
    public String callMethod() throws Exception {
        // Charger la classe du contrôleur
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> clazz = Class.forName(className, true, loader);
        
        // Créer une instance du contrôleur (constructeur par défaut)
        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
        
        // Trouver la méthode à invoquer
        Method method = clazz.getDeclaredMethod(methodName);
        
        // Vérifier que la méthode retourne un String
        if (!method.getReturnType().equals(String.class)) {
            throw new Exception("La méthode " + methodName + " de la classe " + className + 
                              " ne retourne pas un String (retourne: " + method.getReturnType().getName() + ")");
        }
        
        // Invoquer la méthode
        Object result = method.invoke(controllerInstance);
        
        // Retourner le résultat (déjà vérifié comme String)
        return (String) result;
    }

    /**
     * Variante acceptant HttpServletRequest/Response pour permettre aux contrôleurs
     * d'utiliser request.setAttribute(...) directement.
     * Cherche une signature dans l'ordre: (HttpServletRequest, HttpServletResponse),
     * (HttpServletRequest), (), et invoque la méthode trouvée.
     */
    public String callMethod(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> clazz = Class.forName(className, true, loader);
        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

        Method method = null;
        Object[] args = null;

        try {
            method = clazz.getDeclaredMethod(methodName, HttpServletRequest.class, HttpServletResponse.class);
            args = new Object[] { request, response };
        } catch (NoSuchMethodException e1) {
            try {
                method = clazz.getDeclaredMethod(methodName, HttpServletRequest.class);
                args = new Object[] { request };
            } catch (NoSuchMethodException e2) {
                method = clazz.getDeclaredMethod(methodName);
                args = new Object[] {};
            }
        }

        if (!method.getReturnType().equals(String.class)) {
            throw new Exception("La méthode " + methodName + " de la classe " + className +
                    " ne retourne pas un String (retourne: " + method.getReturnType().getName() + ")");
        }

        Object result = method.invoke(controllerInstance, args);
        return (String) result;
    }

    /**
     * Résultat d'invocation contenant la vue et le modèle rempli par le contrôleur.
     */
    public static class InvokeResult {
        private final String view;
        private final Model model;

        public InvokeResult(String view, Model model) {
            this.view = view;
            this.model = model;
        }

        public String getView() { return view; }
        public Model getModel() { return model; }
    }

    /**
     * Variante qui accepte un `Model` et tente d'injecter celui-ci dans la méthode
     * du contrôleur si la signature l'accepte. Retourne la vue et le modèle.
     * Supporte aussi les méthodes retournant directement un ModelView.
     */
    public InvokeResult callMethodWithModel(HttpServletRequest request, HttpServletResponse response, Map<String,String> pathVars) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> clazz = Class.forName(className, true, loader);
        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

        // Préparer un Model pour le contrôleur
        Model model = new Model();

        // Rechercher une méthode du contrôleur avec le bon nom et des paramètres que
        // nous pouvons satisfaire (Model, HttpServletRequest, HttpServletResponse, et/ou path vars)
        Method target = null;
        Object[] args = null;

        for (Method m : clazz.getDeclaredMethods()) {
            if (!m.getName().equals(methodName)) continue;
            java.lang.reflect.Parameter[] params = m.getParameters();
            Object[] candidateArgs = new Object[params.length];
            boolean ok = true;
            for (int i = 0; i < params.length; i++) {
                Class<?> pt = params[i].getType();
                String pname = params[i].getName();
                if (pt.equals(Model.class)) {
                    candidateArgs[i] = model;
                } else if (pt.equals(HttpServletRequest.class)) {
                    candidateArgs[i] = request;
                } else if (pt.equals(HttpServletResponse.class)) {
                    candidateArgs[i] = response;
                } else {
                    // Vérifier si le paramètre a l'annotation @PathVariable
                    PathVariable pathVariableAnnotation = params[i].getAnnotation(PathVariable.class);
                    // Vérifier si le paramètre a l'annotation @RequestParam
                    RequestParam requestParamAnnotation = params[i].getAnnotation(RequestParam.class);
                    
                    String paramNameToLookup = pname; // Par défaut, utiliser le nom du paramètre
                    String defaultValue = null;
                    boolean isPathVariable = false;
                    
                    if (pathVariableAnnotation != null) {
                        // C'est une path variable
                        isPathVariable = true;
                        String annotationValue = pathVariableAnnotation.value();
                        if (annotationValue != null && !annotationValue.isEmpty()) {
                            paramNameToLookup = annotationValue;
                        }
                    } else if (requestParamAnnotation != null) {
                        // C'est un request param
                        String annotationValue = requestParamAnnotation.value();
                        if (annotationValue != null && !annotationValue.isEmpty()) {
                            paramNameToLookup = annotationValue;
                        }
                        // Récupérer la valeur par défaut
                        String annotationDefault = requestParamAnnotation.defaultValue();
                        if (annotationDefault != null && !annotationDefault.isEmpty()) {
                            defaultValue = annotationDefault;
                        }
                    }
                    
                    // Chercher la valeur selon le type d'annotation
                    String raw = null;
                    if (isPathVariable) {
                        // Pour @PathVariable, chercher uniquement dans pathVars
                        if (pathVars != null && pathVars.containsKey(paramNameToLookup)) {
                            raw = pathVars.get(paramNameToLookup);
                        }
                    } else {
                        // Pour @RequestParam ou sans annotation, chercher d'abord pathVars puis request params
                        if (pathVars != null && pathVars.containsKey(paramNameToLookup)) {
                            raw = pathVars.get(paramNameToLookup);
                        } else if (request != null) {
                            raw = request.getParameter(paramNameToLookup);
                        }
                    }
                    
                    // Si aucune valeur trouvée, utiliser la valeur par défaut
                    if (raw == null && defaultValue != null) {
                        raw = defaultValue;
                    }
                    
                    if (raw != null) {
                        Object converted = convertStringToType(raw, pt);
                        if (converted == null) { ok = false; break; }
                        candidateArgs[i] = converted;
                    } else {
                        // Paramètre non fourni : accepter null pour les types wrapper (Integer, Long, etc.)
                        // mais rejeter pour les primitifs (int, long, etc.)
                        if (pt.isPrimitive()) {
                            ok = false;
                            break;
                        }
                        // Pour les wrappers et String, laisser null
                        candidateArgs[i] = null;
                    }
                }
            }
            if (!ok) continue;
            // method found
            target = m;
            args = candidateArgs;
            break;
        }

        if (target == null) {
            throw new Exception("Méthode " + methodName + " non trouvée avec une signature supportée dans " + className);
        }

        // Vérifier type de retour
        Class<?> returnType = target.getReturnType();
        if (!returnType.equals(String.class) && !returnType.equals(ModelView.class)) {
            throw new Exception("La méthode " + methodName + " de la classe " + className +
                    " ne retourne ni String ni ModelView (retourne: " + returnType.getName() + ")");
        }

        Object result = target.invoke(controllerInstance, args == null ? new Object[]{} : args);
        if (result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            String view = mv.getViewPath();
            model.addAllAttributes(mv.getModel());
            return new InvokeResult(view, model);
        }
        String view = (String) result;
        return new InvokeResult(view, model);
    }

    /**
     * Convertit une chaîne vers un type simple supporté (String, wrappers numériques, boolean).
     * Retourne null si la conversion échoue ou type non supporté.
     */
    private static Object convertStringToType(String raw, Class<?> target) {
        if (target.equals(String.class)) return raw;
        try {
            if (target.equals(Long.class) || target.equals(long.class)) return Long.valueOf(raw);
            if (target.equals(Integer.class) || target.equals(int.class)) return Integer.valueOf(raw);
            if (target.equals(Short.class) || target.equals(short.class)) return Short.valueOf(raw);
            if (target.equals(Byte.class) || target.equals(byte.class)) return Byte.valueOf(raw);
            if (target.equals(Double.class) || target.equals(double.class)) return Double.valueOf(raw);
            if (target.equals(Float.class) || target.equals(float.class)) return Float.valueOf(raw);
            if (target.equals(Boolean.class) || target.equals(boolean.class)) return Boolean.valueOf(raw);
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Appelle la méthode du contrôleur et retourne un ModelView.
     * La méthode du contrôleur peut retourner un String (vue) ou un ModelView directement.
     * @return ModelView construit à partir du résultat
     * @throws Exception si le type de retour n'est ni String ni ModelView
     */
    public ModelView callToModelView() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> clazz = Class.forName(className, true, loader);
        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getDeclaredMethod(methodName);

        Object result = method.invoke(controllerInstance);
        if (result == null) {
            throw new Exception("La méthode " + methodName + " de la classe " + className + " a retourné null");
        }
        if (result instanceof String) {
            return new ModelView((String) result);
        }
        if (result instanceof ModelView) {
            return (ModelView) result;
        }
        throw new Exception("Type de retour non supporté: " + result.getClass().getName() + 
                            " (attendu String ou ModelView)");
    }

    /**
     * Liste tous les fichiers .class dans un répertoire
     */
    private static List<Path> listClassFiles(Path root) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".class"))
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * Convertit un chemin relatif en nom de classe Java
     */
    private static String toClassName(Path rel) {
        String path = rel.toString();
        
        // Normaliser tous les séparateurs de chemin en points
        path = path.replace('\\', '.').replace('/', '.');
        
        // Supprimer l'extension .class
        if (path.endsWith(".class")) {
            path = path.substring(0, path.length() - 6);
        }
        
        return path;
    }

    /**
     * Scan classes under the given classes root and return found route mappings.
     * Cette méthode fait tout le scan en interne.
     */
    public static List<RouteMapping> scanFromClassesRoot(Path classesRoot) throws Exception {
        return scanFromClassesRoot(classesRoot, null);
    }

    /**
     * Version avec ClassLoader explicite pour les environnements Servlet.
     * Cette méthode scanne tous les fichiers .class, charge les classes avec les annotations
     * @ControleurAnnotation et collecte les méthodes avec @HandleURL.
     */
    public static List<RouteMapping> scanFromClassesRoot(Path classesRoot, ClassLoader contextClassLoader) throws Exception {
        List<RouteMapping> result = new ArrayList<>();
        
        // Utiliser le ClassLoader approprié
        ClassLoader loader = contextClassLoader;
        URLClassLoader urlLoader = null;
        boolean shouldCloseLoader = false;
        
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        
        // Si toujours null, créer un URLClassLoader (fallback pour tests unitaires)
        if (loader == null) {
            URL url = classesRoot.toUri().toURL();
            urlLoader = new URLClassLoader(new URL[] { url });
            loader = urlLoader;
            shouldCloseLoader = true;
        }
        
        try {
            // Lister tous les fichiers .class
            List<Path> classFiles = listClassFiles(classesRoot);
            System.out.println("[DEBUG RouteMapping] Found " + classFiles.size() + " class files");
            
            // Pour chaque fichier .class
            for (Path p : classFiles) {
                Path rel = classesRoot.relativize(p);
                String className = toClassName(rel);
                
                System.out.println("[DEBUG RouteMapping] Checking class: '" + className + "'");
                
                try {
                    // Charger la classe
                    Class<?> clazz = Class.forName(className, false, loader);
                    
                    // Vérifier si elle a l'annotation @ControleurAnnotation
                    if (clazz.isAnnotationPresent(ControleurAnnotation.class)) {
                        ControleurAnnotation ctrl = clazz.getAnnotation(ControleurAnnotation.class);
                        String controllerValue = ctrl.value();
                        
                        System.out.println("[DEBUG RouteMapping] Found controller: " + className + " with value: " + controllerValue);
                        
                        // Parcourir toutes les méthodes de la classe
                        for (Method m : clazz.getDeclaredMethods()) {
                            // Vérifier si la méthode a l'annotation @HandleURL
                            HandleURL urlAnn = m.getAnnotation(HandleURL.class);
                            if (urlAnn != null) {
                                String urlValue = urlAnn.value();
                                RouteMapping mapping = new RouteMapping(clazz.getName(), controllerValue, urlValue, m.getName());
                                result.add(mapping);
                                System.out.println("[DEBUG RouteMapping] Added route: " + mapping);
                            }
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("Warning: unable to load " + className + " : " + t.getClass().getSimpleName() + " " + t.getMessage());
                }
            }
        } finally {
            if (shouldCloseLoader && urlLoader != null) {
                try {
                    urlLoader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return result;
    }
}