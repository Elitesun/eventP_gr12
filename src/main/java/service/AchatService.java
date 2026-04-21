package service;

import dao.AchatDao;
import dao.TicketDao;
import dao.TicketCategorieDao;
import entities.Achat;
import entities.Ticket;
import entities.Client;
import entities.Evenement;
import entities.TicketCategorie;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.transaction.Transactional;
import java.util.List;

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
    private TicketCategorieDao ticketCategorieDao;

    @EJB
    private TicketService ticketService;

    /**
     * Crée un achat de tickets
     */
    public Achat creerAchat(Client client, Evenement evenement, Integer nombreTickets) {
        return creerAchat(client, evenement, null, nombreTickets);
    }

    /**
     * Crée un achat de tickets sur une catégorie précise
     */
    public Achat creerAchat(Client client, Evenement evenement, TicketCategorie categorie, Integer nombreTickets) {
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

        Long ticketsDisponibles;
        if (categorie != null) {
            TicketCategorie managedCategorie = ticketCategorieDao.trouverParId(categorie.getId());
            if (managedCategorie == null) {
                throw new IllegalArgumentException("Catégorie de ticket introuvable");
            }
            if (!managedCategorie.getEvenement().getId().equals(evenement.getId())) {
                throw new IllegalArgumentException("Cette catégorie n'appartient pas à l'événement sélectionné");
            }
            ticketsDisponibles = ticketDao.compterTicketsDisponiblesParCategorie(managedCategorie.getId());
            categorie = managedCategorie;
        } else {
            ticketsDisponibles = ticketDao.compterTicketsDisponibles(evenement.getId());
        }

        if (ticketsDisponibles < nombreTickets) {
            throw new IllegalStateException("Pas assez de tickets disponibles");
        }

        // Créer l'achat
        Achat achat = new Achat();
        achat.setClient(client);
        achat.setEvenement(evenement);
        achat.setTicketCategorie(categorie);
        achat.setNombreTickets(nombreTickets);
        achat.calculerMontantTotal();

        achatDao.creer(achat);

        // Réserver les tickets
        reserverTicketsPourAchat(achat, categorie);

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

        // Mettre à jour les compteurs de la catégorie, si achat par tier
        if (achat.getTicketCategorie() != null) {
            TicketCategorie categorie = ticketCategorieDao.trouverParId(achat.getTicketCategorie().getId());
            if (categorie != null) {
                Long vendus = ticketDao.compterTicketsVendusParCategorie(categorie.getId());
                Long disponibles = ticketDao.compterTicketsDisponiblesParCategorie(categorie.getId());
                categorie.setQuantiteVendue(vendus.intValue());
                categorie.setQuantiteDisponible(disponibles.intValue());
                ticketCategorieDao.mettreAJour(categorie);
            }
        }
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
        reserverTicketsPourAchat(achat, null);
    }

    /**
     * Réserve des tickets pour un achat, éventuellement sur une catégorie donnée
     */
    private void reserverTicketsPourAchat(Achat achat, TicketCategorie categorie) {
        // Trouver les tickets disponibles pour cet événement
        List<Ticket> ticketsDisponibles;
        if (categorie != null) {
            ticketsDisponibles = ticketDao.trouverTicketsDisponiblesParCategorie(categorie, achat.getNombreTickets());
        } else {
            ticketsDisponibles = ticketDao.trouverTicketsDisponibles(achat.getEvenement());
        }
        
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