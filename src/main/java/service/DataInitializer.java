package service;

import dao.PersonneDao;
import entities.Client;
import entities.Evenement;
import entities.Employe;
import entities.Gerant;
import entities.Organisateur;
import entities.TicketCategorie;
import entities.Personne.Role;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import utils.PasswordUtils;

/**
 * Initialise les données de test au démarrage de l'application
 * @author COMLAN
 */
@Singleton
@Startup
public class DataInitializer {

    @Inject
    private PersonneDao personneDao;

    @Inject
    private EvenementService evenementService;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void init() {
        try {
            System.out.println("=== DÉMARRAGE INITIALISATION DONNÉES ===");

            // Créer le gérant par défaut
            creerGerant();

            // Créer les utilisateurs de test
            creerUtilisateursTest();

            // Créer les employés pour les organisateurs MORALE
            creerEmployes();

            // Créer des événements de démonstration publiés
            creerEvenementsDemo();

            // Vérifier ce qui a été créé
            verifierDonnees();

            System.out.println("=== FIN INITIALISATION DONNÉES ===");
        } catch (Exception e) {
            // Ne jamais empêcher le déploiement en cas d'erreur de seed
            System.err.println("[DataInitializer] Erreur d'initialisation non bloquante: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void creerEvenementsDemo() {
        try {
            if (!evenementService.trouverTousEvenements().isEmpty()) {
                System.out.println("✓ Événements déjà présents, seed ignoré");
                return;
            }

            java.util.List<Organisateur> organisateurs = (java.util.List<Organisateur>) (java.util.List<?>) personneDao.findByRole(Role.ORGANISATEUR);
            if (organisateurs.isEmpty()) {
                System.out.println("⚠ Aucun organisateur trouvé, impossible de créer des événements de démonstration");
                return;
            }

            Organisateur orga = organisateurs.get(0);
            creerEvenementDemo(
                orga,
                "Afro Future Summit",
                "Conférences et networking autour de l'innovation africaine.",
                "Palais des Congrès, Abidjan",
                20,
                "https://images.unsplash.com/photo-1511578314322-379afb476865"
            );

            creerEvenementDemo(
                orga,
                "Festival Urbain de Nuit",
                "Musique live, performances et food court.",
                "Parc Expo, Dakar",
                35,
                "https://images.unsplash.com/photo-1492684223066-81342ee5ff30"
            );

            System.out.println("✓ Événements de démonstration créés et publiés");
        } catch (Exception e) {
            System.err.println("[DataInitializer] Erreur création événements démo: " + e.getMessage());
        }
    }

    private void creerEvenementDemo(Organisateur organisateur, String titre, String description, String lieu, int joursDansLeFutur, String imageUrl) {
        Evenement event = new Evenement();
        event.setTitre(titre);
        event.setDescription(description);
        event.setLieu(lieu);
        event.setImageUrl(imageUrl);
        event.setDateEvenement(new java.util.Date(System.currentTimeMillis() + (joursDansLeFutur * 24L * 60L * 60L * 1000L)));
        event.setStatut(Evenement.StatutEvenement.BROUILLON);
        event.setOrganisateur(organisateur);

        java.util.List<TicketCategorie> categories = new java.util.ArrayList<>();

        TicketCategorie standard = new TicketCategorie();
        standard.setNom("Standard");
        standard.setPrix(15000.0);
        standard.setQuantiteTotale(120);
        standard.setQuantiteVendue(0);
        standard.setQuantiteDisponible(120);
        categories.add(standard);

        TicketCategorie vip = new TicketCategorie();
        vip.setNom("VIP");
        vip.setPrix(35000.0);
        vip.setQuantiteTotale(35);
        vip.setQuantiteVendue(0);
        vip.setQuantiteDisponible(35);
        categories.add(vip);

        event.setCategories(categories);

        Evenement cree = evenementService.creerEvenement(event);
        evenementService.publierEvenement(cree.getId());
    }
    
    private void creerGerant() {
        if (!personneDao.emailExiste("admin@event.com")) {
            Gerant admin = new Gerant();
            admin.setNom("System");
            admin.setPrenom("Admin");
            admin.setEmail("admin@event.com");
            admin.setTelephone("+33612345678");
            admin.setMotDePasse(PasswordUtils.hashPassword("admin123"));
            admin.setRole(Role.GERANT);
            admin.setActif(true);
            admin.setDateInscription(new java.util.Date());
            personneDao.enregistrer(admin);
            System.out.println("✓ Gérant créé: admin@event.com");
        } else {
            System.out.println("✓ Gérant existe déjà");
        }
    }
    
    private void creerUtilisateursTest() {
        // 1 Organisateur PHYSIQUE
        creerOrganisateurPhysique("KAO", "Jean", "orga1@event.com");
        
        // 2 Organisateurs MORALE
        creerOrganisateurMorale("TechEvents", "Lucas", "lucas@techevents.com", 
                                "75001234567890", "Informatique", "123 Rue de la Tech, 75001 Paris",
                                "Jean", "Dupont");
        creerOrganisateurMorale("EventPro Solutions", "Marie", "marie@eventpro.com",
                                "13004567890123", "Événementiel", "456 Avenue des Événements, 13000 Marseille",
                                "Sophie", "Martin");
        
        // 5 Clients
        creerClient("Dubois", "Marie", "client1@event.com");
        creerClient("Leroy", "Thomas", "client2@event.com");
        creerClient("Moreau", "Julie", "client3@event.com");
        creerClient("Simon", "Lucas", "client4@event.com");
        creerClient("Laurent", "Emma", "client5@event.com");
    }
    
    private void creerOrganisateurPhysique(String nom, String prenom, String email) {
        if (!personneDao.emailExiste(email)) {
            Organisateur orga = new Organisateur();
            orga.setNom(nom);
            orga.setPrenom(prenom);
            orga.setEmail(email);
            orga.setTelephone("+33" + (600000000 + (int)(Math.random() * 90000000)));
            orga.setMotDePasse(PasswordUtils.hashPassword("password123"));
            orga.setRole(Role.ORGANISATEUR);
            orga.setTypeOrganisation("PHYSIQUE");
            orga.setActif(true);
            orga.setDateInscription(new java.util.Date());
            personneDao.enregistrer(orga);
            System.out.println("✓ Organisateur PHYSIQUE créé: " + prenom + " " + nom);
        }
    }
    
    private void creerOrganisateurMorale(String nomEntreprise, String prenomRep, String email,
                                        String siret, String secteur, String adresse,
                                        String prenomRepresentant, String nomRepresentant) {
        if (!personneDao.emailExiste(email)) {
            Organisateur orga = new Organisateur();
            orga.setNom(nomEntreprise);
            orga.setPrenom(prenomRep);
            orga.setEmail(email);
            orga.setTelephone("+33" + (600000000 + (int)(Math.random() * 90000000)));
            orga.setMotDePasse(PasswordUtils.hashPassword("password123"));
            orga.setRole(Role.ORGANISATEUR);
            orga.setTypeOrganisation("MORALE");
            orga.setNomEntreprise(nomEntreprise);
            orga.setNumeroSIRET(siret);
            orga.setSecteurActivite(secteur);
            orga.setAdresseSiege(adresse);
            orga.setSiteWeb("https://www." + nomEntreprise.toLowerCase().replace(" ", "").replace("é", "e") + ".com");
            orga.setPrenomRepresentant(prenomRepresentant);
            orga.setNomRepresentant(nomRepresentant);
            orga.setActif(true);
            orga.setDateInscription(new java.util.Date());
            personneDao.enregistrer(orga);
            System.out.println("✓ Organisateur MORALE créé: " + nomEntreprise);
        }
    }
    
    private void creerClient(String nom, String prenom, String email) {
        if (!personneDao.emailExiste(email)) {
            Client client = new Client();
            client.setNom(nom);
            client.setPrenom(prenom);
            client.setEmail(email);
            client.setTelephone("+33" + (600000000 + (int)(Math.random() * 90000000)));
            client.setMotDePasse(PasswordUtils.hashPassword("password123"));
            client.setRole(Role.CLIENT);
            client.setActif(true);
            client.setDateInscription(new java.util.Date());
            personneDao.enregistrer(client);
            System.out.println("✓ Client créé: " + prenom + " " + nom);
        }
    }
    
    private void creerEmployes() {
        // Récupérer les organisateurs MORALE
        java.util.List<Organisateur> organisateursMorale = new java.util.ArrayList<>();
        java.util.List<Organisateur> tousOrgansateurs = (java.util.List<Organisateur>) 
            (java.util.List<?>) personneDao.findByRole(Role.ORGANISATEUR);
        
        for (Organisateur orga : tousOrgansateurs) {
            if ("MORALE".equals(orga.getTypeOrganisation())) {
                organisateursMorale.add(orga);
            }
        }
        
        // Créer des employés pour chaque organisateur MORALE
        for (int i = 0; i < organisateursMorale.size(); i++) {
            Organisateur orga = organisateursMorale.get(i);
            
            // 2 employés par organisateur MORALE
            creerEmploye("Employé" + (i + 1), "Tech", 
                        "emp" + (i + 1) + "@" + orga.getEmail().split("@")[1], 
                        orga);
            creerEmploye("Responsable" + (i + 1), "Événement", 
                        "resp" + (i + 1) + "@" + orga.getEmail().split("@")[1], 
                        orga);
        }
    }
    
    private void creerEmploye(String nom, String prenom, String email, Organisateur employeur) {
        if (!personneDao.emailExiste(email)) {
            Employe employe = new Employe();
            employe.setNom(nom);
            employe.setPrenom(prenom);
            employe.setEmail(email);
            employe.setTelephone("+33" + (600000000 + (int)(Math.random() * 90000000)));
            employe.setMotDePasse(PasswordUtils.hashPassword("password123"));
            employe.setRole(Role.EMPLOYE);
            employe.setEmployeur(employeur);
            employe.setActif(true);
            employe.setDateInscription(new java.util.Date());
            personneDao.enregistrer(employe);
            System.out.println("✓ Employé créé: " + prenom + " " + nom + " pour " + employeur.getNomEntreprise());
        }
    }
    
    private void verifierDonnees() {
        try {
            long nbOrga = personneDao.findByRole(Role.ORGANISATEUR).size();
            long nbClient = personneDao.findByRole(Role.CLIENT).size();
            long nbGerant = personneDao.findByRole(Role.GERANT).size();
            long nbEmploye = personneDao.findByRole(Role.EMPLOYE).size();
            
            System.out.println("=== VÉRIFICATION BASE DE DONNÉES ===");
            System.out.println("Gérants: " + nbGerant);
            System.out.println("Organisateurs: " + nbOrga);
            System.out.println("Clients: " + nbClient);
            System.out.println("Employés: " + nbEmploye);
            System.out.println("TOTAL: " + (nbGerant + nbOrga + nbClient + nbEmploye));
        } catch (Exception e) {
            System.err.println("Erreur vérification: " + e.getMessage());
        }
    }
}