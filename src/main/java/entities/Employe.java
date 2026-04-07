/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author COMLAN
 */
@Entity
@Getter
@Setter
public class Employe extends Personne {
    
    @ManyToOne
    @JoinColumn(name = "employeur_id", nullable = false)
    private Organisateur employeur;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employe employe = (Employe) o;
        return Objects.equals(getId(), employe.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
} 
