# HCEN - Historia Clínica Electrónica Nacional


## Environment Setup

### 1. Configure Environment Variables

Copy the example environment file and configure your credentials:

```bash
cp .env.example .env
```

Edit `.env` and set your MongoDB credentials:

```properties
MONGODB_URI=mongodb://mongouser:yourpassword@localhost:27017/?authSource=admin
MONGODB_DB=hcen_db
```

**⚠️ IMPORTANT**: Never commit the `.env` file to version control. It's already included in `.gitignore`.

### 2. Start MongoDB with Docker

Docker Compose will automatically load environment variables from the `.env` file:

```bash
docker-compose up -d
```

This will start MongoDB on port 27017 with the credentials from your `.env` file.

**Verify MongoDB is running:**
```bash
docker ps | grep mongodb
```

**View MongoDB logs:**
```bash
docker logs mongodb
```

**Stop MongoDB:**
```bash
docker-compose down
```

**Stop and remove data volumes (⚠️ destroys all data):**
```bash
docker-compose down -v
```

### 3. Start WildFly with Environment Variables

Use the provided helper scripts to automatically load environment variables from `.env` and start WildFly:

**Windows (PowerShell):**
```powershell
.\start-wildfly.ps1
```

**Windows (Command Prompt):**
```cmd
start-wildfly.bat
```

**Linux/Mac:**
```bash
chmod +x start-wildfly.sh
./start-wildfly.sh
```

### Build the project

```bash
mvn clean package
```

### Deploy to WildFly

**Option 1: Manual deployment**
```bash
cp ear/target/hcen.ear $WILDFLY_HOME/standalone/deployments/
```

**Option 2: Maven deployment**
```bash
mvn wildfly:deploy
```

## Testing

### Health Check

Test the MongoDB connection:
```bash
curl http://localhost:8080/hcen-web/api/mongo/health
```

Expected response: `ok - collection count: 0`

### API Endpoints

- **MongoDB Health**: `GET /hcen-web/api/mongo/health`
- **Insert Document**: `POST /hcen-web/api/mongo/document`
- **Get Document**: `GET /hcen-web/api/mongo/document/{inus}`
- **List All Documents**: `GET /hcen-web/api/mongo/documents`

## Project Structure

```
hcen/
├── ejb/                    # EJB module (business logic)
│   └── src/main/java/
│       └── uy/edu/tse/hcen/
│           ├── config/     # MongoDB configuration
│           ├── repository/ # Data access layer
│           └── service/    # Business services
├── web/                    # Web module (REST API)
│   └── src/main/java/
│       └── uy/edu/tse/hcen/
│           └── rest/       # REST endpoints
├── ear/                    # Enterprise archive packaging
└── docker-compose.yml      # MongoDB container config
```