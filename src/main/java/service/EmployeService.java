package service;

import entities.Employe;
import entities.Organisateur;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 * Service pour la gestion des employés
 */
@Stateless
public class EmployeService {

    @PersistenceContext(unitName = "EventPU")
    private EntityManager em;

    /**
     * Enregistre un nouvel employé
     */
    public void enregistrer(Employe employe) {
        em.persist(employe);
        em.flush();  // Synchronise avec la base de données immédiatement
    }

    /**
     * Mise à jour d'un employé
     */
    public void mettre_a_jour(Employe employe) {
        em.merge(employe);
        em.flush();  // Synchronise avec la base de données immédiatement
    }

    /**
     * Supprime un employé
     */
    public void supprimer(Employe employe) {
        Employe employeManaged = em.merge(employe);
        em.remove(employeManaged);
        em.flush();  // Synchronise avec la base de données immédiatement
    }

    /**
     * Récupère un employé par ID
     */
    public Employe trouverParId(Long id) {
        return em.find(Employe.class, id);
    }

    /**
     * Récupère tous les employés d'un organisateur
     */
    public List<Employe> trouverParEmployeur(Organisateur organisateur) {
        Query query = em.createQuery(
            "SELECT e FROM Employe e WHERE e.employeur.id = :employeurId ORDER BY e.nom, e.prenom"
        );
        query.setParameter("employeurId", organisateur.getId());
        return query.getResultList();
    }

    /**
     * Récupère tous les employés
     */
    public List<Employe> trouverTous() {
        Query query = em.createQuery("SELECT e FROM Employe e ORDER BY e.nom, e.prenom");
        return query.getResultList();
    }

    /**
     * Vérifie si un email existe
     */
    public boolean emailExiste(String email) {
        Query query = em.createQuery("SELECT COUNT(e) FROM Employe e WHERE e.email = :email");
        query.setParameter("email", email);
        return (Long) query.getSingleResult() > 0;
    }

    /**
     * Vérifie si un email existe sauf pour un employé donné
     */
    public boolean emailExisteSauf(String email, Long employeId) {
        Query query = em.createQuery(
            "SELECT COUNT(e) FROM Employe e WHERE e.email = :email AND e.id != :id"
        );
        query.setParameter("email", email);
        query.setParameter("id", employeId);
        return (Long) query.getSingleResult() > 0;
    }

    /**
     * Désactive un employé
     */
    public void desactiver(Employe employe) {
        employe.setActif(false);
        mettre_a_jour(employe);
    }

    /**
     * Réactive un employé
     */
    public void reactiver(Employe employe) {
        employe.setActif(true);
        mettre_a_jour(employe);
    }
}
