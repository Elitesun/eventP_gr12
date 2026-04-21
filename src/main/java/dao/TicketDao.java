package dao;

import entities.Ticket;
import entities.Evenement;
import entities.Client;
import entities.TicketCategorie;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * DAO pour la gestion des tickets
 */
@Stateless
public class TicketDao {

    @PersistenceContext(unitName = "EventPU")
    private EntityManager em;

    /**
     * Crée un nouveau ticket
     */
    public void creer(Ticket ticket) {
        em.persist(ticket);
    }

    /**
     * Met à jour un ticket existant
     */
    public Ticket mettreAJour(Ticket ticket) {
        return em.merge(ticket);
    }

    /**
     * Supprime un ticket
     */
    public void supprimer(Ticket ticket) {
        em.remove(em.merge(ticket));
    }

    /**
     * Trouve un ticket par son ID
     */
    public Ticket trouverParId(Long id) {
        return em.find(Ticket.class, id);
    }

    /**
     * Trouve un ticket par son code QR avec chargement des relations (JOIN FETCH)
     * Nécessaire pour éviter une LazyInitializationException lors du rendu JSF
     */
    public Ticket trouverParCodeQr(String codeQr) {
        TypedQuery<Ticket> query = em.createQuery(
            "SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.evenement " +
            "LEFT JOIN FETCH t.client " +
            "WHERE t.codeQr = :codeQr",
            Ticket.class
        );
        query.setParameter("codeQr", codeQr);
        List<Ticket> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Récupère tous les tickets d'un événement
     */
    public List<Ticket> trouverParEvenement(Evenement evenement) {
        TypedQuery<Ticket> query = em.createQuery(
            "SELECT t FROM Ticket t WHERE t.evenement = :evenement ORDER BY t.id",
            Ticket.class
        );
        query.setParameter("evenement", evenement);
        return query.getResultList();
    }

    /**
     * Récupère les tickets disponibles d'un événement
     */
    public List<Ticket> trouverTicketsDisponibles(Evenement evenement) {
        TypedQuery<Ticket> query = em.createQuery(
            "SELECT t FROM Ticket t WHERE t.evenement = :evenement AND t.statut = :statut ORDER BY t.id",
            Ticket.class
        );
        query.setParameter("evenement", evenement);
        query.setParameter("statut", entities.Ticket.StatutTicket.DISPONIBLE);
        return query.getResultList();
    }

    /**
     * Récupère les tickets disponibles d'une catégorie donnée (avec limite)
     */
    public List<Ticket> trouverTicketsDisponiblesParCategorie(TicketCategorie categorie, int limite) {
        TypedQuery<Ticket> query = em.createQuery(
            "SELECT t FROM Ticket t WHERE t.categorie = :categorie AND t.statut = :statut ORDER BY t.id",
            Ticket.class
        );
        query.setParameter("categorie", categorie);
        query.setParameter("statut", entities.Ticket.StatutTicket.DISPONIBLE);
        query.setMaxResults(Math.max(1, limite));
        return query.getResultList();
    }

    /**
     * Récupère les tickets d'un client
     */
    public List<Ticket> trouverParClient(Client client) {
        TypedQuery<Ticket> query = em.createQuery(
            "SELECT t FROM Ticket t WHERE t.client = :client ORDER BY t.dateAchat DESC",
            Ticket.class
        );
        query.setParameter("client", client);
        return query.getResultList();
    }

    /**
     * Compte le nombre de tickets disponibles pour un événement
     */
    public Long compterTicketsDisponibles(Long evenementId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(t) FROM Ticket t WHERE t.evenement.id = :evenementId AND t.statut = :statut",
            Long.class
        );
        query.setParameter("evenementId", evenementId);
        query.setParameter("statut", entities.Ticket.StatutTicket.DISPONIBLE);
        return query.getSingleResult();
    }

    /**
     * Compte le nombre de tickets vendus pour un événement
     */
    public Long compterTicketsVendus(Long evenementId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(t) FROM Ticket t WHERE t.evenement.id = :evenementId AND t.statut = :statut",
            Long.class
        );
        query.setParameter("evenementId", evenementId);
        query.setParameter("statut", entities.Ticket.StatutTicket.VENDU);
        return query.getSingleResult();
    }

    /**
     * Compte les tickets disponibles d'une catégorie
     */
    public Long compterTicketsDisponiblesParCategorie(Long categorieId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(t) FROM Ticket t WHERE t.categorie.id = :categorieId AND t.statut = :statut",
            Long.class
        );
        query.setParameter("categorieId", categorieId);
        query.setParameter("statut", entities.Ticket.StatutTicket.DISPONIBLE);
        return query.getSingleResult();
    }

    /**
     * Compte les tickets vendus d'une catégorie
     */
    public Long compterTicketsVendusParCategorie(Long categorieId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(t) FROM Ticket t WHERE t.categorie.id = :categorieId AND t.statut = :statut",
            Long.class
        );
        query.setParameter("categorieId", categorieId);
        query.setParameter("statut", entities.Ticket.StatutTicket.VENDU);
        return query.getSingleResult();
    }

    /**
     * Met à jour le statut de plusieurs tickets
     */
    public void mettreAJourStatut(List<Long> ticketIds, entities.Ticket.StatutTicket statut) {
        Query query = em.createQuery(
            "UPDATE Ticket t SET t.statut = :statut WHERE t.id IN :ticketIds"
        );
        query.setParameter("statut", statut);
        query.setParameter("ticketIds", ticketIds);
        query.executeUpdate();
    }
}