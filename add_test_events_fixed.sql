-- Fixed test events (assume orga1 ID=1 after app redeploy/DataInitializer)
-- Creates 5 PUBLIE events + tickets

INSERT INTO evenement (titre, description, date_evenement, lieu, prix_ticket, nombre_tickets_total, tickets_vendus, tickets_disponibles, statut, organisateur_id) VALUES
('Concert Rock Épique', 'Concert rock inoubliable!', '2026-05-15 20:00:00', 'Zénith Paris', 45.00, 500, 0, 500, 'PUBLIE', 1),
('Conférence IA 2026', 'Avancées IA', '2026-06-10 18:00:00', 'Palais des Congrès', 120.00, 300, 0, 300, 'PUBLIE', 1),
('Festival Culinaire', 'Spécialités mondiales', '2026-04-20 12:00:00', 'Parc des Expositions', 25.00, 1000, 0, 1000, 'PUBLIE', 1),
('Atelier Yoga', 'Yoga & bien-être', '2026-05-01 09:00:00', 'Centre Bien-être', 35.00, 50, 0, 50, 'PUBLIE', 1),
('Networking GLSI', 'Rencontres IT', '2026-05-25 19:00:00', 'Espace GLSI', 20.00, 200, 0, 200, 'PUBLIE', 1)
ON CONFLICT DO NOTHING;

-- Generate tickets for these events
INSERT INTO ticket (code_qr, statut, evenement_id)
SELECT 
    'TKT-' || e.id || '-' || gs.num || '-' || md5(random()::text)[:8],
    'DISPONIBLE',
    e.id
FROM evenement e, generate_series(1, e.nombre_tickets_total) gs
WHERE e.statut = 'PUBLIE' AND e.organisateur_id = 1
ON CONFLICT DO NOTHING;

-- Verify
SELECT titre, date_evenement, prix_ticket, tickets_disponibles FROM evenement WHERE organisateur_id = 1 ORDER BY id DESC LIMIT 5;
