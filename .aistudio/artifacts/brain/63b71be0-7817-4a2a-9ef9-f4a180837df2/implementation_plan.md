# OptiShare QR v1.0.3 — Suite Ottica Air-Gapped, Ottimizzazioni 120Hz & Release

Questo aggiornamento porta **OptiShare QR** alla versione **1.0.3 (`versionCode = 3`)**, introducendo quattro funzionalità offline avanzate (crittografia **AES-256-GCM**, feedback ottico bidirezionale **NACK QR**, archiviazione **Multi-File ZIP**, e **Generatore QR Standard** per Wi-Fi/vCard/Testo) insieme a ottimizzazioni profonde del motore di codifica/scansione e alla generazione della release APK e dei metadati F-Droid.

## User Review & Critical Decisions

> [!IMPORTANT]
> Tutte le scelte funzionali e di ottimizzazione richieste nella fase preliminare sono state integrate nel piano architetturale.

- **Funzionalità Offline Confermate**:
  1. **Crittografia AES-256-GCM con Password**: cifratura autenticata end-to-end opzionale del payload compresso tramite chiave derivata con `PBKDF2WithHmacSHA256` (salt casuale a 16 byte + IV a 12 byte + tag GCM a 128 bit) prima della suddivisione in pacchetti QR.
  2. **QR Inverso NACK (Richiesta Pacchetti Mancanti)**: generazione sul dispositivo ricevente di un singolo codice QR compatto (`QBF1_NACK`) contenente l'ID sessione e gli indici dei pacchetti ancora mancanti; scansionandolo dal trasmettitore, il flusso filtra e trasmette istantaneamente solo i frame richiesti.
  3. **Compressione Multi-File in Archivio ZIP**: selezione multipla di file dal dispositivo e impacchettamento automatico in un unico contenitore `.zip` verificato con SHA-256 prima della trasmissione ottica.
  4. **Generatore QR Standard (Wi-Fi, Contatto vCard, Testo)**: modalità dedicata per generare ed esportare codici QR universali leggibili da qualsiasi fotocamera di sistema (reti Wi-Fi `WIFI:S:...`, biglietti da visita `BEGIN:VCARD`, e testo/URI standard).
- **Ottimizzazioni Prestazionali e Usabilità Confermate**:
  1. **Generazione Parallela Multi-Core delle Bitmap QR + Zero-Alloc YUV**: codifica concorrente dei frame QR su tutti i core CPU (`coroutineScope` + `async(Dispatchers.Default)`) e riuso dei buffer di luminanza nello scanner CameraX per eliminare pause del Garbage Collector a 120Hz.
  2. **Boost Luminosità Automatica in Trasmissione**: innalzamento automatico della luminosità dello schermo al 100% (`screenBrightness = 1.0f`) e blocco spegnimento schermo (`KEEP_SCREEN_ON`) durante la riproduzione attiva dello stream QR, con ripristino automatico in pausa o cambio scheda.
  3. **Torcia Flash e Zoom Rapido (1x / 1.5x / 2x) nello Scanner**: controlli hardware diretti sul `CameraControl` di CameraX integrati nel mirino di ricezione e nella scansione NACK.
  4. **Telemetria Live di Velocità (KB/s) ed ETA**: calcolo in tempo reale del throughput effettivo in `KB/s`, frequenza di acquisizione (`pkt/s`) e stima del tempo residuo (`ETA`) sia sul trasmettitore sia sul ricevitore.
- **Strategia di Release 1.0.3 Confermata**:
  - Aggiornamento a `versionName = "1.0.3"` e `versionCode = 3`, aggiornamento dei changelog Fastlane/F-Droid (`it` ed `en-US`) e compilazione del pacchetto APK di release.

---

## 1. Overview & Core Concept

- **Cosa Fa**: Consente il trasferimento di file singoli o multipli (ZIP), foto, note e credenziali (Wi-Fi / vCard) tra dispositivi completamente offline (Air-Gapped) usando flussi QR ad alta frequenza sincronizzati a 120Hz, con correzione d'errore XOR FEC, crittografia autenticata AES-256-GCM e canale di ritorno ottico NACK.
- **Target Audience / Persona**: Utenti attenti alla privacy, sviluppatori, amministratori di sistema e utenti F-Droid che necessitano di scambiare dati sensibili o configurazioni senza Wi-Fi, Bluetooth, NFC o permessi di rete.
- **Valore Principale**: Massimizza la velocità e l'affidabilità del link ottico eliminando i colli di bottiglia della CPU durante la generazione dei QR e chiudendo il ciclo di trasmissione grazie al QR inverso NACK.

---

## 2. User Experience & Visual Design

- **Flussi Utente Principali**:
  1. **Invio Protetto o Multi-File**: L'utente sceglie *File Singolo*, *Multi-File ZIP*, *Foto*, *Nota* o *QR Standard (Wi-Fi/vCard)*. Può attivare il lucchetto **AES-256-GCM** inserendo una passphrase: l'app comprime, cifra e genera in parallelo su tutti i core i frame QR, portando automaticamente la luminosità del display al 100% durante la riproduzione a 120Hz VSync.
  2. **Ricezione Assistita con Torcia, Zoom e Decrittazione**: L'utente inquadra il flusso QR regolando lo zoom (`1.0x`, `1.5x`, `2.0x`) o attivando il Flash LED in ambienti bui. La barra telemetrica mostra velocità in `KB/s` ed `ETA`. Se lo stream è cifrato AES-256-GCM, al termine dell'assemblaggio viene richiesta la passphrase per decifrare e verificare l'impronta SHA-256.
  3. **Feedback Ottico Inverso (NACK QR)**: Se alcuni pacchetti non sono stati letti a causa di riflessi, il ricevitore tocca *"Mostra QR Mancanti (NACK)"* per visualizzare un QR riepilogativo dei soli indici mancanti. Il mittente tocca *"Scansiona NACK"* e il flusso QR si aggiorna istantaneamente trasmettendo solo i frame richiesti.
- **Identità Visiva & Tema**:
  - *Direzione Estetica*: **Swiss Technical Instrument & Material You (M3)** con supporto opzionale **Liquid Glass** e fluidità nativa **120Hz VSync**.
  - *Palette Cromatica*: Accenti ad alto contrasto (`SignalCobalt` `#2563EB`, `OpticCyan` `#06B6D4`, `VerifiedGreen` `#10B981`, `PhosphorAmber` `#F59E0B`) su superfici tonali calibrate e pad QR bianco puro `#FFFFFF` per il massimo rapporto di contrasto ottico.
  - *Tipografia*: `Space Grotesk` e `Plus Jakarta Sans` per le intestazioni e i controlli, abbinati a `JetBrains Mono` per hash SHA-256, CRC32, velocità `KB/s`, `ETA` e matrici di stato dei pacchetti.
- **Feedback Interattivo & Movimento**:
  - Transizioni `spring` a 120Hz prive di rimbalzo, feedback aptico su ogni nuovo pacchetto validato o NACK acquisito, e indicatori chiari per stream cifrati (badge lucchetto ambra/verde).

---

## 3. Key Product Decisions & Trade-Offs

- **Decisione 1: Crittografia Autenticata AES-256-GCM con PBKDF2 Nativo**
  - *Approccio Scelto*: Utilizzo delle API crittografiche standard `javax.crypto.Cipher` (`AES/GCM/NoPadding`) e `SecretKeyFactory` (`PBKDF2WithHmacSHA256`, 65.536 iterazioni, chiave a 256 bit) sull'array compresso GZIP, incapsulando `[16B Salt + 12B IV + Ciphertext + 16B AuthTag]`.
  - *Perché*: Garantisce riservatezza e autenticità crittografica senza aggiungere librerie esterne proprietarie, mantenendo il 100% di compatibilità F-Droid FOSS.
- **Decisione 2: Protocollo NACK Ottico Compatto (`QBF1_NACK`)**
  - *Approccio Scelto*: Formato QR leggero `QBF1_NACK|<sessionId>|<missingIndicesCsv>` con ECC livello `Q` per garantire una scansione istantanea anche a distanza.
  - *Perché*: Su file da 30–80 pacchetti, attendere l'intero ciclo per 2 pacchetti persi rallenta il trasferimento; un singolo colpo d'occhio al QR NACK riduce il tempo di completamento di oltre l'80%.
- **Decisione 3: Generazione QR Multi-Core & Riuso Buffer YUV**
  - *Approccio Scelto*: Suddivisione della generazione delle `Bitmap` QR in coroutine parallele su `Dispatchers.Default` e allocazione `ThreadLocal`/`remember` dei buffer `ByteArray` nel decoder YUV di CameraX.
  - *Perché*: Riduce il tempo di preparazione fino a 4–6 volte sui moderni SoC octa-core e azzera il jitter di allocazione memoria durante la scansione continua.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architettura & Diagramma dei Componenti**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        UI LAYER (120Hz VSync Compose)                       │
├───────────────────────────┬───────────────────────────┬─────────────────────┤
│       SendQrScreen        │     ReceiveScanScreen     │     VaultScreen     │
│ • Sorgenti: File, ZIP,    │ • CameraX Zoom (1x/1.5x/  │ • Storico Room DB   │
│   Foto, Nota, Standard QR │   2x) + Controllo Torcia  │ • Ricerca & Filtri  │
│ • Lock AES-256-GCM        │ • Telemetria KB/s & ETA   │ • Ritrasmissione QR │
│ • Boost Luminosità 100%   │ • Modale QR NACK Inverso  │ • Export Condiviso  │
│ • Scanner NACK Ricevitore │ • Sblocco Password AES    │                     │
└─────────────┬─────────────┴─────────────┬─────────────┴──────────┬──────────┘
              │                           │                        │
              ▼                           ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   STATE & ORCHESTRATION (TransferViewModel)                 │
│ • Gestione Stream Ottico, Filtro NACK, Password AES, Calcolo KB/s & ETA     │
│ • Generazione QR Parallela Multi-Core (coroutineScope + async Default)      │
└─────────────┬───────────────────────────┬────────────────────────┬──────────┘
              │                           │                        │
              ▼                           ▼                        ▼
┌───────────────────────────┐ ┌───────────────────────┐ ┌─────────────────────┐
│   OpticalPacketProtocol   │ │     QrCodecEngine     │ │ TransferRepository  │
│ • Multi-File ZIP Builder  │ │ • ZXing QR Writer/    │ │ • Room SQLite DAO   │
│ • GZIP + AES-256-GCM      │ │   Reader Ottimizzato  │ │ • Storage Locale    │
│ • XOR Parity FEC & CRC32  │ │ • Buffer YUV Riusati  │ │   Verificato SHA256 │
│ • Codec NACK & Standard   │ │   (Zero-GC Jitter)    │ │                     │
└───────────────────────────┘ └───────────────────────┘ └─────────────────────┘
```

- **Modello Dati & Estensioni Protocollo (`QBF1`)**:
  - **Pacchetto Cifrato**: Il campo `mimeType` del protocollo `QBF1` trasporta il suffisso `;enc=aes256gcm` quando il payload GZIP è cifrato con AES-256-GCM, permettendo al ricevitore di rilevare automaticamente che serve la password per decifrare e decomprimere il file a fine ricezione.
  - **Payload NACK (`QBF1_NACK|<sessionId>|<idx1,idx2,...>`)**: Decodificato da `OpticalPacketProtocol.decodeNackQrString`, aggiorna `selectedMissingDataIndices` nel `SenderUiState` per trasmettere solo i frame mancanti.
  - **Generatori QR Standard (`StandardQrPayload`)**: Costruttori formattati secondo gli standard internazionali per reti Wi-Fi (`WIFI:T:<WPA/WEP/nopass>;S:<SSID>;P:<Pass>;H:<hidden>;;`), contatti (`BEGIN:VCARD\nVERSION:3.0...`) e testo libero/URL.
- **Mappatura Interazioni & Stato**:
  - **Selettore Multi-File ZIP**: Utilizza `ActivityResultContracts.GetMultipleContents()` per raccogliere più `Uri`, li comprime in memoria con `ZipOutputStream` nel background thread e avvia `prepareStreamFromBytes` con nome `Archivio_MultiFile.zip`.
  - **Boost Luminosità & Keep Screen On**: `DisposableEffect(senderState.isPlaying)` in `SendQrScreen` imposta `window.attributes.screenBrightness = 1.0f` e `FLAG_KEEP_SCREEN_ON` quando la trasmissione QR è in riproduzione, ripristinando `BRIGHTNESS_OVERRIDE_NONE` in pausa o uscita.
  - **Controlli CameraX (Torcia & Zoom)**: `CameraXQrScannerPreview` espone lo stato `Camera` per pilotare `camera.cameraControl.enableTorch(Boolean)` e `camera.cameraControl.setZoomRatio(Float)` con pulsanti rapidi sovrapposti al mirino.
  - **Build Release 1.0.3**: Aggiornamento di `app/build.gradle.kts` (`versionCode = 3`, `versionName = "1.0.3"`), creazione dei changelog `fastlane/metadata/android/it/changelogs/3.txt` e `en-US/changelogs/3.txt`, aggiornamento di `docs/fdroiddata/com.aistudio.optishareqr.xkvpzr.yml` ed esecuzione del task Gradle `assembleRelease` copiando l'APK finale nella directory di output.
