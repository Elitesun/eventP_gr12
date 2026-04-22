package controller;

import entities.Evenement;
import entities.Organisateur;
import entities.Personne;
import entities.TicketCategorie;
import service.EvenementService;
import service.PersonneService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Base64;
import java.util.stream.Collectors;

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

    @Inject
    private NotificationController notificationController;

// Propriétés pour la création/modification d'événement
    private Evenement nouvelEvenement;
    private Evenement evenementSelectionne;
    private List<Evenement> evenements;
    private List<TicketCategorie> categoriesSaisie;
    private String message;
    private boolean modeCreation = true;
    private Date todayDate = new Date();

    @PostConstruct
    public void init() {
        nouvelEvenement = new Evenement();
        initialiserCategories();
        chargerEvenements();
    }

    /**
     * Charge tous les événements
     */
    public void chargerEvenements() {
        try {
            Organisateur organisateurConnecte = getOrganisateurConnecte();
            if (organisateurConnecte == null) {
                evenements = new ArrayList<>();
                return;
            }
            evenements = evenementService.trouverEvenementsParOrganisateur(organisateurConnecte);
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
        initialiserCategories();
        message = null;
    }

    /**
     * Prépare la modification d'un événement
     */
    public void preparerModification(Evenement evenement) {
        if (evenement == null || evenement.getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Événement invalide."));
            return;
        }
        preparerModificationParId(evenement.getId());
    }

    public void preparerModificationParId(Long evenementId) {
        if (evenementId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Événement invalide."));
            return;
        }

        modeCreation = false;
        evenementSelectionne = evenementService.trouverEvenementAvecCategories(evenementId);
        if (evenementSelectionne == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Événement introuvable."));
            return;
        }

        if (!estProprietaire(evenementSelectionne)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Accès refusé", "Vous ne pouvez modifier que vos propres événements."));
            modeCreation = true;
            return;
        }

        nouvelEvenement = new Evenement();
        nouvelEvenement.setId(evenementSelectionne.getId());
        nouvelEvenement.setTitre(evenementSelectionne.getTitre());
        nouvelEvenement.setDescription(evenementSelectionne.getDescription());
        nouvelEvenement.setDateEvenement(evenementSelectionne.getDateEvenement());
        nouvelEvenement.setLieu(evenementSelectionne.getLieu());
        nouvelEvenement.setImageUrl(evenementSelectionne.getImageUrl());
        nouvelEvenement.setPrixTicket(evenementSelectionne.getPrixTicket());
        nouvelEvenement.setNombreTicketsTotal(evenementSelectionne.getNombreTicketsTotal());
        nouvelEvenement.setStatut(evenementSelectionne.getStatut());
        nouvelEvenement.setOrganisateur(evenementSelectionne.getOrganisateur());

        categoriesSaisie = new ArrayList<>();
        if (evenementSelectionne.getCategories() != null && !evenementSelectionne.getCategories().isEmpty()) {
            for (TicketCategorie categorie : evenementSelectionne.getCategories()) {
                if (categorie == null || categorie.getNom() == null) {
                    continue;
                }
                TicketCategorie cible = new TicketCategorie();
                cible.setNom(categorie.getNom().trim());
                cible.setPrix(categorie.getPrix());
                cible.setQuantiteTotale(categorie.getQuantiteTotale());
                categoriesSaisie.add(cible);
            }
        }
        if (categoriesSaisie.isEmpty()) {
            ajouterCategorie();
        }

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
                nouvelEvenement.getDescription() == null || nouvelEvenement.getDescription().trim().isEmpty() ||
                nouvelEvenement.getDateEvenement() == null || nouvelEvenement.getLieu() == null || nouvelEvenement.getLieu().trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Tous les champs marqués d'une étoile (*) sont obligatoires."));
                return;
            }

            List<TicketCategorie> categoriesValides = extraireCategoriesValides();
            if (categoriesValides.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Au moins une catégorie est obligatoire avec un nom, un prix et une quantité valides."));
                return;
            }

            int totalTickets = 0;
            double prixMin = Double.MAX_VALUE;
            for (TicketCategorie categorie : categoriesValides) {
                totalTickets += categorie.getQuantiteTotale();
                if (categorie.getPrix() < prixMin) {
                    prixMin = categorie.getPrix();
                }
            }

            nouvelEvenement.setPrixTicket(prixMin == Double.MAX_VALUE ? 0.0 : prixMin);
            nouvelEvenement.setNombreTicketsTotal(totalTickets);
            nouvelEvenement.setTicketsVendus(0);
            nouvelEvenement.setTicketsDisponibles(totalTickets);
            nouvelEvenement.setCategories(categoriesValides);

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



            if (modeCreation) {
                // Créer un nouvel événement
                Evenement created = evenementService.creerEvenement(nouvelEvenement);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Événement créé avec succès !"));
                notificationController.success("Événement créé", "Votre événement \"" + created.getTitre() + "\" est prêt à être publié.");
            } else {
                // Modifier un événement existant
                Organisateur organisateurConnecte = getOrganisateurConnecte();
                if (organisateurConnecte == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Accès refusé", "Vous ne pouvez modifier que vos propres événements."));
                    return;
                }
                nouvelEvenement.setDateModification(new Date());
                Evenement updated = evenementService.mettreAJourEvenement(organisateurConnecte.getId(), nouvelEvenement);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Événement modifié avec succès !"));
                notificationController.info("Événement mis à jour", "Les changements sur \"" + updated.getTitre() + "\" ont été enregistrés.");
            }

            chargerEvenements();
            preparerCreation();

        } catch (ConstraintViolationException cve) {
            String violations = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur de validation", violations));
        } catch (Exception e) {
            e.printStackTrace();
            String detail = e.getMessage();
            if (e.getCause() != null && e.getCause() instanceof ConstraintViolationException) {
                ConstraintViolationException cve = (ConstraintViolationException) e.getCause();
                detail = cve.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .collect(Collectors.joining(", "));
            }
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur lors de la sauvegarde", detail));
        }
    }

    /**
     * Publie un événement
     */
    public void publierEvenement(Evenement evenement) {
        try {
            if (evenement == null || evenement.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Événement invalide."));
                return;
            }
            Organisateur organisateurConnecte = getOrganisateurConnecte();
            if (organisateurConnecte == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Accès refusé", "Vous ne pouvez publier que vos propres événements."));
                return;
            }
            evenementService.publierEvenement(organisateurConnecte.getId(), evenement.getId());
            message = "Événement publié avec succès !";
            notificationController.success("Événement publié", "\"" + evenement.getTitre() + "\" est maintenant visible publiquement.");
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
            if (evenement == null || evenement.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Événement invalide."));
                return;
            }
            Organisateur organisateurConnecte = getOrganisateurConnecte();
            if (organisateurConnecte == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Accès refusé", "Vous ne pouvez annuler que vos propres événements."));
                return;
            }
            evenementService.annulerEvenement(organisateurConnecte.getId(), evenement.getId());
            message = "Événement annulé avec succès !";
            notificationController.warning("Événement annulé", "\"" + evenement.getTitre() + "\" a été annulé.");
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
            if (evenement == null || evenement.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Événement invalide."));
                return;
            }
            Organisateur organisateurConnecte = getOrganisateurConnecte();
            if (organisateurConnecte == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Accès refusé", "Vous ne pouvez supprimer que vos propres événements."));
                return;
            }
            evenementService.supprimerEvenement(organisateurConnecte.getId(), evenement.getId());
            message = "Événement supprimé avec succès !";
            notificationController.warning("Événement supprimé", "\"" + evenement.getTitre() + "\" a été supprimé.");
            chargerEvenements();
        } catch (Exception e) {
            message = "Erreur lors de la suppression: " + e.getMessage();
        }
    }

    private void initialiserCategories() {
        categoriesSaisie = new ArrayList<>();
        ajouterCategorie();
    }

    public void ajouterCategorie() {
        TicketCategorie nouvelle = new TicketCategorie();
        nouvelle.setNom("");
        nouvelle.setPrix(0.0);
        nouvelle.setQuantiteTotale(1);
        categoriesSaisie.add(nouvelle);
    }

    public void supprimerCategorie(TicketCategorie categorie) {
        categoriesSaisie.remove(categorie);
    }

    private List<TicketCategorie> extraireCategoriesValides() {
        List<TicketCategorie> valides = new ArrayList<>();
        if (categoriesSaisie == null) {
            return valides;
        }

        for (TicketCategorie categorie : categoriesSaisie) {
            if (categorie == null) {
                continue;
            }
            if (categorie.getNom() == null || categorie.getNom().trim().isEmpty()) {
                continue;
            }
            if (categorie.getPrix() == null || categorie.getPrix() < 0) {
                continue;
            }
            if (categorie.getQuantiteTotale() == null || categorie.getQuantiteTotale() <= 0) {
                continue;
            }

            TicketCategorie copie = new TicketCategorie();
            copie.setNom(categorie.getNom().trim());
            copie.setPrix(categorie.getPrix());
            copie.setQuantiteTotale(categorie.getQuantiteTotale());
            copie.setQuantiteVendue(0);
            copie.setQuantiteDisponible(categorie.getQuantiteTotale());
            valides.add(copie);
        }

        return valides;
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            UploadedFile file = event.getFile();
            if (file != null && file.getContent() != null) {
                byte[] contents = file.getContent();
                String base64Content = Base64.getEncoder().encodeToString(contents);
                String mimeType = file.getContentType();
                String dataUrl = "data:" + mimeType + ";base64," + base64Content;
                
                if (modeCreation) {
                    nouvelEvenement.setImageUrl(dataUrl);
                } else {
                    nouvelEvenement.setImageUrl(dataUrl);
                }
                
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Image chargée: " + file.getFileName()));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Erreur lors du chargement de l'image"));
        }
    }

    private Organisateur getOrganisateurConnecte() {
        Personne current = authController.getUtilisateurConnecte();
        if (!(current instanceof Organisateur) || current.getId() == null) {
            return null;
        }
        Personne managedUser = personneService.trouverParId(current.getId());
        if (!(managedUser instanceof Organisateur)) {
            return null;
        }
        return (Organisateur) managedUser;
    }

    private boolean estProprietaire(Evenement evenement) {
        Organisateur organisateur = getOrganisateurConnecte();
        return organisateur != null
                && evenement != null
                && evenement.getOrganisateur() != null
                && evenement.getOrganisateur().getId() != null
                && organisateur.getId().equals(evenement.getOrganisateur().getId());
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

    public List<TicketCategorie> getCategoriesSaisie() {
        return categoriesSaisie;
    }

    public void setCategoriesSaisie(List<TicketCategorie> categoriesSaisie) {
        this.categoriesSaisie = categoriesSaisie;
    }
    public Date getTodayDate() {
        return todayDate;
    }

    public void setTodayDate(Date todayDate) {
        this.todayDate = todayDate;
    }
}