# OptiShare QR — Migrazione 100% F-Droid Ready (FOSS & Air-Gapped)

Questo piano aggiorna **OptiShare QR** per renderla **100% conforme agli standard F-Droid (Free and Open Source Software)**, rimuovendo ogni SDK, plugin e file di configurazione proprietario Google/Firebase, eliminando il permesso `INTERNET` e concentrando l'esperienza utente esclusivamente sul trasferimento ottico QR offline e sull'Archivio locale.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Riepilogo delle scelte confermate per garantire la piena conformità F-Droid e l'assenza totale di Anti-Feature.

- **Decisione Confermata 1 — Rimozione Totale Dipendenze Proprietarie Google/Firebase**: Rimozione dei plugin Gradle `com.google.gms.google-services` e `secrets-gradle-plugin`, delle librerie `firebase-bom`, `firebase-ai`, `firebase-appcheck-*` e dei file di configurazione `google-services.json` e `firebase_applet_config.xml`. Tutte le librerie rimanenti (AndroidX Jetpack Compose, CameraX, Room Database, ZXing Core) sono **100% Open Source (Apache 2.0)** e compilabili da sorgente su F-Droid.
- **Decisione Confermata 2 — Rimozione Scheda Analisi AI**: Eliminazione della scheda `AI Studio` e dei relativi componenti, mantenendo una navigazione essenziale e veloce a 3 schede: **Invia QR**, **Ricevi** e **Archivio**.
- **Decisione Confermata 3 — Zero Permessi di Rete (`INTERNET` Rimosso) & Zero Blocchi Metadati Proprietari**: Rimozione di `<uses-permission android:name="android.permission.INTERNET" />` da `AndroidManifest.xml` e disattivazione di `dependenciesInfo` sia per APK che per Bundle (`includeInApk = false`, `includeInBundle = false`), requisito fondamentale dello scanner F-Droid (`fdroidserver`) per evitare il flag di blocco binario proprietario Google.

---

## 1. Overview & Core Concept

- **Cosa Fa**:
  1. **Invia QR (Trasmettitore Ottico)**: Seleziona qualsiasi file dal dispositivo, scatta una foto o crea una nota di testo, comprime i dati con **GZIP**, genera pacchetti con checksum **CRC32**, **SHA-256** e blocchi di **Parità XOR (FEC)** e li riproduce come sequenza di codici QR dinamici ad alto contrasto.
  2. **Ricevi (Scanner Fotocamera CameraX + ZXing)**: Inquadra il flusso QR dinamico, ricostruisce automaticamente i pacchetti persi tramite Parità XOR, mostra la matrice di integrità in tempo reale e verifica l'impronta **SHA-256** finale.
  3. **Archivio (Room Database Locale)**: Conserva localmente la cronologia dei file inviati e ricevuti con possibilità di ritrasmetterli via QR, rinominarli, condividerli con altre app tramite `FileProvider` o eliminarli.
- **Valore Chiave F-Droid**: Un'app di condivisione file realmente *air-gapped* che per architettura e permessi di sistema (`AndroidManifest.xml` privo di `INTERNET`) garantisce matematicamente che nessun byte possa mai uscire dal dispositivo tramite rete.

---

## 2. User Experience & Visual Design

### Flusso di Navigazione Semplificato (3 Schede)
1. **Invia QR**: Selezione file/foto/nota, calibrazione FPS (1–12 FPS), dimensione pacchetto (240B / 380B / 550B), livello ECC QR (L/M/Q/H) e filtro interattivo per ritrasmettere solo specifici pacchetti mancanti.
2. **Ricevi**: Mirino ottico CameraX con decodifica ZXing, griglia di stato dei pacchetti (`In attesa`, `Ricevuto CRC32`, `Recuperato via Parità XOR`), simulatore di loopback integrato e accesso diretto all'Archivio a fine ricezione.
3. **Archivio**: Filtri rapidi (`Tutti`, `Ricevuti`, `Inviati`), badge crittografico SHA-256, rinomina file, condivisione esterna e ritrasmissione QR.

---

## 3. Key Product Decisions & Trade-Offs

- **Decisione 1: Conformità Reproducible Builds & F-Droid Scanner**
  - *Approccio Scelto*: Rimozione del plugin `google-services`, rimozione del blocco `GEMINI_API_KEY` e impostazione di `dependenciesInfo { includeInApk = false; includeInBundle = false }`.
  - *Perché*: Il compilatore Android (AGP) inserisce di default un blocco binario cifrato con la chiave pubblica di Google (`DEPENDENCY_INFO_BLOCK`). Disattivandolo e rimuovendo tutte le librerie `com.google.firebase` / `com.google.android.gms`, l'APK supera i controlli automatici di `fdroid lint` e `fdroid scanner`.
- **Decisione 2: Stack 100% Apache 2.0**
  - *Approccio Scelto*: Mantenimento esclusivo di `androidx.*` (Jetpack Compose, CameraX, Room, Lifecycle), `org.jetbrains.kotlinx.*` e `com.google.zxing:core` (libreria pura Java open-source Apache 2.0 senza alcuna dipendenza da Google Play Services).

---

## 4. Technical Architecture & Data Strategy

### Architettura di Sistema F-Droid Ready

```
┌──────────────────────────────────────────────────────────────────────┐
│                   UI LAYER (Jetpack Compose M3)                      │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌──────────────┐  │
│  │    SendQrScreen     │  │  ReceiveScanScreen  │  │ VaultScreen  │  │
│  │ (Dynamic QR Stream, │  │ (CameraX + ZXing,   │  │ (Local Room  │  │
│  │  FPS/ECC/Filter)    │  │  XOR Matrix, SHA256)│  │  DB History) │  │
│  └──────────┬──────────┘  └──────────┬──────────┘  └──────┬───────┘  │
└─────────────┼────────────────────────┼────────────────────┼──────────┘
              │                        │                    │
              ▼                        ▼                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  VIEWMODEL & STATE LAYER (StateFlow)                 │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                       TransferViewModel                        │  │
│  │   (Sender Stream, Receiver Assembly, Loopback, Vault State)    │  │
│  └──────────────────┬─────────────────────────────┬───────────────┘  │
└─────────────────────┼─────────────────────────────┼──────────────────┘
                      │                             │
                      ▼                             ▼
      ┌──────────────────────────────┐ ┌──────────────────────────┐
      │     OpticalPacketProtocol    │ │    TransferRepository    │
      │ • GZIP Compression           │ │ • Room TransferDao       │
      │ • CRC32 & SHA-256            │ │ • Internal File Storage  │
      │ • XOR Parity (FEC)           │ │ • FileProvider Sharing   │
      │ • ZXing Pure-Java QR Codec   │ └──────────────────────────┘
      └──────────────────────────────┘
```

### Modifiche Tecniche Previste
- **Configurazione Gradle & Version Catalog**:
  - Rimozione dei plugin `secrets` e `google.services` e di tutte le dipendenze `firebase-*`, `credentials`, `googleid`, `play-services-*`, `retrofit`, `okhttp`, `moshi` inutilizzate.
  - Disattivazione completa di `dependenciesInfo` in `app/build.gradle.kts`.
- **Pulizia Risorse & Sorgenti Proprietari**:
  - Eliminazione di `google-services.json`, `firebase_applet_config.xml`, `AppCheckConfig.kt`, `AiStudioViewModel.kt` e `AiStudioScreen.kt`.
- **AndroidManifest.xml**:
  - Rimozione del permesso `android.permission.INTERNET`.
  - Mantenimento dei soli permessi `CAMERA` (con `android:required="false"`) e `VIBRATE`.
