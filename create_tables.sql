-- Tables avec noms de colonnes selon la convention JPA EclipseLink
-- motDePasse -> motdepasse (pas de underscore)

-- Supprimer les tables existantes
DROP TABLE IF EXISTS gerant CASCADE;
DROP TABLE IF EXISTS employe CASCADE;
DROP TABLE IF EXISTS organisateur CASCADE;
DROP TABLE IF EXISTS client CASCADE;
DROP TABLE IF EXISTS personne CASCADE;

-- Table principale Personne
CREATE TABLE personne (
    id BIGSERIAL PRIMARY KEY,
    dtype VARCHAR(31) NOT NULL,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    telephone VARCHAR(20),
    motdepasse VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CLIENT', 'ORGANISATEUR', 'GERANT', 'EMPLOYE')),
    actif BOOLEAN DEFAULT true,
    date_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table Client
CREATE TABLE client (
    id BIGINT PRIMARY KEY REFERENCES personne(id) ON DELETE CASCADE
);

-- Table Organisateur
CREATE TABLE organisateur (
    id BIGINT PRIMARY KEY REFERENCES personne(id) ON DELETE CASCADE,
    typeorganisation VARCHAR(50),
    nomentreprise VARCHAR(200),
    numerosiret VARCHAR(20),
    secteuractivite VARCHAR(100),
    siteweb VARCHAR(100),
    adressesiege TEXT,
    prenomrepresentant VARCHAR(100),
    nomrepresentant VARCHAR(100)
);

-- Table Employe
CREATE TABLE employe (
    id BIGINT PRIMARY KEY REFERENCES personne(id) ON DELETE CASCADE,
    employeur_id BIGINT NOT NULL REFERENCES organisateur(id) ON DELETE CASCADE
);

-- Table Gerant
CREATE TABLE gerant (
    personne_id BIGINT PRIMARY KEY REFERENCES personne(id) ON DELETE CASCADE
);

-- Index pour les performances
CREATE INDEX idx_personne_email ON personne(email);
CREATE INDEX idx_personne_role ON personne(role);
CREATE INDEX idx_personne_dtype ON personne(dtype);

-- Table Evenement
CREATE TABLE evenement (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    date_evenement TIMESTAMP NOT NULL,
    lieu VARCHAR(300) NOT NULL,
    image_url VARCHAR(1024),
    prix_ticket DECIMAL(10,2) NOT NULL CHECK (prix_ticket >= 0),
    nombre_tickets_total INTEGER NOT NULL CHECK (nombre_tickets_total > 0),
    tickets_vendus INTEGER DEFAULT 0 CHECK (tickets_vendus >= 0),
    tickets_disponibles INTEGER NOT NULL CHECK (tickets_disponibles >= 0),
    statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON' CHECK (statut IN ('BROUILLON', 'PUBLIE', 'ANNULE', 'TERMINE')),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP,
    organisateur_id BIGINT NOT NULL REFERENCES organisateur(id) ON DELETE CASCADE
);

-- Table Catégories de Ticket (tiers)
CREATE TABLE ticket_categorie (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(80) NOT NULL,
    prix DECIMAL(10,2) NOT NULL CHECK (prix >= 0),
    quantite_totale INTEGER NOT NULL CHECK (quantite_totale > 0),
    quantite_vendue INTEGER NOT NULL DEFAULT 0 CHECK (quantite_vendue >= 0),
    quantite_disponible INTEGER NOT NULL CHECK (quantite_disponible >= 0),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP,
    evenement_id BIGINT NOT NULL REFERENCES evenement(id) ON DELETE CASCADE
);

-- Table Achat (créée avant Ticket pour éviter les dépendances circulaires)
CREATE TABLE achat (
    id BIGSERIAL PRIMARY KEY,
    montant_total DECIMAL(10,2) NOT NULL CHECK (montant_total >= 0),
    nombre_tickets INTEGER NOT NULL CHECK (nombre_tickets > 0),
    date_achat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut_paiement VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (statut_paiement IN ('EN_ATTENTE', 'PAYE', 'ANNULE', 'REMBOURSE')),
    client_id BIGINT NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    evenement_id BIGINT NOT NULL REFERENCES evenement(id) ON DELETE CASCADE,
    categorie_id BIGINT REFERENCES ticket_categorie(id) ON DELETE SET NULL
);

-- Table Ticket
CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    code_qr VARCHAR(500) NOT NULL UNIQUE,
    statut VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE' CHECK (statut IN ('DISPONIBLE', 'VENDU', 'UTILISE', 'ANNULE')),
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_achat TIMESTAMP,
    prix_vente DECIMAL(10,2),
    evenement_id BIGINT NOT NULL REFERENCES evenement(id) ON DELETE CASCADE,
    client_id BIGINT REFERENCES client(id) ON DELETE SET NULL,
    achat_id BIGINT REFERENCES achat(id) ON DELETE SET NULL,
    categorie_id BIGINT REFERENCES ticket_categorie(id) ON DELETE SET NULL
);

-- Index pour les performances sur les nouvelles tables
CREATE INDEX idx_evenement_organisateur ON evenement(organisateur_id);
CREATE INDEX idx_evenement_date ON evenement(date_evenement);
CREATE INDEX idx_evenement_statut ON evenement(statut);
CREATE INDEX idx_ticket_categorie_evenement ON ticket_categorie(evenement_id);
CREATE INDEX idx_ticket_evenement ON ticket(evenement_id);
CREATE INDEX idx_ticket_client ON ticket(client_id);
CREATE INDEX idx_ticket_code_qr ON ticket(code_qr);
CREATE INDEX idx_ticket_statut ON ticket(statut);
CREATE INDEX idx_ticket_categorie ON ticket(categorie_id);
CREATE INDEX idx_achat_client ON achat(client_id);
CREATE INDEX idx_achat_evenement ON achat(evenement_id);
CREATE INDEX idx_achat_statut ON achat(statut_paiement);
CREATE INDEX idx_achat_categorie ON achat(categorie_id);

-- Contraintes de cohérence
ALTER TABLE evenement ADD CONSTRAINT chk_tickets_coherence CHECK (tickets_disponibles = nombre_tickets_total - tickets_vendus);
ALTER TABLE ticket ADD CONSTRAINT chk_date_achat_coherence CHECK (
    (statut IN ('VENDU', 'UTILISE') AND date_achat IS NOT NULL) OR
    (statut NOT IN ('VENDU', 'UTILISE') AND date_achat IS NULL)
);