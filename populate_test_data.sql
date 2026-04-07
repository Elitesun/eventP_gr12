-- Create orga1 if missing
INSERT INTO personne (nom, prenom, email, telephone, motdepasse, role, actif, date_inscription) VALUES 
('Dupont', 'Jean', 'orga1@event.com', '+33612345678', '$2y$10$N3/OBGlPypgFM8C6MkAv0OlFJfVCbP4c4FJ4tCVwDhRyp7N3A7RBe', 'ORGANISATEUR', true, NOW())
ON CONFLICT (email) DO NOTHING;

INSERT INTO organisateur (id, typeorganisation) 
SELECT id, 'PHYSIQUE' FROM personne WHERE email = 'orga1@event.com' AND id NOT IN (SELECT id FROM organisateur)
ON CONFLICT DO NOTHING;

-- Update existing event to PUBLIE
UPDATE evenement SET statut = 'PUBLIE' WHERE titre = 'fgdfd';

-- Add 4 more events for orga1 (get ID first)
DO $$
DECLARE
    orga1_id BIGINT := (SELECT id FROM personne WHERE email = 'orga1@event.com');
BEGIN
    INSERT INTO evenement (titre, description, date_evenement, lieu, prix_ticket, nombre_tickets_total, tickets_vendus, tickets_disponibles, statut, organisateur_id) VALUES
    ('Concert Rock', 'Rock concert', '2026-05-15 20:00', 'Paris', 45, 100, 0, 100, 'PUBLIE', orga1_id),
    ('Conf IA', 'AI conf', '2026-06-10 18:00', 'Lyon', 120, 50, 0, 50, 'PUBLIE', orga1_id),
    ('Festival Food', 'Food festival', '2026-04-20 12:00', 'Nice', 25, 200, 0, 200, 'PUBLIE', orga1_id),
    ('Yoga Workshop', 'Yoga', '2026-05-01 09:00', 'Paris', 35, 30, 0, 30, 'PUBLIE', orga1_id);
END $$;

-- Generate tickets for all PUBLIE events
DELETE FROM ticket WHERE evenement_id IN (SELECT id FROM evenement WHERE statut = 'PUBLIE');
INSERT INTO ticket (code_qr, statut, evenement_id)
SELECT 'TKT-' || e.id || '-' || generate_series(1, e.nombre_tickets_total), 'DISPONIBLE', e.id
FROM evenement e WHERE statut = 'PUBLIE';

SELECT 'Events PUBLIE:' || count(*) FROM evenement WHERE statut = 'PUBLIE';
SELECT 'Tickets:' || count(*) FROM ticket;
