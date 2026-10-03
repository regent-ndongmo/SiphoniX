# Rapport d’implémentation — Démarrage conditionnel minimal de l’adaptation

**Projet :** SiphoniX  
**Date de mise à jour :** 14 septembre 2026
**Objectif :** attendre que le service distant soit prêt avant de démarrer les scénarios.

## 1. Décision

La solution a volontairement été ramenée au strict minimum.

Aucune modification n’est apportée :

- à l’API des plugins ;
- au `PluginContext` ;
- au registre ;
- au chargeur dynamique ;
- au plugin AdaptiFlow ;
- à la fabrique des scénarios ;
- aux fichiers de scénario YAML, JSON et XML.

Le runtime effectue simplement un contrôle HTTP entre l’initialisation des plugins et leur démarrage.

```text
chargement des JAR
    -> initializeAll()
    -> attente HTTP du service distant
    -> startAll()
    -> démarrage des scénarios et collectes
```

## 2. Constat corrigé

Avant la correction, `SiphoniX.main()` exécutait directement :

```java
pluginRegistry.initializeAll(context);
runtimeLoader.onRuntimeInitialized();
pluginRegistry.startAll();
```

Le plugin AdaptiFlow pouvait donc démarrer ses ordonnanceurs alors que Tomcat, Catalina ou l’application distante n’avaient pas fini de démarrer.

Après la correction :

```java
pluginRegistry.initializeAll(context);
runtimeLoader.onRuntimeInitialized();
awaitTargetService();
pluginRegistry.startAll();
```

Tant que le contrôle n’obtient pas un statut HTTP `2xx`, `startAll()` n’est pas appelé.

Le même contrôle est placé dans le mode commande avant son appel existant à `startAll()`, afin qu’aucun chemin actuel de démarrage global ne contourne l’attente.

## 3. Implémentation retenue

La classe `RemoteServiceReadinessChecker` utilise uniquement le client HTTP standard de Java 11.

Comportement :

- requête HTTP `GET` ;
- timeout de connexion : 2 secondes ;
- timeout de requête : 3 secondes ;
- succès : tout statut `200-299` ;
- échec réseau ou autre statut : nouvelle tentative après 1 seconde ;
- pendant l’attente, ces résultats sont journalisés au niveau `INFO` comme « service pas encore prêt », et non comme une erreur SiphoniX ;
- attente sans limite de durée ;
- interruption du thread respectée ;
- URL obligatoirement absolue en `http` ou `https`.

L’attente illimitée est cohérente avec l’objectif demandé : ne jamais lancer la logique d’adaptation tant que la cible n’est pas prête. Elle évite aussi que la JVM SiphoniX quitte seule pendant que Catalina continue son démarrage.

## 4. Endpoint requis

### Le service doit-il exposer un endpoint ?

Oui, le contrôle HTTP doit appeler une URL qui permet de conclure que le service est réellement prêt.

Il n’est pas obligatoire de créer un endpoint portant exactement le nom `/health` ou `/ready`. Un endpoint existant, non mutateur, peu coûteux et retournant `2xx` seulement lorsque l’application est utilisable convient.

Pour TeaStore, l’endpoint existant retenu est :

```text
/tools.descartes.teastore.image/rest/image/finished
```

Il répond :

- `2xx` lorsque l’initialisation de l’image TeaStore est terminée ;
- `5xx` pendant la phase où elle n’est pas encore prête.

Il n’est donc pas nécessaire d’ajouter un nouvel endpoint à TeaStore.

Une route qui retourne `404` ne convient pas : elle prouve seulement qu’un serveur HTTP répond, pas que la ressource attendue est prête.

## 5. Configuration et injection

Trois variables ont des rôles différents :

| Variable | Rôle |
|---|---|
| `TARGET_URL` | URL de base du service surveillé, exposée aux plugins par `PluginContext` |
| `SIPHONIX_READINESS_URL` | URL précise utilisée une fois par le runtime pour autoriser le démarrage |
| `SIPHONIX_WAIT_FOR_TARGET` | active ou désactive l'attente ; valeur par défaut `true` |

Les exemples Docker Compose injectent directement la variable dans le processus SiphoniX :

```yaml
environment:
  TARGET_URL: "http://image:8080/tools.descartes.teastore.image/rest"
  SIPHONIX_READINESS_URL: "http://localhost:8080/tools.descartes.teastore.image/rest/image/finished"
  SIPHONIX_WAIT_FOR_TARGET: "true"
```

L’exemple YAML utilise le port interne `8081` :

```yaml
SIPHONIX_READINESS_URL: "http://image:8081/tools.descartes.teastore.image/rest/image/finished"
```

Au lancement, Java lit la variable avec `System.getenv()`. Si `SIPHONIX_READINESS_URL` n’est pas définie, le fallback est :

```text
TARGET_URL + "/image/finished"
```

Avec la valeur TeaStore par défaut, cela produit automatiquement la bonne route.

La ligne de commande est prioritaire sur la variable d'environnement :

```bash
java -jar siphonix.jar --wait-for-target false
```

Les formes `--wait-for-target false` et `--wait-for-target=false` sont acceptées. Toute autre
valeur est rejetée. Quand l'option vaut `false`, les plugins démarrent immédiatement après leur
initialisation ; ils doivent alors gérer eux-mêmes l'indisponibilité éventuelle du service cible.

## 6. Pourquoi TARGET_URL n’est plus injectée dans les scénarios

La version précédente remplaçait récursivement `${TARGET_URL}` dans les paramètres YAML, JSON et XML. Cette modification touchait la fabrique AdaptiFlow, tous les exemples et plusieurs tests. Elle n’est pas nécessaire pour répondre au besoin de readiness.

Cette logique a donc été retirée.

Dans la solution minimale :

- les scénarios conservent leurs URL existantes ;
- `TARGET_URL` continue d’alimenter le contexte standard des plugins ;
- `SIPHONIX_READINESS_URL` ne sert qu’à la barrière de démarrage ;
- aucun mécanisme de substitution n’est ajouté au format des scénarios.

Cela réduit le changement et évite de coupler la résolution des configurations de scénario au contrôle de démarrage.

## 7. Ordre de démarrage obtenu

Dans les images d’exemple, le script du conteneur lance SiphoniX en arrière-plan, puis le script Tomcat/Catalina.

L’ordre temporel réel devient :

1. la JVM SiphoniX démarre ;
2. les JAR sont chargés et les plugins sont initialisés, sans appeler `start()` ;
3. SiphoniX tente l’endpoint TeaStore ;
4. Tomcat puis Catalina démarrent en parallèle ;
5. les premières tentatives peuvent échouer par refus de connexion, timeout ou HTTP `5xx` ;
6. TeaStore termine son initialisation et l’endpoint retourne `2xx` ;
7. SiphoniX appelle `pluginRegistry.startAll()` ;
8. le plugin AdaptiFlow démarre les scénarios et leurs collectes.

Ainsi, le fait que la JVM SiphoniX soit lancée avant Catalina n’est plus un problème : elle attend effectivement l’application.

## 8. Fichiers modifiés

Code :

- `siphonix-runtime/src/main/java/tools/spirals/cerberus237/siphonix/SiphoniX.java` ;
- `siphonix-runtime/src/main/java/tools/spirals/cerberus237/siphonix/RemoteServiceReadinessChecker.java` ;
- `siphonix-runtime/src/test/java/tools/spirals/cerberus237/siphonix/RemoteServiceReadinessCheckerTest.java`.

Configuration :

- `samples/adaptable-teastore-image/docker-compose.yml` ;
- `samples/adaptiflow-json-scenarios/docker-compose.yml` ;
- `samples/adaptiflow-xml-scenarios/docker-compose.yml` ;
- `samples/adaptiflow-yaml-scenarios/docker-compose.yml`.

Chaque fichier Compose reçoit une seule ligne : `SIPHONIX_READINESS_URL`.

## 9. Tests

Le test unitaire démarre un serveur HTTP local éphémère et vérifie trois cas :

- un statut `204` est accepté comme prêt ;
- un statut `503` est refusé comme non prêt ;
- après un `503`, l’attente effectue un retry puis s’arrête sur `204`.

La suite Maven complète confirme qu’aucune régression n’a été introduite dans les modules API, kernel, runtime et plugins.

## 10. Limites assumées

Cette correction est volontairement une barrière simple de démarrage.

Elle ne cherche pas à :

- vérifier plusieurs services ;
- appliquer un backoff exponentiel ;
- définir un nombre maximal de tentatives ;
- ajouter un nouvel état au runtime ou aux plugins ;
- revalider périodiquement la cible après le démarrage ;
- bloquer séparément chaque scénario créé dynamiquement ;
- remplacer les URL présentes dans les scénarios.

Si le service devient indisponible après le démarrage, les mécanismes actuels des collecteurs restent responsables de leurs erreurs. Si un futur besoin exige la gestion de plusieurs prérequis ou le chargement dynamique conditionnel, cette évolution devra être spécifiée séparément.

## 11. Conclusion

Le besoin est satisfait avec un changement local au bootstrap : une classe HTTP légère, un appel avant `startAll()`, un test ciblé et une variable dans chaque exemple Compose.

Le service distant doit fournir une URL de readiness exploitable, mais TeaStore possède déjà la route `/rest/image/finished`. SiphoniX n’ajoute rien au service distant ; il attend simplement que cette route retourne un statut `2xx` avant de démarrer les scénarios.
