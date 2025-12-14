package com.monframework.core.util.Formatter;

/**
 * Wrapper pour encapsuler une réponse JSON avec un message et un code personnalisés.
 * Permet aux contrôleurs de définir dynamiquement le message et le code HTTP
 * sans utiliser l'annotation @JsonResponse.
 * 
 * Exemple d'utilisation:
 * <pre>
 * @GetRequest("api/user")
 * public JsonResponseWrapper getUser() {
 *     User user = userService.find(1);
 *     return JsonResponseWrapper.success(user, "Utilisateur trouvé", 200);
 * }
 * </pre>
 */
public class JsonResponseWrapper {
    private final Object data;
    private final String status;
    private final String message;
    private final int code;

    private JsonResponseWrapper(Object data, String status, String message, int code) {
        this.data = data;
        this.status = status;
        this.message = message;
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    /**
     * Crée une réponse de succès avec code 200.
     * 
     * @param data Les données à retourner
     * @param message Le message de succès
     * @return JsonResponseWrapper configuré
     */
    public static JsonResponseWrapper success(Object data, String message) {
        return new JsonResponseWrapper(data, "success", message, 200);
    }

    /**
     * Crée une réponse de succès avec code personnalisé.
     * 
     * @param data Les données à retourner
     * @param message Le message de succès
     * @param code Le code HTTP (ex: 201 pour création)
     * @return JsonResponseWrapper configuré
     */
    public static JsonResponseWrapper success(Object data, String message, int code) {
        return new JsonResponseWrapper(data, "success", message, code);
    }

    /**
     * Crée une réponse d'erreur avec code 400.
     * 
     * @param message Le message d'erreur
     * @return JsonResponseWrapper configuré
     */
    public static JsonResponseWrapper error(String message) {
        return new JsonResponseWrapper(null, "error", message, 400);
    }

    /**
     * Crée une réponse d'erreur avec code personnalisé.
     * 
     * @param message Le message d'erreur
     * @param code Le code HTTP (ex: 404, 500, etc.)
     * @return JsonResponseWrapper configuré
     */
    public static JsonResponseWrapper error(String message, int code) {
        return new JsonResponseWrapper(null, "error", message, code);
    }

    /**
     * Crée une réponse d'erreur avec données et code personnalisé.
     * 
     * @param data Les données d'erreur (ex: détails de validation)
     * @param message Le message d'erreur
     * @param code Le code HTTP
     * @return JsonResponseWrapper configuré
     */
    public static JsonResponseWrapper error(Object data, String message, int code) {
        return new JsonResponseWrapper(data, "error", message, code);
    }

    /**
     * Crée une réponse personnalisée complète.
     * 
     * @param data Les données
     * @param status Le statut ("success", "error", ou autre)
     * @param message Le message
     * @param code Le code HTTP
     * @return JsonResponseWrapper configuré
     */
    public static JsonResponseWrapper custom(Object data, String status, String message, int code) {
        return new JsonResponseWrapper(data, status, message, code);
    }
}
