# Démonstration des risques liés au chargement de plugins

## 1. Objectif

Cette démonstration met en évidence deux risques du mécanisme de plugins de SiphoniX :

1. un plugin Java hérite des permissions du processus SiphoniX et peut accéder aux fichiers, aux processus et au réseau accessibles à ce processus ;
2. un plugin peut embarquer une version de dépendance incompatible avec celle du runtime, tandis que le classloader parent-first charge silencieusement la version du parent.

Le scénario est limité à une fixture jetable. Il ne contrôle aucun conteneur réel par l'API Docker, ne monte pas `/var/run/docker.sock` et ne contacte pas de service extérieur.

## 2. Architecture

```text
samples/siphonix-plugin-risk-demo/run.sh
  |
  +-- construit le runtime et les deux plugins
  +-- prépare une fixture jetable
  +-- lance Docker Compose
        |
        +-- target
        |     GET  /           -> readiness
        |     POST /admin/stop -> modification de target.status
        |
        +-- siphonix
              runtime SiphoniX
              + dependency-conflict-demo-plugin
              + security-demo-plugin
```

Les fichiers principaux sont :

- `siphonix-plugins/security-demo-plugin/` : preuve des permissions héritées ;
- `siphonix-plugins/dependency-conflict-demo-plugin/` : preuve du conflit de dépendance ;
- `samples/siphonix-plugin-risk-demo/docker-compose.yml` : environnement isolé ;
- `samples/siphonix-plugin-risk-demo/run.sh` : construction et exécution ;
- `samples/siphonix-plugin-risk-demo/fixture/` : données factices modifiées pendant le test.

## 3. Plugin de démonstration de sécurité

Classe principale :

```text
tools.spirals.cerberus237.siphonix.demo.security.SecurityDemoPlugin
```

Identifiant : `demo-security-plugin`.

### 3.1 Configuration

| Rôle | Variable d'environnement | Propriété Java | Valeur Docker |
|---|---|---|---|
| Racine jetable | `SIPHONIX_DEMO_ROOT` | `siphonix.demo.root` | `/demo` |
| Confirmation | `SIPHONIX_DEMO_CONFIRM` | `siphonix.demo.confirm` | `YES` |
| Cible HTTP | `SIPHONIX_DEMO_TARGET_URL` | `siphonix.demo.target-url` | `http://target:8080` |

La propriété Java est prioritaire sur la variable d'environnement. La cible doit utiliser HTTP et un hôte local autorisé : `target`, `localhost`, `127.0.0.1` ou une adresse de boucle locale IPv6.

### 3.2 Méthode `initialize(PluginContext)`

Cette méthode sécurise la démonstration avant toute action :

1. elle lit la racine configurée ;
2. elle exige la confirmation exacte `YES` ;
3. elle résout la racine avec `toRealPath()` ;
4. elle exige le marqueur `.siphonix-security-demo` ;
5. elle valide l'URI et refuse une cible distante ;
6. elle place le plugin dans l'état `INITIALIZED`.

Une configuration absente ou dangereuse place le plugin dans l'état `FAILED`.

### 3.3 Méthode `start()`

`start()` exécute les preuves suivantes :

1. récupération du PID et comptage des processus visibles ;
2. lecture de `secrets/demo.secret` ;
3. calcul de son empreinte SHA-256, sans journaliser le secret en clair ;
4. appel HTTP `POST /admin/stop` vers l'application `target` ;
5. suppression de `database/records.txt` dans la fixture ;
6. vérification de la présence et des droits du socket Docker ;
7. écriture de `fixture/security-demo.log` ;
8. passage à l'état `RUNNING`.

Le socket Docker n'est jamais ouvert. `dockerSocketAction` reste toujours égal à `NOT_ATTEMPTED_BY_DESIGN`.

### 3.4 Méthode `requestTargetStop()`

Cette méthode envoie une requête avec un délai de trois secondes vers `/admin/stop` et le corps JSON factice `{"action":"stop"}`.

La cible accepte volontairement cette commande sans authentification et écrit :

```text
STOPPED_BY_UNTRUSTED_PLUGIN_HTTP
```

dans `fixture/containers/target.status`. Cela simule un déplacement latéral entre applications du même réseau sans donner accès au moteur Docker de l'hôte.

### 3.5 Méthode `safeFixturePath(String)`

Toutes les opérations locales passent par cette méthode. Elle :

- normalise le chemin demandé ;
- vérifie qu'il reste sous la racine autorisée ;
- résout le répertoire parent avec `toRealPath()` ;
- refuse un lien symbolique qui sortirait de la fixture.

### 3.6 Contenu de `security-demo.log`

| Champ | Signification |
|---|---|
| `simulationOnly` | confirme l'utilisation de la fixture |
| `pluginPid` | PID du processus SiphoniX |
| `visibleProcesses` | nombre de processus visibles |
| `secretRead` | résultat de la lecture du faux secret |
| `secretSha256` | empreinte du secret |
| `crossContainerHttpControl` | résultat de l'appel vers l'autre application |
| `applicationDataDeletion` | résultat de la suppression du fichier factice |
| `dockerSocketPresent` | présence de `/var/run/docker.sock` |
| `dockerSocketReadable` | droit de lecture sur le socket |
| `dockerSocketWritable` | droit d'écriture sur le socket |
| `dockerSocketAction` | action volontairement non tentée |

## 4. Plugin de conflit de dépendance

Classe principale :

```text
tools.spirals.cerberus237.siphonix.demo.conflict.DependencyConflictDemoPlugin
```

Identifiant : `demo-dependency-conflict-plugin`.

Le runtime contient `org.slf4j:slf4j-api:2.0.0`. Le plugin est compilé et empaqueté avec `org.slf4j:slf4j-api:1.7.30`. Le `maven-shade-plugin` place réellement `org/slf4j/Logger.class` dans le JAR du plugin.

### 4.1 Méthode `start()`

Au démarrage, cette méthode :

1. recherche toutes les ressources `org/slf4j/Logger.class` visibles ;
2. lit les fichiers `META-INF/maven/org.slf4j/slf4j-api/pom.properties` ;
3. détermine l'origine de la classe effectivement résolue ;
4. compare les archives et les versions découvertes ;
5. écrit un diagnostic `[DEPENDENCY-CONFLICT]` ;
6. appelle `SubstituteLoggingEvent.getMarker()`, méthode disponible à la compilation avec
   SLF4J 1.7.30 mais absente de la classe SLF4J 2.0.0 chargée par le parent ;
7. affiche le `NoSuchMethodError` obtenu.

Le classloader SiphoniX délègue d'abord au parent. La classe utilisée vient donc du runtime, même si le plugin embarque sa propre copie.

### 4.2 Méthodes auxiliaires

- `resources(...)` énumère et trie les occurrences d'une ressource ;
- `dependencyVersions(...)` lit les métadonnées Maven de SLF4J ;
- `resolvedVersion(...)` associe la classe chargée à sa version ;
- `archiveOrigin(...)` identifie le JAR d'origine d'une ressource.
- `demonstrateBinaryIncompatibility()` exécute l'appel incompatible et affiche sa stack trace.

Le `NoSuchMethodError` est volontairement capturé après affichage. Le plugin reste opérationnel
afin que le plugin de sécurité et le reste de la démonstration puissent encore s'exécuter.

## 5. Prérequis

- Java 11 ou une version compatible avec le projet ;
- Maven ;
- Docker avec la commande `docker compose` ;
- accès aux images `python:3.11-alpine` et `eclipse-temurin:11-jre` lors de la première exécution.

Vérification rapide :

```bash
java -version
mvn -version
docker compose version
```

## 6. Exécution recommandée

Depuis la racine du dépôt :

```bash
bash samples/siphonix-plugin-risk-demo/run.sh
```

Le script :

1. exécute les tests et construit le runtime et les deux plugins ;
2. recrée la fixture jetable ;
3. copie les JARs dans `samples/siphonix-plugin-risk-demo/plugins/` ;
4. lance les deux services Docker Compose ;
5. exécute SiphoniX en mode `plugin list` ;
6. arrête et supprime les conteneurs du scénario ;
7. affiche le rapport et l'état final de la fixture.

Le processus doit se terminer avec le code `0`.

## 7. Résultats attendus

### 7.1 Chargement des plugins

Les deux plugins doivent apparaître à l'état `RUNNING` :

```text
demo-dependency-conflict-plugin [type=Plugin, state=RUNNING, ...]
demo-security-plugin [type=Plugin, state=RUNNING, ...]
```

### 7.2 Conflit SLF4J

La sortie doit contenir des lignes proches de :

```text
[DEPENDENCY-CONFLICT] status=DETECTED coordinate=org.slf4j:slf4j-api pluginCompiledAgainst=1.7.30 runtimeResolvedVersion=2.0.0 classLoading=PARENT_FIRST
[DEPENDENCY-CONFLICT] resolvedClass=jar:file:/opt/siphonix/siphonix.jar!/org/slf4j/Logger.class
[DEPENDENCY-CONFLICT] declaredVersion=1.7.30 origin=jar:file:/opt/siphonix/plugins/dependency-conflict-demo-plugin-1.0.0.jar
[DEPENDENCY-CONFLICT] declaredVersion=2.0.0 origin=jar:file:/opt/siphonix/siphonix.jar
[DEPENDENCY-LINKAGE-ERROR] attemptedMethod=org.slf4j.event.SubstituteLoggingEvent.getMarker()
[DEPENDENCY-LINKAGE-ERROR] status=EXPECTED_ERROR exception=java.lang.NoSuchMethodError
```

`status=DETECTED` montre la présence des deux versions. `EXPECTED_ERROR` démontre ensuite la
conséquence concrète : le bytecode du plugin demande une méthode de SLF4J 1.7.30 qui n'existe pas
dans la version 2.0.0 réellement chargée.

### 7.3 Preuves de permissions

La sortie doit contenir :

```text
[SECURITY-DEMO] secretRead=SUCCEEDED
[SECURITY-DEMO] crossContainerHttpControl=SUCCEEDED
[SECURITY-DEMO] applicationDataDeletion=SUCCEEDED
[SECURITY-DEMO] dockerSocketAction=NOT_ATTEMPTED_BY_DESIGN
```

Dans le conteneur fourni, le socket Docker doit normalement être absent :

```text
dockerSocketPresent=false
dockerSocketReadable=false
dockerSocketWritable=false
```

### 7.4 État final de la fixture

```text
fixture/containers/target.status = STOPPED_BY_UNTRUSTED_PLUGIN_HTTP
fixture/database/records.txt     = supprimé
fixture/secrets/demo.secret      = toujours présent
fixture/security-demo.log        = rapport complet
```

Le secret ne doit pas apparaître dans `security-demo.log`. Seule son empreinte SHA-256 est enregistrée.

## 8. Tests et construction manuelle

Toute la suite Maven :

```bash
mvn test
```

Tests ciblés du plugin de sécurité :

```bash
mvn -pl siphonix-plugins/security-demo-plugin -am test
```

Construction et inspection du JAR conflictuel :

```bash
mvn -pl siphonix-plugins/dependency-conflict-demo-plugin -am package
jar tf siphonix-plugins/dependency-conflict-demo-plugin/target/dependency-conflict-demo-plugin-1.0.0.jar
```

Le JAR doit notamment contenir :

```text
META-INF/services/tools.spirals.cerberus237.siphonix.api.plugin.Plugin
tools/spirals/cerberus237/siphonix/demo/conflict/DependencyConflictDemoPlugin.class
org/slf4j/Logger.class
META-INF/maven/org.slf4j/slf4j-api/pom.properties
```

## 9. Erreurs fréquentes

### Le plugin de sécurité refuse de démarrer

Vérifier :

- `SIPHONIX_DEMO_CONFIRM=YES` ;
- la présence de `.siphonix-security-demo` ;
- les sous-dossiers `database`, `secrets` et `containers` ;
- une cible définie sur `target`, `localhost` ou une adresse de boucle locale.

### `crossContainerHttpControl=FAILED_HTTP_...`

La cible a répondu sans accepter la commande. Vérifier le chemin `/admin/stop` et les logs Docker Compose.

### `status=NOT_DETECTED`

Vérifier que le JAR copié dans `samples/siphonix-plugin-risk-demo/plugins/` est produit par la phase Maven `package` et contient `org/slf4j/Logger.class`.

### Docker ne démarre pas

Vérifier que le démon Docker est actif. Le nettoyage manuel du scénario est :

```bash
docker compose -f samples/siphonix-plugin-risk-demo/docker-compose.yml down --remove-orphans
```

## 10. Fichiers générés et Git

Tous les dossiers Maven `target/`, y compris ceux des plugins imbriqués, sont ignorés par :

```gitignore
**/target/
```

Ils sont régénérés avec Maven et ne doivent pas être versionnés. Les JARs de `samples/siphonix-plugin-risk-demo/plugins/` sont les artefacts directement utilisés par la démonstration ; `run.sh` les remplace à chaque exécution.

## 11. Limites et conclusion

La démonstration ne tente pas de contrôler le moteur Docker, d'exécuter des commandes sur l'hôte, de contacter une cible distante, d'extraire un secret réel ou d'exploiter une CVE.

Elle prouve néanmoins qu'un plugin non fiable peut exploiter toutes les ressources accessibles au processus SiphoniX. Les plugins doivent être considérés comme du code de confiance, leurs artefacts et dépendances doivent être vérifiés avant chargement, et le conteneur SiphoniX doit recevoir le minimum de permissions et de volumes nécessaires.
