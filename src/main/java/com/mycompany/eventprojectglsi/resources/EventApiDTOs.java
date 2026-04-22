package com.mycompany.eventprojectglsi.resources;

import java.util.List;

/**
 * Data Transfer Objects for Event API
 * Centralized DTOs to avoid duplication and improve maintainability
 */
public class EventApiDTOs {

    // ==================== Request DTOs ====================
    public static class CreateEventRequest {
        public String titre;
        public String description;
        public String dateEvenement;
        public String lieu;
        public String imageBase64;
        public List<CategoryRequest> categories;
        public String email;
        public String password;
    }

    public static class UpdateEventRequest {
        public Long id;
        public String titre;
        public String description;
        public String dateEvenement;
        public String lieu;
        public String imageBase64;
        public List<CategoryRequest> categories;
        public String email;
        public String password;
    }

    public static class DeleteEventRequest {
        public String email;
        public String password;
    }

    public static class CategoryRequest {
        public String nom;
        public Double prix;
        public Integer quantite;
    }

    // ==================== Response DTOs ====================
    public static class CreateEventResponse {
        public Long id;
        public String titre;
        public String statut;
        public Double prixTicket;
        public Integer nombreTicketsTotal;
        public List<Long> categoryIds;
        public String message;
    }

    public static class UpdateEventResponse {
        public Long id;
        public String titre;
        public String statut;
        public Double prixTicket;
        public Integer nombreTicketsTotal;
        public List<Long> categoryIds;
        public String message;
    }

    public static class DeleteEventResponse {
        public boolean success;
        public String message;
        public Long eventId;
    }

    public static class ScanEventResponse {
        public String codeQr;
        public String statutTicket;
        public boolean peutEtreValide;
        public Long evenementId;
        public String evenementTitre;
        public String clientNom;
        public String categorie;
    }

    public static class ApiResponse {
        public boolean success;
        public String message;

        public static ApiResponse success(String message) {
            ApiResponse r = new ApiResponse();
            r.success = true;
            r.message = message;
            return r;
        }

        public static ApiResponse error(String message) {
            ApiResponse r = new ApiResponse();
            r.success = false;
            r.message = message;
            return r;
        }
    }
}
