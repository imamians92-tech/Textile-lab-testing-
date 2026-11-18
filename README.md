# TEXTILE LAB PRO

Offline-first Android Textile Testing Laboratory Management Platform built with **Kotlin**, **Jetpack Compose (Material 3)**, **MVVM**, **Room SQLite**, **Embedded Ktor HTTP & WebSocket Server**, and **WorkManager**.

Designed for a **4GB RAM / 64GB Storage Android Host Device (e.g. Infinix)** serving **20+ Android Wi-Fi client devices** on a local Wi-Fi LAN with zero internet dependency.

---

## Architecture & Folder Structure

```text
app/src/main/java/com/example/
├── TextileLabApp.kt                  # Application entry, DI bootstrap, daily backup scheduler, auto-start server
├── MainActivity.kt                   # Edge-to-edge M3 adaptive NavigationBar/NavigationRail & role routing
├── core/
│   ├── AppContainer.kt               # Manual dependency injection container
│   ├── NetworkUtils.kt               # LAN Wi-Fi IPv4 discovery & date/byte formatters
│   └── SecurityUtils.kt              # PBKDF2-HMAC-SHA256 salted password hashing & token generator
├── data/
│   ├── backup/
│   │   ├── BackupManager.kt          # WAL-checkpointed SQLite + photos ZIP backup engine
│   │   └── DailyBackupWorker.kt      # 24h periodic WorkManager daily backup job
│   ├── local/
│   │   ├── TextileLabDatabase.kt     # Room SQLite database + WAL checkpoint support
│   │   ├── EntityMappers.kt          # Room Entity <-> DTO mappers
│   │   ├── SessionPreferences.kt     # DataStore preferences (auth token, host/client mode, dark theme)
│   │   ├── dao/LabDaos.kt            # 8 Room DAOs with reactive Flow queries
│   │   └── entities/LabEntities.kt   # Users, Jobs, Samples, TestResults, Reports, Attachments, AuditLogs, Backups
│   ├── model/
│   │   └── LabModels.kt              # Domain enums (UserRole, 9 TextileTestTypes) & @Serializable DTOs
│   ├── network/
│   │   └── LabSyncClient.kt          # Ktor OkHttp REST + WebSocket client for Wi-Fi client tablets/phones
│   ├── repository/
│   │   └── LabRepository.kt          # Unified Host SQLite & Client REST/WS repository with RBAC
│   └── server/
│       ├── LabLocalServer.kt         # Embedded Ktor CIO HTTP & WebSocket server (port 8080)
│       └── LabServerService.kt       # Android dataSync ForegroundService keeping host online
└── ui/
    ├── LabViewModel.kt               # Reactive StateFlow ViewModel & WebSocket auto-sync
    ├── components/LabComponents.kt   # Reusable M3 cards, badges, photo viewer, live connection banner
    ├── screens/
    │   ├── LoginScreen.kt            # Role-specific Admin/Technician/Guest login + Host/Client Wi-Fi config
    │   ├── DashboardScreen.kt        # Live KPI dashboard & activity feeds
    │   ├── JobsScreen.kt             # Job Receiving & Job Card editor
    │   ├── SamplesScreen.kt          # Auto unique Sample IDs, job/sample/tech tracking & counts
    │   ├── TestingScreen.kt          # 9-module textile testing workbench
    │   ├── ReportsScreen.kt          # Direct publishing, Camera/Gallery photo upload, Guest read-only portal
    │   └── AdminCenterScreen.kt      # Wi-Fi server toggle, user management, backups & security audit logs
    └── theme/
        ├── Color.kt                  # Navy Blue, White & Slate Grey light/dark color tokens
        ├── Theme.kt                  # Material 3 TextileLabTheme
        └── Type.kt                   # Plus Jakarta Sans & JetBrains Mono typography
```

---

## User Roles & Server-Side Security

| Capability | ADMIN | TECHNICIAN | GUEST |
| :--- | :---: | :---: | :---: |
| Login & PBKDF2-HMAC-SHA256 Auth | Yes | Yes | Yes |
| Create / Edit Jobs & Samples | Yes | Yes | No (403) |
| Perform All 9 Textile Tests | Yes | Yes | No (403) |
| Upload Camera / Gallery Photos | Yes | Yes | No (403) |
| Publish Final Reports Directly | Yes | Yes | No (403) |
| View Published Reports & Search | Yes | Yes | Yes |
| **Delete Files (`DELETE /api/files/{id}`)** | **Yes** | **NO (HTTP 403)** | **NO (HTTP 403)** |
| Manage Users, Backups & Audit Logs | Yes | No (403) | No (403) |

Every authorization decision is enforced server-side in `LabLocalServer.kt` and logged to `audit_logs` (`ALLOWED` / `DENIED`).

---

## 9 Textile Testing Modules

1. **GSM** (`g/m²`)
2. **Width** (`in`)
3. **Tear** (`N`)
4. **Tensile** (`N`)
5. **Stretch Growth** (`%`)
6. **Pilling** (`Grade`)
7. **Abrasion** (`Cycles`)
8. **Shrinkage** (`%`)
9. **Washing** (`Grade`)

---

## Local Wi-Fi Server Deployment (Infinix Host + 20+ Clients)

1. Connect the Infinix host phone and all lab client phones/tablets to the same local Wi-Fi router (no internet required).
2. On the Infinix host device, select **Host Server (Local DB)** on the login screen and sign in as `admin` (default initial password: `admin123`).
3. Note the displayed LAN URL (e.g., `http://192.168.1.45:8080`) in the **Dashboard** or **Admin > Wi-Fi Server** console.
4. On client devices, select **Client Device**, enter `http://192.168.1.45:8080`, tap **Ping**, and sign in with Technician (`tech1` / `tech123`) or Guest (`guest` / `guest123`) credentials.
5. Changes made on any device broadcast immediately to all connected clients via WebSocket (`/ws/updates`).

---

## Build, Test & APK Generation

```bash
# Run JVM & Robolectric integration tests (Room DB + Ktor Server RBAC + Backup)
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug
```

Output APK path: `app/build/outputs/apk/debug/app-debug.apk`.
