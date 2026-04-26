# Benchmark Éco-Conception - Kanbin Projest

## 📋 Scénario mesuré

Ce benchmark exécute un scénario complet d'utilisation de Kanbin Projest et mesure :

1. **Consultation du Board** - Chargement de la page principale
2. **Création colonne simple** - Ajout d'une colonne de base
3. **Création colonne double** - Ajout d'une colonne avec sous-colonnes
4. **Création story** - Création d'une nouvelle tâche
5. **Modification story** - Édition des détails de la tâche
6. **Drag & Drop story** - Déplacement d'une tâche entre colonnes
7. **Reordering colonnes** - Réorganisation des colonnes
8. **Passage en Closed** - Marquage d'une tâche comme complétée
9. **Suppression** - Suppression de story et colonne

## 🚀 Exécution

### Prérequis

- Java 21+
- Maven 3.9+
- Docker et Docker Compose (pour les mesures conteneurisées)
- Chrome/Chromium installé localement

### Mode conteneurisé (Docker avec monitoring complet)

```bash
# À la racine du projet

# Démarrer les services
docker compose up -d

# Attendre que l'application soit prête (~30s)
sleep 30

# Vérifier que tous les services sont up
docker compose ps

# Lancer le benchmark
cd testbench
mvn clean test
```

Accéder aux dashboards :
- Prometheus (métriques brutes) : http://localhost:9090
- Grafana (visualisation) : http://localhost:3000 (login: admin/admin)
- cAdvisor (détails conteneurs) : http://localhost:8081

```bash
# Arrêter les services
docker compose down
```