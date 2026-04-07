package controller;

import entities.Evenement;
import entities.Organisateur;
import entities.Personne;
import service.EvenementService;
import service.PersonneService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Date;

/**
 * Contrôleur pour la gestion des événements par l'admin
 */
@Named("evenementController")
@ViewScoped
public class EvenementController implements Serializable {

    @Inject
    private EvenementService evenementService;

    @Inject
    private AuthController authController;

@Inject
private SecurityHelper securityHelper;

@Inject
private PersonneService personneService;

// Propriétés pour la création/modification d'événement
    private Evenement nouvelEvenement;
    private Evenement evenementSelectionne;
    private List<Evenement> evenements;
    private String message;
    private boolean modeCreation = true;

    @PostConstruct
    public void init() {
        nouvelEvenement = new Evenement();
        chargerEvenements();
    }

    /**
     * Charge tous les événements
     */
    public void chargerEvenements() {
        try {
            evenements = evenementService.trouverTousEvenements();
        } catch (Exception e) {
            message = "Erreur lors du chargement des événements: " + e.getMessage();
        }
    }

    /**
     * Prépare la création d'un nouvel événement
     */
    public void preparerCreation() {
        modeCreation = true;
        nouvelEvenement = new Evenement();
        nouvelEvenement.setDateCreation(new Date());
        message = null;
    }

    /**
     * Prépare la modification d'un événement
     */
    public void preparerModification(Evenement evenement) {
        modeCreation = false;
        evenementSelectionne = evenement;
        nouvelEvenement = new Evenement();
        nouvelEvenement.setId(evenement.getId());
        nouvelEvenement.setTitre(evenement.getTitre());
        nouvelEvenement.setDescription(evenement.getDescription());
        nouvelEvenement.setDateEvenement(evenement.getDateEvenement());
        nouvelEvenement.setLieu(evenement.getLieu());
        nouvelEvenement.setPrixTicket(evenement.getPrixTicket());
        nouvelEvenement.setNombreTicketsTotal(evenement.getNombreTicketsTotal());
        nouvelEvenement.setStatut(evenement.getStatut());
        nouvelEvenement.setOrganisateur(evenement.getOrganisateur());
        message = null;
    }

    /**
     * Sauvegarde un événement (création ou modification)
     */
    public void sauvegarderEvenement() {
        try {
            // Vérifier que l'utilisateur est bien authentifié et est organisateur
            if (authController.getUtilisateurConnecte() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Utilisateur non authentifié."));
                return;
            }

            if (!(authController.getUtilisateurConnecte() instanceof Organisateur)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Seul un organisateur peut créer un événement."));
                return;
            }

            // Enhanced validation
            if (nouvelEvenement.getTitre() == null || nouvelEvenement.getTitre().trim().isEmpty() ||
                nouvelEvenement.getNombreTicketsTotal() == null || nouvelEvenement.getNombreTicketsTotal() <= 0 ||
                nouvelEvenement.getPrixTicket() == null || nouvelEvenement.getPrixTicket() < 0 ||
                nouvelEvenement.getDateEvenement() == null || nouvelEvenement.getLieu() == null || nouvelEvenement.getLieu().trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Champs obligatoires invalides."));
                return;
            }

            // Ensure managed organisateur
            Personne userConnecte = authController.getUtilisateurConnecte();
            if (userConnecte.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Utilisateur invalide (ID null)."));
                return;
            }
            Personne managedUser = personneService.trouverParId(userConnecte.getId());
            if (managedUser == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Utilisateur non trouvé en base."));
                return;
            }
            nouvelEvenement.setOrganisateur((Organisateur) managedUser);

            System.out.println("[EvenementController] sauvegarderEvenement appelé (modeCreation=" + modeCreation +
                               ") | organisateur.id=" + nouvelEvenement.getOrganisateur().getId() +
                               " | ticketsTotal=" + nouvelEvenement.getNombreTicketsTotal() +
                               " | titre=" + nouvelEvenement.getTitre());

            if (modeCreation) {
                // Créer un nouvel événement
                nouvelEvenement.setOrganisateur((Organisateur) authController.getUtilisateurConnecte());
                Evenement created = evenementService.creerEvenement(nouvelEvenement);
                System.out.println("[EvenementController] événement créé id=" + created.getId());
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Événement créé avec succès !"));
            } else {
                // Modifier un événement existant
                nouvelEvenement.setDateModification(new Date());
                Evenement updated = evenementService.mettreAJourEvenement(nouvelEvenement);
                System.out.println("[EvenementController] événement modifié id=" + updated.getId());
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Événement modifié avec succès !"));
            }

            chargerEvenements();
            preparerCreation();

        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur lors de la sauvegarde", e.getMessage()));
        }
    }

    /**
     * Publie un événement
     */
    public void publierEvenement(Evenement evenement) {
        try {
            evenementService.publierEvenement(evenement.getId());
            message = "Événement publié avec succès !";
            chargerEvenements();
        } catch (Exception e) {
            message = "Erreur lors de la publication: " + e.getMessage();
        }
    }

    /**
     * Annule un événement
     */
    public void annulerEvenement(Evenement evenement) {
        try {
            evenementService.annulerEvenement(evenement.getId());
            message = "Événement annulé avec succès !";
            chargerEvenements();
        } catch (Exception e) {
            message = "Erreur lors de l'annulation: " + e.getMessage();
        }
    }

    /**
     * Supprime un événement
     */
    public void supprimerEvenement(Evenement evenement) {
        try {
            evenementService.supprimerEvenement(evenement.getId());
            message = "Événement supprimé avec succès !";
            chargerEvenements();
        } catch (Exception e) {
            message = "Erreur lors de la suppression: " + e.getMessage();
        }
    }

    // Getters et Setters
    public Evenement getNouvelEvenement() {
        return nouvelEvenement;
    }

    public void setNouvelEvenement(Evenement nouvelEvenement) {
        this.nouvelEvenement = nouvelEvenement;
    }

    public Evenement getEvenementSelectionne() {
        return evenementSelectionne;
    }

    public void setEvenementSelectionne(Evenement evenementSelectionne) {
        this.evenementSelectionne = evenementSelectionne;
    }

    public List<Evenement> getEvenements() {
        return evenements;
    }

    public void setEvenements(List<Evenement> evenements) {
        this.evenements = evenements;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isModeCreation() {
        return modeCreation;
    }

    public void setModeCreation(boolean modeCreation) {
        this.modeCreation = modeCreation;
    }
}