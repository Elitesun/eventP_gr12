package controller;

import entities.Client;
import entities.Employe;
import entities.Organisateur;
import entities.Personne;
import entities.Personne.Role;
import service.PersonneService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller pour la gestion des utilisateurs par l'Organisateur
 * Permet à un organisateur de gérer ses employés et clients
 * @author COMLAN
 */
@Named("userOrgaController")
@ViewScoped
@Getter
@Setter
public class UserOrgaController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Inject
    private PersonneService personneService;
    
    @Inject
    private SecurityHelper securityHelper;
    
    @Inject
    private AuthController authController;
    
    // Données
    private List<Personne> mesUtilisateurs;
    private Personne nouvelUtilisateur;
    private Personne utilisateurSelectionne;
    private Personne utilisateurASupprimer;
    private String roleSelectionne;
    private boolean modeEdition = false;
    
    // Filtres - NETTOYAGE : Suppression des filtres Statut inutiles
    private String rechercheTexte;
    private boolean filtreClient;
    private boolean filtreEmploye;
    
    @PostConstruct
    public void init() {

        
        // SÉCURITÉ : Vérifier que l'utilisateur est ORGANISATEUR
        if (!securityHelper.isOrganisateur()) {

            securityHelper.redirectToUnauthorized();
            return;
        }
        

        chargerMesUtilisateurs();
        preparerAjout();

    }
    
    /**
     * Charge les utilisateurs liés à cet organisateur
     */
    private void chargerMesUtilisateurs() {
        try {
            Organisateur organisateurConnecte = (Organisateur) authController.getUtilisateurConnecte();
            Long organisateurId = organisateurConnecte.getId();
            
            mesUtilisateurs = new ArrayList<>();
            
            // TODO: Implémenter les vraies requêtes
            // mesUtilisateurs.addAll(personneService.findEmployesByOrganisateur(organisateurId));
            // mesUtilisateurs.addAll(personneService.findClientsByOrganisateur(organisateurId));
            
            // Pour l'instant, simulation avec quelques utilisateurs
            simulerUtilisateurs();
            

            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());

            mesUtilisateurs = new ArrayList<>();
        }
    }
    
    /**
     * Simulation temporaire des utilisateurs
     */
    private void simulerUtilisateurs() {
        // Simulation d'employés
        Employe emp1 = new Employe();
        emp1.setId(101L);
        emp1.setNom("Dupont");
        emp1.setPrenom("Pierre");
        emp1.setEmail("pierre.dupont@event.com");
        emp1.setRole(Role.EMPLOYE);
        emp1.setDateInscription(new java.util.Date());
        mesUtilisateurs.add(emp1);
        
        // Simulation de clients
        Client client1 = new Client();
        client1.setId(102L);
        client1.setNom("Martin");
        client1.setPrenom("Sophie");
        client1.setEmail("sophie.martin@client.com");
        client1.setRole(Role.CLIENT);
        client1.setDateInscription(new java.util.Date());
        mesUtilisateurs.add(client1);
    }
    
    /**
     * Prépare l'ajout d'un nouvel utilisateur
     */
    public void preparerAjout() {
        nouvelUtilisateur = new Client(); // Par défaut, on crée un Client
        roleSelectionne = "CLIENT";
        modeEdition = false;
        

    }
    
    /**
     * Prépare la modification d'un utilisateur
     */
    public void preparerModification(Personne user) {
        try {
            if (user == null) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Utilisateur introuvable.', 'error');");
                return;
            }
            
            // Copier les données de l'utilisateur sélectionné
            if (user.getRole() == Role.CLIENT) {
                nouvelUtilisateur = new Client();
                roleSelectionne = "CLIENT";
            } else {
                nouvelUtilisateur = new Employe();
                roleSelectionne = "EMPLOYE";
            }
            
            nouvelUtilisateur.setId(user.getId());
            nouvelUtilisateur.setNom(user.getNom());
            nouvelUtilisateur.setPrenom(user.getPrenom());
            nouvelUtilisateur.setEmail(user.getEmail());
            nouvelUtilisateur.setMotDePasse(""); // Sécurité : ne pas afficher le mot de passe
            nouvelUtilisateur.setRole(user.getRole());
            nouvelUtilisateur.setDateInscription(user.getDateInscription());
            
            modeEdition = true;
            

            
        } catch (Exception e) {
            System.err.println("Erreur lors de la préparation de la modification: " + e.getMessage());
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Impossible de préparer la modification.', 'error');");
        }
    }
    
    /**
     * Méthode appelée quand le rôle change dans le formulaire
     */
    public void onRoleChange() {

        
        if (roleSelectionne != null && !roleSelectionne.isEmpty()) {
            // Sauvegarder les données existantes
            String nom = nouvelUtilisateur != null ? nouvelUtilisateur.getNom() : null;
            String prenom = nouvelUtilisateur != null ? nouvelUtilisateur.getPrenom() : null;
            String email = nouvelUtilisateur != null ? nouvelUtilisateur.getEmail() : null;
            String motDePasse = nouvelUtilisateur != null ? nouvelUtilisateur.getMotDePasse() : null;
            Long id = nouvelUtilisateur != null ? nouvelUtilisateur.getId() : null;
            java.util.Date dateInscription = nouvelUtilisateur != null ? nouvelUtilisateur.getDateInscription() : null;
            
            // Créer l'instance appropriée selon le rôle
            switch (roleSelectionne) {
                case "CLIENT":
                    nouvelUtilisateur = new Client();
                    break;
                case "EMPLOYE":
                    nouvelUtilisateur = new Employe();
                    break;
                default:
                    nouvelUtilisateur = new Client(); // Par défaut
            }
            
            // Restaurer les données
            if (id != null) nouvelUtilisateur.setId(id);
            if (nom != null) nouvelUtilisateur.setNom(nom);
            if (prenom != null) nouvelUtilisateur.setPrenom(prenom);
            if (email != null) nouvelUtilisateur.setEmail(email);
            if (motDePasse != null) nouvelUtilisateur.setMotDePasse(motDePasse);
            if (dateInscription != null) nouvelUtilisateur.setDateInscription(dateInscription);
            
            nouvelUtilisateur.setRole(Role.valueOf(roleSelectionne));
            

        }
    }
    
    /**
     * Sauvegarde un utilisateur (ajout ou modification)
     */
    public void sauvegarderUtilisateur() {
        if (modeEdition) {
            modifierUtilisateur();
        } else {
            ajouterUtilisateur();
        }
    }
    
    /**
     * Ajoute un nouvel utilisateur (Client ou Employé)
     */
    public void ajouterUtilisateur() {
        try {

            
            if (nouvelUtilisateur == null || roleSelectionne == null || roleSelectionne.isEmpty()) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Veuillez remplir tous les champs obligatoires.', 'error');");
                return;
            }
            
            // Validation des champs obligatoires
            if (nouvelUtilisateur.getNom() == null || nouvelUtilisateur.getNom().trim().isEmpty() ||
                nouvelUtilisateur.getPrenom() == null || nouvelUtilisateur.getPrenom().trim().isEmpty() ||
                nouvelUtilisateur.getEmail() == null || nouvelUtilisateur.getEmail().trim().isEmpty() ||
                nouvelUtilisateur.getMotDePasse() == null || nouvelUtilisateur.getMotDePasse().trim().isEmpty()) {
                
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Veuillez remplir tous les champs obligatoires.', 'error');");
                return;
            }
            
            // VALIDATION CRITIQUE : Vérifier si l'email existe déjà
            if (personneService.emailExiste(nouvelUtilisateur.getEmail())) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Email déjà utilisé', 'Cet email est déjà associé à un compte existant.', 'error');");
                return;
            }
            
            Organisateur organisateurConnecte = (Organisateur) authController.getUtilisateurConnecte();
            
            // LOGIQUE MÉTIER CRITIQUE : Lier l'utilisateur à l'organisateur
            if (nouvelUtilisateur instanceof Employe) {
                ((Employe) nouvelUtilisateur).setEmployeur(organisateurConnecte);
            }
            
            // Sauvegarder en base
            personneService.save(nouvelUtilisateur);
            
            // CORRECTION CRITIQUE : Recharger la liste pour rafraîchir la table
            chargerMesUtilisateurs();
            
            // Réinitialiser le formulaire
            preparerAjout();
            
            // Utiliser SweetAlert pour le succès
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Succès', 'L\\'utilisateur a été ajouté avec succès.', 'success');");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout de l'utilisateur: " + e.getMessage());

            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Une erreur inattendue s\\'est produite.', 'error');");
        }
    }
    
    /**
     * Modifie un utilisateur existant
     */
    public void modifierUtilisateur() {
        try {
            if (nouvelUtilisateur == null || nouvelUtilisateur.getId() == null) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Utilisateur invalide pour la modification.', 'error');");
                return;
            }
            
            // Validation des champs obligatoires
            if (nouvelUtilisateur.getNom() == null || nouvelUtilisateur.getNom().trim().isEmpty() ||
                nouvelUtilisateur.getPrenom() == null || nouvelUtilisateur.getPrenom().trim().isEmpty() ||
                nouvelUtilisateur.getEmail() == null || nouvelUtilisateur.getEmail().trim().isEmpty()) {
                
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Veuillez remplir tous les champs obligatoires.', 'error');");
                return;
            }
            
            // Si le mot de passe est vide en mode modification, garder l'ancien
            if (nouvelUtilisateur.getMotDePasse() == null || nouvelUtilisateur.getMotDePasse().trim().isEmpty()) {
                Personne utilisateurExistant = personneService.trouverParId(nouvelUtilisateur.getId());
                if (utilisateurExistant != null) {
                    nouvelUtilisateur.setMotDePasse(utilisateurExistant.getMotDePasse());
                }
            }
            
            // Mettre à jour en base
            personneService.save(nouvelUtilisateur);
            
            // Recharger la liste
            chargerMesUtilisateurs();
            
            // Réinitialiser le formulaire
            preparerAjout();
            
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Succès', 'L\\'utilisateur a été modifié avec succès.', 'success');");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la modification de l'utilisateur: " + e.getMessage());

            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Une erreur inattendue s\\'est produite.', 'error');");
        }
    }
    
    /**
     * Voir les détails d'un utilisateur
     */
    public void voirDetails(Personne user) {
        try {
            utilisateurSelectionne = user;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage des détails: " + e.getMessage());
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Impossible d\\'afficher les détails.', 'error');");
        }
    }
    
    /**
     * Confirme la suppression d'un utilisateur
     */
    public void confirmerSuppression() {
        try {
            if (utilisateurASupprimer == null) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Aucun utilisateur sélectionné pour suppression.', 'error');");
                return;
            }
            
            Organisateur organisateurConnecte = (Organisateur) authController.getUtilisateurConnecte();
            
            // Vérification supplémentaire : ne pas supprimer l'organisateur lui-même
            if (utilisateurASupprimer.getId().equals(organisateurConnecte.getId())) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Vous ne pouvez pas vous supprimer vous-même.', 'error');");
                return;
            }
            
            String nomComplet = utilisateurASupprimer.getPrenom() + " " + utilisateurASupprimer.getNom();
            
            personneService.delete(utilisateurASupprimer.getId());
            
            // CORRECTION CRITIQUE : Recharger la liste pour rafraîchir la table
            chargerMesUtilisateurs();
            
            // Réinitialiser la sélection
            utilisateurASupprimer = null;
            
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Supprimé', 'L\\'utilisateur " + nomComplet + " a été supprimé avec succès.', 'success');");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression: " + e.getMessage());

            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Impossible de supprimer cet utilisateur.', 'error');");
        }
    }
    
    /**
     * Formate la date d'inscription avec vérification de sécurité
     */
    public String getDateInscription(Personne user) {
        // CORRECTION CRITIQUE : Vérification de sécurité pour éviter NullPointerException
        if (user == null || user.getDateInscription() == null) {
            return "Non renseignée";
        }
        
        try {
            return new java.text.SimpleDateFormat("dd/MM/yyyy").format(user.getDateInscription());
        } catch (Exception e) {
            System.err.println("Erreur lors du formatage de la date: " + e.getMessage());
            return "Non renseignée";
        }
    }
    
    /**
     * Retourne la liste filtrée des utilisateurs - NETTOYAGE : Suppression des filtres Statut
     */
    public List<Personne> getUtilisateursFiltres() {
        if (mesUtilisateurs == null) {
            return new ArrayList<>();
        }
        
        // Si aucun filtre n'est activé, retourner tous les utilisateurs
        if (!filtreClient && !filtreEmploye) {
            return mesUtilisateurs;
        }
        
        return mesUtilisateurs.stream()
            .filter(user -> {
                // Filtre par rôle uniquement
                if (filtreClient || filtreEmploye) {
                    return (filtreClient && user.getRole() == Role.CLIENT) ||
                           (filtreEmploye && user.getRole() == Role.EMPLOYE);
                }
                return true;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Getter sécurisé pour nouvelUtilisateur
     */
    public Personne getNouvelUtilisateur() {
        if (nouvelUtilisateur == null) {
            preparerAjout();
        }
        return nouvelUtilisateur;
    }
    
    /**
     * Test method to verify controller is working
     */
    public String getTestMessage() {
        return "UserOrgaController is working correctly!";
    }
}