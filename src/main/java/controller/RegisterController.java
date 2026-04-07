package controller;

import entities.Client;
import entities.Organisateur;
import entities.Personne.Role;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import lombok.Data;
import service.PersonneService;
import utils.PasswordUtils;

/**
 * Contrôleur pour l'inscription des clients et organisateurs
 */
@Named("registerController")
@RequestScoped
@Data
public class RegisterController implements Serializable {

    private static final long serialVersionUID = 1L;

    // Type d'inscription
    private String typeInscription; // "CLIENT" ou "ORGANISATEUR"
    
    // Type d'organisateur (si applicable)
    private String typeOrganisateur; // "PHYSIQUE" ou "MORALE"

    // Champs communs
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String motDePasse;
    private String confirmPassword;
    private boolean acceptTerms;
    
    // Champs spécifiques Organisateur Morale
    private String nomEntreprise;
    private String numeroSIRET;
    private String secteurActivite;
    private String siteWeb;
    private String adresseSiege;
    private String prenomRepresentant;
    private String nomRepresentant;

    @Inject
    private PersonneService personneService;

    @Inject
    private FacesContext facesContext;

    @Inject
    private AuthController authController;

    /**
     * Valide l'email en temps réel
     */
    public void validateEmail(FacesContext context, UIComponent component, Object value) 
            throws ValidatorException {
        String emailValue = (String) value;
        
        if (emailValue == null || emailValue.trim().isEmpty()) {
            return;
        }

        if (!isValidEmail(emailValue)) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Email invalide",
                "Veuillez entrer une adresse email valide"
            ));
        }

        if (personneService.emailExiste(emailValue)) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Email déjà utilisé",
                "Cet email est déjà associé à un compte."
            ));
        }
    }

    /**
     * Valide le format d'une adresse email
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    /**
     * Définit le type d'inscription à CLIENT
     */
    public void setTypeInscriptionClient() {
        this.typeInscription = "CLIENT";
    }

    /**
     * Définit le type d'inscription à ORGANISATEUR
     */
    public void setTypeInscriptionOrganisateur() {
        this.typeInscription = "ORGANISATEUR";
    }

    /**
     * Définit le type d'organisateur à PHYSIQUE
     */
    public void setTypeOrganisateurPhysique() {
        this.typeOrganisateur = "PHYSIQUE";
    }

    /**
     * Définit le type d'organisateur à MORALE
     */
    public void setTypeOrganisateurMorale() {
        this.typeOrganisateur = "MORALE";
    }

    /**
     * Méthode principale d'inscription
     */
    public String inscrire() {
        if ("CLIENT".equals(typeInscription)) {
            return inscrireClient();
        } else if ("ORGANISATEUR".equals(typeInscription)) {
            return inscrireOrganisateur();
        }
        return null;
    }

    /**
     * Inscription d'un client
     */
    public String inscrireClient() {
        System.out.println("=== INSCRIPTION CLIENT ===");
        
        if (!validateFormClient()) {
            return null;
        }

        try {
            Client nouveauClient = new Client();
            nouveauClient.setNom(nom);
            nouveauClient.setPrenom(prenom);
            nouveauClient.setEmail(email);
            nouveauClient.setTelephone(telephone);
            nouveauClient.setMotDePasse(PasswordUtils.hashPassword(motDePasse));
            nouveauClient.setRole(Role.CLIENT);
            nouveauClient.setActif(true);

            personneService.enregistrer(nouveauClient);
            
            // Auto-login
            authController.setEmail(email);
            authController.setMotDePasse(motDePasse);
            authController.setUtilisateurConnecte(nouveauClient);

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Inscription réussie !",
                    "Bienvenue " + prenom + " !"));
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            
            return "dashboard_client?faces-redirect=true";

        } catch (Exception e) {
            System.err.println("Erreur inscription client: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Impossible de créer votre compte."));
            return null;
        }
    }

    /**
     * Inscription d'un organisateur (Physique ou Morale)
     */
    public String inscrireOrganisateur() {
        System.out.println("=== INSCRIPTION ORGANISATEUR (" + typeOrganisateur + ") ===");
        
        if (!validateFormOrganisateur()) {
            return null;
        }

        try {
            Organisateur nouvelOrganisateur = new Organisateur();
            nouvelOrganisateur.setNom(nom);
            nouvelOrganisateur.setPrenom(prenom);
            nouvelOrganisateur.setEmail(email);
            nouvelOrganisateur.setTelephone(telephone);
            nouvelOrganisateur.setMotDePasse(PasswordUtils.hashPassword(motDePasse));
            nouvelOrganisateur.setRole(Role.ORGANISATEUR);
            nouvelOrganisateur.setActif(true);
            nouvelOrganisateur.setTypeOrganisation(typeOrganisateur);
            
            // Champs spécifiques Personne Morale
            if ("MORALE".equals(typeOrganisateur)) {
                nouvelOrganisateur.setNomEntreprise(nomEntreprise);
                nouvelOrganisateur.setNumeroSIRET(numeroSIRET);
                nouvelOrganisateur.setSecteurActivite(secteurActivite);
                nouvelOrganisateur.setSiteWeb(siteWeb);
                nouvelOrganisateur.setAdresseSiege(adresseSiege);
                nouvelOrganisateur.setPrenomRepresentant(prenomRepresentant);
                nouvelOrganisateur.setNomRepresentant(nomRepresentant);
            }

            personneService.enregistrer(nouvelOrganisateur);
            
            // Auto-login
            authController.setEmail(email);
            authController.setMotDePasse(motDePasse);
            authController.setUtilisateurConnecte(nouvelOrganisateur);

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Inscription réussie !",
                    "Bienvenue organisateur !"));
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            
            return "dashboard_orga?faces-redirect=true";

        } catch (Exception e) {
            System.err.println("Erreur inscription organisateur: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Impossible de créer votre compte."));
            return null;
        }
    }

    /**
     * Valide le formulaire Client
     */
    private boolean validateFormClient() {
        if (nom == null || nom.trim().isEmpty() ||
            prenom == null || prenom.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            telephone == null || telephone.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Tous les champs obligatoires doivent être remplis"));
            return false;
        }

        if (!isValidEmail(email)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Format d'email invalide"));
            return false;
        }

        if (personneService.emailExiste(email)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Cet email est déjà utilisé"));
            return false;
        }

        if (motDePasse == null || motDePasse.length() < 6) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Le mot de passe doit contenir au moins 6 caractères"));
            return false;
        }

        if (!motDePasse.equals(confirmPassword)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Les mots de passe ne correspondent pas"));
            return false;
        }

        if (!acceptTerms) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Acceptez les conditions d'utilisation"));
            return false;
        }

        return true;
    }

    /**
     * Valide le formulaire Organisateur
     */
    private boolean validateFormOrganisateur() {
        // Champs communs
        if (nom == null || nom.trim().isEmpty() ||
            prenom == null || prenom.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            telephone == null || telephone.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Tous les champs obligatoires doivent être remplis"));
            return false;
        }

        if (!isValidEmail(email)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Format d'email invalide"));
            return false;
        }

        if (personneService.emailExiste(email)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Cet email est déjà utilisé"));
            return false;
        }

        if (motDePasse == null || motDePasse.length() < 6) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Le mot de passe doit contenir au moins 6 caractères"));
            return false;
        }

        if (!motDePasse.equals(confirmPassword)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Les mots de passe ne correspondent pas"));
            return false;
        }

        // Champs Personne Morale
        if ("MORALE".equals(typeOrganisateur)) {
            if (nomEntreprise == null || nomEntreprise.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                        "Le nom de l'entreprise est obligatoire"));
                return false;
            }

            if (secteurActivite == null || secteurActivite.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                        "Le secteur d'activité est obligatoire"));
                return false;
            }

            if (adresseSiege == null || adresseSiege.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                        "L'adresse du siège est obligatoire"));
                return false;
            }

            if (prenomRepresentant == null || prenomRepresentant.trim().isEmpty() ||
                nomRepresentant == null || nomRepresentant.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                        "Les informations du représentant sont obligatoires"));
                return false;
            }
        }

        if (!acceptTerms) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", 
                    "Acceptez les conditions d'utilisation"));
            return false;
        }

        return true;
    }
}
