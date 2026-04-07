package controller;

import entities.Client;
import entities.Organisateur;
import entities.Personne;
import entities.Personne.Role;
import entities.Employe;
import service.PersonneService;
import service.EmployeService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller unifié pour la gestion de tous les utilisateurs par le Gérant
 * @author COMLAN
 */
@Named("userAdminController")
@ViewScoped
@Data
public class UserAdminController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Inject
    private PersonneService personneService;
    
    @Inject
    private EmployeService employeService;
    
    @Inject
    private AuthController authController;
    
    @Inject
    private SecurityHelper securityHelper;
    
    // Données principales
    private Personne nouvelUtilisateur = new Organisateur();
    private List<Personne> tousLesUtilisateurs;
    private Personne utilisateurSelectionne;
    private String roleSelectionne = "ORGANISATEUR";
    private String rechercheTexte;
    private boolean modeEdition = false;
    
    // Employés pour organisateur
    private List<Employe> employesOrganisateurSelectionne;
    private Employe employeSelectionne;
    
    // Filtres
    private boolean filtreOrganisateur = false;
    private boolean filtreClient = false;
    
    @PostConstruct
    public void init() {
        // SÉCURITÉ : Vérifier que l'utilisateur est GERANT
        if (!securityHelper.isGerant()) {
            securityHelper.redirectToUnauthorized();
            return;
        }
        
        chargerTousLesUtilisateurs();
    }
    
    /**
     * Charge tous les utilisateurs de la plateforme
     */
    private void chargerTousLesUtilisateurs() {
        try {
            tousLesUtilisateurs = personneService.findAllUsers();
            
            System.out.println("=== UTILISATEURS CHARGÉS ===");
            System.out.println("Total utilisateurs: " + (tousLesUtilisateurs != null ? tousLesUtilisateurs.size() : 0));
            
            if (tousLesUtilisateurs != null && !tousLesUtilisateurs.isEmpty()) {
                long organisateurs = tousLesUtilisateurs.stream().filter(u -> u.getRole() == Role.ORGANISATEUR).count();
                long clients = tousLesUtilisateurs.stream().filter(u -> u.getRole() == Role.CLIENT).count();
                System.out.println("Organisateurs: " + organisateurs + ", Clients: " + clients);
                System.out.println("Premier utilisateur: " + tousLesUtilisateurs.get(0).getPrenom() + " " + tousLesUtilisateurs.get(0).getNom() + " (" + tousLesUtilisateurs.get(0).getEmail() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());
            e.printStackTrace();
            tousLesUtilisateurs = new ArrayList<>();
        }
    }
    
    /**
     * Retourne la liste filtrée des utilisateurs
     */
    public List<Personne> getUtilisateursFiltres() {
        if (tousLesUtilisateurs == null) {
            return new ArrayList<>();
        }
        
        // Si aucun filtre n'est activé, retourner tous les utilisateurs
        if (!filtreOrganisateur && !filtreClient) {
            return tousLesUtilisateurs;
        }
        
        return tousLesUtilisateurs.stream()
            .filter(user -> {
                if (filtreOrganisateur || filtreClient) {
                    return (filtreOrganisateur && user.getRole() == Role.ORGANISATEUR) ||
                           (filtreClient && user.getRole() == Role.CLIENT);
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des clients
     */
    public List<Personne> getClientsFiltres() {
        if (tousLesUtilisateurs == null) {
            return new ArrayList<>();
        }
        
        return tousLesUtilisateurs.stream()
            .filter(user -> user.getRole() == Role.CLIENT)
            .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des organisateurs
     */
    public List<Personne> getOrganisateursFiltres() {
        if (tousLesUtilisateurs == null) {
            return new ArrayList<>();
        }
        
        return tousLesUtilisateurs.stream()
            .filter(user -> user.getRole() == Role.ORGANISATEUR)
            .collect(Collectors.toList());
    }
    
    /**
     * Getter sécurisé pour nouvelUtilisateur
     */
    public Personne getNouvelUtilisateur() {
        if (nouvelUtilisateur == null) {
            nouvelUtilisateur = new Organisateur();
            roleSelectionne = "ORGANISATEUR";
        }
        return nouvelUtilisateur;
    }
    
    /**
     * Setter pour nouvelUtilisateur
     */
    public void setNouvelUtilisateur(Personne nouvelUtilisateur) {
        this.nouvelUtilisateur = nouvelUtilisateur;
    }
    
    /**
     * Formate la date d'inscription avec sécurité null
     */
    public String getDerniereActivite(Personne user) {
        // CORRECTION CRITIQUE : Vérification de sécurité pour éviter NullPointerException
        if (user == null) {
            return "Utilisateur non défini";
        }
        
        if (user.getDateInscription() == null) {
            return "Non renseignée";
        }
        
        try {
            return new java.text.SimpleDateFormat("dd/MM/yyyy").format(user.getDateInscription());
        } catch (Exception e) {
            System.err.println("Erreur lors du formatage de la date pour l'utilisateur " + user.getId() + ": " + e.getMessage());
            return "Erreur de format";
        }
    }
    
    /**
     * Méthode appelée quand le rôle change dans le formulaire
     */
    public void onRoleChange() {
        if (roleSelectionne != null && !roleSelectionne.isEmpty()) {
            switch (roleSelectionne) {
                case "ORGANISATEUR":
                    nouvelUtilisateur = new Organisateur();
                    break;
                case "CLIENT":
                    nouvelUtilisateur = new Client();
                    break;
                default:
                    nouvelUtilisateur = new Organisateur();
            }
            nouvelUtilisateur.setRole(Role.valueOf(roleSelectionne));
        }
    }
    
    /**
     * Ajoute un nouvel utilisateur
     */
    public void ajouterUtilisateur() {
        try {
            System.out.println("=== DÉBUT AJOUT UTILISATEUR - DEBUG ===");
            
            // DÉBOGAGE : Afficher les valeurs avant validation
            if (nouvelUtilisateur != null) {
                System.out.println("Nom: '" + nouvelUtilisateur.getNom() + "'");
                System.out.println("Prénom: '" + nouvelUtilisateur.getPrenom() + "'");
                System.out.println("Email: '" + nouvelUtilisateur.getEmail() + "'");
                System.out.println("Mot de passe: '" + (nouvelUtilisateur.getMotDePasse() != null ? "[DÉFINI]" : "[NULL]") + "'");
                System.out.println("Rôle: '" + nouvelUtilisateur.getRole() + "'");
                System.out.println("Type d'objet: " + nouvelUtilisateur.getClass().getSimpleName());
            } else {
                System.out.println("ERREUR: nouvelUtilisateur est NULL");
            }
            System.out.println("Rôle sélectionné: '" + roleSelectionne + "'");
            
            if (nouvelUtilisateur == null || roleSelectionne == null || roleSelectionne.isEmpty()) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Veuillez remplir tous les champs obligatoires.', 'error');");
                return;
            }
            
            // Validation des champs obligatoires
            if (nouvelUtilisateur.getNom() == null || nouvelUtilisateur.getNom().trim().isEmpty() ||
                nouvelUtilisateur.getPrenom() == null || nouvelUtilisateur.getPrenom().trim().isEmpty() ||
                nouvelUtilisateur.getEmail() == null || nouvelUtilisateur.getEmail().trim().isEmpty() ||
                nouvelUtilisateur.getMotDePasse() == null || nouvelUtilisateur.getMotDePasse().trim().isEmpty()) {
                
                System.out.println("ERREUR: Champs obligatoires manquants");
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Veuillez remplir tous les champs obligatoires.', 'error');");
                return;
            }
            
            // CORRECTION CRITIQUE : S'assurer que le rôle est bien défini
            if (nouvelUtilisateur.getRole() == null) {
                nouvelUtilisateur.setRole(Role.valueOf(roleSelectionne));
                System.out.println("Rôle défini: " + nouvelUtilisateur.getRole());
            }
            
            // Vérifier si l'email existe déjà
            if (personneService.emailExiste(nouvelUtilisateur.getEmail())) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Email déjà utilisé', 'Cet email est déjà associé à un compte existant.', 'error');");
                return;
            }
            
            System.out.println("Tentative de sauvegarde...");
            
            // Sauvegarder en base
            personneService.save(nouvelUtilisateur);
            
            System.out.println("Sauvegarde réussie !");
            
            // Recharger la liste
            chargerTousLesUtilisateurs();
            
            // Réinitialiser le formulaire
            nouvelUtilisateur = new Organisateur();
            roleSelectionne = "ORGANISATEUR";
            
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Succès', 'L\\'utilisateur a été ajouté avec succès.', 'success');");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Une erreur inattendue s\\'est produite.', 'error');");
        }
    }
    
    /**
     * Prépare l'ajout d'un nouvel utilisateur
     */
    public void preparerAjout() {
        nouvelUtilisateur = new Organisateur();
        roleSelectionne = "ORGANISATEUR";
        modeEdition = false;
        
        System.out.println("=== PRÉPARATION AJOUT ===");
        System.out.println("Formulaire réinitialisé pour ajout");
    }
    
    /**
     * Prépare l'affichage des détails d'un utilisateur
     */
    public void preparerDetails(Personne user) {
        try {
            utilisateurSelectionne = user;
            System.out.println("=== DÉTAILS UTILISATEUR ===");
            System.out.println("Utilisateur: " + (user != null ? user.getPrenom() + " " + user.getNom() : "NULL"));
        } catch (Exception e) {
            System.err.println("Erreur lors de la préparation des détails: " + e.getMessage());
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Impossible d\\'afficher les détails.', 'error');");
        }
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
            
            // Copier les données de l'utilisateur sélectionné dans nouvelUtilisateur pour modification
            if (user.getRole() == Role.ORGANISATEUR) {
                nouvelUtilisateur = new Organisateur();
                roleSelectionne = "ORGANISATEUR";
            } else {
                nouvelUtilisateur = new Client();
                roleSelectionne = "CLIENT";
            }
            
            nouvelUtilisateur.setId(user.getId());
            nouvelUtilisateur.setNom(user.getNom());
            nouvelUtilisateur.setPrenom(user.getPrenom());
            nouvelUtilisateur.setEmail(user.getEmail());
            // Ne pas copier le mot de passe pour des raisons de sécurité
            nouvelUtilisateur.setMotDePasse(""); // L'utilisateur devra saisir un nouveau mot de passe
            nouvelUtilisateur.setRole(user.getRole());
            nouvelUtilisateur.setDateInscription(user.getDateInscription());
            
            modeEdition = true;
            
            System.out.println("=== PRÉPARATION MODIFICATION ===");
            System.out.println("Utilisateur: " + user.getPrenom() + " " + user.getNom());
            System.out.println("Mode édition activé");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la préparation de la modification: " + e.getMessage());
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Impossible de préparer la modification.', 'error');");
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
            chargerTousLesUtilisateurs();
            
            // Réinitialiser le formulaire
            nouvelUtilisateur = new Organisateur();
            roleSelectionne = "ORGANISATEUR";
            modeEdition = false;
            
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Succès', 'L\\'utilisateur a été modifié avec succès.', 'success');");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la modification de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Une erreur inattendue s\\'est produite.', 'error');");
        }
    }
    
    /**
     * Supprime un utilisateur avec validation de sécurité
     */
    public void supprimerUtilisateur(Personne user) {
        try {
            if (user == null) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Utilisateur introuvable.', 'error');");
                return;
            }
            
            Personne utilisateurConnecte = authController.getUtilisateurConnecte();
            
            // Vérification : ne pas supprimer l'utilisateur connecté
            if (user.getId().equals(utilisateurConnecte.getId())) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Vous ne pouvez pas vous supprimer vous-même.', 'error');");
                return;
            }
            
            String nomComplet = user.getPrenom() + " " + user.getNom();
            
            personneService.delete(user.getId());
            
            // Recharger la liste
            chargerTousLesUtilisateurs();
            
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Supprimé', 'L\\'utilisateur " + nomComplet + " a été supprimé avec succès.', 'success');");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression: " + e.getMessage());
            e.printStackTrace();
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Impossible de supprimer cet utilisateur.', 'error');");
        }
    }
    
    /**
     * Supprime un utilisateur par ID (pour RemoteCommand)
     */
    public void supprimerUtilisateurParId() {
        try {
            // Récupérer le paramètre depuis la requête
            String userIdParam = jakarta.faces.context.FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get("userIdParam");
            
            System.out.println("=== SUPPRESSION PAR ID ===");
            System.out.println("Paramètre reçu: " + userIdParam);
            
            if (userIdParam == null || userIdParam.isEmpty()) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'ID utilisateur manquant.', 'error');");
                return;
            }
            
            Long userId = Long.parseLong(userIdParam);
            Personne user = personneService.trouverParId(userId);
            
            if (user == null) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Utilisateur introuvable.', 'error');");
                return;
            }
            
            supprimerUtilisateur(user);
            
        } catch (NumberFormatException e) {
            System.err.println("Erreur de format ID: " + e.getMessage());
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'ID utilisateur invalide.', 'error');");
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression par ID: " + e.getMessage());
            e.printStackTrace();
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Une erreur inattendue s\\'est produite.', 'error');");
        }
    }

    /**
     * Désactive un utilisateur (le compte reste en base mais devient inactif)
     */
    public void desactiverUtilisateur(Personne user) {
        try {
            if (user == null) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Utilisateur introuvable.', 'error');");
                return;
            }

            Personne utilisateurConnecte = authController.getUtilisateurConnecte();

            // Vérification : ne pas se désactiver soi-même
            if (user.getId().equals(utilisateurConnecte.getId())) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Vous ne pouvez pas vous désactiver vous-même.', 'error');");
                return;
            }

            // Désactiver l'utilisateur
            user.setActif(false);
            personneService.save(user);

            // Recharger la liste
            chargerTousLesUtilisateurs();

            String nomComplet = user.getPrenom() + " " + user.getNom();
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Succès', 'Le compte de " + nomComplet + " a été désactivé avec succès.', 'success');");

        } catch (Exception e) {
            System.err.println("Erreur lors de la désactivation: " + e.getMessage());
            e.printStackTrace();
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Impossible de désactiver cet utilisateur.', 'error');");
        }
    }

    /**
     * Réactive un utilisateur (le compte devient actif à nouveau)
     */
    public void reactiverUtilisateur(Personne user) {
        try {
            if (user == null) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Utilisateur introuvable.', 'error');");
                return;
            }

            Personne utilisateurConnecte = authController.getUtilisateurConnecte();

            // Vérification : ne pas réactiver soi-même (bien qu'on ne puisse pas se désactiver)
            if (user.getId().equals(utilisateurConnecte.getId())) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Impossible de réactiver votre propre compte.', 'error');");
                return;
            }

            // Réactiver l'utilisateur
            user.setActif(true);
            personneService.save(user);

            // Recharger la liste
            chargerTousLesUtilisateurs();

            String nomComplet = user.getPrenom() + " " + user.getNom();
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Succès', 'Le compte de " + nomComplet + " a été réactivé avec succès.', 'success');");

        } catch (Exception e) {
            System.err.println("Erreur lors de la réactivation: " + e.getMessage());
            e.printStackTrace();
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Impossible de réactiver cet utilisateur.', 'error');");
        }
    }

    /**
     * Bascule l'état actif/inactif d'un utilisateur
     */
    public void basculerEtatUtilisateur(Personne user) {
        if (user.isActif()) {
            desactiverUtilisateur(user);
        } else {
            reactiverUtilisateur(user);
        }
    }

    /**
     * Désactive un utilisateur par ID
     */
    public void desactiverUtilisateurParId() {
        try {
            String userIdParam = jakarta.faces.context.FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get("userIdParam");

            if (userIdParam == null || userIdParam.isEmpty()) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'ID utilisateur manquant.', 'error');");
                return;
            }

            Long userId = Long.parseLong(userIdParam);
            Personne user = personneService.trouverParId(userId);

            if (user == null) {
                org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'Utilisateur introuvable.', 'error');");
                return;
            }

            desactiverUtilisateur(user);

        } catch (NumberFormatException e) {
            System.err.println("Erreur de format ID: " + e.getMessage());
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur', 'ID utilisateur invalide.', 'error');");
        } catch (Exception e) {
            System.err.println("Erreur lors de la désactivation par ID: " + e.getMessage());
            e.printStackTrace();
            org.primefaces.PrimeFaces.current().executeScript("Swal.fire('Erreur système', 'Une erreur inattendue s\\'est produite.', 'error');");
        }
    }
    
    /**
     * Charge les employés d'un organisateur
     */
    public void chargerEmployesOrganisateur(Personne organisateur) {
        try {
            if (organisateur instanceof Organisateur) {
                Organisateur orga = (Organisateur) organisateur;
                if (orga.isPersonneMorale()) {
                    employesOrganisateurSelectionne = employeService.trouverParEmployeur(orga);
                    System.out.println("✓ Employés chargés pour " + orga.getNomEntreprise() + ": " 
                        + (employesOrganisateurSelectionne != null ? employesOrganisateurSelectionne.size() : 0));
                } else {
                    employesOrganisateurSelectionne = new ArrayList<>();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des employés: " + e.getMessage());
            e.printStackTrace();
            employesOrganisateurSelectionne = new ArrayList<>();
        }
    }

    public void preparerDetailsEmploye(Employe employe) {
        this.employeSelectionne = employe;
        System.out.println("✓ Employé sélectionné pour détails: " + employe.getPrenom() + " " + employe.getNom());
    }

    // Getters et Setters
    public Employe getEmployeSelectionne() {
        return employeSelectionne;
    }

    public void setEmployeSelectionne(Employe employeSelectionne) {
        this.employeSelectionne = employeSelectionne;
    }

    public List<Employe> getEmployesOrganisateurSelectionne() {
        return employesOrganisateurSelectionne;
    }

    public void setEmployesOrganisateurSelectionne(List<Employe> employesOrganisateurSelectionne) {
        this.employesOrganisateurSelectionne = employesOrganisateurSelectionne;
    }
}