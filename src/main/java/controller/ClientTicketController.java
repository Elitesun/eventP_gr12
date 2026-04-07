package controller;

import entities.Ticket;
import entities.Client;
import service.TicketService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Contrôleur pour la gestion des tickets côté client
 */
@Named
@ViewScoped
public class ClientTicketController implements Serializable {

    @Inject
    private TicketService ticketService;

    @Inject
    private AuthController authController;

    // Propriétés
    private List<Ticket> ticketsClient;
    private String message;
    private String codeQrValidation;
    private boolean validationReussie;

    @PostConstruct
    public void init() {
        chargerTicketsClient();
    }

    /**
     * Charge les tickets du client connecté
     */
    public void chargerTicketsClient() {
        try {
            Client client = (Client) authController.getUtilisateurConnecte();
            if (client != null) {
                ticketsClient = ticketService.trouverTicketsParClient(client);
            }
        } catch (Exception e) {
            message = "Erreur lors du chargement des tickets: " + e.getMessage();
        }
    }

    /**
     * Valide un ticket avec son code QR
     */
    public void validerTicket() {
        try {
            if (codeQrValidation == null || codeQrValidation.trim().isEmpty()) {
                message = "Veuillez saisir un code QR";
                validationReussie = false;
                return;
            }

            validationReussie = ticketService.validerTicket(codeQrValidation.trim());

            if (validationReussie) {
                message = "Ticket validé avec succès !";
            } else {
                message = "Ticket invalide ou déjà utilisé";
            }

            // Recharger les tickets pour mettre à jour les statuts
            chargerTicketsClient();

        } catch (Exception e) {
            message = "Erreur lors de la validation: " + e.getMessage();
            validationReussie = false;
        }
    }

    /**
     * Réinitialise le formulaire de validation
     */
    public void reinitialiserValidation() {
        codeQrValidation = null;
        message = null;
        validationReussie = false;
    }

    /**
     * Vérifie si un ticket peut être utilisé
     */
    public boolean peutUtiliserTicket(Ticket ticket) {
        return ticket.getStatut() == Ticket.StatutTicket.VENDU;
    }

    /**
     * Obtient le libellé du statut d'un ticket
     */
    public String getLibelleStatut(Ticket ticket) {
        switch (ticket.getStatut()) {
            case DISPONIBLE:
                return "Disponible";
            case VENDU:
                return "Vendu";
            case UTILISE:
                return "Utilisé";
            case ANNULE:
                return "Annulé";
            default:
                return "Inconnu";
        }
    }

    /**
     * Obtient la classe CSS pour le statut d'un ticket
     */
    public String getClasseCssStatut(Ticket ticket) {
        switch (ticket.getStatut()) {
            case DISPONIBLE:
                return "badge-success";
            case VENDU:
                return "badge-info";
            case UTILISE:
                return "badge-secondary";
            case ANNULE:
                return "badge-danger";
            default:
                return "badge-light";
        }
    }

    // Getters et Setters
    public List<Ticket> getTicketsClient() {
        return ticketsClient;
    }

    public void setTicketsClient(List<Ticket> ticketsClient) {
        this.ticketsClient = ticketsClient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCodeQrValidation() {
        return codeQrValidation;
    }

    public void setCodeQrValidation(String codeQrValidation) {
        this.codeQrValidation = codeQrValidation;
    }

    public boolean isValidationReussie() {
        return validationReussie;
    }

    public void setValidationReussie(boolean validationReussie) {
        this.validationReussie = validationReussie;
    }
}