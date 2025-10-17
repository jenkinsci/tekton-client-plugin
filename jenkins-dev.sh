#!/bin/bash
# Smart wrapper that makes Enter key work properly for Jenkins reload

echo "Jenkins Development Mode"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Commands:"
echo "   Enter/Return  = Redeploy (with clean)"
echo "   Ctrl+C        = Stop Jenkins"
echo ""

# Function to start Jenkins
start_jenkins() {
    echo "Building and starting Jenkins..."
    mvn compile -DskipTests -q
    mvn hpi:run &
    JENKINS_PID=$!
    echo "[OK] Jenkins started (PID: $JENKINS_PID)"
    echo ""
    echo "Hit Enter to redeploy with clean..."
}

# Function to reload
reload_jenkins() {
    echo ""
    echo "Reloading Jenkins..."
    
    # Kill current Jenkins
    if [ ! -z "$JENKINS_PID" ]; then
        kill $JENKINS_PID 2>/dev/null
        wait $JENKINS_PID 2>/dev/null
    fi
    pkill -f "mvn.*hpi:run" 2>/dev/null
    
    # Clean work directory (this fixes the classloader issue)
    echo "Cleaning plugin cache..."
    rm -rf work/plugins/tekton-client
    rm -rf work/plugins/tekton-client.hpl  
    
    # Quick rebuild
    echo "Rebuilding..."
    mvn compile -DskipTests -q
    
    # Restart
    echo "Restarting Jenkins..."
    mvn hpi:run &
    JENKINS_PID=$!
    
    echo "[OK] Jenkins reloaded successfully!"
    echo ""
    echo "Hit Enter to redeploy again..."
}

# Trap Ctrl+C
trap 'echo ""; echo "Stopping Jenkins..."; pkill -f "mvn.*hpi:run"; exit 0' INT

# Start Jenkins initially
start_jenkins

# Wait for Enter key and reload
while true; do
    read -r
    reload_jenkins
done

