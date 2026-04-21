package dao;

import entities.Evenement;
import entities.Organisateur;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * DAO pour la gestion des événements
 */
@Stateless
public class EvenementDao {

    @PersistenceContext(unitName = "EventPU")
    private EntityManager em;

    /**
     * Crée un nouvel événement
     */
    public void creer(Evenement evenement) {
        em.persist(evenement);
    }

    /**
     * Synchronise le contexte de persistance avec la base de données
     */
    public void flush() {
        em.flush();
    }

    /**
     * Met à jour un événement existant
     */
    public Evenement mettreAJour(Evenement evenement) {
        return em.merge(evenement);
    }

    /**
     * Supprime un événement
     */
    public void supprimer(Evenement evenement) {
        em.remove(em.merge(evenement));
    }

    /**
     * Trouve un événement par son ID
     */
    public Evenement trouverParId(Long id) {
        return em.find(Evenement.class, id);
    }

    /**
     * Trouve un événement avec ses catégories chargées
     */
    public Evenement trouverParIdAvecCategories(Long id) {
        try {
            TypedQuery<Evenement> query = em.createQuery(
                "SELECT DISTINCT e FROM Evenement e " +
                "LEFT JOIN FETCH e.categories " +
                "WHERE e.id = :id",
                Evenement.class
            );
            query.setParameter("id", id);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Evenement trouverParIdEtOrganisateurId(Long evenementId, Long organisateurId) {
        try {
            TypedQuery<Evenement> query = em.createQuery(
                "SELECT e FROM Evenement e WHERE e.id = :evenementId AND e.organisateur.id = :organisateurId",
                Evenement.class
            );
            query.setParameter("evenementId", evenementId);
            query.setParameter("organisateurId", organisateurId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Récupère tous les événements
     */
    public List<Evenement> trouverTous() {
        TypedQuery<Evenement> query = em.createQuery("SELECT e FROM Evenement e ORDER BY e.dateCreation DESC", Evenement.class);
        return query.getResultList();
    }

    /**
     * Récupère les événements d'un organisateur
     */
    public List<Evenement> trouverParOrganisateur(Organisateur organisateur) {
        TypedQuery<Evenement> query = em.createQuery(
            "SELECT e FROM Evenement e WHERE e.organisateur = :organisateur ORDER BY e.dateCreation DESC",
            Evenement.class
        );
        query.setParameter("organisateur", organisateur);
        return query.getResultList();
    }

    /**
     * Récupère les événements publiés
     */
    public List<Evenement> trouverEvenementsPublies() {
        TypedQuery<Evenement> query = em.createQuery(
            "SELECT e FROM Evenement e WHERE e.statut = :statut ORDER BY e.dateEvenement ASC",
            Evenement.class
        );
        query.setParameter("statut", Evenement.StatutEvenement.PUBLIE);
        return query.getResultList();
    }

    /**
     * Récupère les événements à venir
     */
    public List<Evenement> trouverEvenementsAVenir() {
        TypedQuery<Evenement> query = em.createQuery(
            "SELECT e FROM Evenement e WHERE e.statut = :statut AND e.dateEvenement > CURRENT_TIMESTAMP ORDER BY e.dateEvenement ASC",
            Evenement.class
        );
        query.setParameter("statut", Evenement.StatutEvenement.PUBLIE);
        return query.getResultList();
    }

    /**
     * Recherche des événements par titre ou description
     */
    public List<Evenement> rechercher(String terme) {
        TypedQuery<Evenement> query = em.createQuery(
            "SELECT e FROM Evenement e WHERE e.statut = :statut AND (LOWER(e.titre) LIKE LOWER(:terme) OR LOWER(e.description) LIKE LOWER(:terme)) ORDER BY e.dateEvenement ASC",
            Evenement.class
        );
        query.setParameter("statut", Evenement.StatutEvenement.PUBLIE);
        query.setParameter("terme", "%" + terme + "%");
        return query.getResultList();
    }

    /**
     * Met à jour le nombre de tickets vendus pour un événement
     */
    public void mettreAJourTicketsVendus(Long evenementId) {
        // Compter les tickets vendus
        Long ticketsVendus = em.createQuery(
            "SELECT COUNT(t) FROM Ticket t WHERE t.evenement.id = :evenementId AND t.statut = :statut",
            Long.class
        )
        .setParameter("evenementId", evenementId)
        .setParameter("statut", entities.Ticket.StatutTicket.VENDU)
        .getSingleResult();
        
        // Mettre à jour l'événement
        Evenement evenement = em.find(Evenement.class, evenementId);
        if (evenement != null) {
            evenement.setTicketsVendus(ticketsVendus != null ? ticketsVendus.intValue() : 0);
            em.merge(evenement);
        }
    }
}