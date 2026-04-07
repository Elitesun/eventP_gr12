/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package dao;

import entities.Personne;
import jakarta.ejb.Local;

/**
 *
 * @author COMLAN
 */
@Local
public interface PersonneDao {
    void enregistrer(Personne p);
    Personne authentifier(String email, String mdp);
    boolean emailExiste(String email);
    java.util.List<Personne> findAll();
    java.util.List<Personne> findByRole(Personne.Role role);
}