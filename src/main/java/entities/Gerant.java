/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entities;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
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
@Table(name = "gerant")
@PrimaryKeyJoinColumn(name = "personne_id")
public class Gerant extends Personne {
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gerant gerant = (Gerant) o;
        return Objects.equals(getId(), gerant.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
