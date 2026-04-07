package controller;

import entities.Ticket;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import service.TicketService;
import java.io.Serializable;

/**
 * Controller pour la gestion du scan et de la validation des tickets.
 */
@Named("scanBean")
@ViewScoped
public class ScanBean implements Serializable {

    @EJB
    private TicketService ticketService;

    @Inject
    private SecurityHelper securityHelper;

    private String codeQr;
    private Ticket ticket;
    private boolean ticketValide;
    private String messageErreur;
    private String debugInfo;

    @PostConstruct
    public void init() {
        debugInfo = "";
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc != null) {
                String paramCode = fc.getExternalContext()
                        .getRequestParameterMap()
                        .get("code");

                debugInfo = "Param=[" + paramCode + "] ";

                if (paramCode != null && !paramCode.trim().isEmpty()) {
                    this.codeQr = paramCode.trim();
                    chargerTicket();
                } else {
                    debugInfo += "Aucun code dans URL. ";
                }
            }
        } catch (Exception e) {
            debugInfo += "ERR_INIT:" + e.getMessage();
            System.err.println("SCAN_BEAN ERR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void chargerTicket() {
        if (codeQr == null || codeQr.trim().isEmpty()) {
            messageErreur = "Veuillez saisir un code.";
            ticket = null;
            return;
        }

        try {
            ticket = ticketService.trouverTicketParCodeQr(codeQr.trim());

            if (ticket == null) {
                messageErreur = "Ticket introuvable pour : " + codeQr;
                ticketValide = false;
                debugInfo += "NOT_FOUND. ";
            } else {
                messageErreur = null;
                ticketValide = (ticket.getStatut() == Ticket.StatutTicket.VENDU);
                debugInfo += "OK statut=" + ticket.getStatut() + ". ";

                // Forcer le chargement des relations LAZY
                if (ticket.getEvenement() != null) {
                    ticket.getEvenement().getTitre();
                    ticket.getEvenement().getLieu();
                    ticket.getEvenement().getDateEvenement();
                }
                if (ticket.getClient() != null) {
                    ticket.getClient().getNom();
                    ticket.getClient().getPrenom();
                }
            }
        } catch (Exception e) {
            messageErreur = "Erreur: " + e.getMessage();
            ticket = null;
            debugInfo += "ERR:" + e.getMessage();
            System.err.println("SCAN_BEAN chargerTicket ERR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void validerTicket() {
        try {
            if (securityHelper == null || !securityHelper.isPersonnel()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Refusé", "Seul le personnel peut valider."));
                return;
            }
            if (ticket == null) return;

            boolean success = ticketService.validerTicket(codeQr);
            if (success) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Ticket validé !"));
                chargerTicket();
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Échec", "Ticket déjà utilisé ou invalide."));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage()));
        }
    }

    public boolean peutValider() {
        try {
            return securityHelper != null && securityHelper.isPersonnel() && ticketValide && ticket != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Getters / Setters
    public String getCodeQr() { return codeQr; }
    public void setCodeQr(String codeQr) { this.codeQr = codeQr; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public boolean isTicketValide() { return ticketValide; }
    public void setTicketValide(boolean v) { this.ticketValide = v; }
    public String getMessageErreur() { return messageErreur; }
    public void setMessageErreur(String m) { this.messageErreur = m; }
    public String getDebugInfo() { return debugInfo; }
    public void setDebugInfo(String d) { this.debugInfo = d; }
}
