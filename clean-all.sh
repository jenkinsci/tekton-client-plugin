#!/bin/bash
# Script to clean all caches

echo "Cleaning all caches..."
echo ""

# Delete build artifacts
echo "Removing target/..."
rm -rf target

# Delete Jenkins work directory
echo "Removing work/..."
rm -rf work

# Delete plugin from local Maven repository
echo "Removing from local Maven repository..."
rm -rf ~/.m2/repository/org/waveywaves/jenkins/plugins/tekton-client

echo ""
echo "[OK] All caches cleaned!"
echo ""
echo "Next steps:"
echo "  mvn clean install -DskipTests"
echo "  mvn hpi:run"