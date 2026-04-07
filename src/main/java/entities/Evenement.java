package entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * Entité Événement
 * Représente un événement créé par un organisateur
 */
@Entity
@Table(name = "evenement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
    @Column(nullable = false, length = 200)
    private String titre;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    @Column(nullable = false, length = 2000)
    private String description;

    @NotNull(message = "La date est obligatoire")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_evenement", nullable = false)
    private Date dateEvenement;

    @NotBlank(message = "Le lieu est obligatoire")
    @Size(max = 300, message = "Le lieu ne peut pas dépasser 300 caractères")
    @Column(nullable = false, length = 300)
    private String lieu;

    @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
    @Column(name = "prix_ticket", nullable = false)
    private Double prixTicket;

    @Min(value = 1, message = "Le nombre de tickets doit être au moins 1")
    @Column(name = "nombre_tickets_total", nullable = false)
    private Integer nombreTicketsTotal;

    @Column(name = "tickets_vendus", nullable = false)
    private Integer ticketsVendus = 0;

    @Column(name = "tickets_disponibles", nullable = false)
    private Integer ticketsDisponibles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEvenement statut = StatutEvenement.BROUILLON;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_creation", nullable = false)
    private Date dateCreation;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_modification")
    private Date dateModification;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Organisateur organisateur;

    @OneToMany(mappedBy = "evenement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    // Énumération pour le statut
    public enum StatutEvenement {
        BROUILLON,
        PUBLIE,
        ANNULE,
        TERMINE
    }

    // Méthodes métier
@PrePersist
    protected void onCreate() {
        dateCreation = new Date();
        if (nombreTicketsTotal == null) {
            nombreTicketsTotal = 0;
        }
        ticketsDisponibles = nombreTicketsTotal;
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = new Date();
    }

    /**
     * Vérifie si des tickets sont encore disponibles
     */
    public boolean hasTicketsDisponibles() {
        return ticketsDisponibles > 0;
    }

    /**
     * Vérifie si l'événement peut être modifié
     */
    public boolean peutEtreModifie() {
        return statut == StatutEvenement.BROUILLON || statut == StatutEvenement.PUBLIE;
    }

    /**
     * Calcule le nombre de tickets restants
     */
    public void recalculerTicketsDisponibles() {
        this.ticketsDisponibles = this.nombreTicketsTotal - this.ticketsVendus;
    }
}