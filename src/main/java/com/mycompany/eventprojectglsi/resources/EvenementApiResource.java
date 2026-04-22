package com.mycompany.eventprojectglsi.resources;

import controller.AuthController;
import entities.Evenement;
import entities.Organisateur;
import entities.Personne;
import entities.Ticket;
import entities.TicketCategorie;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import service.EvenementService;
import service.PersonneService;
import service.TicketService;
import com.mycompany.eventprojectglsi.resources.EventApiDTOs.*;

@Path("events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvenementApiResource {

    @EJB
    private EvenementService evenementService;

    @EJB
    private TicketService ticketService;

    @EJB
    private PersonneService personneService;

    @Inject
    private AuthController authController;

    @POST
    public Response createEvent(CreateEventRequest request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("Le corps de requête est obligatoire"))
                .build();
        }

        if (isBlank(request.titre) || isBlank(request.description) || isBlank(request.lieu) || isBlank(request.dateEvenement)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("titre, description, lieu et dateEvenement sont obligatoires"))
                .build();
        }

        if (request.standardPrix == null || request.standardPrix < 0
            || request.standardQuantite == null || request.standardQuantite <= 0
            || request.vipPrix == null || request.vipPrix < 0
            || request.vipQuantite == null || request.vipQuantite <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("Les catégories Standard/VIP doivent avoir un prix >= 0 et une quantité > 0"))
                .build();
        }

        Organisateur organisateur = resolveOrganisateur(request);
        if (organisateur == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiResponse.error("Authentification organisateur requise"))
                .build();
        }

        Date dateEvenement;
        try {
            dateEvenement = parseDate(request.dateEvenement);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        }

        Evenement evenement = new Evenement();
        evenement.setTitre(request.titre.trim());
        evenement.setDescription(request.description.trim());
        evenement.setLieu(request.lieu.trim());
        evenement.setDateEvenement(dateEvenement);
        evenement.setImageUrl(request.imageBase64);
        evenement.setStatut(Evenement.StatutEvenement.BROUILLON);
        evenement.setOrganisateur(organisateur);

        List<TicketCategorie> categories = new ArrayList<>();
        categories.add(buildCategory("Standard", request.standardPrix, request.standardQuantite));
        categories.add(buildCategory("VIP", request.vipPrix, request.vipQuantite));
        evenement.setCategories(categories);

        try {
            Evenement created = evenementService.creerEvenement(evenement);
            CreateEventResponse response = new CreateEventResponse();
            response.id = created.getId();
            response.titre = created.getTitre();
            response.statut = created.getStatut() != null ? created.getStatut().name() : null;
            response.prixTicket = created.getPrixTicket();
            response.nombreTicketsTotal = created.getNombreTicketsTotal();
            response.message = "Événement créé avec succès";
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("scan/{codeQr}")
    public Response scan(@PathParam("codeQr") String codeQr) {
        if (isBlank(codeQr)) {
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

        ScanEventResponse response = new ScanEventResponse();
        response.codeQr = ticket.getCodeQr();
        response.statutTicket = ticket.getStatut() != null ? ticket.getStatut().name() : null;
        response.peutEtreValide = ticket.getStatut() == Ticket.StatutTicket.VENDU;
        response.evenementId = ticket.getEvenement() != null ? ticket.getEvenement().getId() : null;
        response.evenementTitre = ticket.getEvenement() != null ? ticket.getEvenement().getTitre() : null;
        response.clientNom = ticket.getClient() != null ? ticket.getClient().getPrenom() + " " + ticket.getClient().getNom() : null;
        response.categorie = ticket.getCategorie() != null ? ticket.getCategorie().getNom() : null;
        return Response.ok(response).build();
    }

    @POST
    @Path("scan/{codeQr}/validate")
    @Consumes(MediaType.WILDCARD)
    public Response validateScan(@PathParam("codeQr") String codeQr) {
        if (isBlank(codeQr)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("Le code QR est obligatoire"))
                .build();
        }

        boolean success = ticketService.validerTicket(codeQr.trim());
        if (!success) {
            return Response.status(Response.Status.CONFLICT)
                .entity(ApiResponse.error("Ticket invalide, déjà utilisé ou non payé"))
                .build();
        }

        return Response.ok(ApiResponse.success("Ticket validé avec succès")).build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEvent(@PathParam("id") Long eventId, UpdateEventRequest request) {
        if (eventId == null || eventId <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("ID événement invalide"))
                .build();
        }

        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("Le corps de requête est obligatoire"))
                .build();
        }

        if (isBlank(request.titre) || isBlank(request.description) || isBlank(request.lieu) || isBlank(request.dateEvenement)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("titre, description, lieu et dateEvenement sont obligatoires"))
                .build();
        }

        if (request.standardPrix == null || request.standardPrix < 0
            || request.standardQuantite == null || request.standardQuantite <= 0
            || request.vipPrix == null || request.vipPrix < 0
            || request.vipQuantite == null || request.vipQuantite <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("Les catégories Standard/VIP doivent avoir un prix >= 0 et une quantité > 0"))
                .build();
        }

        Organisateur organisateur = resolveOrganisateur(request);
        if (organisateur == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiResponse.error("Authentification organisateur requise"))
                .build();
        }

        Date dateEvenement;
        try {
            dateEvenement = parseDate(request.dateEvenement);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        }

        Evenement evenement = new Evenement();
        evenement.setId(eventId);
        evenement.setTitre(request.titre.trim());
        evenement.setDescription(request.description.trim());
        evenement.setLieu(request.lieu.trim());
        evenement.setDateEvenement(dateEvenement);
        evenement.setImageUrl(request.imageBase64);
        evenement.setOrganisateur(organisateur);

        List<TicketCategorie> categories = new ArrayList<>();
        categories.add(buildCategory("Standard", request.standardPrix, request.standardQuantite));
        categories.add(buildCategory("VIP", request.vipPrix, request.vipQuantite));
        evenement.setCategories(categories);

        try {
            Evenement updated = evenementService.mettreAJourEvenement(organisateur.getId(), evenement);
            UpdateEventResponse response = new UpdateEventResponse();
            response.id = updated.getId();
            response.titre = updated.getTitre();
            response.statut = updated.getStatut() != null ? updated.getStatut().name() : null;
            response.prixTicket = updated.getPrixTicket();
            response.nombreTicketsTotal = updated.getNombreTicketsTotal();
            response.message = "Événement mis à jour avec succès";
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        }
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEvent(@PathParam("id") Long eventId,
                                @QueryParam("email") String email,
                                @QueryParam("password") String password) {
        if (eventId == null || eventId <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("ID événement invalide"))
                .build();
        }

        String requestEmail = email;
        String requestPassword = password;

        Organisateur organisateur = null;
        if (authController != null && authController.getUtilisateurConnecte() instanceof Organisateur) {
            organisateur = (Organisateur) authController.getUtilisateurConnecte();
        } else if (requestEmail != null && requestPassword != null) {
            Personne personne = personneService.authentifier(requestEmail, requestPassword);
            if (personne instanceof Organisateur) {
                organisateur = (Organisateur) personne;
            }
        }

        if (organisateur == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiResponse.error("Authentification organisateur requise"))
                .build();
        }

        try {
            evenementService.supprimerEvenement(organisateur.getId(), eventId);
            EventApiDTOs.DeleteEventResponse response = new EventApiDTOs.DeleteEventResponse();
            response.success = true;
            response.message = "Événement supprimé avec succès";
            response.eventId = eventId;
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        }
    }

    private Organisateur resolveOrganisateur(Object request) {
        if (authController != null && authController.getUtilisateurConnecte() instanceof Organisateur) {
            return (Organisateur) authController.getUtilisateurConnecte();
        }

        String email = null;
        String password = null;
        
        if (request instanceof CreateEventRequest) {
            CreateEventRequest req = (CreateEventRequest) request;
            email = req.email;
            password = req.password;
        } else if (request instanceof UpdateEventRequest) {
            UpdateEventRequest req = (UpdateEventRequest) request;
            email = req.email;
            password = req.password;
        }

        if (email == null || password == null) {
            return null;
        }

        Personne personne = personneService.authentifier(email, password);
        if (personne instanceof Organisateur) {
            return (Organisateur) personne;
        }
        return null;
    }

    private Date parseDate(String value) {
        try {
            LocalDateTime dt = LocalDateTime.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {
        }

        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime dt = LocalDateTime.parse(value.trim(), f);
            return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("dateEvenement doit être au format ISO (ex: 2026-04-28T19:30:00)");
    }

    private TicketCategorie buildCategory(String nom, Double prix, Integer quantite) {
        TicketCategorie c = new TicketCategorie();
        c.setNom(nom);
        c.setPrix(prix);
        c.setQuantiteTotale(quantite);
        c.setQuantiteVendue(0);
        c.setQuantiteDisponible(quantite);
        return c;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
