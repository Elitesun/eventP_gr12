package controller;

import entities.Evenement;
import entities.Achat;
import entities.Client;
import service.EvenementService;
import service.AchatService;
import service.TicketService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Contrôleur pour la gestion des événements côté client
 */
@Named
@ViewScoped
public class ClientEvenementController implements Serializable {

    @Inject
    private EvenementService evenementService;

    @Inject
    private AchatService achatService;

    @Inject
    private TicketService ticketService;

    @Inject
    private AuthController authController;
    
    @Inject
    private ClientTicketController clientTicketController;

    // Propriétés
    private List<Evenement> evenementsPublies;
    private Evenement evenementSelectionne;
    private List<Achat> achatsClient;
    private Integer nombreTicketsAcheter = 1;
    private String message;
    private String termeRecherche;
    private boolean achatReussi = false;

    @PostConstruct
    public void init() {
        chargerEvenementsPublies();
        chargerAchatsClient();
    }

    /**
     * Charge les événements publiés
     */
    public void chargerEvenementsPublies() {
        try {
            evenementsPublies = evenementService.trouverEvenementsPublies();
        } catch (Exception e) {
            message = "Erreur lors du chargement des événements: " + e.getMessage();
        }
    }

    /**
     * Charge les achats du client connecté
     */
    public void chargerAchatsClient() {
        try {
            Client client = (Client) authController.getUtilisateurConnecte();
            if (client != null) {
                achatsClient = achatService.trouverAchatsParClient(client);
        } else {
                achatsClient = java.util.Collections.emptyList();
            }
        } catch (Exception e) {
            achatsClient = java.util.Collections.emptyList(); 
            message = "Erreur lors du chargement des achats: " + e.getMessage();
        }
    }

    /**
     * Recherche des événements
     */
    public void rechercherEvenements() {
        try {
            if (termeRecherche != null && !termeRecherche.trim().isEmpty()) {
                evenementsPublies = evenementService.rechercherEvenements(termeRecherche.trim());
            } else {
                chargerEvenementsPublies();
            }
        } catch (Exception e) {
            message = "Erreur lors de la recherche: " + e.getMessage();
        }
    }

    /**
     * Sélectionne un événement pour l'achat
     */
    public void selectionnerEvenement(Evenement evenement) {
        this.evenementSelectionne = evenement;
        this.nombreTicketsAcheter = 1;
        message = null;
    }

    /**
     * Achète des tickets pour un événement
     */
    public void acheterTickets() {
        achatReussi = false;
        try {
            Client client = (Client) authController.getUtilisateurConnecte();
            if (client == null) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erreur d'authentification", 
                    "Vous devez être connecté pour acheter des tickets"));
                return;
            }

            if (evenementSelectionne == null) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erreur de sélection", 
                    "Aucun événement sélectionné"));
                return;
            }

            if (nombreTicketsAcheter == null || nombreTicketsAcheter <= 0) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Nombre de tickets invalide", 
                    "Le nombre de tickets doit être positif"));
                return;
            }

            // Vérifier la disponibilité
            if (!ticketService.ticketsDisponibles(evenementSelectionne.getId())) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Tickets indisponibles", 
                    "Désolé, il n'y a plus de tickets disponibles pour cet événement"));
                return;
            }

            // Créer l'achat
            Achat achat = achatService.creerAchat(client, evenementSelectionne, nombreTicketsAcheter);

            // Traiter le paiement (simulation)
            boolean paiementReussi = achatService.traiterPaiement(achat);

            if (paiementReussi) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Succès", 
                    "Achat effectué avec succès ! Vos tickets sont maintenant disponibles dans votre profil."));
                chargerAchatsClient();
                chargerEvenementsPublies(); // Rafraîchir les compteurs
                // Rafraîchir les tickets du client pour qu'ils apparaissent immédiatement
                if (clientTicketController != null) {
                    clientTicketController.chargerTicketsClient();
                }
                evenementSelectionne = null;
                nombreTicketsAcheter = 1;
                achatReussi = true;
            } else {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erreur de paiement", 
                    "Erreur lors du paiement. Veuillez réessayer."));
            }

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erreur d'achat", 
                "Erreur lors de l'achat: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * Vérifie si des tickets sont disponibles pour un événement
     */
    public boolean ticketsDisponibles(Evenement evenement) {
        return ticketService.ticketsDisponibles(evenement.getId());
    }

    /**
     * Calcule le prix total pour l'achat en cours
     */
    public Double getPrixTotal() {
        if (evenementSelectionne != null && nombreTicketsAcheter != null) {
            return evenementSelectionne.getPrixTicket() * nombreTicketsAcheter;
        }
        return 0.0;
    }

    // Getters et Setters
    public List<Evenement> getEvenementsPublies() {
        return evenementsPublies;
    }

    public void setEvenementsPublies(List<Evenement> evenementsPublies) {
        this.evenementsPublies = evenementsPublies;
    }

    public Evenement getEvenementSelectionne() {
        return evenementSelectionne;
    }

    public void setEvenementSelectionne(Evenement evenementSelectionne) {
        this.evenementSelectionne = evenementSelectionne;
    }

    public List<Achat> getAchatsClient() {
        return achatsClient;
    }

    public void setAchatsClient(List<Achat> achatsClient) {
        this.achatsClient = achatsClient;
    }

    public Integer getNombreTicketsAcheter() {
        return nombreTicketsAcheter;
    }

    public void setNombreTicketsAcheter(Integer nombreTicketsAcheter) {
        this.nombreTicketsAcheter = nombreTicketsAcheter;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTermeRecherche() {
        return termeRecherche;
    }

    public void setTermeRecherche(String termeRecherche) {
        this.termeRecherche = termeRecherche;
    }

    public boolean isAchatReussi() {
        return achatReussi;
    }

    public void setAchatReussi(boolean achatReussi) {
        this.achatReussi = achatReussi;
    }
}