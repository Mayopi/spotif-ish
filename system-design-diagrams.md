# System Design Diagrams

## Project: Spotify Clone (Kotlin Native, No Backend, Google Drive + Local Files)

---

# 1. High-Level System Architecture

```mermaid
flowchart TD
    U[User] --> UI[Android UI Layer]

    UI --> VM[ViewModels]
    VM --> UC[Use Cases / Domain Layer]
    UC --> REP[Repository Layer]

    REP --> LOCAL[Local Music Data Source]
    REP --> DRIVE[Google Drive Data Source]
    REP --> PREFS[Preferences / JSON Storage]
    REP --> PLAYER[Player Engine]

    LOCAL --> MEDIASTORE[Android MediaStore / File Picker]
    DRIVE --> GSIGN[Google Sign-In]
    DRIVE --> GAPI[Google Drive API]

    PLAYER --> EXO[ExoPlayer]
    EXO --> AUDIO[Audio Output]

    PREFS --> SHARED[SharedPreferences / EncryptedSharedPreferences]
    PREFS --> JSON[Local JSON Files]

    EXO --> NOTIF[Media Notification]
    EXO --> SESSION[MediaSession]
```

---

# 2. Clean Architecture Layer Diagram

```mermaid
flowchart TB
    subgraph Presentation Layer
        A1[Activities / Fragments / Compose UI]
        A2[ViewModels]
        A3[UI State]
    end

    subgraph Domain Layer
        B1[Use Cases]
        B2[Domain Models]
        B3[Business Rules]
    end

    subgraph Data Layer
        C1[Repositories]
        C2[Local Data Source]
        C3[Drive Data Source]
        C4[Preferences Data Source]
        C5[Player Data Source]
    end

    subgraph External Systems
        D1[MediaStore]
        D2[Google Drive API]
        D3[Google Sign-In]
        D4[ExoPlayer]
        D5[File System]
    end

    A1 --> A2
    A2 --> A3
    A2 --> B1
    B1 --> B2
    B1 --> B3
    B1 --> C1
    C1 --> C2
    C1 --> C3
    C1 --> C4
    C1 --> C5
    C2 --> D1
    C2 --> D5
    C3 --> D2
    C3 --> D3
    C5 --> D4
```

---

# 3. App Module Structure Diagram

```mermaid
flowchart LR
    subgraph app
        UI[UI / Navigation / DI]
    end

    subgraph domain
        USECASES[Use Cases]
        MODELS[Domain Models]
        CONTRACTS[Repository Interfaces]
    end

    subgraph data
        REPOIMPL[Repository Implementations]
        LOCALDS[Local Data Sources]
        DRIVEDS[Drive Data Sources]
        PREFSDS[Preferences + JSON]
        MAPPERS[Mappers]
    end

    subgraph player
        PLAYERCORE[Player Controller]
        MEDIASSN[MediaSession Handler]
        NOTIFICATION[Notification Manager]
    end

    UI --> USECASES
    USECASES --> CONTRACTS
    REPOIMPL --> CONTRACTS
    REPOIMPL --> LOCALDS
    REPOIMPL --> DRIVEDS
    REPOIMPL --> PREFSDS
    UI --> PLAYERCORE
    PLAYERCORE --> MEDIASSN
    PLAYERCORE --> NOTIFICATION
```

---

# 4. Local Music File Flow

```mermaid
sequenceDiagram
    participant User
    participant UI
    participant ViewModel
    participant UseCase
    participant Repository
    participant LocalSource
    participant MediaStore
    participant FileSystem

    User->>UI: Open Local Library
    UI->>ViewModel: Request local songs
    ViewModel->>UseCase: GetLocalSongs()
    UseCase->>Repository: fetchLocalSongs()
    Repository->>LocalSource: scanDeviceMusic()
    LocalSource->>MediaStore: query audio files
    MediaStore-->>LocalSource: metadata results
    LocalSource->>FileSystem: optional direct scan
    FileSystem-->>LocalSource: matching files
    LocalSource-->>Repository: song list
    Repository-->>UseCase: domain song models
    UseCase-->>ViewModel: processed result
    ViewModel-->>UI: render song list
```

---

# 5. Google Drive Music Flow

```mermaid
sequenceDiagram
    participant User
    participant UI
    participant ViewModel
    participant UseCase
    participant Repository
    participant DriveSource
    participant GoogleSignIn
    participant DriveAPI

    User->>UI: Connect Google Drive
    UI->>ViewModel: connectDrive()
    ViewModel->>UseCase: ConnectGoogleDrive()
    UseCase->>Repository: authenticateDrive()
    Repository->>GoogleSignIn: request sign-in
    GoogleSignIn-->>Repository: auth token / account
    Repository-->>UseCase: success
    UseCase-->>ViewModel: connected state
    ViewModel-->>UI: show folder picker

    User->>UI: Select folder
    UI->>ViewModel: folderSelected(folderId)
    ViewModel->>UseCase: LoadDriveFolder(folderId)
    UseCase->>Repository: fetchDriveSongs(folderId)
    Repository->>DriveSource: listAudioFiles(folderId)
    DriveSource->>DriveAPI: fetch file metadata
    DriveAPI-->>DriveSource: file list
    DriveSource-->>Repository: drive songs
    Repository-->>UseCase: mapped song models
    UseCase-->>ViewModel: result
    ViewModel-->>UI: render Drive library
```

---

# 6. Unified Music Source Design

```mermaid
flowchart TD
    SONGREQ[Song Request] --> REPO[Music Repository]

    REPO --> SRCDECIDE{Source Type?}

    SRCDECIDE -->|Local| LOCALSRC[Local Music Source]
    SRCDECIDE -->|Drive| DRIVESRC[Drive Music Source]

    LOCALSRC --> LOCALMETA[Local Metadata Reader]
    DRIVESRC --> DRIVEMETA[Drive Metadata Reader]

    LOCALMETA --> UNIFIED[Unified Song Model]
    DRIVEMETA --> UNIFIED

    UNIFIED --> VM[ViewModel]
    VM --> UI[UI Rendering]
```

---

# 7. Song Domain Model Relationship

```mermaid
classDiagram
    class Song {
        +String id
        +String title
        +String artist
        +String album
        +Long duration
        +String albumArtUri
        +String sourceType
        +String playableUri
        +String mimeType
        +Boolean isFavorite
    }

    class Playlist {
        +String id
        +String name
        +List~String~ songIds
        +Long createdAt
        +Long updatedAt
    }

    class FolderConnection {
        +String provider
        +String folderId
        +String folderName
        +Boolean active
    }

    class PlaybackQueue {
        +List~Song~ items
        +Int currentIndex
        +Boolean shuffleEnabled
        +String repeatMode
    }

    Playlist --> Song : contains
    PlaybackQueue --> Song : queues
    FolderConnection --> Song : source reference
```

---

# 8. Playback System Design

```mermaid
flowchart LR
    UI[Now Playing UI] --> VM[Player ViewModel]
    VM --> PLAYERCTRL[Player Controller]
    PLAYERCTRL --> QUEUE[Queue Manager]
    PLAYERCTRL --> EXO[ExoPlayer Wrapper]
    PLAYERCTRL --> SESSION[MediaSession Manager]
    PLAYERCTRL --> NOTIF[Notification Manager]

    QUEUE --> EXO
    EXO --> SOURCESEL{Media Source Type}
    SOURCESEL --> LOCALURI[Local File URI]
    SOURCESEL --> DRIVEURL[Drive Stream URL]

    EXO --> AUDIOOUT[Device Audio Output]
    SESSION --> LOCK[Lock Screen Controls]
    NOTIF --> STATUSBAR[Playback Notification]
```

---

# 9. Playback Sequence: Local or Drive Song

```mermaid
sequenceDiagram
    participant User
    participant UI
    participant PlayerVM
    participant PlayerController
    participant QueueManager
    participant Repository
    participant ExoPlayer

    User->>UI: Tap song
    UI->>PlayerVM: playSong(songId)
    PlayerVM->>Repository: getSong(songId)
    Repository-->>PlayerVM: song model
    PlayerVM->>PlayerController: play(song)
    PlayerController->>QueueManager: setCurrent(song)
    QueueManager-->>PlayerController: playback item
    PlayerController->>ExoPlayer: prepare media item
    ExoPlayer-->>PlayerController: ready state
    PlayerController-->>PlayerVM: playback started
    PlayerVM-->>UI: update now playing
```

---

# 10. Search System Design

```mermaid
flowchart TD
    USERINPUT[Search Query] --> UI[Search UI]
    UI --> VM[Search ViewModel]
    VM --> DEBOUNCE[Debounce 300ms]
    DEBOUNCE --> USECASE[SearchSongs Use Case]
    USECASE --> REPO[Music Repository]

    REPO --> LOCALINDEX[Local Song Cache]
    REPO --> DRIVEINDEX[Drive Song Cache]

    LOCALINDEX --> MERGE[Merge Results]
    DRIVEINDEX --> MERGE
    MERGE --> FILTER[Filter + Rank]
    FILTER --> VMRESULT[Search UI State]
    VMRESULT --> UIRESULT[Render Search Results]
```

---

# 11. Playlist Management Design

```mermaid
flowchart TD
    UI[Playlist UI] --> VM[Playlist ViewModel]
    VM --> UC[Playlist Use Cases]
    UC --> REPO[Playlist Repository]
    REPO --> JSONSTORE[JSON File Storage]

    JSONSTORE --> FILES[playlists.json]

    UC --> ACTIONS{Action Type}
    ACTIONS --> CREATE[Create Playlist]
    ACTIONS --> UPDATE[Rename Playlist]
    ACTIONS --> ADDSONG[Add Song]
    ACTIONS --> REMOVESONG[Remove Song]
    ACTIONS --> DELETE[Delete Playlist]
```

---

# 12. Favorites / Likes Design

```mermaid
flowchart LR
    SongListUI[Song List / Now Playing] --> FavVM[Favorites ViewModel]
    FavVM --> ToggleUC[ToggleFavorite Use Case]
    ToggleUC --> FavRepo[Favorites Repository]
    FavRepo --> Prefs[SharedPreferences or favorites.json]
    Prefs --> FavRepo
    FavRepo --> ToggleUC
    ToggleUC --> FavVM
    FavVM --> SongListUI
```

---

# 13. Settings and Persistence Design

```mermaid
flowchart TD
    SETTINGSUI[Settings UI] --> SETTINGSVM[Settings ViewModel]
    SETTINGSVM --> SETTINGSUC[Settings Use Cases]
    SETTINGSUC --> SETTINGSREPO[Settings Repository]

    SETTINGSREPO --> ESP[EncryptedSharedPreferences]
    SETTINGSREPO --> JSON[JSON Config Files]

    ESP --> TOKENS[Google auth tokens / secure references]
    JSON --> CONFIG[folder config / cache / playlists / favorites]
```

---

# 14. Google Drive Authentication and Access Design

```mermaid
flowchart TD
    USER[User] --> SIGNINUI[Connect Drive Button]
    SIGNINUI --> AUTHVM[Auth ViewModel]
    AUTHVM --> AUTHUC[Google Drive Connect Use Case]
    AUTHUC --> AUTHREPO[Auth Repository]

    AUTHREPO --> GSIGNIN[Google Sign-In SDK]
    GSIGNIN --> TOKEN[OAuth Token]

    TOKEN --> DRIVECLIENT[Drive API Client]
    DRIVECLIENT --> FOLDERLIST[List Drive Folder]
    FOLDERLIST --> FILELIST[List Audio Files]
```

---

# 15. Caching Strategy Diagram

```mermaid
flowchart TB
    START[App Requests Data] --> CACHECHECK{In-Memory Cache Available?}

    CACHECHECK -->|Yes| MEMORY[Return In-Memory Data]
    CACHECHECK -->|No| DISKCHECK{Disk Cache Available?}

    DISKCHECK -->|Yes| DISK[Load JSON Cache]
    DISKCHECK -->|No| FETCH[Fetch from MediaStore / Drive API]

    FETCH --> MAP[Map to Domain Models]
    MAP --> SAVEMEM[Save to In-Memory Cache]
    MAP --> SAVEDISK[Save to Disk Cache]
    SAVEMEM --> RESULT[Return to UI]
    SAVEDISK --> RESULT
    MEMORY --> RESULT
    DISK --> RESULT
```

---

# 16. Background Playback and Notification Flow

```mermaid
sequenceDiagram
    participant User
    participant UI
    participant PlayerController
    participant MediaSession
    participant NotificationManager
    participant ExoPlayer
    participant SystemUI

    User->>UI: Start playback
    UI->>PlayerController: play()
    PlayerController->>ExoPlayer: prepare + play
    PlayerController->>MediaSession: update metadata/state
    PlayerController->>NotificationManager: show notification
    NotificationManager->>SystemUI: media notification displayed

    User->>SystemUI: Tap pause on notification
    SystemUI->>NotificationManager: pause action
    NotificationManager->>PlayerController: pause()
    PlayerController->>ExoPlayer: pause
    PlayerController->>MediaSession: update paused state
```

---

# 17. Error Handling Design

```mermaid
flowchart TD
    ACTION[User Action] --> PROCESS[Processing Request]
    PROCESS --> CHECK{Success?}

    CHECK -->|Yes| SUCCESS[Update UI State]
    CHECK -->|No| TYPE{Error Type}

    TYPE -->|Permission Denied| PERM[Show permission guidance]
    TYPE -->|No Internet| NET[Show retry message]
    TYPE -->|Drive Token Expired| AUTH[Re-authenticate Google account]
    TYPE -->|Unsupported File| UNSUP[Skip file and log]
    TYPE -->|Playback Failure| PLAYERR[Show playback failed state]

    PERM --> UI[User Feedback]
    NET --> UI
    AUTH --> UI
    UNSUP --> UI
    PLAYERR --> UI
```

---

# 18. Permissions Design

```mermaid
flowchart LR
    APPSTART[App Start] --> PERMCHECK{Permissions Granted?}

    PERMCHECK -->|No| REQUEST[Request Audio/File Permissions]
    PERMCHECK -->|Yes| LOCALREADY[Enable Local Music]

    REQUEST --> RESULT{Granted?}
    RESULT -->|Yes| LOCALREADY
    RESULT -->|No| LIMITED[Show limited mode]

    LOCALREADY --> SCAN[Scan Local Files]
    LIMITED --> DRIVEONLY[Allow Drive-only usage]
```

---

# 19. End-to-End User Journey Diagram

```mermaid
flowchart TD
    START[Open App] --> HOME[Home Screen]
    HOME --> CHOICE{Choose Source}

    CHOICE --> LOCAL[Local Music]
    CHOICE --> DRIVE[Google Drive Music]

    LOCAL --> SCAN[Scan Device Audio]
    SCAN --> LOCALLIST[Show Local Songs]

    DRIVE --> LOGIN[Google Sign-In]
    LOGIN --> PICK[Select Drive Folder]
    PICK --> DRIVELIST[Show Drive Songs]

    LOCALLIST --> PLAY[Play Song]
    DRIVELIST --> PLAY

    PLAY --> NOWPLAYING[Now Playing Screen]
    NOWPLAYING --> LIKE[Like Song]
    NOWPLAYING --> ADDPL[Add to Playlist]
    NOWPLAYING --> BG[Background Playback]
```

---

# 20. Recommended Package Structure

```mermaid
flowchart TD
    ROOT[com.example.spotifish]

    ROOT --> PRESENTATION[presentation]
    ROOT --> DOMAIN[domain]
    ROOT --> DATA[data]
    ROOT --> PLAYER[player]
    ROOT --> CORE[core]

    PRESENTATION --> HOMEUI[home]
    PRESENTATION --> SEARCHUI[search]
    PRESENTATION --> LIBRARYUI[library]
    PRESENTATION --> PLAYERUI[player]
    PRESENTATION --> SETTINGSUI[settings]

    DOMAIN --> USECASES[usecase]
    DOMAIN --> MODELS[model]
    DOMAIN --> REPOIF[repository]

    DATA --> REPOIMPL[repository]
    DATA --> LOCAL[local]
    DATA --> DRIVE[drive]
    DATA --> PREFS[prefs]
    DATA --> CACHE[cache]
    DATA --> MAPPER[mapper]

    PLAYER --> CTRL[controller]
    PLAYER --> SESSION[session]
    PLAYER --> NOTIF[notification]
    PLAYER --> SERVICE[service]

    CORE --> DI[di]
    CORE --> UTIL[util]
    CORE --> EXT[extensions]
```

---

# 21. Suggested Service-Level Design for Playback

```mermaid
flowchart TD
    UI[UI Layer] --> BINDER[Bound Player Service Interface]
    BINDER --> SERVICE[Foreground Playback Service]
    SERVICE --> PLAYERCTRL[Player Controller]
    PLAYERCTRL --> EXO[ExoPlayer]
    PLAYERCTRL --> SESSION[MediaSession]
    SERVICE --> NOTIF[Persistent Notification]
```

---

# 22. Data Source Decision Diagram

```mermaid
flowchart TD
    SONG[Selected Song] --> TYPE{sourceType}

    TYPE -->|LOCAL| LOCALFLOW[Use file:// or content:// URI]
    TYPE -->|DRIVE| DRIVEFLOW[Request authenticated Drive stream URL]

    LOCALFLOW --> PLAYER[Pass media item to ExoPlayer]
    DRIVEFLOW --> TOKENCHECK{Valid Token?}
    TOKENCHECK -->|Yes| PLAYER
    TOKENCHECK -->|No| REFRESH[Re-authenticate / refresh token]
    REFRESH --> PLAYER
```

---

# 23. Offline-First Behavior Diagram

```mermaid
flowchart LR
    OPENAPP[Open App] --> LOADCACHED[Load Cached Metadata]
    LOADCACHED --> SHOWUI[Render UI Quickly]

    SHOWUI --> SYNCCHK{Source Available?}
    SYNCCHK -->|Local Available| REFRESHLOCAL[Refresh Local Scan]
    SYNCCHK -->|Drive Connected + Internet| REFRESHDRIVE[Refresh Drive File List]
    SYNCCHK -->|Unavailable| KEEPOLD[Keep Cached State]

    REFRESHLOCAL --> UPDATEUI[Update UI]
    REFRESHDRIVE --> UPDATEUI
    KEEPOLD --> UPDATEUI
```

---

# 24. JSON Storage Design

```mermaid
flowchart TD
    subgraph App Storage
        A[favorites.json]
        B[playlists.json]
        C[drive_folder_config.json]
        D[cached_songs.json]
        E[settings.json]
    end

    A --> F[Favorite Song IDs]
    B --> G[Playlist Definitions]
    C --> H[Selected Drive Folder ID]
    D --> I[Cached metadata]
    E --> J[Theme / preferences]
```

---

# 25. Deployment Context Diagram

```mermaid
flowchart LR
    USER[Android User] --> APP[Native Kotlin Android App]
    APP --> STORAGE[Device Storage / MediaStore]
    APP --> GOOGLEAUTH[Google Sign-In]
    APP --> GDRIVE[Google Drive API]
    APP --> AUDIO[Android Audio System]
    APP --> SYSUI[Notifications / Lock Screen]
```

---

## Notes for Implementation

### Recommended stack

- **UI:** Jetpack Compose or XML
- **Playback:** ExoPlayer
- **DI:** Hilt or Koin
- **Async:** Kotlin Coroutines + Flow
- **Architecture:** MVVM + Clean Architecture
- **Storage:** SharedPreferences + JSON files
- **Drive:** Google Sign-In + Drive REST API

### Important design choice

Since the app has **no backend and no traditional database**, the architecture should treat:

- **MediaStore** as local metadata provider
- **Google Drive API** as remote metadata provider
- **JSON + preferences** as lightweight persistence layer
- **In-memory cache** as performance layer
