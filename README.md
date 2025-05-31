# Plateforme d'Annotation de Textes

Une plateforme web moderne pour l'annotation collaborative de textes, développée avec Spring Boot et Thymeleaf.

## 🌟 Fonctionnalités

- **Gestion des utilisateurs**
  - Authentification sécurisée
  - Rôles (Admin, Annotateur)
  - Gestion des profils utilisateurs
  - Changement de mot de passe sécurisé

- **Gestion des datasets**
  - Création et importation de datasets
  - Export des annotations en CSV
  - Suivi de la progression par dataset
  - Visualisation des statistiques

- **Annotation de textes**
  - Interface intuitive pour l'annotation
  - Catégorisation des textes
  - Validation des annotations
  - Historique des modifications

- **Tableau de bord**
  - Statistiques en temps réel
  - Vue d'ensemble des tâches
  - Suivi de la progression
  - Filtres et recherche avancée

## 🛠️ Technologies utilisées

- **Backend**
  - Java 17
  - Spring Boot 3.2.3
  - Spring Security
  - Spring Data JPA
  - Hibernate
  - PostgreSQL

- **Frontend**
  - Thymeleaf
  - Bootstrap 5.3.3
  - JavaScript
  - CSS3
  - Bootstrap Icons

## 🚀 Installation

1. **Prérequis**
   - Java 17 ou supérieur
   - Maven
   - MySQL

2. **Configuration de la base de données**
   ```sql
   CREATE DATABASE annotation_platform;
   ```

3. **Configuration de l'application**
   - Copier `application.properties.example` vers `application.properties`
   - Modifier les paramètres de connexion à la base de données
   - Configurer les paramètres SMTP pour les emails

4. **Installation**
   ```bash
   # Cloner le repository
   git clone https://github.com/votre-username/annotation-platform.git
   
   # Se déplacer dans le dossier
   cd annotation-platform
   
   # Compiler le projet
   mvn clean install
   
   # Lancer l'application
   mvn spring-boot:run
   ```

## 📝 Utilisation

1. **Accès à l'application**
   - URL : `http://localhost:8080`
   - Compte admin par défaut : admin/admin123
   - Compte annotateur par défaut : annotator/annotator123

2. **Rôles et permissions**
   - **Admin** : Gestion complète (utilisateurs, datasets, annotations)
   - **Annotateur** : Annotation de textes, gestion de profil

3. **Workflow d'annotation**
   - Sélection d'un dataset
   - Annotation des textes
   - Validation des annotations
   - Export des résultats

## 🔒 Sécurité

- Authentification sécurisée avec Spring Security
- Protection CSRF
- Validation des entrées
- Hachage des mots de passe
- Gestion des sessions

## 📊 Statistiques et rapports

- Progression par annotateur
- Statistiques par dataset
- Taux de complétion
- Qualité des annotations
- Export des données

## 🤝 Contribution

Les contributions sont les bienvenues ! Pour contribuer :

1. Fork le projet
2. Créer une branche (`git checkout -b feature/AmazingFeature`)
3. Commit les changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 👥 Auteurs

- SILUE GNINNINMAGUIGNON - Développement initial

## 🙏 Remerciements

- Spring Boot Team
- Bootstrap Team
- Tous les contributeurs

## 📞 Support

Pour toute question ou problème, veuillez :
- Ouvrir une issue sur GitHub
- Contacter l'équipe de support à dev225tech@gmail.com