# Matchmaking Application

## EC2 Setup & Deployment

### Prerequisites

#### 1. Install Redis
```bash
sudo dnf install -y redis6
sudo systemctl enable redis6
sudo systemctl start redis6

# Verify
sudo systemctl status redis6
redis6-cli ping   # should return PONG
```

#### 2. Install Java 17
```bash
sudo dnf install -y java-17-amazon-corretto
java -version
```

#### 3. Install Tomcat
```bash
sudo dnf install -y tomcat
sudo systemctl enable tomcat
sudo systemctl start tomcat
```

#### 4. Install Maven
```bash
sudo dnf install -y maven
mvn -version
```

---

### Deployment

```bash
# Pull latest code
git pull origin main

# Build WAR (skip tests)
mvn clean package -DskipTests

# Deploy to Tomcat
sudo cp target/v1.war /opt/tomcat/webapps/ROOT.war
sudo systemctl restart tomcat

# Tail logs
tail -f /opt/tomcat/logs/catalina.out
```

Or use the deploy script:
```bash
./deploy.sh
```

---

### Check Scheduler is Running

After startup, check the `WaitingQueueMatcherJob` fires every 30 seconds:
```bash
grep "WaitingQueueMatcherJob" /opt/tomcat/logs/catalina.out | tail -10
```

Expected output (if Redis is up and no users are waiting):
```
WaitingQueueMatcherJob tick
```

Expected output (if Redis is down):
```
WARN  WaitingQueueMatcherJob : WaitingQueueMatcherJob tick failed: Unable to connect to Redis
```

---

### Troubleshooting

| Error | Fix |
|---|---|
| `missing table [...]` | `ddl-auto` is `validate` but tables don't exist — switch to `update` |
| `Unable to connect to Redis` | Run `sudo systemctl start redis6` |
| `ConflictingBeanDefinitionException` | Duplicate `@Component` classes — check for duplicates in `controller/` vs `controller/ws/` |
| `Schema-validation: missing table` | Tables were dropped by `create-drop` — switch to `update` to recreate |
