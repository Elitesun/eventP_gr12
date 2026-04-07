package controller;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

/**
 * Controller pour index.xhtml - utilise ClientEvenementController pour événements réels DB
 */
@Named
@ViewScoped
public class IndexController implements Serializable {
    
    @Inject
    private ClientEvenementController clientEvenementController;
    
    @PostConstruct
    public void init() {
        clientEvenementController.chargerEvenementsPublies();
    }

    public ClientEvenementController getClientEvenementController() {
        return clientEvenementController;
    }
    
    public void rechercher() {
        clientEvenementController.rechercherEvenements();
    }
}
