# EventPlatform - Plateforme de Gestion d'Événements (GLSI)

## 📋 Description du Projet

EventPlatform est une application web de gestion d'événements développée avec **Jakarta EE 10** et **PrimeFaces 15.0.13**. Cette plateforme permet la gestion complète des utilisateurs, événements et billets avec une architecture multi-rôles (Gérant, Organisateur, Client, Employé). L'interface propose un design professionnel avec thème EventPlatform Blue, des profils personnalisés pour chaque rôle et un système d'inscription flexible (Personne Physique ou Morale).

## 🛠️ Prérequis Techniques

### Environnement de Développement

- **JDK** : OpenJDK 11+ (recommandé : JDK 17 ou 21)
- **IDE** : NetBeans 21+ ou IntelliJ IDEA Ultimate
- **Serveur d'Application** : GlassFish 7.x
- **Base de Données** : PostgreSQL 13+
- **Build Tool** : Maven 3.8+

### Technologies Utilisées

- **Jakarta EE 10.0.0** (CDI, JPA, JSF, EJB)
- **PrimeFaces 15.0.13** (Jakarta)
- **Lombok 1.18.44** (Annotations)
- **EclipseLink** (JPA Provider)
- **SweetAlert2 11.x** + **PrimeIcons** (UI/UX)
- **CSS personnalisé** avec thème EventPlatform Blue (#0047FF)

## 🗄️ Configuration de la Base de Données

### 1. Création de la Base de Données PostgreSQL

```sql
-- Connexion en tant que postgres
psql -U postgres

-- Création de la base de données
CREATE DATABASE event_platform;

-- Création de l'utilisateur dédié
CREATE USER eventuser WITH PASSWORD 'eventpass123';
GRANT ALL PRIVILEGES ON DATABASE event_platform TO eventuser;

-- Vérification
\c event_platform;
\dt;
```

### 2. Configuration du Pool de Connexions GlassFish

#### Étape 1 : Créer le Pool de Connexions JDBC

1. Accédez à la console d'administration GlassFish : `http://localhost:4848`
2. Naviguez vers **Resources > JDBC > JDBC Connection Pools**
3. Cliquez sur **New** et configurez :

```
Pool Name: EventPool
Resource Type: javax.sql.DataSource
Database Driver Vendor: PostgreSQL
```

#### Étape 2 : Propriétés du Pool

Ajoutez les propriétés suivantes :

```
serverName: localhost
portNumber: 5432
databaseName: event_platform
user: eventuser
password: eventpass123
```

#### Étape 3 : Créer la Ressource JNDI

1. Naviguez vers **Resources > JDBC > JDBC Resources**
2. Cliquez sur **New** et configurez :

```
JNDI Name: jdbc/jpa
Pool Name: EventPool
```

#### Étape 4 : Test de Connexion

- Retournez au pool **EventPool**
- Cliquez sur **Ping** pour tester la connexion

## 🚀 Instructions de Lancement

## 🐳 Lancement avec Docker

### Prérequis

- Docker Desktop installé et démarré

### Démarrage rapide

```bash
docker compose up --build -d
```

### Vérification

```bash
docker compose ps
docker compose logs app --tail 100
```

### URLs

- Application : http://localhost:8080/eventProjectGlsi
- Login : http://localhost:8080/eventProjectGlsi/login.xhtml
- Admin Payara : http://localhost:4848

### Arrêt

```bash
docker compose down
```

### Réinitialiser la base PostgreSQL

```bash
docker compose down -v
```

### 1. Clonage du Projet

```bash
# Cloner le repository
git clone [URL_DU_REPOSITORY]
cd eventProjectGlsi

# Vérifier la structure
ls -la
```

### 2. Configuration dans NetBeans

#### Import du Projet

1. Ouvrez NetBeans
2. **File > Open Project**
3. Sélectionnez le dossier `eventProjectGlsi`
4. Cliquez sur **Open Project**

#### Configuration du Serveur

1. Clic droit sur le projet > **Properties**
2. **Run** > Server : Sélectionnez **GlassFish Server**
3. **Apply** et **OK**

### 3. Build et Déploiement

```bash
# Via NetBeans (Recommandé)
# Clic droit sur le projet > Clean and Build
# Puis : Run Project (F6)

# Via Maven (Alternative)
mvn clean compile
mvn package
```

### 4. Déploiement Manuel (si nécessaire)

1. Copiez le fichier `target/eventProjectGlsi-1.0-SNAPSHOT.war`
2. Dans GlassFish Admin Console : **Applications > Deploy**
3. Sélectionnez le fichier WAR et déployez

### 5. Accès à l'Application

```
URL: http://localhost:8080/eventProjectGlsi-1.0-SNAPSHOT
```

**Page de Connexion** : `http://localhost:8080/eventProjectGlsi-1.0-SNAPSHOT/login.xhtml`

## 👤 Comptes par Défaut

### Gérant (Administrateur)

```
Email: admin@event.com
Mot de passe: admin123
Rôle: GERANT
```

### Comptes de Test

- **Organisateurs (Personne Physique)** : `orga1@event.com`, `orga2@event.com`
- **Organisateurs (Personne Morale)** : `lucas@techevents.com`
- **Clients** : `client1@event.com`, `client2@event.com`
- **Mot de passe universel** : `password123`

### Inscription

- **Page d'Inscription** : `http://localhost:8080/eventProjectGlsi-1.0-SNAPSHOT/register.xhtml`
- **Deux types de compte** : Client ou Organisateur
- **Pour Organisateur** : Choix entre Personne Physique ou Personne Morale avec champs d'entreprise optionnels

## 🏗️ Structure du Projet

```
src/main/java/com/mycompany/
├── entities/                    # Entités JPA
│   ├── Personne.java           # Classe parent pour tous les utilisateurs
│   ├── Client.java             # Clients
│   ├── Employe.java            # Employés
│   └── Organisateur.java       # Organisateurs (Physique/Morale)
├── dao/                        # Data Access Objects
│   ├── PersonneDao.java        # Interface DAO
│   └── PersonneDaoImpl.java     # Implémentation DAO
├── service/                    # Services métier
│   └── PersonneService.java    # Logique métier
├── controller/                 # ManagedBeans JSF
│   ├── AuthController.java     # Authentification
│   ├── RegisterController.java # Inscription (Client/Organisateur)
│   ├── ClientProfileController.java          # Profil Client
│   ├── OrganizerProfileController.java       # Profil Organisateur
│   ├── AdminProfileController.java           # Profil Gérant
│   ├── UserAdminController.java              # Gestion des utilisateurs
│   └── *.java                  # Autres contrôleurs
└── utils/                      # Utilitaires
    └── SecurityHelper.java     # Gestion de la sécurité

src/main/webapp/
├── WEB-INF/
│   ├── template.xhtml              # Template principal (avec sidebar)
│   ├── public_template.xhtml       # Template public (login/register)
│   ├── beans.xml                   # Configuration CDI
│   ├── web.xml                     # Configuration web
│   └── glassfish-web.xml          # Configuration GlassFish
├── index.xhtml                     # Page d'accueil
├── login.xhtml                     # Page de connexion
├── register.xhtml                  # Page d'inscription (onglets Client/Organisateur)
├── dashboard_home.xhtml            # Accueil du tableau de bord
├── dashboard_client.xhtml          # Tableau de bord Client
├── dashboard_orga.xhtml            # Tableau de bord Organisateur
├── client_settings.xhtml           # Profil Client
├── organizer_settings.xhtml        # Profil Organisateur
├── admin_settings.xhtml            # Profil Gérant
├── users_management.xhtml          # Gestion des utilisateurs (Admin)
├── users_management_orga.xhtml     # Gestion des utilisateurs (Organisateur)
├── clients_management.xhtml        # Gestion des clients
├── manage_employees.xhtml          # Gestion des employés
├── organisateurs_management.xhtml  # Gestion des organisateurs
└── resources/css/                  # Feuilles de style personnalisées

src/main/resources/
└── META-INF/
    └── persistence.xml    # Configuration JPA (unitName: EventPU)
```

### Architecture en Couches

1. **Entities** : Modèles de données avec annotations JPA
2. **DAO** : Accès aux données avec interface et implémentation
3. **Service** : Logique métier et transactions
4. **Controller** : ManagedBeans pour l'interface utilisateur

### Bonnes Pratiques

1. **Templates** :
   - Pages authentifiées : `template="/WEB-INF/template.xhtml"` (avec sidebar)
   - Pages publiques : `template="/WEB-INF/public_template.xhtml"` (sans sidebar)

2. **Sécurité** :
   - Vérifiez les rôles avec `SecurityHelper.getRoleUtilisateur()`
   - Rôles disponibles : CLIENT, ORGANISATEUR, GERANT, EMPLOYE
   - Sessions sécurisées avec verification de connexion

3. **Formules CSS** :
   - Couleur primaire : `#0047FF` (EventPlatform Blue)
   - Couleur secondaire : `#0039CC` (EventPlatform Blue Dark)
   - Arrière-plan : `#f4f7f9`
   - Utiliser les variables CSS : `var(--eventplatform-blue)`

4. **Styles de Composants** :
   - Formulaires : `styleClass="form-input"`
   - Boutons primaires : `styleClass="btn btn-primary"`
   - Sections : `styleClass="profile-section"`

5. **Modals et Dialogs** :
   - Toujours utiliser `update=":mainForm"` pour rafraîchir après action
   - Utiliser PrimeFaces 15 pour les dialogs modernes

6. **Validation Backend** :
   - Synchroniser frontend et backend pour les champs obligatoires
   - Exemple: Champs Client/Organisateur (nom, prenom, email, telephone) obligatoires

7. **Messages et Notifications** :
   - Utiliser SweetAlert2 pour les confirmations
   - Utiliser `p:messages` pour les erreurs de formulaire

## 🐛 Gestion des Erreurs Fréquentes

### ViewExpiredException

**Symptôme** : Session expirée, page blanche

**Solution** :

```bash
# Nettoyer le projet
rm -rf target/
mvn clean compile

# Dans NetBeans
# Clic droit > Clean and Build
```

### Erreurs de Déploiement

**Symptôme** : Erreur au déploiement sur GlassFish

**Solutions** :

1. Vérifiez que GlassFish est démarré
2. Undeploy l'ancienne version
3. Redémarrez GlassFish si nécessaire

```bash
# Redémarrer GlassFish
asadmin stop-domain domain1
asadmin start-domain domain1
```

### Problèmes de Base de Données

**Symptôme** : Erreur de connexion JDBC

**Solutions** :

1. Vérifiez que MySQL est démarré
2. Testez la connexion dans GlassFish Admin Console
3. Vérifiez les credentials dans le pool JDBC

### Erreurs de Compilation Lombok

**Symptôme** : Getters/Setters non reconnus

**Solution** :

```bash
# Installer Lombok dans l'IDE
# NetBeans : Tools > Plugins > Available Plugins > Lombok
# Redémarrer l'IDE
```

## 📚 Ressources Utiles

- [Documentation Jakarta EE 10](https://jakarta.ee/specifications/platform/10/)
- [PrimeFaces Showcase](https://www.primefaces.org/showcase/)
- [GlassFish Documentation](https://eclipse-ee4j.github.io/glassfish/)
- [MySQL Documentation](https://dev.mysql.com/doc/)

## 🤝 Contribution

1. Créez une branche pour votre fonctionnalité
2. Respectez l'architecture existante
3. Testez vos modifications
4. Documentez les nouvelles fonctionnalités

## 📄 Licence

Ce projet est développé dans le cadre académique - GLSI.

---

## 🎨 Design et UX

### Thème EventPlatform

- **Couleur Primaire** : #0047FF (EventPlatform Blue)
- **Couleur Secondaire** : #0039CC (Dark Blue)
- **Arrière-plan** : #f4f7f9 (Light Gray)
- **Icônes** : PrimeIcons intégrées dans l'interface

### Alternative: Tailwind CSS et Font Awesome

Le projet utilise actuellement **CSS personnalisé + PrimeIcons**. Pour une approche alternative plus moderne, vous pouvez utiliser:

#### 🎯 Tailwind CSS

```html
<!-- Ajouter dans WEB-INF/public_template.xhtml ou WEB-INF/template.xhtml -->
<link href="https://cdn.tailwindcss.com" rel="stylesheet" />
```

**Documentation** : https://tailwindcss.com/docs

**Exemple d'utilisation** :

```html
<div class="flex items-center justify-between p-6 bg-blue-50 rounded-lg">
  <h1 class="text-2xl font-bold text-gray-900">Mon Profil</h1>
  <button class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700">
    Modifier
  </button>
</div>
```

#### 🎯 Font Awesome 6

```html
<!-- Ajouter dans WEB-INF/public_template.xhtml ou WEB-INF/template.xhtml -->
<link
  rel="stylesheet"
  href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css"
/>
```

**Documentation** : https://fontawesome.com/docs/web/setup/get-started

**Exemple d'utilisation** :

```html
<!-- Remplacement des icônes PrimeIcons -->
<i class="fas fa-user"></i>
<!-- User icon -->
<i class="fas fa-pencil"></i>
<!-- Edit/Pencil icon -->
<i class="fas fa-lock"></i>
<!-- Lock icon -->
<i class="fas fa-key"></i>
<!-- Key icon -->
<i class="fas fa-building"></i>
<!-- Building icon -->
<i class="fas fa-briefcase"></i>
<!-- Briefcase/Organization icon -->
```

**Comparaison avec PrimeIcons** :
| Besoin | PrimeIcons | Font Awesome |
|--------|-----------|--------------|
| Icons disponibles | ~500 | 6000+ |
| Intégration | Native | CDN |
| Animations | Basiques | Avancées (spin, bounce, fade) |
| Performance | Légère | Moyenne |
| Taille du bundle | Incluse dans PrimeFaces | ~100KB |

### Pages de Profil

Chaque rôle dispose d'une page de profil personnalisée avec :

- **Section Informations Personnelles** : Affichage et modification des données
- **Section Sécurité** : Changement de mot de passe
- **Modals Responsifs** : Pour éditer profil et changer mot de passe

### Page d'Inscription

- **Onglets** : Client vs Organisateur
- **Validation Frontend et Backend** : Synchronisée
- **Champs Dynamiques** : Modale "Personne Morale" conditionnelle pour Organisateur
- **Design Responsive** : Mobile-friendly avec grid CSS

## 📋 Fonctionnalités Principales

- ✅ **Authentification Sécurisée** : Login/Register avec contrôle de rôles
- ✅ **Gestion Multi-Rôles** : Client, Organisateur (Physique/Morale), Gérant, Employé
- ✅ **Profils Personnalisés** : Affichage et édition selon le rôle
- ✅ **Inscription Flexible** : Choix de type d'organisateur
- ✅ **Interface Responsive** : Compatible desktop et mobile
- ✅ **Design Moderne** : Thème EventPlatform Blue avec PrimeFaces 15
- ✅ **Validation Complète** : Frontend et backend synchronisée

---

**Développé par** : GROUP1 (Semestre 6, POO Avancée, GLSI)

**Version** : 1.0-SNAPSHOT  
**Dernière mise à jour** : Mars 2026  
**Repository** : `glsib_eventPatform` (branch: `event`)
"# GLSI_B_2026_EventPlatform"
