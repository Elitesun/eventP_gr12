/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entities;

import jakarta.persistence.Entity;
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
public class Client extends Personne {
    
    /*@OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Billet> billets = new ArrayList<>();*/
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(getId(), client.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
