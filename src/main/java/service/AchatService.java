package service;

import dao.AchatDao;
import dao.TicketDao;
import dao.EvenementDao;
import entities.Achat;
import entities.Ticket;
import entities.Client;
import entities.Evenement;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.ArrayList;

/**
 * Service métier pour la gestion des achats
 */
@Stateless
@Transactional
public class AchatService {

    @EJB
    private AchatDao achatDao;

@EJB
    private TicketDao ticketDao;

    @EJB
    private EvenementDao evenementDao;

    @EJB
    private TicketService ticketService;

    /**
     * Crée un achat de tickets
     */
    public Achat creerAchat(Client client, Evenement evenement, Integer nombreTickets) {
        // Validation
        if (client == null) {
            throw new IllegalArgumentException("Client requis");
        }

        if (evenement == null) {
            throw new IllegalArgumentException("Événement requis");
        }

        if (nombreTickets <= 0) {
            throw new IllegalArgumentException("Le nombre de tickets doit être positif");
        }

        // Vérifier la disponibilité des tickets
        Long ticketsDisponibles = ticketDao.compterTicketsDisponibles(evenement.getId());
        if (ticketsDisponibles < nombreTickets) {
            throw new IllegalStateException("Pas assez de tickets disponibles");
        }

        // Créer l'achat
        Achat achat = new Achat();
        achat.setClient(client);
        achat.setEvenement(evenement);
        achat.setNombreTickets(nombreTickets);
        achat.calculerMontantTotal();

        achatDao.creer(achat);

        // Réserver les tickets
        reserverTicketsPourAchat(achat);

        return achat;
    }

    /**
     * Confirme le paiement d'un achat
     */
    public void confirmerPaiement(Long achatId) {
        Achat achat = achatDao.trouverParId(achatId);
        if (achat == null) {
            throw new IllegalArgumentException("Achat non trouvé");
        }

        if (achat.getStatutPaiement() != Achat.StatutPaiement.EN_ATTENTE) {
            throw new IllegalStateException("Cet achat ne peut pas être confirmé");
        }

        achat.confirmerPaiement();
        achatDao.mettreAJour(achat);
        
        // Persister les tickets
        for (Ticket ticket : achat.getTickets()) {
            ticketDao.mettreAJour(ticket);
        }

        // Mettre à jour les compteurs de l'événement
        ticketService.mettreAJourCompteursTickets(achat.getEvenement().getId());
    }

    /**
     * Annule un achat
     */
    public void annulerAchat(Long achatId) {
        Achat achat = achatDao.trouverParId(achatId);
        if (achat == null) {
            throw new IllegalArgumentException("Achat non trouvé");
        }

        if (achat.getStatutPaiement() != Achat.StatutPaiement.EN_ATTENTE) {
            throw new IllegalStateException("Cet achat ne peut pas être annulé");
        }

        achat.annuler();
        achatDao.mettreAJour(achat);

        // Mettre à jour les compteurs de l'événement
        ticketService.mettreAJourCompteursTickets(achat.getEvenement().getId());
    }

    /**
     * Récupère les achats d'un client
     */
    public List<Achat> trouverAchatsParClient(Client client) {
        return achatDao.trouverParClient(client);
    }

    /**
     * Récupère les achats pour un événement
     */
    public List<Achat> trouverAchatsParEvenement(Evenement evenement) {
        return achatDao.trouverParEvenement(evenement);
    }

    /**
     * Récupère les achats en attente de paiement
     */
    public List<Achat> trouverAchatsEnAttente() {
        return achatDao.trouverAchatsEnAttente();
    }

    /**
     * Calcule le revenu total d'un événement
     */
    public Double calculerRevenuTotal(Long evenementId) {
        return achatDao.calculerRevenuTotal(evenementId);
    }

    /**
     * Réserve des tickets pour un achat
     */
    private void reserverTicketsPourAchat(Achat achat) {
        // Trouver les tickets disponibles pour cet événement
        List<Ticket> ticketsDisponibles = ticketDao.trouverTicketsDisponibles(achat.getEvenement());
        
        if (ticketsDisponibles.size() < achat.getNombreTickets()) {
            throw new IllegalStateException("Pas assez de tickets disponibles");
        }
        
        // Réserver les premiers tickets disponibles
        for (int i = 0; i < achat.getNombreTickets(); i++) {
            Ticket ticket = ticketsDisponibles.get(i);
            ticket.reserverPourAchat(achat);
            achat.getTickets().add(ticket);
        }
    }

    /**
     * Traite le paiement (simulation)
     */
    public boolean traiterPaiement(Achat achat) {
        // Simulation de traitement de paiement
        // Dans un vrai système, on intégrerait une API de paiement

        if (achat.getMontantTotal() > 0) {
            // Simuler un paiement réussi
            confirmerPaiement(achat.getId());
            return true;
        }

        return false;
    }
}