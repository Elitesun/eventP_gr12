package service;

import dao.EvenementDao;
import dao.TicketDao;
import dao.TicketCategorieDao;
import entities.Evenement;
import entities.Organisateur;
import entities.Ticket;
import entities.TicketCategorie;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.ArrayList;

/**
 * Service métier pour la gestion des événements
 */
@Stateless
@Transactional
public class EvenementService {

    @EJB
    private EvenementDao evenementDao;

    @EJB
    private TicketDao ticketDao;

    @EJB
    private TicketCategorieDao ticketCategorieDao;

    /**
     * Crée un nouvel événement avec ses tickets
     */
    public Evenement creerEvenement(Evenement evenement) {
        List<TicketCategorie> categories = evenement.getCategories() != null
            ? evenement.getCategories()
            : new ArrayList<>();

        if (!categories.isEmpty()) {
            double prixMin = Double.MAX_VALUE;
            int quantiteTotale = 0;
            for (TicketCategorie categorie : categories) {
                if (categorie.getNom() == null || categorie.getNom().trim().isEmpty()) {
                    throw new IllegalArgumentException("Chaque catégorie doit avoir un nom");
                }
                if (categorie.getPrix() == null || categorie.getPrix() < 0) {
                    throw new IllegalArgumentException("Le prix d'une catégorie ne peut pas être négatif");
                }
                if (categorie.getQuantiteTotale() == null || categorie.getQuantiteTotale() <= 0) {
                    throw new IllegalArgumentException("La quantité d'une catégorie doit être positive");
                }

                quantiteTotale += categorie.getQuantiteTotale();
                if (categorie.getPrix() < prixMin) {
                    prixMin = categorie.getPrix();
                }
                categorie.setEvenement(evenement);
            }

            evenement.setNombreTicketsTotal(quantiteTotale);
            evenement.setPrixTicket(prixMin == Double.MAX_VALUE ? 0.0 : prixMin);
            evenement.setTicketsVendus(0);
            evenement.setTicketsDisponibles(quantiteTotale);
        }

        // Validation métier
        if (evenement.getNombreTicketsTotal() <= 0) {
            throw new IllegalArgumentException("Le nombre de tickets doit être positif");
        }

        if (evenement.getPrixTicket() < 0) {
            throw new IllegalArgumentException("Le prix du ticket ne peut pas être négatif");
        }

        // Validate organisateur
        if (evenement.getOrganisateur() == null || evenement.getOrganisateur().getId() == null) {
            throw new IllegalArgumentException("Organisateur invalide pour création d'événement (ID null)");
        }

        System.out.println("[EvenementService] Creating event for organisateur.id=" + evenement.getOrganisateur().getId() + ", tickets=" + evenement.getNombreTicketsTotal());

        // Create event
        try {
            evenementDao.creer(evenement);
            evenementDao.flush(); // Garantir la génération de l'ID
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la persistance de l'événement: " + e.getMessage(), e);
        }

        // Generate tickets
        try {
            genererTicketsPourEvenement(evenement);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération des tickets: " + e.getMessage(), e);
        }

        return evenement;
    }

    /**
     * Met à jour un événement
     */
    public Evenement mettreAJourEvenement(Evenement evenement) {
        Evenement evenementExistant = evenementDao.trouverParIdAvecCategories(evenement.getId());
        if (evenementExistant == null) {
            throw new IllegalArgumentException("Événement introuvable");
        }

        // Règle métier demandée: modification possible uniquement avant la première vente
        if (ticketDao.compterTicketsVendus(evenementExistant.getId()) > 0) {
            throw new IllegalStateException("Cet événement ne peut plus être modifié après la première vente");
        }

        if (!evenementExistant.peutEtreModifie()) {
            throw new IllegalStateException("Cet événement ne peut plus être modifié");
        }

        // Copier les champs modifiables
        evenementExistant.setTitre(evenement.getTitre());
        evenementExistant.setDescription(evenement.getDescription());
        evenementExistant.setDateEvenement(evenement.getDateEvenement());
        evenementExistant.setLieu(evenement.getLieu());
        evenementExistant.setImageUrl(evenement.getImageUrl());
        evenementExistant.setStatut(evenement.getStatut());

        List<TicketCategorie> categories = evenement.getCategories() != null
            ? evenement.getCategories()
            : new ArrayList<>();

        if (!categories.isEmpty()) {
            // Supprimer les tickets existants et régénérer selon les nouvelles catégories
            List<Ticket> ticketsExistants = ticketDao.trouverParEvenement(evenementExistant);
            for (Ticket ticket : ticketsExistants) {
                ticketDao.supprimer(ticket);
            }

            evenementExistant.getCategories().clear();

            double prixMin = Double.MAX_VALUE;
            int quantiteTotale = 0;
            for (TicketCategorie categorie : categories) {
                if (categorie.getNom() == null || categorie.getNom().trim().isEmpty()) {
                    continue;
                }
                if (categorie.getQuantiteTotale() == null || categorie.getQuantiteTotale() <= 0) {
                    continue;
                }
                if (categorie.getPrix() == null || categorie.getPrix() < 0) {
                    continue;
                }

                TicketCategorie nouvelleCategorie = new TicketCategorie();
                nouvelleCategorie.setNom(categorie.getNom().trim());
                nouvelleCategorie.setPrix(categorie.getPrix());
                nouvelleCategorie.setQuantiteTotale(categorie.getQuantiteTotale());
                nouvelleCategorie.setQuantiteVendue(0);
                nouvelleCategorie.setQuantiteDisponible(categorie.getQuantiteTotale());
                nouvelleCategorie.setEvenement(evenementExistant);
                evenementExistant.getCategories().add(nouvelleCategorie);

                quantiteTotale += nouvelleCategorie.getQuantiteTotale();
                if (nouvelleCategorie.getPrix() < prixMin) {
                    prixMin = nouvelleCategorie.getPrix();
                }
            }

            if (evenementExistant.getCategories().isEmpty()) {
                throw new IllegalArgumentException("Au moins une catégorie de ticket valide est requise");
            }

            evenementExistant.setNombreTicketsTotal(quantiteTotale);
            evenementExistant.setPrixTicket(prixMin == Double.MAX_VALUE ? 0.0 : prixMin);
            evenementExistant.setTicketsVendus(0);
            evenementExistant.setTicketsDisponibles(quantiteTotale);
        } else {
            evenementExistant.setPrixTicket(evenement.getPrixTicket());
            evenementExistant.setNombreTicketsTotal(evenement.getNombreTicketsTotal());
            evenementExistant.setTicketsVendus(0);
            evenementExistant.setTicketsDisponibles(evenement.getNombreTicketsTotal());
        }

        Evenement updated = evenementDao.mettreAJour(evenementExistant);
        evenementDao.flush();

        // Régénérer les tickets selon la nouvelle configuration
        genererTicketsPourEvenement(updated);
        return updated;
    }

    public Evenement mettreAJourEvenement(Long organisateurId, Evenement evenement) {
        Evenement owned = trouverEvenementParIdEtOrganisateur(evenement.getId(), organisateurId);
        if (owned == null) {
            throw new IllegalStateException("Accès refusé: événement non autorisé");
        }
        evenement.setOrganisateur(owned.getOrganisateur());
        return mettreAJourEvenement(evenement);
    }

    /**
     * Publie un événement
     */
    public Evenement publierEvenement(Long evenementId) {
        Evenement evenement = evenementDao.trouverParId(evenementId);
        if (evenement == null) {
            throw new IllegalArgumentException("Événement non trouvé");
        }

        if (evenement.getStatut() != Evenement.StatutEvenement.BROUILLON) {
            throw new IllegalStateException("Seul un événement en brouillon peut être publié");
        }

        evenement.setStatut(Evenement.StatutEvenement.PUBLIE);
        return evenementDao.mettreAJour(evenement);
    }

    public Evenement publierEvenement(Long organisateurId, Long evenementId) {
        Evenement evenement = trouverEvenementParIdEtOrganisateur(evenementId, organisateurId);
        if (evenement == null) {
            throw new IllegalStateException("Accès refusé: événement non autorisé");
        }
        return publierEvenement(evenementId);
    }

    /**
     * Annule un événement
     */
    public Evenement annulerEvenement(Long evenementId) {
        Evenement evenement = evenementDao.trouverParId(evenementId);
        if (evenement == null) {
            throw new IllegalArgumentException("Événement non trouvé");
        }

        if (evenement.getStatut() == Evenement.StatutEvenement.TERMINE) {
            throw new IllegalStateException("Un événement terminé ne peut pas être annulé");
        }

        evenement.setStatut(Evenement.StatutEvenement.ANNULE);
        return evenementDao.mettreAJour(evenement);
    }

    public Evenement annulerEvenement(Long organisateurId, Long evenementId) {
        Evenement evenement = trouverEvenementParIdEtOrganisateur(evenementId, organisateurId);
        if (evenement == null) {
            throw new IllegalStateException("Accès refusé: événement non autorisé");
        }
        return annulerEvenement(evenementId);
    }

    /**
     * Supprime un événement
     */
    public void supprimerEvenement(Long evenementId) {
        Evenement evenement = evenementDao.trouverParId(evenementId);
        if (evenement == null) {
            throw new IllegalArgumentException("Événement non trouvé");
        }

        // Vérifier qu'il n'y a pas de tickets vendus
        Long ticketsVendus = ticketDao.compterTicketsVendus(evenementId);
        if (ticketsVendus > 0) {
            throw new IllegalStateException("Impossible de supprimer un événement avec des tickets vendus");
        }

        // Supprimer tous les tickets
        List<Ticket> tickets = ticketDao.trouverParEvenement(evenement);
        for (Ticket ticket : tickets) {
            ticketDao.supprimer(ticket);
        }

        // Supprimer l'événement
        evenementDao.supprimer(evenement);
    }

    public void supprimerEvenement(Long organisateurId, Long evenementId) {
        Evenement evenement = trouverEvenementParIdEtOrganisateur(evenementId, organisateurId);
        if (evenement == null) {
            throw new IllegalStateException("Accès refusé: événement non autorisé");
        }
        supprimerEvenement(evenementId);
    }

    /**
     * Récupère un événement par son ID
     */
    public Evenement trouverEvenementParId(Long id) {
        return evenementDao.trouverParId(id);
    }

    /**
     * Récupère tous les événements
     */
    public List<Evenement> trouverTousEvenements() {
        return evenementDao.trouverTous();
    }

    /**
     * Récupère les événements d'un organisateur
     */
    public List<Evenement> trouverEvenementsParOrganisateur(Organisateur organisateur) {
        return evenementDao.trouverParOrganisateur(organisateur);
    }

    public Evenement trouverEvenementAvecCategories(Long id) {
        return evenementDao.trouverParIdAvecCategories(id);
    }

    public Evenement trouverEvenementParIdEtOrganisateur(Long evenementId, Long organisateurId) {
        if (evenementId == null || organisateurId == null) {
            return null;
        }
        return evenementDao.trouverParIdEtOrganisateurId(evenementId, organisateurId);
    }

    public List<TicketCategorie> trouverCategoriesDisponibles(Long evenementId) {
        return ticketCategorieDao.trouverDisponiblesParEvenementId(evenementId);
    }

    /**
     * Récupère les événements publiés
     */
    public List<Evenement> trouverEvenementsPublies() {
        return evenementDao.trouverEvenementsPublies();
    }

    /**
     * Recherche des événements
     */
    public List<Evenement> rechercherEvenements(String terme) {
        return evenementDao.rechercher(terme);
    }

    /**
     * Génère les tickets pour un événement
     */
    private void genererTicketsPourEvenement(Evenement evenement) {
        int sequence = 1;

        if (evenement.getCategories() != null && !evenement.getCategories().isEmpty()) {
            for (TicketCategorie categorie : evenement.getCategories()) {
                for (int i = 0; i < categorie.getQuantiteTotale(); i++) {
                    Ticket ticket = new Ticket();
                    ticket.setEvenement(evenement);
                    ticket.setCategorie(categorie);
                    ticket.setCodeQr(Ticket.genererCodeQr(Long.valueOf(sequence++), evenement.getId()));
                    ticketDao.creer(ticket);
                }
            }
            return;
        }

        for (int i = 1; i <= evenement.getNombreTicketsTotal(); i++) {
            Ticket ticket = new Ticket();
            ticket.setEvenement(evenement);
            ticket.setCodeQr(Ticket.genererCodeQr(Long.valueOf(i), evenement.getId()));
            ticketDao.creer(ticket);
        }
    }
}