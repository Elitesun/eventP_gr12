package dao;

import entities.Personne;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import utils.PasswordUtils;

/**
 *
 * @author COMLAN
 */
@Stateless
public class PersonneDaoImpl implements PersonneDao {

    @PersistenceContext(unitName = "EventPU")
    private EntityManager em;

    @Override
    public void enregistrer(Personne p) {
        em.persist(p);
         em.flush();
    }
    
    @Override
    public Personne authentifier(String email, String mdp) {
        try {
            // Trouver l'utilisateur par email
            TypedQuery<Personne> query = em.createQuery(
                "SELECT p FROM Personne p WHERE p.email = :email", Personne.class);
            query.setParameter("email", email);
            Personne personne = query.getSingleResult();
            
            // Vérifier le mot de passe avec hachage
            if (personne != null && PasswordUtils.verifyPassword(mdp, personne.getMotDePasse())) {
                return personne;
            }
            return null; // Mot de passe incorrect
        } catch (NoResultException e) {
            return null; // Utilisateur non trouvé
        }
    }
    
    @Override
    public boolean emailExiste(String email) {
        try {
            Long count = em.createQuery(
                "SELECT COUNT(p) FROM Personne p WHERE p.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public java.util.List<Personne> findAll() {
        TypedQuery<Personne> query = em.createQuery("SELECT p FROM Personne p", Personne.class);
        return query.getResultList();
    }
    
    @Override
    public java.util.List<Personne> findByRole(Personne.Role role) {
        TypedQuery<Personne> query = em.createQuery(
            "SELECT p FROM Personne p WHERE p.role = :role", Personne.class);
        query.setParameter("role", role);
        return query.getResultList();
    }
}