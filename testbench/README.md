Benchmark Éco-Conception - Kanbin Projest
Objectif
Ce module fige un scénario fonctionnel Selenium et produit un benchmark reproductible pour comparer avant et après une amélioration.
Le rôle de chaque brique :
KanbinEcoScenarioIT : décrit le scénario métier
EcoExtension : orchestre l'exécution JUnit/Selenium et la campagne de benchmark
Prometheus + cAdvisor : fournissent les métriques conteneur
EcoGatling : exécute une montée en charge
EcoBenchmarkAnalyzer : compare plusieurs runs
Scénario mesuré
Consultation du Board
Création colonne simple
Création colonne double
Création story
Modification story
Drag & Drop story
Réorganisation colonnes
Passage en Closed
Suppression
Exécution
Prérequis
Java 21+
Maven 3.9+
Docker + Docker Compose
Lancement standard
```bash
docker compose up -d --build
cd testbench
export POD_IP=127.0.0.1
mvn -Dtest=KanbinEcoScenarioIT test
```
Lancement en une commande
```bash
./run-eco-benchmark.sh
```
Résultats
Rapports EcoExtension
Les rapports EcoExtension sont générés sous :
```text
testbench/target/ecoconception/
```
JSON de benchmark
Le scénario exporte aussi un JSON sous :
```text
testbench/target/eco-benchmark-YYYY-MM-DD_HH-mm-ss.json
```
Contenu :
durée par action
mémoire JVM
CPU conteneur
mémoire moyenne/pic du conteneur
réseau RX/TX
Comparer plusieurs runs
```bash
cd testbench
mvn exec:java   -Dexec.mainClass="fr.uha.ensisa.gl.kanbin.eco.EcoBenchmarkAnalyzer"   -Dexec.args="target/eco-benchmark-run1.json target/eco-benchmark-run2.json"
```
Dashboards
Prometheus : http://localhost:9090
cAdvisor : http://localhost:8081
Grafana : http://localhost:3000