package dao;

import entities.Achat;
import entities.Client;
import entities.Evenement;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * DAO pour la gestion des achats
 */
@Stateless
public class AchatDao {

    @PersistenceContext(unitName = "EventPU")
    private EntityManager em;

    /**
     * Crée un nouvel achat
     */
    public void creer(Achat achat) {
        em.persist(achat);
    }

    /**
     * Met à jour un achat existant
     */
    public Achat mettreAJour(Achat achat) {
        return em.merge(achat);
    }

    /**
     * Supprime un achat
     */
    public void supprimer(Achat achat) {
        em.remove(em.merge(achat));
    }

    /**
     * Trouve un achat par son ID
     */
    public Achat trouverParId(Long id) {
        return em.find(Achat.class, id);
    }

    /**
     * Récupère tous les achats d'un client
     */
    public List<Achat> trouverParClient(Client client) {
        TypedQuery<Achat> query = em.createQuery(
            "SELECT a FROM Achat a WHERE a.client = :client ORDER BY a.dateAchat DESC",
            Achat.class
        );
        query.setParameter("client", client);
        return query.getResultList();
    }

    /**
     * Récupère tous les achats pour un événement
     */
    public List<Achat> trouverParEvenement(Evenement evenement) {
        TypedQuery<Achat> query = em.createQuery(
            "SELECT a FROM Achat a WHERE a.evenement = :evenement ORDER BY a.dateAchat DESC",
            Achat.class
        );
        query.setParameter("evenement", evenement);
        return query.getResultList();
    }

    /**
     * Récupère les achats en attente de paiement
     */
    public List<Achat> trouverAchatsEnAttente() {
        TypedQuery<Achat> query = em.createQuery(
            "SELECT a FROM Achat a WHERE a.statutPaiement = :statut ORDER BY a.dateAchat DESC",
            Achat.class
        );
        query.setParameter("statut", entities.Achat.StatutPaiement.EN_ATTENTE);
        return query.getResultList();
    }

    /**
     * Récupère les achats payés
     */
    public List<Achat> trouverAchatsPayes() {
        TypedQuery<Achat> query = em.createQuery(
            "SELECT a FROM Achat a WHERE a.statutPaiement = :statut ORDER BY a.dateAchat DESC",
            Achat.class
        );
        query.setParameter("statut", entities.Achat.StatutPaiement.PAYE);
        return query.getResultList();
    }

    /**
     * Calcule le revenu total d'un événement
     */
    public Double calculerRevenuTotal(Long evenementId) {
        TypedQuery<Double> query = em.createQuery(
            "SELECT COALESCE(SUM(a.montantTotal), 0.0) FROM Achat a WHERE a.evenement.id = :evenementId AND a.statutPaiement = :statut",
            Double.class
        );
        query.setParameter("evenementId", evenementId);
        query.setParameter("statut", entities.Achat.StatutPaiement.PAYE);
        return query.getSingleResult();
    }

    /**
     * Compte le nombre total d'achats pour un événement
     */
    public Long compterAchatsParEvenement(Long evenementId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM Achat a WHERE a.evenement.id = :evenementId AND a.statutPaiement = :statut",
            Long.class
        );
        query.setParameter("evenementId", evenementId);
        query.setParameter("statut", entities.Achat.StatutPaiement.PAYE);
        return query.getSingleResult();
    }
}