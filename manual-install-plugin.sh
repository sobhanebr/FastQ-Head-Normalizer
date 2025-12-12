#!/bin/bash
# Script to fix "Group ID mismatch" and "zip END header" errors for CDAP Maven Plugin.
# We download version 1.2.0 from the official location but install it as 'io.cdap' to match its internal descriptor.

PLUGIN_VERSION="1.2.0"
JAR_NAME="cdap-maven-plugin-${PLUGIN_VERSION}.jar"
DOWNLOAD_URL="https://repo1.maven.org/maven2/io/cdap/cdap-maven-plugin/${PLUGIN_VERSION}/${JAR_NAME}"

echo "1. Cleaning potential corrupt artifacts..."
rm -rf ~/.m2/repository/io/cdap/cdap/cdap-maven-plugin
rm -rf ~/.m2/repository/io/cdap/cdap-maven-plugin

echo "2. Downloading ${JAR_NAME}... from ${DOWNLOAD_URL}"
curl -L -f -o ${JAR_NAME} "${DOWNLOAD_URL}"

if [ $? -ne 0 ]; then
    echo "Download failed! Please check your internet connection."
    exit 1
fi

echo "3. Installing to local Maven repository with GroupID 'io.cdap'..."
# Note: We intentionally set groupId=io.cdap to match the plugin.xml descriptor inside the JAR
mvn install:install-file \
   -Dfile=${JAR_NAME} \
   -DgroupId=io.cdap \
   -DartifactId=cdap-maven-plugin \
   -Dversion=${PLUGIN_VERSION} \
   -Dpackaging=jar

# 4. Cleanup
rm ${JAR_NAME}

echo "-------------------------------------------------------"
echo "SUCCESS! Plugin installed as io.cdap:cdap-maven-plugin:${PLUGIN_VERSION}"
echo "You can now run 'mvn clean package'."
echo "-------------------------------------------------------"
