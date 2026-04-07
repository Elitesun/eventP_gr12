package controller;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Data;
import java.io.Serializable;

/**
 * Controller simplifié - Navigation maintenant statique dans template.xhtml
 * @author COMLAN
 */
@Named("menuController")
@SessionScoped
@Data
public class MenuController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Garde seulement les propriétés essentielles au cas où elles sont référencées
    private boolean sidebarCollapsed = false;
    
    /**
     * Toggle l'état de la sidebar (collapsed/expanded)
     * Méthode conservée pour compatibilité mais la logique est maintenant en JavaScript
     */
    public void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
    }
}
