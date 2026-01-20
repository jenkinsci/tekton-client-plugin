#!/bin/bash

# Script to generate Jelly UI config files for all generated Tekton POJOs
# This analyzes the generated Java classes and creates appropriate Jenkins UI forms

set -e

echo "Jelly Config Generator"
echo "========================="
echo ""

# Directories
GENERATED_SOURCES="target/generated-sources/tekton"
RESOURCES_DIR="src/main/resources"
TARGET_CLASSES="target/classes"

# Check if generated sources exist
if [ ! -d "$GENERATED_SOURCES" ]; then
    echo "[ERROR] Generated sources not found at: $GENERATED_SOURCES"
    echo "   Please run: mvn compile first"
    exit 1
fi

# Check if classes are compiled
if [ ! -d "$TARGET_CLASSES" ]; then
    echo "[WARN] Compiled classes not found. Running mvn compile..."
    mvn compile -DskipTests
fi

echo "Generated sources: $GENERATED_SOURCES"
echo "Resources output: $RESOURCES_DIR"
echo ""

# Build classpath including all dependencies
echo "Building classpath..."
CLASSPATH="$TARGET_CLASSES"

# Add all dependencies from Maven
CLASSPATH="$CLASSPATH:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)"

echo "[OK] Classpath ready"
echo ""

# Run the generator
echo "Running Jelly Config Generator..."
echo ""

java -cp "$CLASSPATH" \
    org.waveywaves.jenkins.plugins.tekton.generator.JellyConfigGenerator \
    "$GENERATED_SOURCES" \
    "$RESOURCES_DIR"

echo ""
echo "[OK] Jelly config generation complete!"
echo ""
echo "Next steps:"
echo "   1. Review the generated files in: $RESOURCES_DIR"
echo "   2. Customize any fields as needed"
echo "   3. Run: mvn package to build the plugin"
echo ""

