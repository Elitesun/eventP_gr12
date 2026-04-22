package service;

import dao.TicketDao;
import dao.EvenementDao;
import dao.TicketCategorieDao;
import entities.Ticket;
import entities.Evenement;
import entities.Client;
import entities.TicketCategorie;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Service métier pour la gestion des tickets
 */
@Stateless
@Transactional
public class TicketService {

    @EJB
    private TicketDao ticketDao;

    @EJB
    private EvenementDao evenementDao;

    @EJB
    private TicketCategorieDao ticketCategorieDao;

    /**
     * Trouve un ticket par son code QR
     */
    public Ticket trouverTicketParCodeQr(String codeQr) {
        return ticketDao.trouverParCodeQr(codeQr);
    }

    /**
     * Trouve un ticket par son identifiant.
     */
    public Ticket trouverTicketParId(Long ticketId) {
        if (ticketId == null || ticketId <= 0) {
            return null;
        }
        return ticketDao.trouverParId(ticketId);
    }

    /**
     * Valide un ticket (le marque comme utilisé)
     */
    public boolean validerTicket(String codeQr) {
        Ticket ticket = ticketDao.trouverParCodeQr(codeQr);
        if (ticket == null) {
            return false;
        }

        if (ticket.getStatut() != Ticket.StatutTicket.VENDU) {
            return false;
        }

        ticket.marquerCommeUtilise();
        ticketDao.mettreAJour(ticket);
        
        // Mettre à jour les compteurs de l'événement
        mettreAJourCompteursTickets(ticket.getEvenement().getId());
        
        return true;
    }

    /**
     * Récupère les tickets d'un client
     */
    public List<Ticket> trouverTicketsParClient(Client client) {
        return ticketDao.trouverParClient(client);
    }

    /**
     * Récupère les tickets disponibles d'un événement
     */
    public List<Ticket> trouverTicketsDisponibles(Evenement evenement) {
        return ticketDao.trouverTicketsDisponibles(evenement);
    }

    /**
     * Supprime un ticket quand il n'est pas encore vendu.
     */
    public void supprimerTicket(Long ticketId) {
        if (ticketId == null || ticketId <= 0) {
            throw new IllegalArgumentException("ID ticket invalide");
        }

        Ticket ticket = ticketDao.trouverParId(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket introuvable");
        }

        if (ticket.getStatut() == Ticket.StatutTicket.VENDU || ticket.getStatut() == Ticket.StatutTicket.UTILISE) {
            throw new IllegalStateException("Impossible de supprimer un ticket vendu ou utilisé");
        }

        Long evenementId = ticket.getEvenement() != null ? ticket.getEvenement().getId() : null;
        ticketDao.supprimer(ticket);

        if (evenementId != null) {
            mettreAJourCompteursTickets(evenementId);
        }
    }

    /**
     * Compte les tickets disponibles pour un événement
     */
    public Long compterTicketsDisponibles(Long evenementId) {
        return ticketDao.compterTicketsDisponibles(evenementId);
    }

    /**
     * Compte les tickets vendus pour un événement
     */
    public Long compterTicketsVendus(Long evenementId) {
        return ticketDao.compterTicketsVendus(evenementId);
    }

    /**
     * Vérifie si des tickets sont disponibles pour un événement
     */
    public boolean ticketsDisponibles(Long evenementId) {
        Long disponibles = ticketDao.compterTicketsDisponibles(evenementId);
        return disponibles > 0;
    }

    /**
     * Met à jour les compteurs de tickets pour un événement
     */
public void mettreAJourCompteursTickets(Long evenementId) {
        Evenement evt = evenementDao.trouverParId(evenementId);
        Long vendu = ticketDao.compterTicketsVendus(evenementId);
        evt.setTicketsVendus(vendu.intValue());
        evt.setTicketsDisponibles(evt.getNombreTicketsTotal() - evt.getTicketsVendus());
        evt.setDateModification(new java.util.Date());
        evenementDao.mettreAJour(evt);

        // Synchroniser les compteurs par catégorie de ticket
        List<TicketCategorie> categories = ticketCategorieDao.trouverParEvenement(evt);
        for (TicketCategorie categorie : categories) {
            Long vendusCategorie = ticketDao.compterTicketsVendusParCategorie(categorie.getId());
            Long disponiblesCategorie = ticketDao.compterTicketsDisponiblesParCategorie(categorie.getId());
            categorie.setQuantiteVendue(vendusCategorie.intValue());
            categorie.setQuantiteDisponible(disponiblesCategorie.intValue());
            ticketCategorieDao.mettreAJour(categorie);
        }
    }

    /**
     * Annule des tickets (les remet disponibles)
     */
    public void annulerTickets(List<Long> ticketIds) {
        ticketDao.mettreAJourStatut(ticketIds, Ticket.StatutTicket.DISPONIBLE);
    }

    /**
     * Génère un nouveau code QR pour un ticket
     */
    public String regenererCodeQr(Long ticketId) {
        Ticket ticket = ticketDao.trouverParId(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket non trouvé");
        }

        String nouveauCode = Ticket.genererCodeQr(ticket.getId(), ticket.getEvenement().getId());
        ticket.setCodeQr(nouveauCode);
        ticketDao.mettreAJour(ticket);

        return nouveauCode;
    }
}