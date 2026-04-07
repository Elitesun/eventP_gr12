-- Test events for orga1@event.com (ID=1 from DataInitializer)
-- Correct column names: motdepasse, typeorganisation
-- Creates PUBLIE events with tickets

-- Get orga1 ID (assume 1 from DataInitializer)
DO $$
DECLARE
    orga_id BIGINT;
BEGIN
    SELECT id INTO orga_id FROM personne WHERE email = 'orga1@event.com';
    
    IF orga_id IS NULL THEN
        RAISE EXCEPTION 'Organisateur orga1@event.com not found';
    END IF;
    
    -- Event 1: Concert Rock
    INSERT INTO evenement (titre, description, date_evenement, lieu, prix_ticket, nombre_tickets_total, tickets_vendus, tickets_disponibles, statut, organisateur_id)
    VALUES ('Concert Rock Épique', 'Venez vivre un concert rock inoubliable avec les meilleurs groupes locaux!', '2026-05-15 20:00:00', 'Zénith Paris', 45.00, 500, 0, 500, 'PUBLIE', orga_id);
    
    -- Event 2: Conférence Tech
    INSERT INTO evenement (titre, description, date_evenement, lieu, prix_ticket, nombre_tickets_total, tickets_vendus, tickets_disponibles, statut, organisateur_id)
    VALUES ('Conférence IA 2026', 'Découvrez les dernières avancées en Intelligence Artificielle.', '2026-06-10 18:00:00', 'Palais des Congrès', 120.00, 300, 0, 300, 'PUBLIE', orga_id);
    
    -- Event 3: Festival Food
    INSERT INTO evenement (titre, description, date_evenement, lieu, prix_ticket, nombre_tickets_total, tickets_vendus, tickets_disponibles, statut, organisateur_id)
    VALUES ('Festival Culinaire International', 'Dégustez des spécialités du monde entier!', '2026-04-20 12:00:00', 'Parc des Expositions', 25.00, 1000, 0, 1000, 'PUBLIE', orga_id);
    
    -- Event 4: Atelier Yoga
    INSERT INTO evenement (titre, description, date_evenement, lieu, prix_ticket, nombre_tickets_total, tickets_vendus, tickets_disponibles, statut, organisateur_id)
    VALUES ('Atelier Yoga & Bien-être', 'Séance de yoga guidée par des experts.', '2026-05-01 09:00:00', 'Centre Bien-être Paris', 35.00, 50, 0, 50, 'PUBLIE', orga_id);
    
    -- Event 5: Soirée Networking
    INSERT INTO evenement (titre, description, date_evenement, lieu, prix_ticket, nombre_tickets_total, tickets_vendus, tickets_disponibles, statut, organisateur_id)
    VALUES ('Soirée Networking GLSI', 'Rencontrez les professionnels de l''IT!', '2026-05-25 19:00:00', 'Espace GLSI', 20.00, 200, 0, 200, 'PUBLIE', orga_id);
    
    RAISE NOTICE 'Created 5 test events for orga_id: %', orga_id;
END $$;

-- Generate tickets for all new events
INSERT INTO ticket (code_qr, statut, evenement_id)
SELECT 
    'TKT-' || e.id || '-' || generate_series(1, e.nombre_tickets_total)::text || '-' || random()::text[1:8],
    'DISPONIBLE',
    e.id
FROM evenement e 
WHERE e.organisateur_id = (SELECT id FROM personne WHERE email = 'orga1@event.com')
  AND e.statut = 'PUBLIE';

-- Update counters (in case)
UPDATE evenement 
SET tickets_vendus = 0, tickets_disponibles = nombre_tickets_total 
WHERE organisateur_id = (SELECT id FROM personne WHERE email = 'orga1@event.com');

-- Verify
SELECT titre, date_evenement, prix_ticket, tickets_disponibles, statut FROM evenement WHERE organisateur_id = (SELECT id FROM personne WHERE email = 'orga1@event.com') ORDER BY date_evenement;
