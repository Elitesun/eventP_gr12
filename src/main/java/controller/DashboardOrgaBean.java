package controller;

import entities.Personne;
import entities.Evenement;
import entities.Organisateur;
import service.PersonneService;
import service.EvenementService;
import service.TicketService;
import service.EmployeService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dashboard Organisateur Bean - ORGANISATEUR UNIQUEMENT
 * Affiche les KPIs spécifiques à l'organisateur connecté
 * @author COMLAN
 */
@Named("dashboardOrgaBean")
@ViewScoped
@Data
public class DashboardOrgaBean implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Inject
    private PersonneService personneService;
    
    @Inject
    private EvenementService evenementService;
    
    @Inject
    private TicketService ticketService;
    
    @Inject
    private EmployeService employeService;
    
    @Inject
    private SecurityHelper securityHelper;
    
    @Inject
    private AuthController authController;
    
    // KPIs ORGANISATEUR - CORRECTION : Chiffre d'affaires en FCFA
    private long mesEvenements;
    private long billetsVendus;
    private long mesEmployes;
    private long mesClients;
    private String chiffreAffaires = "1,245,000 FCFA"; // Simulation en FCFA
    
    // Données spécifiques à l'organisateur
    private List<EvenementData> evenementsRecents;
    private List<Personne> employesOrganisateur;
    
    @PostConstruct
    public void init() {
        // SÉCURITÉ : Vérifier que l'utilisateur est ORGANISATEUR
        if (!securityHelper.isOrganisateur()) {
            securityHelper.redirectToUnauthorized();
            return;
        }
        
        loadOrganisateurData();
    }
    
    /**
     * Charge uniquement les données de l'ORGANISATEUR connecté
     */
    private void loadOrganisateurData() {
        try {
            Personne utilisateurConnecte = authController.getUtilisateurConnecte();
            
            // Vérifier que c'est un organisateur
            if (!(utilisateurConnecte instanceof Organisateur)) {
                securityHelper.redirectToUnauthorized();
                return;
            }
            
            Organisateur organisateur = (Organisateur) utilisateurConnecte;
            Long organisateurId = organisateur.getId();
            
            // 1. Charger les événements de cet organisateur
            List<Evenement> evenements = evenementService.trouverEvenementsParOrganisateur(organisateur);
            mesEvenements = (long) evenements.size();
            
            // 2. Calculer les billets vendus pour cet organisateur
            long totalBilletsVendus = 0;
            long totalChiffreAffaires = 0;
            for (Evenement evt : evenements) {
                Long billetsVendus = ticketService.compterTicketsVendus(evt.getId());
                if (billetsVendus != null) {
                    totalBilletsVendus += billetsVendus;
                    // Calculer le chiffre d'affaires (prix du ticket * nombre de billets vendus)
                    if (evt.getPrixTicket() != null) {
                        totalChiffreAffaires += (billetsVendus * evt.getPrixTicket().longValue());
                    }
                }
            }
            billetsVendus = totalBilletsVendus;
            chiffreAffaires = formatMontant(totalChiffreAffaires);
            
            // 3. Charger les employés de cet organisateur
            List<entities.Employe> employes = employeService.trouverParEmployeur(organisateur);
            mesEmployes = (long) employes.size();
            employesOrganisateur = new ArrayList<>(employes);
            
            // 4. Charger les événements récents
            chargerEvenementsRecents(evenements);
            
            System.out.println("=== DASHBOARD ORGANISATEUR CHARGÉ (VRAIES DONNÉES) ===");
            System.out.println("Organisateur ID: " + organisateurId);
            System.out.println("Mes Événements: " + mesEvenements);
            System.out.println("Billets Vendus: " + billetsVendus);
            System.out.println("Mes Employés: " + mesEmployes);
            System.out.println("Chiffre d'Affaires: " + chiffreAffaires);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des données ORGANISATEUR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge les événements récents depuis les vraies données
     */
    private void chargerEvenementsRecents(List<Evenement> evenements) {
        evenementsRecents = new ArrayList<>();
        
        // Trier pour obtenir les plus récents (les 4 derniers)
        int nbAfficher = Math.min(4, evenements.size());
        for (int i = Math.max(0, evenements.size() - nbAfficher); i < evenements.size(); i++) {
            Evenement evt = evenements.get(i);
            
            // Déterminer le statut
            String statut = determinerStatutEvenement(evt);
            
            // Compter les billets vendus pour cet événement
            Long billetsVendus = ticketService.compterTicketsVendus(evt.getId());
            int nbBilletsVendus = billetsVendus != null ? billetsVendus.intValue() : 0;
            
            // Formater la date
            String dateStr = evt.getDateEvenement() != null ? 
                new SimpleDateFormat("dd/MM/yyyy").format(evt.getDateEvenement()) : "N/A";
            
            evenementsRecents.add(new EvenementData(
                evt.getTitre(),
                dateStr,
                nbBilletsVendus,
                statut
            ));
        }
    }
    
    /**
     * Détermine le statut d'un événement
     */
    private String determinerStatutEvenement(Evenement evt) {
        if (evt.getDateEvenement() == null) {
            return "Non défini";
        }
        
        Date maintenant = new Date();
        Date dateEvenement = evt.getDateEvenement();
        
        if (dateEvenement.after(maintenant)) {
            // Événement futur
            if (evt.getTicketsVendus() != null && evt.getNombreTicketsTotal() != null) {
                if (evt.getTicketsVendus() >= evt.getNombreTicketsTotal()) {
                    return "Complet";
                }
            }
            return "Actif";
        } else {
            // Événement passé
            return "Terminé";
        }
    }
    
    /**
     * Formate un montant en FCFA
     */
    private String formatMontant(long montant) {
        return String.format("%,d €", montant).replace(",", " ");
    }
    
    /**
     * Navigation vers la gestion des employés
     */
    public String gererEmployes() {
        return "users_management_orga?faces-redirect=true";
    }
    
    /**
     * Navigation vers la gestion des clients
     */
    public String gererClients() {
        return "users_management_orga?faces-redirect=true";
    }
    
    /**
     * Navigation vers la création d'événement
     */
    public String creerEvenement() {
        return "creer_evenement?faces-redirect=true";
    }
    
    // Getters pour les taux (simulés)
    public String getTauxConversion() {
        return "77.1%";
    }
    
    public String getCroissanceCA() {
        return "+18.5%";
    }
    
    public String getCroissanceEvenements() {
        return "+12.3%";
    }
    
    /**
     * Classe interne pour représenter un événement
     */
    @Data
    public static class EvenementData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String nom;
        private String date;
        private int billets;
        private String statut;
        
        public EvenementData(String nom, String date, int billets, String statut) {
            this.nom = nom;
            this.date = date;
            this.billets = billets;
            this.statut = statut;
        }
        
        public String getCouleurStatut() {
            switch (statut) {
                case "Actif": return "#2e7d32";
                case "Complet": return "#d32f2f";
                case "Bientôt": return "#f57c00";
                default: return "#6c757d";
            }
        }
    }
}