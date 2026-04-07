package entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Entité Organisateur
 * Peut être Personne Physique ou Personne Morale
 */
@Entity
@Getter
@Setter
public class Organisateur extends Personne {
    
    @OneToMany(mappedBy = "employeur", cascade = CascadeType.ALL)
    private List<Employe> employes = new ArrayList<>();
    
    // Champs pour distinction Personne Physique vs Morale
    @Column(nullable = true, length = 50)
    private String typeOrganisation; // "PHYSIQUE" ou "MORALE"
    
    // Champs spécifiques Personne Morale
    @Column(nullable = true, length = 200)
    private String nomEntreprise;
    
    @Column(nullable = true, length = 20)
    private String numeroSIRET;
    
    @Column(nullable = true, length = 100)
    private String secteurActivite;
    
    @Column(nullable = true, length = 100)
    private String siteWeb;
    
    @Column(nullable = true, length = 300)
    private String adresseSiege;
    
    @Column(nullable = true, length = 100)
    private String prenomRepresentant;
    
    @Column(nullable = true, length = 100)
    private String nomRepresentant;
    
    /**
     * Retourne true si c'est une personne morale
     */
    public boolean isPersonneMorale() {
        return "MORALE".equalsIgnoreCase(typeOrganisation);
    }
    
    /**
     * Retourne true si c'est une personne physique
     */
    public boolean isPersonnePhysique() {
        return "PHYSIQUE".equalsIgnoreCase(typeOrganisation);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Organisateur that = (Organisateur) o;
        return Objects.equals(getId(), that.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
