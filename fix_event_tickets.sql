-- Fix ticket coherence constraint issue
-- Run this as postgres user

ALTER TABLE evenement DROP CONSTRAINT IF EXISTS chk_tickets_coherence;
UPDATE evenement SET tickets_disponibles = nombre_tickets_total - tickets_vendus;
-- Optional: Add back constraint after fixing data
-- ALTER TABLE evenement ADD CONSTRAINT chk_tickets_coherence CHECK (tickets_disponibles = nombre_tickets_total - tickets_vendus);
