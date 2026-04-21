package dao;

import entities.Evenement;
import entities.TicketCategorie;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * DAO pour la gestion des catégories de tickets.
 */
@Stateless
public class TicketCategorieDao {

    @PersistenceContext(unitName = "EventPU")
    private EntityManager em;

    public void creer(TicketCategorie categorie) {
        em.persist(categorie);
    }

    public TicketCategorie mettreAJour(TicketCategorie categorie) {
        return em.merge(categorie);
    }

    public TicketCategorie trouverParId(Long id) {
        return em.find(TicketCategorie.class, id);
    }

    public List<TicketCategorie> trouverParEvenement(Evenement evenement) {
        TypedQuery<TicketCategorie> query = em.createQuery(
            "SELECT c FROM TicketCategorie c WHERE c.evenement = :evenement ORDER BY c.prix ASC",
            TicketCategorie.class
        );
        query.setParameter("evenement", evenement);
        return query.getResultList();
    }

    public List<TicketCategorie> trouverDisponiblesParEvenementId(Long evenementId) {
        TypedQuery<TicketCategorie> query = em.createQuery(
            "SELECT c FROM TicketCategorie c WHERE c.evenement.id = :evenementId AND c.quantiteDisponible > 0 ORDER BY c.prix ASC",
            TicketCategorie.class
        );
        query.setParameter("evenementId", evenementId);
        return query.getResultList();
    }

    public long compterTicketsVendusParCategorie(Long categorieId) {
        return em.createQuery(
            "SELECT COUNT(t) FROM Ticket t WHERE t.categorie.id = :categorieId AND t.statut = :statut",
            Long.class
        )
            .setParameter("categorieId", categorieId)
            .setParameter("statut", entities.Ticket.StatutTicket.VENDU)
            .getSingleResult();
    }
}