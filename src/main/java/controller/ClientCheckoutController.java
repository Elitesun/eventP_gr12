package controller;

import entities.Achat;
import entities.Client;
import entities.Evenement;
import entities.TicketCategorie;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import service.AchatService;
import service.EvenementService;
import service.TicketService;

@Named("clientCheckoutController")
@ViewScoped
public class ClientCheckoutController implements Serializable {

    @Inject
    private EvenementService evenementService;

    @Inject
    private AchatService achatService;

    @Inject
    private TicketService ticketService;

    @Inject
    private AuthController authController;

    @Inject
    private NotificationController notificationController;

    private Long eventId;
    private Evenement evenement;
    private List<TicketCategorie> categoriesSelectionnables = new ArrayList<>();
    private Long categorieSelectionneeId;
    private Integer nombreTicketsAcheter = 1;
    private boolean achatReussi;

    @PostConstruct
    public void init() {
        // View params are applied before the first render via f:viewAction.
    }

    public void initialiser() {
        if (eventId == null) {
            return;
        }

        evenement = evenementService.trouverEvenementAvecCategories(eventId);
        if (evenement == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Événement introuvable."));
            return;
        }

        categoriesSelectionnables = new ArrayList<>();
        if (evenement.getCategories() != null) {
            categoriesSelectionnables.addAll(evenement.getCategories());
        }

        if (!categoriesSelectionnables.isEmpty()) {
            categorieSelectionneeId = categoriesSelectionnables.get(0).getId();
        }
        nombreTicketsAcheter = 1;
        achatReussi = false;
    }

    public void acheterTickets() {
        achatReussi = false;

        if (authController.getUtilisateurConnecte() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Connexion requise", "Veuillez vous connecter pour acheter des tickets."));
            return;
        }

        if (!(authController.getUtilisateurConnecte() instanceof Client)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profil invalide", "Seuls les comptes client peuvent acheter des tickets."));
            return;
        }

        if (evenement == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucun événement sélectionné."));
            return;
        }

        if (nombreTicketsAcheter == null || nombreTicketsAcheter <= 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le nombre de tickets doit être positif."));
            return;
        }

        TicketCategorie categorieSelectionnee = getCategorieSelectionnee();
        if (categoriesSelectionnables != null && !categoriesSelectionnables.isEmpty() && categorieSelectionnee == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Veuillez choisir une catégorie."));
            return;
        }

        Client client = (Client) authController.getUtilisateurConnecte();
        try {
            Achat achat = achatService.creerAchat(client, evenement, categorieSelectionnee, nombreTicketsAcheter);
            boolean paiementReussi = achatService.traiterPaiement(achat);

            if (paiementReussi) {
                achatReussi = true;
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Achat confirmé avec succès. Vos tickets sont disponibles dans votre profil."));
                notificationController.success("Achat confirmé", "Vos billets sont disponibles dans Mon espace billets.");
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Paiement refusé", "Impossible de finaliser l'achat."));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur d'achat", e.getMessage()));
        }
    }

    public Double getPrixTotal() {
        if (evenement == null || nombreTicketsAcheter == null) {
            return 0.0;
        }

        TicketCategorie categorie = getCategorieSelectionnee();
        if (categorie != null && categorie.getPrix() != null) {
            return categorie.getPrix() * nombreTicketsAcheter;
        }

        if (evenement.getPrixTicket() != null) {
            return evenement.getPrixTicket() * nombreTicketsAcheter;
        }
        return 0.0;
    }

    public TicketCategorie getCategorieSelectionnee() {
        if (categorieSelectionneeId == null || categoriesSelectionnables == null) {
            return null;
        }
        for (TicketCategorie categorie : categoriesSelectionnables) {
            if (categorieSelectionneeId.equals(categorie.getId())) {
                return categorie;
            }
        }
        return null;
    }

    public String getPrixAffichage(Evenement evenement) {
        if (evenement == null) {
            return "0 XOF";
        }
        Double prix = evenement.getPrixAffichage();
        if (prix == null) {
            prix = 0.0;
        }
        return String.format("%,.0f XOF", prix).replace(',', ' ');
    }

    public String prixAffichage(Evenement evenement) {
        return getPrixAffichage(evenement);
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Evenement getEvenement() {
        return evenement;
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
    }

    public List<TicketCategorie> getCategoriesSelectionnables() {
        return categoriesSelectionnables;
    }

    public void setCategoriesSelectionnables(List<TicketCategorie> categoriesSelectionnables) {
        this.categoriesSelectionnables = categoriesSelectionnables;
    }

    public Long getCategorieSelectionneeId() {
        return categorieSelectionneeId;
    }

    public void setCategorieSelectionneeId(Long categorieSelectionneeId) {
        this.categorieSelectionneeId = categorieSelectionneeId;
    }

    public Integer getNombreTicketsAcheter() {
        return nombreTicketsAcheter;
    }

    public void setNombreTicketsAcheter(Integer nombreTicketsAcheter) {
        this.nombreTicketsAcheter = nombreTicketsAcheter;
    }

    public boolean isAchatReussi() {
        return achatReussi;
    }

    public void setAchatReussi(boolean achatReussi) {
        this.achatReussi = achatReussi;
    }
}
