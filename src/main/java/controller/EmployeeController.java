package controller;

import entities.Employe;
import entities.Organisateur;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import service.EmployeService;
import utils.PasswordUtils;

/**
 * Contrôleur pour la gestion des employés
 */
@Named("employeeController")
@SessionScoped
@Data
public class EmployeeController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AuthController authController;

    @Inject
    private EmployeService employeService;

    // Liste des employés
    private List<Employe> employes;

    // Formulaire pour créer/éditer
    private Employe employeForm;
    private boolean modifMode = false;

    // Organisateur connecté
    private Organisateur organisateurConnecte;

    /**
     * Initialise la page - charge les employés
     */
    public void chargerEmployes() {
        try {
            if (authController.getUtilisateurConnecte() instanceof Organisateur) {
                organisateurConnecte = (Organisateur) authController.getUtilisateurConnecte();
                employes = employeService.trouverParEmployeur(organisateurConnecte);
                System.out.println("✓ Employés chargés: " + (employes != null ? employes.size() : 0));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Accès non autorisé"));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des employés: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Impossible de charger les employés"));
        }
    }

    /**
     * Réinitialise le formulaire de création
     */
    public void initialiserFormulaireCreation() {
        employeForm = new Employe();
        modifMode = false;
    }

    /**
     * Prépare l'édition d'un employé
     */
    public void editerEmploye(Employe employe) {
        this.employeForm = new Employe();
        this.employeForm.setId(employe.getId());
        this.employeForm.setNom(employe.getNom());
        this.employeForm.setPrenom(employe.getPrenom());
        this.employeForm.setEmail(employe.getEmail());
        this.employeForm.setTelephone(employe.getTelephone());
        this.employeForm.setMotDePasse(employe.getMotDePasse());
        this.modifMode = true;
    }

    /**
     * Crée ou met à jour un employé
     */
    public String sauvegarderEmploye() {
        try {
            if (!validerFormulaire()) {
                return null;
            }

            if (modifMode) {
                // Mode édition
                Employe employeExistant = employeService.trouverParId(employeForm.getId());
                if (employeExistant == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Employé introuvable"));
                    return null;
                }

                // Vérifier l'email (sauf si c'est le même)
                if (!employeExistant.getEmail().equals(employeForm.getEmail()) &&
                    employeService.emailExiste(employeForm.getEmail())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Cet email est déjà utilisé"));
                    return null;
                }

                employeExistant.setNom(employeForm.getNom());
                employeExistant.setPrenom(employeForm.getPrenom());
                employeExistant.setEmail(employeForm.getEmail());
                employeExistant.setTelephone(employeForm.getTelephone());

                // Ne mettre à jour le mot de passe que s'il est fourni
                if (employeForm.getMotDePasse() != null && !employeForm.getMotDePasse().isEmpty()) {
                    employeExistant.setMotDePasse(PasswordUtils.hashPassword(employeForm.getMotDePasse()));
                }

                employeService.mettre_a_jour(employeExistant);
                chargerEmployes();

                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                        "Employé modifié avec succès"));
            } else {
                // Mode création
                if (employeService.emailExiste(employeForm.getEmail())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Cet email est déjà utilisé"));
                    return null;
                }

                employeForm.setRole(entities.Personne.Role.EMPLOYE);
                employeForm.setActif(true);
                employeForm.setEmployeur(organisateurConnecte);
                employeForm.setMotDePasse(PasswordUtils.hashPassword(employeForm.getMotDePasse()));
                employeForm.setDateInscription(new java.util.Date());
                employeService.enregistrer(employeForm);
                chargerEmployes();

                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                        "Employé créé avec succès"));
            }

            employeForm = null;
            modifMode = false;
            return null;

        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Impossible de sauvegarder l'employé"));
            return null;
        }
    }

    /**
     * Valide le formulaire
     */
    private boolean validerFormulaire() {
        if (employeForm.getNom() == null || employeForm.getNom().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Le nom est obligatoire"));
            return false;
        }

        if (employeForm.getPrenom() == null || employeForm.getPrenom().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Le prénom est obligatoire"));
            return false;
        }

        if (employeForm.getEmail() == null || employeForm.getEmail().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "L'email est obligatoire"));
            return false;
        }

        if (employeForm.getTelephone() == null || employeForm.getTelephone().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Le téléphone est obligatoire"));
            return false;
        }

        if (!modifMode) { // Vérifier le mot de passe uniquement en création
            if (employeForm.getMotDePasse() == null || employeForm.getMotDePasse().length() < 8) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Le mot de passe doit contenir au moins 8 caractères"));
                return false;
            }
        }

        return true;
    }

    /**
     * Désactive un employé
     */
    public String desactiverEmploye(Employe employe) {
        try {
            employeService.desactiver(employe);
            chargerEmployes();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "Employé désactivé"));
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de la désactivation: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Impossible de désactiver l'employé"));
            return null;
        }
    }

    /**
     * Réactive un employé
     */
    public String reactiveEmploye(Employe employe) {
        try {
            employeService.reactiver(employe);
            chargerEmployes();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "Employé réactivé"));
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de la réactivation: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Impossible de réactiver l'employé"));
            return null;
        }
    }

    /**
     * Bascule l'état d'activation (actif/inactif) d'un employé
     */
    public String toggleActivationEmploye(Employe employe) {
        try {
            if (employe.isActif()) {
                employeService.desactiver(employe);
            } else {
                employeService.reactiver(employe);
            }
            chargerEmployes();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    employe.isActif() ? "Employé réactivé" : "Employé désactivé"));
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors du changement d'état: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Impossible de changer l'état de l'employé"));
            return null;
        }
    }

    /**
     * Supprime un employé
     */
    public String supprimerEmploye(Employe employe) {
        try {
            employeService.supprimer(employe);
            chargerEmployes();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "Employé supprimé"));
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Impossible de supprimer l'employé"));
            return null;
        }
    }

    /**
     * Ferme le formulaire
     */
    public void fermerDialogue() {
        employeForm = null;
        modifMode = false;
    }
}
