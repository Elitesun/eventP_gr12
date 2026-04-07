/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import dao.PersonneDao;
import entities.Personne;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 *
 * @author COMLAN
 */
@Stateless
public class PersonneService {

    @Inject
    private PersonneDao personneDao;
    
    @PersistenceContext(unitName = "EventPU")
    private EntityManager em;

    public void enregistrer(Personne p) {
        personneDao.enregistrer(p);
    }
    
    public Personne authentifier(String email, String mdp) {
        return personneDao.authentifier(email, mdp);
    }
    
    public boolean emailExiste(String email) {
        return personneDao.emailExiste(email);
    }
    
    public java.util.List<Personne> findAll() {
        return personneDao.findAll();
    }
    
    public java.util.List<Personne> findByRole(Personne.Role role) {
        return personneDao.findByRole(role);
    }
    
    /**
     * Trouve une personne par son ID
     * @param id L'identifiant de la personne
     * @return La personne trouvée ou null
     */
    public Personne trouverParId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID ne peut pas être null");
        }
        return em.find(Personne.class, id);
    }
    
    /**
     * Met à jour une personne existante
     * @param p La personne à mettre à jour
     * @return La personne mise à jour
     */
    public Personne mettreAJour(Personne p) {
        if (p == null || p.getId() == null) {
            throw new IllegalArgumentException("La personne et son ID ne peuvent pas être null");
        }
        return em.merge(p);
    }
    
    /**
     * Supprime une personne par son ID
     * @param id L'identifiant de la personne à supprimer
     */
    public void supprimer(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID ne peut pas être null");
        }
        Personne personne = em.find(Personne.class, id);
        if (personne != null) {
            em.remove(personne);
        }
    }
    
    /**
     * Supprime une personne
     * @param p La personne à supprimer
     */
    public void supprimer(Personne p) {
        if (p != null && p.getId() != null) {
            supprimer(p.getId());
        }
    }
    
    /**
     * Compte le nombre de personnes par rôle
     * @param role Le rôle à compter
     * @return Le nombre de personnes avec ce rôle
     */
    public long countByRole(Personne.Role role) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM Personne p WHERE p.role = :role", Long.class);
        query.setParameter("role", role);
        return query.getSingleResult();
    }
    
    /**
     * Trouve les utilisateurs récents (5 derniers inscrits)
     * @return Liste des 5 derniers utilisateurs inscrits
     */
    public List<Personne> findRecentUsers() {
        TypedQuery<Personne> query = em.createQuery(
            "SELECT p FROM Personne p WHERE p.role != :gerant ORDER BY p.dateInscription DESC", 
            Personne.class);
        query.setParameter("gerant", Personne.Role.GERANT);
        query.setMaxResults(5);
        return query.getResultList();
    }
    
    /**
     * Compte le total des utilisateurs (sans les gérants)
     * @return Le nombre total d'utilisateurs
     */
    public long countTotalUsers() {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(p) FROM Personne p WHERE p.role != :gerant", Long.class);
        query.setParameter("gerant", Personne.Role.GERANT);
        return query.getSingleResult();
    }
    
    /**
     * Trouve tous les utilisateurs sauf les gérants
     * @return Liste des utilisateurs (organisateurs, clients, employés)
     */
    public List<Personne> findAllUsers() {
        TypedQuery<Personne> query = em.createQuery(
            "SELECT p FROM Personne p WHERE p.role != :gerant ORDER BY p.dateInscription DESC", 
            Personne.class);
        query.setParameter("gerant", Personne.Role.GERANT);
        return query.getResultList();
    }
    
    /**
     * Sauvegarde une personne (create ou update)
     * @param p La personne à sauvegarder
     * @return La personne sauvegardée
     */
    public Personne save(Personne p) {
        if (p == null) {
            throw new IllegalArgumentException("La personne ne peut pas être null");
        }
        
        if (p.getId() == null) {
            // Nouvelle personne
            enregistrer(p);
            return p;
        } else {
            // Mise à jour
            return mettreAJour(p);
        }
    }
    
    /**
     * Supprime une personne par son ID (alias pour compatibilité)
     * @param id L'identifiant de la personne à supprimer
     */
    public void delete(Long id) {
        supprimer(id);
    }
    
    /**
     * Trouve les employés d'un organisateur spécifique
     * TODO: Implémenter quand la relation employeur_id sera ajoutée à l'entité Employe
     * @param organisateurId L'ID de l'organisateur
     * @return Liste des employés de cet organisateur
     */
    public List<Personne> findEmployesByOrganisateur(Long organisateurId) {
        // TODO: Implémenter la requête avec employeur_id
        // TypedQuery<Personne> query = em.createQuery(
        //     "SELECT e FROM Employe e WHERE e.employeurId = :orgId", Personne.class);
        // query.setParameter("orgId", organisateurId);
        // return query.getResultList();
        
        // Pour l'instant, retourne une liste vide
        return List.of();
    }
    
    /**
     * Trouve les clients liés à un organisateur spécifique
     * TODO: Implémenter quand la relation organisateur-client sera définie
     * @param organisateurId L'ID de l'organisateur
     * @return Liste des clients de cet organisateur
     */
    public List<Personne> findClientsByOrganisateur(Long organisateurId) {
        // TODO: Implémenter la logique de liaison client-organisateur
        // (via les événements ou une relation directe)
        
        // Pour l'instant, retourne une liste vide
        return List.of();
    }
}