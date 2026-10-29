# OptiShare QR — Condivisione File Ottica via Fotocamera & AI

**OptiShare QR** è un'applicazione Android progettata per trasferire file completamente offline da un dispositivo all'altro attraverso un flusso ottico di **codici QR dinamici (Multi-Frame Fountain QR)** generati sullo schermo del mittente e catturati in tempo reale dalla fotocamera del destinatario, arricchita da un motore di **intelligenza artificiale avanzata (Gemini 4 Argon con High Thinking)** per estrazione testo, riassunto, compressione semantica e rinomina intelligente dei file.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Riepilogo delle scelte confermate e dell'architettura tecnica progettata per garantire trasferimenti ottici affidabili al 100% e zero perdita di dati.

- **Decisione Confermata 1 — Protocollo Ottico QR Dinamico Full-Offline**: I file selezionati o acquisiti vengono compressi (GZIP), suddivisi in pacchetti indicizzati e codificati in una sequenza ciclica di codici QR ad alto contrasto. Ogni frame include checksum **CRC32** del singolo pacchetto, **SHA-256** dell'intero file e **pacchetti di parità XOR (Forward Error Correction)** per ricostruire automaticamente i frame persi senza bisogno di connessione internet, Bluetooth o Wi-Fi.
- **Decisione Confermata 2 — Estrazione Testo, Riassunto e Rinomina Smart con AI (High Thinking)**: Integrazione con **Firebase AI Logic** utilizzando il modello `gemini-4-argon` configurato con `ThinkingLevel.HIGH` per analizzare foto, documenti e file, estrarre il testo (OCR), generare un riassunto strutturato, suggerire nomi file semantici e offrire una modalità **"Compressione Smart AI"** che riduce drasticamente il peso dei documenti fotografati prima dell'invio via QR.
- **Decisione Confermata 3 — Archivio Locale Veloce e Offline**: Tutti i file inviati, ricevuti e analizzati vengono salvati nella memoria locale dell'app con metadati indicizzati in **Room Database** (hash SHA-256, stato verifica integrità, testo estratto, riassunto AI e cronologia trasferimenti).

---

## 1. Overview & Core Concept

- **Cosa Fa**:
  1. **Trasmettitore Ottico (Invia File)**: L'utente seleziona un file dal dispositivo (o scatta una foto/documento) e l'app genera uno stream fluido di codici QR dinamici con velocità (FPS), dimensione pacchetto e livello di correzione errori regolabili, oltre al filtro rapido per ritrasmettere solo specifici pacchetti mancanti.
  2. **Ricevitore Fotocamera (Ricevi File)**: L'altro utente inquadra lo schermo con la fotocamera (**CameraX** + decodifica QR ad alta velocità 100% offline). Un'interfaccia HUD mostra in tempo reale la matrice dei pacchetti ricevuti, i pacchetti recuperati tramite parità XOR, la velocità di trasferimento, la barra di avanzamento e la verifica crittografica finale SHA-256.
  3. **AI Studio Documentale (OCR, Riassunto & Smart Rename)**: Analisi approfondita con `gemini-4-argon` (`ThinkingLevel.HIGH`) per estrarre testo da immagini o file, riassumere documenti complessi e rinominare automaticamente i file prima o dopo il trasferimento.
  4. **Simulatore Loopback Integrato**: Permette di testare e verificare l'intero flusso di codifica/decodifica QR multi-frame e recupero errori anche su un singolo dispositivo o emulatore.
- **Target Audience**: Utenti che necessitano di scambiare documenti, note, immagini compresse, chiavi o file in ambienti completamente isolati dalla rete (air-gapped / full offline) con verifica matematica dell'integrità.
- **Valore Chiave**: Condivisione "punta e inquadra" istantanea, immune da problemi di accoppiamento Bluetooth/Wi-Fi Direct, con garanzia di integrità crittografica e intelligenza documentale.

---

## 2. User Experience & Visual Design

### Flussi Utente Principali
1. **Flusso Invio (Trasmettitore QR Dinamico)**:
   - Selezione file tramite File Picker di sistema, acquisizione fotocamera o creazione rapida di nota/testo.
   - Analisi preliminare del file: calcolo automatico dimensione originale vs compressa (GZIP), impronta SHA-256, numero di pacchetti dati + pacchetti di parità XOR.
   - Schermata di riproduzione QR ad alta visibilità (cornice bianca ad alto contrasto per massimizzare la lettura ottica anche in condizioni di luce difficili):
     - Controlli in tempo reale per **Frequenza Frame (1–12 FPS)**, **Dimensione Chunk** e **Livello ECC QR (L / M / Q / H)**.
     - Selettore interattivo **"Ritrasmetti Pacchetti Specifici"**: consente di toccare i numeri dei pacchetti eventualmente mancanti sul ricevitore per trasmettere solo quelli in loop rapido.
2. **Flusso Ricezione (Scanner Fotocamera con HUD in Tempo Reale)**:
   - Apertura mirino **CameraX** con guida ottica di allineamento e feedback aptico al rilevamento di ogni nuovo frame QR.
   - **Matrice di Integrità Live**: griglia visiva in cui ogni cella rappresenta un pacchetto del file (`In attesa`, `Ricevuto e Verificato CRC32`, `Recuperato via Parità XOR`, `Errore CRC scartato`).
   - Avanzamento percentuale, contatore pacchetti (`es. 18/20`), bitrate stimato e ricostruzione automatica non appena tutti i pacchetti (o `N-1` per blocco grazie alla parità) sono disponibili.
   - Verifica finale **SHA-256** con badge di conferma ("Integrità Verificata al 100%") e salvataggio automatico nell'archivio locale.
3. **Flusso AI Smart Studio**:
   - Dal dettaglio di qualsiasi file inviato/ricevuto o dalla scheda dedicata **AI Studio**, l'utente avvia l'analisi con `gemini-4-argon` (`ThinkingLevel.HIGH`).
   - L'AI restituisce in formato strutturato: **Nome File Consigliato** (applicabile con un tocco), **Categoria**, **Riassunto Esecutivo**, **Punti Chiave** e **Testo Completo Estratto (OCR)**, con opzione per generare subito un QR Stream ultraleggero del testo estratto.

### Identità Visiva & Tema
- **Direzione Estetica**: *Precision Optical Instrument (Tactical High-Contrast Dark & Daylight Studio)* — ispirato alle interfacce degli strumenti ottici professionali e degli scanner telemetrici.
- **Palette Colori (Material 3 Semantic Tokens)**:
  - **Sfondo & Superfici**: `Background` `#0B0F17` (Deep Obsidian) e `SurfaceContainer` `#151C28` (Slate Navy) in modalità scura; `#F6F8FC` e `#FFFFFF` in modalità chiara.
  - **Accento Primario**: `#00E5FF` (Optical Cyan) per il mirino laser, indicatori di scansione attiva e controlli principali.
  - **Accento Secondario / Parità**: `#7C4DFF` (Quantum Violet) per l'elaborazione AI High-Thinking e i blocchi di recupero parità FEC.
  - **Conferma Integrità**: `#00E676` (Verified Emerald) per checksum CRC32/SHA-256 validati; `#FF5252` (Signal Coral) per pacchetti corrotti scartati.
- **Tipografia & Gerarchia (Font Locali Bundle in `res/font/`)**:
  - **Display & Titoli (`Space Grotesk`)**: Geometrico e tecnico per intestazioni, contatori di progresso e badge di stato.
  - **Corpo Testo (`Plus Jakarta Sans`)**: Massima leggibilità per riassunti AI, testo estratto e controlli UI.
  - **Dati Tecnici & Hash (`JetBrains Mono`)**: Monospazio per impronte SHA-256, CRC32, indici pacchetto e telemetria FPS.

---

## 3. Key Product Decisions & Trade-Offs

- **Decisione 1: Codifica e Decodifica QR 100% Offline + Forward Error Correction (FEC)**
  - *Approccio Scelto*: Utilizzo di un motore di codifica/decodifica QR interamente on-device abbinato a un protocollo custom a pacchetti (`QBF1`) con compressione GZIP, checksum CRC32 per singolo frame, hash SHA-256 globale e **pacchetti di parità XOR** (1 pacchetto di parità ogni blocco di $K$ pacchetti dati).
  - *Perché*: Durante la scansione ad alta velocità tramite fotocamera, un singolo riflesso o battito di ciglia può far perdere un frame. Grazie ai pacchetti di parità XOR e alla ripetizione ciclica (Fountain Loop), il ricevitore ricostruisce il file senza errori al primo giro completo o recupera istantaneamente un frame mancante senza dover attendere l'intero ciclo.
- **Decisione 2: Compressione Pre-Trasferimento & Modalità Testo/Immagine Ottimizzata**
  - *Approccio Scelto*: Compressione automatica GZIP dei byte del file e ridimensionamento opzionale intelligente per le immagini pesanti, più estrazione OCR AI per inviare il contenuto testuale in pochi QR code.
  - *Perché*: I codici QR hanno una capacità fisica limitata (~500–1200 byte per frame per mantenere una lettura rapida da fotocamera). Ottimizzare il payload garantisce trasferimenti fluidi e privi di errori di messa a fuoco.
- **Decisione 3: Persistenza Locale Room Database + File System Interno**
  - *Approccio Scelto*: I file ricostruiti vengono salvati nella directory sicura dell'app (`context.filesDir/transfers`) e indicizzati in **Room Database** con flussi reattivi `StateFlow`.
  - *Perché*: Garantisce accesso istantaneo 100% offline, verifica storica degli hash SHA-256 e possibilità di condividere o ri-trasmettere qualsiasi file ricevuto in qualsiasi momento.

---

## 4. Technical Architecture & Data Strategy

### Architettura di Sistema & Componenti

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        UI LAYER (Jetpack Compose M3)                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌─────────────┐  ┌──────────┐  │
│  │  SendQrScreen    │  │ ReceiveScanScreen│  │ AiStudioScr │  │ VaultScr │  │
│  │ (Dynamic QR Gen, │  │ (CameraX HUD,    │  │ (OCR, Smart │  │ (Room DB │  │
│  │  FPS/ECC/Filter) │  │  Chunk Matrix)   │  │  Rename/Sum)│  │  History)│  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬──────┘  └────┬─────┘  │
└───────────┼─────────────────────┼───────────────────┼──────────────┼────────┘
            │                     │                   │              │
            ▼                     ▼                   ▼              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     VIEWMODEL & STATE LAYER (StateFlow)                     │
│  ┌────────────────────────────────────┐  ┌───────────────────────────────┐  │
│  │       TransferViewModel            │  │        AiStudioViewModel      │  │
│  │ (Sender Stream, Receiver Assembly, │  │ (Firebase AI gemini-4-argon,  │  │
│  │  Loopback Simulator, Vault State)  │  │  ThinkingLevel.HIGH, Schema)  │  │
│  └────────┬───────────────────┬───────┘  └───────────────┬───────────────┘  │
└───────────┼───────────────────┼──────────────────────────┼──────────────────┘
            │                   │                          │
            ▼                   ▼                          ▼
┌──────────────────────┐ ┌────────────────────────┐ ┌─────────────────────────┐
│ OpticalQrProtocol    │ │ TransferRepository     │ │ Firebase AI Logic SDK   │
│ • GZIP Compression   │ │ • Room TransferDao     │ │ • AppCheck Configured   │
│ • CRC32 & SHA-256    │ │ • Local File Storage   │ │ • gemini-4-argon        │
│ • XOR Parity (FEC)   │ │ • Integrity Validator  │ │ • ThinkingLevel.HIGH    │
│ • QR Bitmap Codec    │ └────────────────────────┘ └─────────────────────────┘
└──────────────────────┘
```

### Protocollo Pacchetti Ottici (`OpticalPacketProtocol`)
Ogni codice QR del flusso codifica una stringa strutturata compatta:
```
QBF1|<sessionId>|<packetType:D/P>|<index>|<totalData>|<blockSize>|<crc32Hex>|<sha256Prefix>|<fileName>|<mimeType>|<base64Payload>
```
- `packetType`: `D` (Data Chunk) oppure `P` (Parity XOR Chunk calcolato su un gruppo di $K$ pacchetti dati).
- `crc32Hex`: Checksum a 32 bit calcolato sui byte del singolo pacchetto per scartare immediatamente letture ottiche parziali o corrotte.
- `sha256Prefix` + Header Metadata: Permette al ricevitore di agganciarsi al flusso a partire da **qualsiasi** frame (anche se inizia a inquadrare a metà trasmissione) e verificare l'hash SHA-256 finale del file riassemblato.

### Mappatura Componenti Interattivi & Gestione Stato
- **`SendQrScreen` (Trasmettitore)**:
  - Selezione file tramite `ActivityResultContracts.GetContent()` / `TakePicturePreview()` o creazione nota rapida.
  - Generazione asincrona dei pacchetti e rendering fluido con `LaunchedEffect(isPlaying, fps, selectedChunkFilter)` che cicla i bitmap QR.
  - Controlli interattivi per FPS (1–12 FPS), dimensione pacchetto (250B / 450B / 700B), livello ECC e filtro per ritrasmettere specifici pacchetti mancanti.
- **`ReceiveScanScreen` (Ricevitore CameraX)**:
  - Gestione permesso `Manifest.permission.CAMERA` tramite `ActivityResultContracts.RequestPermission()`.
  - Analisi frame `CameraX ImageAnalysis` (`STRATEGY_KEEP_ONLY_LATEST`) con decodifica ZXing multi-orientamento e inversione luminanza per schermi scuri/chiari.
  - Aggiornamento reattivo della griglia dei pacchetti, calcolo recupero parità XOR quando manca un solo pacchetto in un blocco, verifica SHA-256 finale e salvataggio automatico.
  - Modalità **Simulazione Loopback** integrata per testare l'intero flusso di ricezione e ricostruzione errori direttamente sull'emulatore.
- **`AiStudioScreen` & Dialoghi Smart (`gemini-4-argon` + `ThinkingLevel.HIGH`)**:
  - Configurazione `Firebase.ai.generativeModel` con `modelName = "gemini-4-argon"`, `thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.HIGH }` e `responseSchema = Schema.obj(...)`.
  - Funzionalità **Analizza File / Foto**: estrae il testo completo, genera un riassunto strutturato, elenca i punti chiave e propone un nome file semantico applicabile con un tap nel database Room.
