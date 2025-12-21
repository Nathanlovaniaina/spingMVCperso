package com.monframework.core.util.FileUpload;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitaire pour gérer l'extraction des fichiers uploadés
 * depuis une requête HTTP multipart/form-data.
 */
public class FileUploadHandler {
    
    /**
     * Extrait tous les fichiers uploadés d'une requête et les retourne
     * dans une Map avec le nom du fichier comme clé et son contenu en bytes.
     * 
     * @param request La requête HTTP contenant les fichiers
     * @return Map<String, byte[]> où la clé est le nom du fichier et la valeur est son contenu
     */
    public static Map<String, byte[]> extractUploadedFiles(HttpServletRequest request) {
        Map<String, byte[]> filesMap = new HashMap<>();
        
        try {
            // Vérifier si la requête contient des données multipart
            String contentType = request.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
                Collection<Part> parts = request.getParts();
                
                for (Part part : parts) {
                    // Vérifier si cette partie est un fichier (et pas un champ texte)
                    String submittedFileName = part.getSubmittedFileName();
                    if (submittedFileName != null && !submittedFileName.trim().isEmpty()) {
                        // C'est un fichier
                        byte[] fileContent = readPartContent(part);
                        filesMap.put(submittedFileName, fileContent);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction des fichiers uploadés: " + e.getMessage());
            e.printStackTrace();
        }
        
        return filesMap;
    }
    
    /**
     * Lit le contenu d'une Part et le retourne sous forme de tableau de bytes.
     * 
     * @param part La partie de la requête multipart
     * @return Le contenu en bytes
     * @throws Exception si une erreur survient lors de la lecture
     */
    private static byte[] readPartContent(Part part) throws Exception {
        try (InputStream inputStream = part.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Extrait un seul fichier uploadé par son nom de champ.
     * 
     * @param request La requête HTTP
     * @param fieldName Le nom du champ input file
     * @return byte[] contenant le fichier, ou null si non trouvé
     */
    public static byte[] extractSingleFile(HttpServletRequest request, String fieldName) {
        try {
            String contentType = request.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
                Part part = request.getPart(fieldName);
                if (part != null && part.getSubmittedFileName() != null) {
                    return readPartContent(part);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction du fichier '" + fieldName + "': " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Récupère le nom du fichier uploadé pour un champ donné.
     * 
     * @param request La requête HTTP
     * @param fieldName Le nom du champ input file
     * @return Le nom du fichier, ou null si non trouvé
     */
    public static String getUploadedFileName(HttpServletRequest request, String fieldName) {
        try {
            Part part = request.getPart(fieldName);
            if (part != null) {
                return part.getSubmittedFileName();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du nom de fichier: " + e.getMessage());
        }
        return null;
    }
}
