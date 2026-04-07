package controller;


import entities.Personne;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import lombok.Data;
import service.PersonneService;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author COMLAN
 */
@Named("authController") // C'est le nom qu'on va utiliser dans la page web (le XHTML)
@SessionScoped           // Ça garde la session ouverte tant que l'utilisateur ne se déconnecte pas
@Data                    // Lombok crée automatiquement les getters et setters
public class AuthController implements Serializable {

    private String email;
    private String motDePasse;
    private Personne utilisateurConnecte;

    @Inject
    private PersonneService personneService; // On appelle notre fameux serveur !

    // Méthode déclenchée par le bouton "Se connecter"
    public String seConnecter() {
        // On demande au service de vérifier dans la base de données
        utilisateurConnecte = personneService.authentifier(email, motDePasse);

        if (utilisateurConnecte != null) {
            // Vérifier si le compte est actif
            if (!utilisateurConnecte.isActif()) {
                System.out.println("=== COMPTE DÉSACTIVÉ ===");
                System.out.println("Utilisateur: " + utilisateurConnecte.getPrenom() + " " + utilisateurConnecte.getNom());
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Compte désactivé", "Votre compte a été désactivé. Contactez un administrateur."));
                utilisateurConnecte = null;
                return null;
            }
            
            // REDIRECTION INTELLIGENTE STRICTE SELON LE RÔLE :
            System.out.println("=== CONNEXION RÉUSSIE ===");
            System.out.println("Utilisateur: " + utilisateurConnecte.getPrenom() + " " + utilisateurConnecte.getNom());
            System.out.println("Rôle: " + utilisateurConnecte.getRole());
            
            switch (utilisateurConnecte.getRole()) {
                case GERANT: 
                    System.out.println("Redirection GERANT -> dashboard_home.xhtml");
                    return "dashboard_home?faces-redirect=true";
                    
                case ORGANISATEUR: 
                    System.out.println("Redirection ORGANISATEUR -> dashboard_orga.xhtml");
                    return "dashboard_orga?faces-redirect=true";
                    
                case CLIENT: 
                    System.out.println("Redirection CLIENT -> dashboard_client.xhtml");
                    return "dashboard_client?faces-redirect=true";
                    
                case EMPLOYE: 
                    System.out.println("Redirection EMPLOYE -> dashboard_client.xhtml");
                    return "dashboard_client?faces-redirect=true";
                    
                default: 
                    System.out.println("Rôle non reconnu, redirection vers index");
                    return "index?faces-redirect=true";
            }
        } else {
            // ERREUR : Le login ou le mot de passe est faux
            System.out.println("=== ÉCHEC CONNEXION ===");
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Email ou mot de passe incorrect."));
            return null; // On reste sur la page de connexion
        }
    }

    // Méthode déclenchée par le bouton "Se déconnecter"
    public String seDeconnecter() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        utilisateurConnecte = null;
        return "index?faces-redirect=true"; // Redirection vers la page publique
    }
}