package entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Catégorie/tier de ticket pour un événement.
 */
@Entity
@Table(name = "ticket_categorie")
@Getter
@Setter
@ToString(exclude = {"evenement", "tickets"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class TicketCategorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Size(max = 80, message = "Le nom de la catégorie ne peut pas dépasser 80 caractères")
    @Column(nullable = false, length = 80)
    private String nom;

    @NotNull(message = "Le prix de la catégorie est obligatoire")
    @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
    @Column(nullable = false)
    private Double prix;

    @NotNull(message = "La quantité totale est obligatoire")
    @Min(value = 1, message = "La quantité totale doit être au moins 1")
    @Column(name = "quantite_totale", nullable = false)
    private Integer quantiteTotale;

    @Column(name = "quantite_vendue", nullable = false)
    private Integer quantiteVendue = 0;

    @Column(name = "quantite_disponible", nullable = false)
    private Integer quantiteDisponible;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_creation", nullable = false)
    private Date dateCreation;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_modification")
    private Date dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", nullable = false)
    private Evenement evenement;

    @OneToMany(mappedBy = "categorie", fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateCreation = new Date();
        if (quantiteVendue == null) {
            quantiteVendue = 0;
        }
        if (quantiteTotale == null) {
            quantiteTotale = 0;
        }
        quantiteDisponible = Math.max(0, quantiteTotale - quantiteVendue);
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = new Date();
        quantiteDisponible = Math.max(0, quantiteTotale - quantiteVendue);
    }

    public boolean hasStock(int quantite) {
        return quantite > 0 && quantiteDisponible != null && quantiteDisponible >= quantite;
    }
}