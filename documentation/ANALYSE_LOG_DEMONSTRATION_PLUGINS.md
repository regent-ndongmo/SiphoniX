# Analyse du log de démonstration des risques liés aux plugins

## 1. Conclusion principale

Le scénario s'est terminé correctement et les deux plugins ont atteint l'état `RUNNING`.

Le conflit de dépendance observé concerne :

```text
org.slf4j:slf4j-api
```

Deux versions sont présentes :

| Emplacement | Version présente | Utilisée à l'exécution |
|---|---:|---|
| Runtime SiphoniX (`/opt/siphonix/siphonix.jar`) | `2.0.0` | **Oui** |
| Plugin de conflit (`dependency-conflict-demo-plugin-1.0.0.jar`) | `1.7.30` | Non, masquée par le parent |

La dépendance finalement prise en compte est donc :

```text
org.slf4j:slf4j-api:2.0.0
```

Elle provient du runtime SiphoniX, et non du plugin.

## 2. Pourquoi la version 2.0.0 est-elle sélectionnée ?

Le plugin est chargé par un `URLClassLoader` dont le parent est le classloader de SiphoniX. Le mécanisme standard utilisé est `PARENT_FIRST` :

1. le classloader du plugin demande d'abord au parent de charger la classe ;
2. le parent trouve `org.slf4j.Logger` dans le JAR du runtime ;
3. cette classe est retournée au plugin ;
4. la copie de SLF4J 1.7.30 contenue dans le plugin n'est donc pas utilisée pour cette classe.

La ligne suivante annonce le diagnostic général :

```text
[DEPENDENCY-CONFLICT] status=DETECTED coordinate=org.slf4j:slf4j-api pluginCompiledAgainst=1.7.30 runtimeResolvedVersion=2.0.0 classLoading=PARENT_FIRST
```

Elle signifie que :

- le plugin a été compilé et empaqueté avec SLF4J `1.7.30` ;
- le runtime fournit SLF4J `2.0.0` ;
- les deux versions sont présentes simultanément ;
- le chargement parent-first sélectionne la version du runtime.

La preuve de la classe réellement chargée est cette ligne :

```text
[DEPENDENCY-CONFLICT] resolvedClass=jar:file:/opt/siphonix/siphonix.jar!/org/slf4j/Logger.class
```

`Logger.class` vient de `/opt/siphonix/siphonix.jar`. C'est cette origine, et non l'ordre d'affichage des candidats, qui permet de déterminer la version effectivement utilisée.

Les deux copies détectées sont ensuite affichées :

```text
[DEPENDENCY-CONFLICT] duplicateClassCandidate=jar:file:/opt/siphonix/plugins/dependency-conflict-demo-plugin-1.0.0.jar!/org/slf4j/Logger.class
[DEPENDENCY-CONFLICT] duplicateClassCandidate=jar:file:/opt/siphonix/siphonix.jar!/org/slf4j/Logger.class
```

Enfin, les métadonnées Maven confirment la version de chaque archive :

```text
[DEPENDENCY-CONFLICT] declaredVersion=1.7.30 origin=jar:file:/opt/siphonix/plugins/dependency-conflict-demo-plugin-1.0.0.jar
[DEPENDENCY-CONFLICT] declaredVersion=2.0.0 origin=jar:file:/opt/siphonix/siphonix.jar
```

Le log analysé ici a été produit avant l'ajout de l'appel incompatible. Le plugin appelle désormais
`SubstituteLoggingEvent.getMarker()`, méthode présente en 1.7.30 mais absente en 2.0.0. Les nouvelles
exécutions montrent donc un véritable `NoSuchMethodError`, capturé après affichage pour permettre au
reste de la démonstration de continuer.

## 3. Déroulement complet du log

### 3.1 Compilation et tests Maven

Le script commence par construire le runtime et les plugins avec Maven. Les messages suivants appartiennent à cette phase, avant le lancement de Docker :

```text
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
```

Cet avertissement vient de la version de Guava utilisée par l'installation locale de Maven :

```text
file:/usr/share/maven/lib/guava.jar
```

Il ne vient ni du runtime SiphoniX ni du plugin de démonstration.

Les messages suivants apparaissent également pendant les tests Maven :

```text
SLF4J: No SLF4J providers were found.
SLF4J: Defaulting to no-operation (NOP) logger implementation
```

Ils indiquent que certains tests possèdent `slf4j-api` dans leur classpath sans implémentation de journalisation. SLF4J utilise alors un logger silencieux. Cela n'empêche pas la compilation ni les tests de continuer.

Les messages concernant `127.0.0.1:39795` proviennent des tests de `RemoteServiceReadinessChecker`. Le passage de `503` à un état prêt valide le mécanisme de nouvelle tentative. Le message `503` affiché après le message de succès appartient à un autre cas de test ; il ne signifie pas que le même service est redevenu indisponible pendant le démarrage du conteneur.

Les premières lignes `[SECURITY-DEMO]`, avec un PID élevé et un socket Docker accessible, sont également produites par les tests exécutés directement sur la machine hôte :

```text
[SECURITY-DEMO] pluginPid=557081
[SECURITY-DEMO] dockerSocketPresent=true
```

Elles doivent être distinguées des résultats du plugin exécuté ensuite dans le conteneur.

### 3.2 Construction de l'environnement Docker

Docker télécharge l'image de la cible, construit l'image SiphoniX puis crée :

- le réseau `siphonix-plugin-risk-demo_default` ;
- le conteneur cible ;
- le conteneur SiphoniX.

Les temps de téléchargement et d'extraction ne signalent aucune erreur. La construction se termine avec :

```text
Service siphonix Built
```

### 3.3 Chargement des plugins

SiphoniX trouve et charge les deux JARs déposés dans `/opt/siphonix/plugins` :

```text
Loaded plugin demo-dependency-conflict-plugin
Loaded plugin demo-security-plugin
```

À ce moment, les plugins sont enregistrés. SiphoniX attend ensuite la disponibilité de la cible :

```text
[Readiness] Waiting for http://target:8080
[Readiness] Monitored service is ready
```

Le démarrage des plugins intervient seulement après cette vérification.

### 3.4 Résultats du plugin de sécurité dans le conteneur

Les lignes importantes sont :

```text
[SECURITY-DEMO] secretRead=SUCCEEDED
[SECURITY-DEMO] crossContainerHttpControl=SUCCEEDED
[SECURITY-DEMO] applicationDataDeletion=SUCCEEDED
```

Elles prouvent que le plugin a pu :

- lire le faux secret de la fixture ;
- envoyer une commande HTTP à l'autre conteneur ;
- supprimer le fichier applicatif factice.

Le secret lui-même n'est pas écrit dans les logs. Seule son empreinte SHA-256 est affichée.

Dans le conteneur SiphoniX, le socket Docker n'est pas monté :

```text
[SECURITY-DEMO] dockerSocketPresent=false
[SECURITY-DEMO] dockerSocketReadable=false
[SECURITY-DEMO] dockerSocketWritable=false
[SECURITY-DEMO] dockerSocketAction=NOT_ATTEMPTED_BY_DESIGN
```

Le plugin n'a donc pas contrôlé directement Docker. La démonstration de déplacement latéral utilise uniquement l'endpoint HTTP factice du conteneur `target`.

### 3.5 État final des plugins

Le runtime affiche finalement :

```text
demo-dependency-conflict-plugin [type=Plugin, state=RUNNING, ...]
demo-security-plugin [type=Plugin, state=RUNNING, ...]
```

Cela confirme que la détection du conflit n'a pas placé le plugin dans l'état `FAILED`.

Le conteneur SiphoniX se termine avec le code `0`. L'arrêt de Compose qui suit est normal, car le script utilise le conteneur SiphoniX comme processus principal du scénario.

## 4. État final de la fixture

Le rapport final contient :

```text
target.status: STOPPED_BY_UNTRUSTED_PLUGIN_HTTP
database/records.txt: DELETED
```

Cela confirme que :

- la cible a accepté l'appel HTTP non authentifié envoyé par le plugin ;
- le plugin a pu supprimer la donnée factice accessible au processus SiphoniX ;
- le scénario de sécurité s'est exécuté complètement ;
- aucun accès direct au moteur Docker n'a été tenté dans le conteneur.

## 5. Résultat synthétique

| Vérification | Résultat |
|---|---|
| Construction Maven | Réussie |
| Construction Docker | Réussie |
| Chargement du plugin de conflit | Réussi |
| Chargement du plugin de sécurité | Réussi |
| Conflit SLF4J détecté | Oui |
| Version SLF4J du plugin | `1.7.30` |
| Version SLF4J réellement utilisée | **`2.0.0` du runtime** |
| Stratégie de classloading | `PARENT_FIRST` |
| Deux plugins à l'état final `RUNNING` | Oui |
| Appel HTTP interconteneur | Réussi |
| Suppression de la donnée factice | Réussie |
| Accès au socket Docker depuis le conteneur | Non |
| Code de sortie du conteneur SiphoniX | `0` |
