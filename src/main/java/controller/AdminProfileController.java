package controller;

import entities.Personne;
import service.PersonneService;
import utils.PasswordUtils;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Data;
import java.io.Serializable;

/**
 * Controller pour la gestion du profil du Gérant (Admin)
 * @author COMLAN
 */
@Named("adminProfileController")
@ViewScoped
@Data
public class AdminProfileController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Inject
    private AuthController authController;
    
    @Inject
    private PersonneService personneService;
    
    // Profil
    private String nom;
    private String prenom;
    private String email;
    private String emailOriginal;
    private String telephone;
    
    // Mot de passe
    private String ancienMotDePasse;
    private String nouveauMotDePasse;
    private String confirmationMotDePasse;
    
    // Modes
    private boolean editMode = false;
    private boolean changePasswordMode = false;
    
    @PostConstruct
    public void init() {
        chargerDonneesProfil();
    }
    
    /**
     * Charge les données du profil du gérant connecté
     */
    private void chargerDonneesProfil() {
        try {
            Personne utilisateur = authController.getUtilisateurConnecte();
            if (utilisateur != null) {
                this.nom = utilisateur.getNom();
                this.prenom = utilisateur.getPrenom();
                this.email = utilisateur.getEmail();
                this.emailOriginal = utilisateur.getEmail();
                this.telephone = utilisateur.getTelephone();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du profil: " + e.getMessage());
        }
    }
    
    /**
     * Passe en mode édition du profil
     */
    public void activateEditMode() {
        editMode = true;
    }
    
    /**
     * Passe en mode changement de mot de passe
     */
    public void activateChangePasswordMode() {
        changePasswordMode = true;
    }
    
    /**
     * Annule les modifications et revient au mode lecture
     */
    public void cancelEdit() {
        editMode = false;
        changePasswordMode = false;
        chargerDonneesProfil();
        
        // Réinitialiser les champs de mot de passe
        ancienMotDePasse = null;
        nouveauMotDePasse = null;
        confirmationMotDePasse = null;
    }
    
    /**
     * Valide les données du profil
     */
    private boolean validateProfileData() {
        // Validation des champs obligatoires
        if (nom == null || nom.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("nom", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le nom est obligatoire"));
            return false;
        }
        
        if (prenom == null || prenom.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("prenom", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le prénom est obligatoire"));
            return false;
        }
        
        if (email == null || email.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("email", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "L'email est obligatoire"));
            return false;
        }
        
        if (telephone == null || telephone.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("telephone", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le téléphone est obligatoire"));
            return false;
        }
        
        // Validation format email
        if (!isValidEmail(email)) {
            FacesContext.getCurrentInstance().addMessage("email", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Format d'email invalide"));
            return false;
        }
        
        // Vérification email unique (si changé)
        if (!email.equals(emailOriginal) && personneService.emailExiste(email)) {
            FacesContext.getCurrentInstance().addMessage("email", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Cet email est déjà utilisé"));
            return false;
        }
        
        return true;
    }
    
    /**
     * Valide les données du mot de passe
     */
    private boolean validatePasswordData() {
        // Vérification de l'ancien mot de passe
        if (ancienMotDePasse == null || ancienMotDePasse.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("ancienMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "L'ancien mot de passe est obligatoire"));
            return false;
        }
        
        // Vérification du nouveau mot de passe
        if (nouveauMotDePasse == null || nouveauMotDePasse.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("nouveauMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le nouveau mot de passe est obligatoire"));
            return false;
        }
        
        // Longueur minimale du nouveau mot de passe
        if (nouveauMotDePasse.length() < 8) {
            FacesContext.getCurrentInstance().addMessage("nouveauMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le mot de passe doit contenir au minimum 8 caractères"));
            return false;
        }
        
        // Vérification confirmation
        if (confirmationMotDePasse == null || !confirmationMotDePasse.equals(nouveauMotDePasse)) {
            FacesContext.getCurrentInstance().addMessage("confirmMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Les mots de passe ne correspondent pas"));
            return false;
        }
        
        // Vérifier que le nouvel mot de passe est différent de l'ancien
        Personne utilisateur = authController.getUtilisateurConnecte();
        if (PasswordUtils.verifyPassword(nouveauMotDePasse, utilisateur.getMotDePasse())) {
            FacesContext.getCurrentInstance().addMessage("nouveauMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le nouveau mot de passe doit être différent de l'ancien"));
            return false;
        }
        
        return true;
    }
    
    /**
     * Valide un email avec regex
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }
    
    /**
     * Sauvegarde les modifications du profil
     */
    public void saveProfile() {
        try {
            if (!validateProfileData()) {
                return;
            }
            
            Personne utilisateur = authController.getUtilisateurConnecte();
            
            // Mettre à jour les données
            utilisateur.setNom(nom.trim());
            utilisateur.setPrenom(prenom.trim());
            utilisateur.setEmail(email.trim());
            utilisateur.setTelephone(telephone.trim());
            
            // Sauvegarder en base
            personneService.save(utilisateur);
            
            // Mettre à jour les infos locales
            emailOriginal = email;
            
            // Sortir du mode édition
            editMode = false;
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Vos informations ont été mises à jour avec succès"));
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du profil: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Impossible de sauvegarder les modifications"));
        }
    }
    
    /**
     * Sauvegarde le nouveau mot de passe
     */
    public void savePassword() {
        try {
            if (!validatePasswordData()) {
                return;
            }
            
            Personne utilisateur = authController.getUtilisateurConnecte();
            
            // Vérifier que l'ancien mot de passe correspond
            if (!PasswordUtils.verifyPassword(ancienMotDePasse, utilisateur.getMotDePasse())) {
                FacesContext.getCurrentInstance().addMessage("ancienMdp", 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "L'ancien mot de passe est incorrect"));
                return;
            }
            
            // Hacher le nouveau mot de passe
            String motDePasseHashe = PasswordUtils.hashPassword(nouveauMotDePasse);
            
            // Mettre à jour
            utilisateur.setMotDePasse(motDePasseHashe);
            personneService.save(utilisateur);
            
            // Réinitialiser les champs
            ancienMotDePasse = null;
            nouveauMotDePasse = null;
            confirmationMotDePasse = null;
            changePasswordMode = false;
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Votre mot de passe a été changé avec succès"));
            
        } catch (Exception e) {
            System.err.println("Erreur lors du changement de mot de passe: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Impossible de changer le mot de passe"));
        }
    }
}
