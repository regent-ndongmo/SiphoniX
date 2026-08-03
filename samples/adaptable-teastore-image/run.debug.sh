#!/bin/bash

# 1. Copier le JAR compilé depuis le dossier cible vers le dossier de build Docker
# On cible spécifiquement le JAR exécutable avec ses dépendances
JAR_SOURCE="../../siphonix-runtime/target/io.github.brice10.siphonix-jar-with-dependencies.jar"

# (Note : selon comment Maven nomme le fichier, il peut s'appeler io.github.brice10.siphonix-1.0.0-jar-with-dependencies.jar.
# Si la ligne au-dessus échoue, utilisez : JAR_SOURCE="../../siphonix-runtime/target/*-jar-with-dependencies.jar" )

if ls ../../siphonix-runtime/target/*-jar-with-dependencies.jar 1> /dev/null 2>&1; then
    echo "Copie du fichier Fat JAR vers le répertoire courant..."
    cp ../../siphonix-runtime/target/*-jar-with-dependencies.jar ./siphonix-app.jar
else
    echo "Erreur : Le fichier *-jar-with-dependencies.jar n'existe pas."
    echo "Veuillez exécuter 'mvn clean package' à la racine du projet en premier."
    exit 1
fi

if ls ../../siphonix-plugins/adaptiflow-engine-plugin/target/*-jar-with-dependencies.jar 1> /dev/null 2>&1; then
    echo "Copie du plugin AdaptiFlow vers le répertoire courant..."
    cp ../../siphonix-plugins/adaptiflow-engine-plugin/target/*-jar-with-dependencies.jar ./adaptiflow-engine-plugin.jar
else
    echo "Erreur : Le plugin AdaptiFlow n'existe pas."
    echo "Veuillez exécuter 'mvn clean package' à la racine du projet en premier."
    exit 1
fi

# 3. Lancement de docker-compose
docker-compose -f docker-compose.yml -p adaptable-teastore-image up -d --build