#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
KIND_CLUSTER_NAME="tekton-e2e-test"
TEKTON_VERSION="v1.0.0"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    if ! command_exists kind; then
        print_error "Kind is not installed. Please install Kind first:"
        echo "https://kind.sigs.k8s.io/docs/user/quick-start/"
        exit 1
    fi
    
    if ! command_exists kubectl; then
        print_error "kubectl is not installed. Please install kubectl first:"
        echo "https://kubernetes.io/docs/tasks/tools/"
        exit 1
    fi
    
    if ! command_exists mvn; then
        print_error "Maven is not installed. Please install Maven first:"
        echo "https://maven.apache.org/install.html"
        exit 1
    fi
    
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker first:"
        echo "https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    print_success "All prerequisites are installed"
}

# Function to setup Kind cluster
setup_kind_cluster() {
    print_status "Setting up Kind cluster: $KIND_CLUSTER_NAME"
    
    # Check if cluster already exists
    if kind get clusters | grep -q "^${KIND_CLUSTER_NAME}$"; then
        print_warning "Kind cluster '$KIND_CLUSTER_NAME' already exists"
        
        # In CI or when REUSE_CLUSTER is set, automatically reuse
        if [ "$CI" = "true" ] || [ "$REUSE_CLUSTER" = "true" ]; then
            print_status "Reusing existing cluster for speed"
            return 0
        fi
        
        read -p "Do you want to delete and recreate it? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_status "Deleting existing cluster..."
            kind delete cluster --name "$KIND_CLUSTER_NAME"
        else
            print_status "Using existing cluster"
            return 0
        fi
    fi
    
    # Create Kind cluster configuration
    cat > /tmp/kind-config.yaml << EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  extraPortMappings:
  - containerPort: 30080
    hostPort: 30080
    protocol: TCP
  - containerPort: 30443
    hostPort: 30443
    protocol: TCP
networking:
  apiServerAddress: "127.0.0.1"
  apiServerPort: 6443
EOF
    
    # Create cluster
    print_status "Creating Kind cluster..."
    kind create cluster --name "$KIND_CLUSTER_NAME" --config /tmp/kind-config.yaml
    
    # Wait for cluster to be ready
    print_status "Waiting for cluster to be ready..."
    kubectl wait --for=condition=Ready nodes --all --timeout=300s
    
    print_success "Kind cluster created and ready"
}

# Function to install Tekton
install_tekton() {
    print_status "Installing Tekton Pipelines $TEKTON_VERSION..."
    
    # Install Tekton Pipelines
    kubectl apply -f "https://storage.googleapis.com/tekton-releases/pipeline/previous/${TEKTON_VERSION}/release.yaml"
    
    # Wait for Tekton to be ready
    print_status "Waiting for Tekton to be ready..."
    kubectl wait --for=condition=Ready pods --all -n tekton-pipelines --timeout=300s
    
    print_success "Tekton Pipelines installed and ready"
}

# Function to build the plugin
build_plugin() {
    print_status "Building Jenkins plugin..."
    
    cd "$PROJECT_ROOT"
    
    # Build the plugin without running tests first (optimized for speed)
    mvn compile test-compile -DskipTests -T 1C --batch-mode \
        -Dmaven.javadoc.skip=true \
        -Dmaven.source.skip=true \
        -Dcheckstyle.skip=true \
        -Dspotbugs.skip=true
    
    print_success "Plugin built successfully"
}

# Function to run e2e tests
run_e2e_tests() {
    print_status "Running E2E tests..."
    
    cd "$PROJECT_ROOT"
    
    # Set environment variables for tests
    export KUBECONFIG="$(kind get kubeconfig --name "$KIND_CLUSTER_NAME" 2>/dev/null)"
    export KIND_CLUSTER_NAME="$KIND_CLUSTER_NAME"
    
    # Run only the e2e tests
    mvn test -Dtest="*E2ETest" -Dmaven.test.failure.ignore=false
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        print_success "E2E tests passed!"
    else
        print_error "E2E tests failed!"
        return $exit_code
    fi
}

# Function to cleanup
cleanup() {
    print_status "Cleaning up..."
    
    if [ "$SKIP_CLEANUP" != "true" ]; then
        if kind get clusters | grep -q "^${KIND_CLUSTER_NAME}$"; then
            print_status "Deleting Kind cluster..."
            kind delete cluster --name "$KIND_CLUSTER_NAME"
            print_success "Kind cluster deleted"
        fi
    else
        print_warning "Skipping cleanup (SKIP_CLEANUP=true)"
    fi
    
    # Clean up temporary files
    rm -f /tmp/kind-config.yaml
}

# Function to print usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --skip-setup       Skip Kind cluster setup (use existing cluster)"
    echo "  --skip-cleanup     Don't delete the Kind cluster after tests"
    echo "  --build-only       Only build the plugin, don't run tests"
    echo "  --test-only        Only run tests (assumes cluster is already set up)"
    echo "  --fast             Fast mode: reuse cluster, skip cleanup, show progress"
    echo "  --help             Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  SKIP_CLEANUP       Set to 'true' to skip cleanup"
    echo "  REUSE_CLUSTER      Set to 'true' to reuse existing cluster"
    echo "  KIND_CLUSTER_NAME  Name of the Kind cluster (default: tekton-e2e-test)"
    echo "  TEKTON_VERSION     Version of Tekton to install (default: v1.0.0)"
}

# Parse command line arguments
SKIP_SETUP=false
SKIP_CLEANUP=false
BUILD_ONLY=false
TEST_ONLY=false
FAST_MODE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-setup)
            SKIP_SETUP=true
            shift
            ;;
        --skip-cleanup)
            SKIP_CLEANUP=true
            shift
            ;;
        --build-only)
            BUILD_ONLY=true
            shift
            ;;
        --test-only)
            TEST_ONLY=true
            shift
            ;;
        --fast)
            FAST_MODE=true
            REUSE_CLUSTER=true
            SKIP_CLEANUP=true
            shift
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Set environment variables from command line flags
if [ "$SKIP_CLEANUP" = true ]; then
    export SKIP_CLEANUP=true
fi

if [ "$REUSE_CLUSTER" = true ]; then
    export REUSE_CLUSTER=true
fi

if [ "$FAST_MODE" = true ]; then
    print_status "Fast mode enabled: reusing cluster, skipping cleanup"
fi

# Main execution
main() {
    print_status "Starting Jenkins Tekton Plugin E2E Test Suite"
    
    # Set trap for cleanup
    if [ "$SKIP_CLEANUP" != "true" ]; then
        trap cleanup EXIT
    fi
    
    check_prerequisites
    
    if [ "$TEST_ONLY" != "true" ]; then
        if [ "$SKIP_SETUP" != "true" ]; then
            setup_kind_cluster
            install_tekton
        fi
        
        build_plugin
    fi
    
    if [ "$BUILD_ONLY" != "true" ]; then
        run_e2e_tests
    fi
    
    print_success "E2E test suite completed successfully!"
}

# Run main function
main "$@" 