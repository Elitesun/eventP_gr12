package service;

import dao.EvenementDao;
import dao.TicketDao;
import entities.Evenement;
import entities.Organisateur;
import entities.Ticket;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Service métier pour la gestion des événements
 */
@Stateless
@Transactional
public class EvenementService {

    @EJB
    private EvenementDao evenementDao;

    @EJB
    private TicketDao ticketDao;

    /**
     * Crée un nouvel événement avec ses tickets
     */
    public Evenement creerEvenement(Evenement evenement) {
        // Validation métier
        if (evenement.getNombreTicketsTotal() <= 0) {
            throw new IllegalArgumentException("Le nombre de tickets doit être positif");
        }

        if (evenement.getPrixTicket() < 0) {
            throw new IllegalArgumentException("Le prix du ticket ne peut pas être négatif");
        }

        // Validate organisateur
        if (evenement.getOrganisateur() == null || evenement.getOrganisateur().getId() == null) {
            throw new IllegalArgumentException("Organisateur invalide pour création d'événement (ID null)");
        }

        System.out.println("[EvenementService] Creating event for organisateur.id=" + evenement.getOrganisateur().getId() + ", tickets=" + evenement.getNombreTicketsTotal());

        // Create event
        try {
            evenementDao.creer(evenement);
            evenementDao.flush(); // Garantir la génération de l'ID
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la persistance de l'événement: " + e.getMessage(), e);
        }

        // Generate tickets
        try {
            genererTicketsPourEvenement(evenement);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération des tickets: " + e.getMessage(), e);
        }

        return evenement;
    }

    /**
     * Met à jour un événement
     */
    public Evenement mettreAJourEvenement(Evenement evenement) {
        // Vérifier que l'événement peut être modifié
        if (!evenement.peutEtreModifie()) {
            throw new IllegalStateException("Cet événement ne peut plus être modifié");
        }

        // Si le nombre de tickets a changé, ajuster les tickets
        Evenement evenementExistant = evenementDao.trouverParId(evenement.getId());
        if (!evenement.getNombreTicketsTotal().equals(evenementExistant.getNombreTicketsTotal())) {
            ajusterNombreTickets(evenement, evenementExistant);
        }

        return evenementDao.mettreAJour(evenement);
    }

    /**
     * Publie un événement
     */
    public Evenement publierEvenement(Long evenementId) {
        Evenement evenement = evenementDao.trouverParId(evenementId);
        if (evenement == null) {
            throw new IllegalArgumentException("Événement non trouvé");
        }

        if (evenement.getStatut() != Evenement.StatutEvenement.BROUILLON) {
            throw new IllegalStateException("Seul un événement en brouillon peut être publié");
        }

        evenement.setStatut(Evenement.StatutEvenement.PUBLIE);
        return evenementDao.mettreAJour(evenement);
    }

    /**
     * Annule un événement
     */
    public Evenement annulerEvenement(Long evenementId) {
        Evenement evenement = evenementDao.trouverParId(evenementId);
        if (evenement == null) {
            throw new IllegalArgumentException("Événement non trouvé");
        }

        if (evenement.getStatut() == Evenement.StatutEvenement.TERMINE) {
            throw new IllegalStateException("Un événement terminé ne peut pas être annulé");
        }

        evenement.setStatut(Evenement.StatutEvenement.ANNULE);
        return evenementDao.mettreAJour(evenement);
    }

    /**
     * Supprime un événement
     */
    public void supprimerEvenement(Long evenementId) {
        Evenement evenement = evenementDao.trouverParId(evenementId);
        if (evenement == null) {
            throw new IllegalArgumentException("Événement non trouvé");
        }

        // Vérifier qu'il n'y a pas de tickets vendus
        Long ticketsVendus = ticketDao.compterTicketsVendus(evenementId);
        if (ticketsVendus > 0) {
            throw new IllegalStateException("Impossible de supprimer un événement avec des tickets vendus");
        }

        // Supprimer tous les tickets
        List<Ticket> tickets = ticketDao.trouverParEvenement(evenement);
        for (Ticket ticket : tickets) {
            ticketDao.supprimer(ticket);
        }

        // Supprimer l'événement
        evenementDao.supprimer(evenement);
    }

    /**
     * Récupère un événement par son ID
     */
    public Evenement trouverEvenementParId(Long id) {
        return evenementDao.trouverParId(id);
    }

    /**
     * Récupère tous les événements
     */
    public List<Evenement> trouverTousEvenements() {
        return evenementDao.trouverTous();
    }

    /**
     * Récupère les événements d'un organisateur
     */
    public List<Evenement> trouverEvenementsParOrganisateur(Organisateur organisateur) {
        return evenementDao.trouverParOrganisateur(organisateur);
    }

    /**
     * Récupère les événements publiés
     */
    public List<Evenement> trouverEvenementsPublies() {
        return evenementDao.trouverEvenementsPublies();
    }

    /**
     * Recherche des événements
     */
    public List<Evenement> rechercherEvenements(String terme) {
        return evenementDao.rechercher(terme);
    }

    /**
     * Génère les tickets pour un événement
     */
    private void genererTicketsPourEvenement(Evenement evenement) {
        for (int i = 1; i <= evenement.getNombreTicketsTotal(); i++) {
            Ticket ticket = new Ticket();
            ticket.setEvenement(evenement);
            ticket.setCodeQr(Ticket.genererCodeQr(Long.valueOf(i), evenement.getId()));
            ticketDao.creer(ticket);
        }
    }

    /**
     * Ajuste le nombre de tickets d'un événement
     */
    private void ajusterNombreTickets(Evenement evenementNouveau, Evenement evenementExistant) {
        int difference = evenementNouveau.getNombreTicketsTotal() - evenementExistant.getNombreTicketsTotal();

        if (difference > 0) {
            // Ajouter des tickets
            List<Ticket> ticketsExistants = ticketDao.trouverParEvenement(evenementExistant);
            int dernierNumero = ticketsExistants.size();

            for (int i = 1; i <= difference; i++) {
                Ticket ticket = new Ticket();
                ticket.setEvenement(evenementNouveau);
                ticket.setCodeQr(Ticket.genererCodeQr(Long.valueOf(dernierNumero + i), evenementNouveau.getId()));
                ticketDao.creer(ticket);
            }
        } else if (difference < 0) {
            // Supprimer des tickets (seulement les disponibles)
            List<Ticket> ticketsDisponibles = ticketDao.trouverTicketsDisponibles(evenementExistant);
            int nombreASupprimer = Math.abs(difference);

            if (ticketsDisponibles.size() < nombreASupprimer) {
                throw new IllegalStateException("Impossible de réduire le nombre de tickets : pas assez de tickets disponibles");
            }

            for (int i = 0; i < nombreASupprimer; i++) {
                ticketDao.supprimer(ticketsDisponibles.get(i));
            }
        }

        // Recalculer les compteurs
        evenementNouveau.recalculerTicketsDisponibles();
    }
}