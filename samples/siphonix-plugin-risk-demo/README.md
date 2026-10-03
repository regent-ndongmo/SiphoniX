# Démonstration des risques liés aux plugins SiphoniX

Le guide technique détaillé — architecture, méthodes principales, configuration,
exécution et interprétation des résultats — se trouve dans
[`documentation/DEMONSTRATION_RISQUES_PLUGINS.md`](../../documentation/DEMONSTRATION_RISQUES_PLUGINS.md).

Cette démonstration isolée charge exactement deux plugins depuis le dossier `plugins` :

1. `security-demo-plugin` prouve qu'un plugin hérite des droits du processus SiphoniX : lecture d'un faux secret, suppression d'un faux fichier applicatif, visibilité des processus et appel HTTP non authentifié vers une autre application du réseau Compose ;
2. `dependency-conflict-demo-plugin` embarque volontairement `slf4j-api:1.7.30`, alors que le runtime embarque `slf4j-api:2.0.0`. Le classloader parent-first choisit la copie du runtime ; le plugin journalise les deux versions puis provoque et capture un `NoSuchMethodError` représentatif d'une incompatibilité binaire réelle.

Le scénario ne monte jamais `/var/run/docker.sock` et n'appelle pas l'API Docker. Le « contrôle de conteneur » est représenté sans danger par une cible factice qui expose volontairement `/admin/stop`. Cette cible écrit son état dans `fixture/containers/target.status`. Le plugin refuse de démarrer sans confirmation explicite et sans le marqueur `.siphonix-security-demo`.

## Exécution

Depuis la racine du projet :

```bash
bash samples/siphonix-plugin-risk-demo/run.sh
```

Le script :

- exécute les tests et compile SiphoniX avec les deux plugins de démonstration ;
- copie le runtime et les plugins dans le contexte Docker ;
- crée une fixture factice ;
- démarre une cible HTTP factice pour la readiness et l'action de contrôle ;
- démarre SiphoniX en mode `plugin list`, puis termine automatiquement ;
- affiche l'état de la fixture après le passage des plugins.

## Résultats attendus

Les logs du conflit doivent contenir :

```text
[DEPENDENCY-CONFLICT] status=DETECTED coordinate=org.slf4j:slf4j-api pluginCompiledAgainst=1.7.30 runtimeResolvedVersion=2.0.0 classLoading=PARENT_FIRST
[DEPENDENCY-CONFLICT] duplicateClassCandidate=jar:file:...siphonix.jar!/org/slf4j/Logger.class
[DEPENDENCY-CONFLICT] duplicateClassCandidate=jar:file:...dependency-conflict-demo-plugin-1.0.0.jar!/org/slf4j/Logger.class
[DEPENDENCY-LINKAGE-ERROR] attemptedMethod=org.slf4j.event.SubstituteLoggingEvent.getMarker()
[DEPENDENCY-LINKAGE-ERROR] status=EXPECTED_ERROR exception=java.lang.NoSuchMethodError
```

SiphoniX attend la cible par défaut. Pour désactiver volontairement cette barrière lors d'un autre
essai, ajouter `--wait-for-target false` à la commande Java, ou définir
`SIPHONIX_WAIT_FOR_TARGET=false`.

Les logs et la fixture de sécurité doivent montrer :

```text
[SECURITY-DEMO] secretRead=SUCCEEDED
[SECURITY-DEMO] crossContainerHttpControl=SUCCEEDED
[SECURITY-DEMO] applicationDataDeletion=SUCCEEDED
fixture/containers/target.status -> STOPPED_BY_UNTRUSTED_PLUGIN_HTTP
fixture/database/records.txt     -> supprimé
fixture/security-demo.log        -> résultats et empreinte SHA-256 du faux secret
```

`dockerSocketPresent`, `dockerSocketReadable` et `dockerSocketWritable` sont également journalisés. L'action reste toujours `NOT_ATTEMPTED_BY_DESIGN`. Si le socket était monté, sa simple accessibilité serait un risque critique à traiter séparément.

## Plugins

- `dependency-conflict-demo-plugin` : ID `demo-dependency-conflict-plugin`, version `2.0.0`, fat JAR de test avec SLF4J 1.7.30 ;
- `security-demo-plugin` : ID `demo-security-plugin`, version `2.0.0`, accès limité à `SIPHONIX_DEMO_ROOT` et à une cible locale autorisée.

## Garde-fous

- n'exécuter ce scénario que sur la fixture fournie ;
- ne pas ajouter le socket Docker aux volumes ;
- ne pas remplacer `SIPHONIX_DEMO_TARGET_URL` par un service réel ;
- conserver les plugins de démonstration hors d'un déploiement de production.
