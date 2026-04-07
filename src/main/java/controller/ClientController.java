package controller;

import entities.Client;
import entities.Personne;
import entities.Personne.Role;
import service.PersonneService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Data;
import java.util.List;

/**
 * Controller pour la gestion des clients
 * @author COMLAN
 */
@Named("clientController")
@RequestScoped
@Data
public class ClientController {
    
    private Client nouveauClient = new Client();
    private List<Personne> clients;
    
    @Inject
    private PersonneService personneService;
    
    @Inject
    private AuthController authController;
    
    @PostConstruct
    public void init() {
        chargerClients();
    }
    
    /**
     * Charge la liste complète des clients
     */
    public void chargerClients() {
        clients = personneService.findByRole(Role.CLIENT);
    }
    
    /**
     * Crée un nouveau client
     * @return navigation
     */
    public String creerClient() {
        try {
            // Vérifier si l'email existe déjà
            if (personneService.emailExiste(nouveauClient.getEmail())) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Erreur", "Cet email est déjà utilisé"));
                return null;
            }
            
            // Définir le rôle et enregistrer
            nouveauClient.setRole(Role.CLIENT);
            personneService.enregistrer(nouveauClient);
            
            // Message de succès
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Succès", "Client créé avec succès"));
            
            // Reset le formulaire et recharger la liste
            nouveauClient = new Client();
            chargerClients();
            
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erreur", "Impossible de créer le client: " + e.getMessage()));
            return null;
        }
    }
    
    /**
     * Retourne la liste des clients
     * @return liste des clients
     */
    public List<Personne> getClients() {
        if (clients == null) {
            chargerClients();
        }
        return clients;
    }
}
