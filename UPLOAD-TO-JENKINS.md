# How to Upload .hpi to Jenkins

## Step 1: Build the Plugin Package

```bash
cd /Users/maeve/Downloads/tekton-client-plugin
mvn clean package -DskipTests
```

The `.hpi` file will be created at: `target/tekton-client-1.1.1-SNAPSHOT.hpi`

## Step 2: Upload to Jenkins UI

### Method 1: Upload via Web UI (Easiest)

1. **Open Jenkins in browser**:
   - Go to: `http://localhost:8080` (or your Jenkins URL)

2. **Navigate to Plugin Manager**:
   - Click: **Manage Jenkins** (left sidebar)
   - Click: **Manage Plugins**

3. **Go to Advanced Tab**:
   - Click the **Advanced** tab at the top

4. **Upload Plugin**:
   - Scroll down to "Upload Plugin" section
   - Click **Choose File**
   - Select: `/Users/maeve/Downloads/tekton-client-plugin/target/tekton-client-1.1.1-SNAPSHOT.hpi`
   - Click **Upload**

5. **Restart Jenkins**:
   - After upload, you'll see a message
   - Click **Restart Jenkins when installation is complete and no jobs are running**
   - Or manually restart: `sudo systemctl restart jenkins`

### Method 2: Copy to Jenkins Directory

```bash
# Stop Jenkins first
sudo systemctl stop jenkins

# Copy the plugin
sudo cp target/tekton-client-1.1.1-SNAPSHOT.hpi /var/lib/jenkins/plugins/tekton-client.hpi

# Start Jenkins
sudo systemctl start jenkins
```

### Method 3: Run Jenkins with Plugin (Development)

For testing during development:

```bash
# Run Jenkins with your plugin loaded
mvn hpi:run

# Jenkins will start on:
# http://localhost:8080/jenkins
```

## Step 3: Verify Plugin is Loaded

1. Go to: **Manage Jenkins** → **Manage Plugins** → **Installed** tab
2. Search for: `Tekton Client`
3. You should see it in the list

## Step 4: Test the Generated UI

1. **Create a new Job**:
   - Click **New Item**
   - Enter name: `test-tekton-ui`
   - Select **Freestyle project**
   - Click **OK**

2. **Add Build Step**:
   - Scroll to **Build** section
   - Click **Add build step** dropdown
   - You should see new options:
     - ✨ **Create Pipeline Run Typed**
     - ✨ **Create Task Run Typed**
     - ✨ **Create Custom Run Typed**
     - ✨ **Create Pipeline Typed**
     - ✨ **Create Task Typed**
     - ✨ **Create Step Action Typed**

3. **View Generated UI**:
   - Click on any "Create *Typed" option
   - You'll see the auto-generated form with:
     - Text fields for apiVersion, kind
     - Nested forms for metadata
     - Nested forms for spec
     - Repeatable sections for lists

## Quick Commands

```bash
# Complete workflow:

# 1. Generate POJOs and Jelly configs
mvn clean compile && ./scripts/generate-jelly-configs.sh

# 2. Build .hpi package
mvn package -DskipTests

# 3. The file is ready at:
# target/tekton-client-1.1.1-SNAPSHOT.hpi

# 4. Upload to Jenkins UI (see steps above)
```

## Troubleshooting

### Problem: Upload button grayed out

**Solution**: Make sure you're logged in as admin user

### Problem: Plugin doesn't appear after upload

**Solution**: 
```bash
# Check Jenkins logs
tail -f /var/lib/jenkins/logs/jenkins.log

# Restart Jenkins
sudo systemctl restart jenkins
```

### Problem: Old version still showing

**Solution**:
```bash
# Remove old plugin first
sudo rm /var/lib/jenkins/plugins/tekton-client.hpi
sudo rm -rf /var/lib/jenkins/plugins/tekton-client/

# Upload new version
# Then restart Jenkins
```

## Jenkins Locations by OS

### Linux
- Plugins: `/var/lib/jenkins/plugins/`
- Logs: `/var/lib/jenkins/logs/jenkins.log`

### macOS (Homebrew)
- Plugins: `/usr/local/var/jenkins_home/plugins/`
- Logs: `/usr/local/var/log/jenkins/jenkins.log`

### Docker
```bash
# Copy to running container
docker cp target/tekton-client-1.1.1-SNAPSHOT.hpi jenkins:/var/jenkins_home/plugins/

# Restart container
docker restart jenkins
```

## Summary

**Fastest way to upload:**

1. Build: `mvn package -DskipTests`
2. Open Jenkins: `http://localhost:8080`
3. Go to: **Manage Jenkins** → **Manage Plugins** → **Advanced**
4. Upload: `target/tekton-client-1.1.1-SNAPSHOT.hpi`
5. Restart Jenkins
6. Test: Create job → Add build step → See new "Create *Typed" options! 🎉

