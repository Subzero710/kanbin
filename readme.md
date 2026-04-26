# Kanbin Projest

## Prérequis
- Java + Maven installés

## Lancer l’application (Jetty)
Depuis la racine du projet :

```bash
mvn org.eclipse.jetty:jetty-maven-plugin:11.0.25:run
```

## Rapport JaCoCo (tests unitaires)
1) Générer les rapports :

```bash
mvn clean install
```

2) Ouvrir le rapport agrégé :
- `kanbin-projest-reporting/target/jacoco-aggregate/index.html`

## Rapport de tests de mutation (PIT)
Générer le rapport PIT (dans chaque module) :

```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

Puis ouvrir, pour chaque module :
- `kanbin-projest-app/target/pit-reports/**/index.html`
- `kanbin-projest-model/target/pit-reports/**/index.html`
- `kanbin-projest-repo-mem/target/pit-reports/**/index.html`

## Tests d’intégration (IT)
```bash
mvn clean verify
```

## Tests de Benchmark Éco-Conception

Le testbench pour les mesures d'éco-conception est un projet séparé et doit être exécuté indépendamment. Consultez le fichier `testbench/README.md` pour les instructions détaillées d'exécution.
