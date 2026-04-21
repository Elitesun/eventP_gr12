package com.mycompany.eventprojectglsi.resources;

import controller.AuthController;
import entities.Achat;
import entities.Client;
import entities.Evenement;
import entities.Personne;
import entities.Ticket;
import entities.TicketCategorie;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import service.AchatService;
import service.EvenementService;
import service.PersonneService;
import service.TicketService;

/**
 * API JSON minimale pour scan QR et achat de tickets.
 */
@Path("tickets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TicketApiResource {

    @EJB
    private TicketService ticketService;

    @EJB
    private AchatService achatService;

    @EJB
    private EvenementService evenementService;

    @EJB
    private PersonneService personneService;

    @Inject
    private AuthController authController;

    @GET
    @Path("scan/{codeQr}")
    public Response scanTicket(@PathParam("codeQr") String codeQr) {
        if (codeQr == null || codeQr.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("Le code QR est obligatoire"))
                .build();
        }

        Ticket ticket = ticketService.trouverTicketParCodeQr(codeQr.trim());
        if (ticket == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponse.error("Ticket introuvable"))
                .build();
        }

        TicketScanResult result = new TicketScanResult();
        result.codeQr = ticket.getCodeQr();
        result.statut = ticket.getStatut().name();
        result.valide = ticket.getStatut() == Ticket.StatutTicket.VENDU;
        result.evenementId = ticket.getEvenement() != null ? ticket.getEvenement().getId() : null;
        result.evenementTitre = ticket.getEvenement() != null ? ticket.getEvenement().getTitre() : null;
        result.clientNom = ticket.getClient() != null ? ticket.getClient().getPrenom() + " " + ticket.getClient().getNom() : null;
        result.categorie = ticket.getCategorie() != null ? ticket.getCategorie().getNom() : null;

        return Response.ok(result).build();
    }

    @POST
    @Path("scan/{codeQr}/validate")
    @Consumes(MediaType.WILDCARD)
    public Response validateTicket(@PathParam("codeQr") String codeQr) {
        if (codeQr == null || codeQr.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("Le code QR est obligatoire"))
                .build();
        }

        boolean valide = ticketService.validerTicket(codeQr.trim());
        if (!valide) {
            return Response.status(Response.Status.CONFLICT)
                .entity(ApiResponse.error("Ticket invalide, déjà utilisé ou non payé"))
                .build();
        }

        return Response.ok(ApiResponse.success("Ticket validé avec succès")).build();
    }

    @POST
    @Path("buy")
    public Response buyTickets(BuyTicketRequest request) {
        if (request == null || request.eventId == null || request.quantity == null || request.quantity <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("eventId et quantity (>0) sont obligatoires"))
                .build();
        }

        Client client = resolveClient(request);
        if (client == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiResponse.error("Authentification client requise"))
                .build();
        }

        Evenement evenement = evenementService.trouverEvenementAvecCategories(request.eventId);
        if (evenement == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponse.error("Événement introuvable"))
                .build();
        }

        TicketCategorie categorie = null;
        if (request.categorieId != null && evenement.getCategories() != null) {
            for (TicketCategorie c : evenement.getCategories()) {
                if (request.categorieId.equals(c.getId())) {
                    categorie = c;
                    break;
                }
            }
            if (categorie == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("La catégorie demandée est invalide pour cet événement"))
                    .build();
            }
        }

        try {
            Achat achat = achatService.creerAchat(client, evenement, categorie, request.quantity);
            boolean paiement = achatService.traiterPaiement(achat);

            if (!paiement) {
                return Response.status(Response.Status.PAYMENT_REQUIRED)
                    .entity(ApiResponse.error("Le paiement simulé a échoué"))
                    .build();
            }

            BuyTicketResponse response = new BuyTicketResponse();
            response.achatId = achat.getId();
            response.eventId = evenement.getId();
            response.quantity = achat.getNombreTickets();
            response.total = achat.getMontantTotal();
            response.categorie = categorie != null ? categorie.getNom() : "STANDARD";
            response.message = "Achat confirmé";
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        }
    }

    private Client resolveClient(BuyTicketRequest request) {
        if (authController != null && authController.getUtilisateurConnecte() instanceof Client) {
            return (Client) authController.getUtilisateurConnecte();
        }

        if (request.email == null || request.password == null) {
            return null;
        }

        Personne personne = personneService.authentifier(request.email, request.password);
        if (personne instanceof Client) {
            return (Client) personne;
        }
        return null;
    }

    public static class BuyTicketRequest {
        public Long eventId;
        public Long categorieId;
        public Integer quantity;
        public String email;
        public String password;
    }

    public static class BuyTicketResponse {
        public Long achatId;
        public Long eventId;
        public Integer quantity;
        public Double total;
        public String categorie;
        public String message;
    }

    public static class TicketScanResult {
        public String codeQr;
        public String statut;
        public boolean valide;
        public Long evenementId;
        public String evenementTitre;
        public String clientNom;
        public String categorie;
    }

    public static class ApiResponse {
        public boolean success;
        public String message;

        public static ApiResponse success(String message) {
            ApiResponse r = new ApiResponse();
            r.success = true;
            r.message = message;
            return r;
        }

        public static ApiResponse error(String message) {
            ApiResponse r = new ApiResponse();
            r.success = false;
            r.message = message;
            return r;
        }
    }
}