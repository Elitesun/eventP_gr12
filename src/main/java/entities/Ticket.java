package entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.UUID;
import utils.QrCodeUtils;

/**
 * Entité Ticket
 * Représente un ticket pour un événement
 */
@Entity
@Table(name = "ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le code QR est obligatoire")
    @Size(max = 500, message = "Le code QR ne peut pas dépasser 500 caractères")
    @Column(name = "code_qr", nullable = false, unique = true, length = 500)
    private String codeQr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTicket statut = StatutTicket.DISPONIBLE;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_creation", nullable = false)
    private Date dateCreation;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_achat")
    private Date dateAchat;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", nullable = false)
    private Evenement evenement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achat_id")
    private Achat achat;

    @Transient
    private String qrCodeImage;

    // Énumération pour le statut du ticket
    public enum StatutTicket {
        DISPONIBLE,
        VENDU,
        UTILISE,
        ANNULE
    }

    // Méthodes métier
    @PrePersist
    protected void onCreate() {
        dateCreation = new Date();
    }

    /**
     * Vérifie si le ticket peut être acheté
     */
    public boolean peutEtreAchete() {
        return statut == StatutTicket.DISPONIBLE;
    }

    /**
     * Marque le ticket comme vendu
     */
    public void marquerCommeVendu(Client acheteur) {
        this.statut = StatutTicket.VENDU;
        this.client = acheteur;
        this.dateAchat = new Date();
    }

    /**
     * Marque le ticket comme utilisé
     */
    public void marquerCommeUtilise() {
        if (statut == StatutTicket.VENDU) {
            this.statut = StatutTicket.UTILISE;
        }
    }

    /**
     * Annule le ticket
     */
    public void annuler() {
        if (statut == StatutTicket.VENDU) {
            this.statut = StatutTicket.DISPONIBLE;
            this.client = null;
            this.dateAchat = null;
        }
    }

    /**
     * Réserve le ticket pour un achat (sans changer le statut)
     */
    public void reserverPourAchat(Achat achat) {
        this.achat = achat;
    }

    /**
     * Retourne l'image du QR Code pour affichage (data:image/png;base64,...)
     */
    /**
     * Retourne l'image du QR Code pour affichage (data:image/png;base64,...)
     */
    public String getQrCodeImage() {
        if (qrCodeImage == null && codeQr != null) {
            // On encode l'URL de scan complète pour que les téléphones l'ouvrent directement
            String scanUrl = utils.ApplicationConfig.getScanUrl(codeQr);
            qrCodeImage = QrCodeUtils.getQrCodeImageSrc(scanUrl, 250, 250);
        }
        return qrCodeImage;
    }

    /**
     * Génère un code QR unique et sécurisé pour ce ticket.
     * On garde une version courte pour stockage en base de données.
     */
    public static String genererCodeQr(Long ticketId, Long evenementId) {
        String evtPart = (evenementId != null) ? evenementId.toString() : "0";
        String tktPart = (ticketId != null) ? ticketId.toString() : UUID.randomUUID().toString().substring(0, 8);
        return "TICKET-" + evtPart + "-" + tktPart + "-" + UUID.randomUUID().toString();
    }
}