package controller;

import entities.Personne;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Data;
import service.PersonneService;
import utils.PasswordUtils;
import java.io.Serializable;

/**
 * Contrôleur pour la gestion du profil organisateur
 * Permet à l'organisateur de voir et modifier ses informations personnelles
 */
@Named("organizerProfileController")
@SessionScoped
@Data
public class OrganizerProfileController implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Inject
    private AuthController authController;
    
    @Inject
    private PersonneService personneService;
    
    // Champs affichés/modifiés
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String emailOriginal;
    
    // Champs entreprise (Organisateur MORALE)
    private String nomEntreprise;
    private String numeroSIRET;
    private String secteurActivite;
    private String siteWeb;
    private String adresseSiege;
    private String prenomRepresentant;
    private String nomRepresentant;
    private String typeOrganisation;
    
    // Champs pour le changement de mot de passe
    private String ancienMotDePasse;
    private String nouveauMotDePasse;
    private String confirmationMotDePasse;
    
    // Flags pour les modes d'édition
    private boolean editMode = false;
    private boolean changePasswordMode = false;
    
    @PostConstruct
    public void init() {
        try {
            Personne utilisateur = authController.getUtilisateurConnecte();
            if (utilisateur != null) {
                this.nom = utilisateur.getNom();
                this.prenom = utilisateur.getPrenom();
                this.email = utilisateur.getEmail();
                this.telephone = utilisateur.getTelephone();
                this.emailOriginal = this.email;
                
                // Charger les champs entreprise si Organisateur MORALE
                if (utilisateur instanceof entities.Organisateur) {
                    entities.Organisateur orga = (entities.Organisateur) utilisateur;
                    this.typeOrganisation = orga.getTypeOrganisation();
                    this.nomEntreprise = orga.getNomEntreprise();
                    this.numeroSIRET = orga.getNumeroSIRET();
                    this.secteurActivite = orga.getSecteurActivite();
                    this.siteWeb = orga.getSiteWeb();
                    this.adresseSiege = orga.getAdresseSiege();
                    this.prenomRepresentant = orga.getPrenomRepresentant();
                    this.nomRepresentant = orga.getNomRepresentant();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du profil organisateur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Active le mode édition du profil
     */
    public void activateEditMode() {
        this.editMode = true;
    }
    
    /**
     * Active le mode changement de mot de passe
     */
    public void activateChangePasswordMode() {
        this.changePasswordMode = true;
    }
    
    /**
     * Valide les données du profil avant sauvegarde
     */
    private boolean validateProfileData() {
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
        
        if (!isValidEmail(email.trim())) {
            FacesContext.getCurrentInstance().addMessage("email", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "L'email n'est pas valide"));
            return false;
        }
        
        if (telephone == null || telephone.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("telephone", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le téléphone est obligatoire"));
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
            
            // Mettre à jour les champs entreprise si MORALE
            if (utilisateur instanceof entities.Organisateur) {
                entities.Organisateur orga = (entities.Organisateur) utilisateur;
                if ("MORALE".equalsIgnoreCase(typeOrganisation)) {
                    orga.setNomEntreprise(nomEntreprise != null ? nomEntreprise.trim() : null);
                    orga.setNumeroSIRET(numeroSIRET != null ? numeroSIRET.trim() : null);
                    orga.setSecteurActivite(secteurActivite != null ? secteurActivite.trim() : null);
                    orga.setSiteWeb(siteWeb != null ? siteWeb.trim() : null);
                    orga.setAdresseSiege(adresseSiege != null ? adresseSiege.trim() : null);
                    orga.setPrenomRepresentant(prenomRepresentant != null ? prenomRepresentant.trim() : null);
                    orga.setNomRepresentant(nomRepresentant != null ? nomRepresentant.trim() : null);
                }
            }
            
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
     * Valide les données du changement de mot de passe
     */
    private boolean validatePasswordData() {
        if (ancienMotDePasse == null || ancienMotDePasse.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("ancienMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "L'ancien mot de passe est obligatoire"));
            return false;
        }
        
        if (nouveauMotDePasse == null || nouveauMotDePasse.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("nouveauMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le nouveau mot de passe est obligatoire"));
            return false;
        }
        
        if (nouveauMotDePasse.length() < 8) {
            FacesContext.getCurrentInstance().addMessage("nouveauMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le mot de passe doit contenir au moins 8 caractères"));
            return false;
        }
        
        if (confirmationMotDePasse == null || confirmationMotDePasse.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("confirmMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Veuillez confirmer votre mot de passe"));
            return false;
        }
        
        if (!nouveauMotDePasse.equals(confirmationMotDePasse)) {
            FacesContext.getCurrentInstance().addMessage("confirmMdp", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Les mots de passe ne correspondent pas"));
            return false;
        }
        
        return true;
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
    
    /**
     * Annule l'édition et restaure les valeurs originales
     */
    public void cancelEdit() {
        editMode = false;
        changePasswordMode = false;
        
        // Restaurer les valeurs originales
        Personne utilisateur = authController.getUtilisateurConnecte();
        if (utilisateur != null) {
            this.nom = utilisateur.getNom();
            this.prenom = utilisateur.getPrenom();
            this.email = utilisateur.getEmail();
            this.telephone = utilisateur.getTelephone();
            
            if (utilisateur instanceof entities.Organisateur) {
                entities.Organisateur orga = (entities.Organisateur) utilisateur;
                this.typeOrganisation = orga.getTypeOrganisation();
                this.nomEntreprise = orga.getNomEntreprise();
                this.numeroSIRET = orga.getNumeroSIRET();
                this.secteurActivite = orga.getSecteurActivite();
                this.siteWeb = orga.getSiteWeb();
                this.adresseSiege = orga.getAdresseSiege();
                this.prenomRepresentant = orga.getPrenomRepresentant();
                this.nomRepresentant = orga.getNomRepresentant();
            }
        }
        
        // Réinitialiser les champs de mot de passe
        ancienMotDePasse = null;
        nouveauMotDePasse = null;
        confirmationMotDePasse = null;
    }
}
