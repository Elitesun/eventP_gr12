package entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * Entité Achat
 * Représente un achat de tickets par un client
 */
@Entity
@Table(name = "achat")
@Getter
@Setter
@ToString(exclude = {"client", "evenement", "ticketCategorie", "tickets"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Achat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @DecimalMin(value = "0.0", message = "Le montant total ne peut pas être négatif")
    @Column(name = "montant_total", nullable = false)
    private Double montantTotal;

    @Min(value = 1, message = "Le nombre de tickets doit être au moins 1")
    @Column(name = "nombre_tickets", nullable = false)
    private Integer nombreTickets;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_achat", nullable = false)
    private Date dateAchat;

    @Enumerated(EnumType.STRING)
@Column(name = "statut_paiement", nullable = false)
    private StatutPaiement statutPaiement = StatutPaiement.EN_ATTENTE;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", nullable = false)
    private Evenement evenement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private TicketCategorie ticketCategorie;

    @OneToMany(mappedBy = "achat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    // Énumération pour le statut du paiement
    public enum StatutPaiement {
        EN_ATTENTE,
        PAYE,
        ANNULE,
        REMBOURSE
    }

    // Méthodes métier
    @PrePersist
    protected void onCreate() {
        dateAchat = new Date();
    }

    /**
     * Confirme le paiement
     */
    public void confirmerPaiement() {
        if (statutPaiement == StatutPaiement.EN_ATTENTE) {
            this.statutPaiement = StatutPaiement.PAYE;
            // Marquer tous les tickets comme vendus
            for (Ticket ticket : tickets) {
                ticket.marquerCommeVendu(client);
            }
        }
    }

    /**
     * Annule l'achat
     */
    public void annuler() {
        if (statutPaiement == StatutPaiement.EN_ATTENTE) {
            this.statutPaiement = StatutPaiement.ANNULE;
            // Remettre les tickets comme disponibles
            for (Ticket ticket : tickets) {
                ticket.annuler();
            }
        }
    }

    /**
     * Calcule le montant total basé sur les tickets
     */
    public void calculerMontantTotal() {
        if (nombreTickets == null) {
            return;
        }

        if (ticketCategorie != null && ticketCategorie.getPrix() != null) {
            this.montantTotal = ticketCategorie.getPrix() * nombreTickets;
            return;
        }

        if (evenement != null && evenement.getPrixTicket() != null) {
            this.montantTotal = evenement.getPrixTicket() * nombreTickets;
        }
    }
}