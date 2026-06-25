#!/bin/bash
set -e  # stop on any error

echo "=========================================="
echo "  Matchmaking Backend — Deployment Start"
echo "=========================================="

# ── Config ────────────────────────────────────────────────────────────────────
APP_DIR="/home/ec2-user/mm/MatchmakingApplication"
TOMCAT_WEBAPPS="/opt/tomcat/webapps"
WAR_NAME="v1.war"
DEPLOY_NAME="ROOT.war"

# ── Detect branch → profile ───────────────────────────────────────────────────
cd "$APP_DIR"
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

if [ "$CURRENT_BRANCH" = "main" ]; then
  SPRING_PROFILE="prod"
elif [ "$CURRENT_BRANCH" = "uat" ]; then
  SPRING_PROFILE="dev"
else
  echo "ERROR: Unsupported branch '$CURRENT_BRANCH'. Deploy from 'main' (prod) or 'uat' (dev)."
  exit 1
fi

echo ""
echo "Branch  : $CURRENT_BRANCH"
echo "Profile : $SPRING_PROFILE"
echo ""

# ── Pull latest code ──────────────────────────────────────────────────────────
echo "[1/4] Pulling latest code from $CURRENT_BRANCH..."
git pull origin "$CURRENT_BRANCH"

# ── Build ─────────────────────────────────────────────────────────────────────
echo ""
echo "[2/4] Building WAR (skipping tests)..."
MAVEN_OPTS="-Xms256m -Xmx768m" mvn clean package -DskipTests

if [ ! -f "target/$WAR_NAME" ]; then
  echo "ERROR: WAR file not found at target/$WAR_NAME — build may have failed."
  exit 1
fi
echo "WAR built successfully: target/$WAR_NAME"

# ── Deploy to Tomcat ──────────────────────────────────────────────────────────
echo ""
echo "[3/4] Deploying to Tomcat with profile=$SPRING_PROFILE..."
sudo systemctl stop tomcat

# Inject Spring profile via setenv.sh — the standard Tomcat mechanism for JVM env vars
SETENV="/opt/tomcat/bin/setenv.sh"
sudo tee "$SETENV" > /dev/null <<EOF
#!/bin/bash
export SPRING_PROFILES_ACTIVE=$SPRING_PROFILE
# Load remaining env vars from tomcat.conf (DB, Redis, API keys)
if [ -f /opt/tomcat/conf/tomcat.conf ]; then
  set -a
  source /opt/tomcat/conf/tomcat.conf
  set +a
fi
EOF
sudo chmod +x "$SETENV"
echo "Written SPRING_PROFILES_ACTIVE=$SPRING_PROFILE to $SETENV"

# Remove old deployment
rm -rf "$TOMCAT_WEBAPPS/ROOT"
rm -f  "$TOMCAT_WEBAPPS/ROOT.war"

# Copy new WAR
cp "target/$WAR_NAME" "$TOMCAT_WEBAPPS/$DEPLOY_NAME"
echo "Copied to $TOMCAT_WEBAPPS/$DEPLOY_NAME"

sudo systemctl start tomcat

# ── Update Nginx config ───────────────────────────────────────────────────────
echo ""
echo "Updating Nginx config..."
sudo cp nginx/matchmaking.conf /etc/nginx/conf.d/matchmaking.conf
sudo nginx -t
sudo systemctl reload nginx

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo "[4/4] Deployment complete!"
echo "  Branch  : $CURRENT_BRANCH"
echo "  Profile : $SPRING_PROFILE"
echo "=========================================="
echo "  Tailing Tomcat logs (Ctrl+C to exit)"
echo "=========================================="
tail -f /opt/tomcat/logs/catalina.out
