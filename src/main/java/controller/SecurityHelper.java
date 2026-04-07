package controller;

import entities.Personne;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;

/**
 * Classe utilitaire pour la sécurité RBAC
 * Gère l'authentification et les autorisations avec redirection intelligente
 * @author COMLAN
 */
@Named("securityHelper")
@ApplicationScoped
public class SecurityHelper {
    
    @Inject
    private AuthController authController;
    
    /**
     * Vérifie si l'utilisateur est connecté
     */
    public boolean isAuthenticated() {
        return authController != null && authController.getUtilisateurConnecte() != null;
    }
    
    /**
     * Vérifie si l'utilisateur connecté est un GERANT
     */
    public boolean isGerant() {
        return isAuthenticated() && 
               authController.getUtilisateurConnecte().getRole() == Personne.Role.GERANT;
    }
    
    /**
     * Vérifie si l'utilisateur connecté est un ORGANISATEUR
     */
    public boolean isOrganisateur() {
        return isAuthenticated() && 
               authController.getUtilisateurConnecte().getRole() == Personne.Role.ORGANISATEUR;
    }
    
    /**
     * Vérifie si l'utilisateur connecté est un EMPLOYE
     */
    public boolean isEmploye() {
        return isAuthenticated() && 
               authController.getUtilisateurConnecte().getRole() == Personne.Role.EMPLOYE;
    }

    /**
     * Vérifie si l'utilisateur est un membre du personnel (Gérant, Orga ou Employé)
     */
    public boolean isPersonnel() {
        if (!isAuthenticated()) return false;
        Personne.Role role = authController.getUtilisateurConnecte().getRole();
        return role == Personne.Role.GERANT || 
               role == Personne.Role.ORGANISATEUR || 
               role == Personne.Role.EMPLOYE;
    }
    
    /**
     * Vérifie si l'utilisateur connecté est un CLIENT
     */
    public boolean isClient() {
        return isAuthenticated() && 
               authController.getUtilisateurConnecte().getRole() == Personne.Role.CLIENT;
    }
    
    /**
     * Redirection vers login
     */
    public void redirectToLogin() {
        try {
            FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("login.xhtml");
        } catch (Exception e) {
            System.err.println("Erreur redirection login: " + e.getMessage());
        }
    }
    
    /**
     * Redirection vers une page d'erreur d'autorisation
     */
    public void redirectToUnauthorized() {
        try {
            FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("login.xhtml?error=unauthorized");
        } catch (Exception e) {
            System.err.println("Erreur redirection unauthorized: " + e.getMessage());
        }
    }
    
    // ========== MÉTHODES STATIQUES (LEGACY) ==========
    
    /**
     * Vérifie si l'utilisateur est connecté et redirige vers login si nécessaire
     */
    public static boolean checkAuthentication(AuthController authController) {
        if (authController == null || authController.getUtilisateurConnecte() == null) {
            try {
                System.out.println("=== SÉCURITÉ: Utilisateur non connecté, redirection vers login ===");
                FacesContext.getCurrentInstance().getExternalContext().redirect("login.xhtml");
                return false;
            } catch (IOException e) {
                System.err.println("Erreur de redirection vers login: " + e.getMessage());
                return false;
            }
        }
        return true;
    }
    
    /**
     * Vérifie si l'utilisateur a le rôle requis et redirige intelligemment si nécessaire
     */
    public static boolean checkRole(AuthController authController, Personne.Role requiredRole) {
        if (!checkAuthentication(authController)) {
            return false;
        }
        
        Personne.Role userRole = authController.getUtilisateurConnecte().getRole();
        if (!userRole.equals(requiredRole)) {
            try {
                System.out.println("=== SÉCURITÉ: Accès refusé - Rôle requis: " + requiredRole + ", Rôle utilisateur: " + userRole + " ===");
                
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Accès refusé", "Vous n'avez pas les droits pour accéder à cette page."));
                
                // Redirection intelligente selon le rôle de l'utilisateur
                String redirectPage = getDefaultPageForRole(userRole);
                FacesContext.getCurrentInstance().getExternalContext().redirect(redirectPage);
                return false;
            } catch (IOException e) {
                System.err.println("Erreur de redirection: " + e.getMessage());
                return false;
            }
        }
        return true;
    }
    
    /**
     * Vérifie si l'utilisateur a l'un des rôles requis
     */
    public static boolean checkAnyRole(AuthController authController, Personne.Role... allowedRoles) {
        if (!checkAuthentication(authController)) {
            return false;
        }
        
        Personne.Role userRole = authController.getUtilisateurConnecte().getRole();
        for (Personne.Role allowedRole : allowedRoles) {
            if (userRole.equals(allowedRole)) {
                return true;
            }
        }
        
        try {
            System.out.println("=== SÉCURITÉ: Accès refusé - Rôles autorisés: " + java.util.Arrays.toString(allowedRoles) + ", Rôle utilisateur: " + userRole + " ===");
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Accès refusé", "Vous n'avez pas les droits pour accéder à cette page."));
            
            // Redirection intelligente selon le rôle de l'utilisateur
            String redirectPage = getDefaultPageForRole(userRole);
            FacesContext.getCurrentInstance().getExternalContext().redirect(redirectPage);
            return false;
        } catch (IOException e) {
            System.err.println("Erreur de redirection: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Retourne la page par défaut selon le rôle de l'utilisateur
     */
    private static String getDefaultPageForRole(Personne.Role role) {
        switch (role) {
            case GERANT:
                return "dashboard_home.xhtml";
            case ORGANISATEUR:
                return "dashboard_orga.xhtml";
            case CLIENT:
                return "dashboard_client.xhtml";
            default:
                return "index.xhtml";
        }
    }
    
    /**
     * Vérifie si l'utilisateur connecté est un Gérant
     */
    public static boolean isGerant(AuthController authController) {
        return authController != null && 
               authController.getUtilisateurConnecte() != null && 
               authController.getUtilisateurConnecte().getRole() == Personne.Role.GERANT;
    }
    
    /**
     * Vérifie si l'utilisateur connecté est un Organisateur
     */
    public static boolean isOrganisateur(AuthController authController) {
        return authController != null && 
               authController.getUtilisateurConnecte() != null && 
               authController.getUtilisateurConnecte().getRole() == Personne.Role.ORGANISATEUR;
    }
    
    /**
     * Vérifie si l'utilisateur connecté est un Client
     */
    public static boolean isClient(AuthController authController) {
        return authController != null && 
               authController.getUtilisateurConnecte() != null && 
               authController.getUtilisateurConnecte().getRole() == Personne.Role.CLIENT;
    }
}