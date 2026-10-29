# Preparazione per F-Droid

Questa procedura riguarda il **catalogo ufficiale di F-Droid**, non un repository
F-Droid gestito in proprio. La presenza dei metadati in questo progetto non
significa che l'app sia stata approvata o pubblicata. Una richiesta è aperta:
[F-Droid RFP #4700](https://gitlab.com/fdroid/rfp/-/work_items/4700).

## Cosa è stato sistemato per la richiesta

- **Gradle wrapper completo.** Il repository contiene ora `gradlew`,
  `gradlew.bat` e `gradle/wrapper/gradle-wrapper.jar`, presi dalla distribuzione
  ufficiale di Gradle 9.3.1. `gradle/wrapper/gradle-wrapper.properties` indica
  la versione e il relativo `distributionSha256Sum`
  (`b266d5ff…aff06`, identico a quello pubblicato nel
  [gradle-transparency-log](https://gitlab.com/fdroid/gradle-transparency-log)
  usato da F-Droid). In precedenza c'era solo il file `.properties`, quindi
  l'etichetta `insecure-gradlew` dell'_issuebot_: era un avviso "giallo", non
  bloccante per la build di F-Droid, ma va comunque corretto upstream.
- **Build di release senza firma.** `app/build.gradle.kts` applica la
  configurazione di firma di AI Studio solo quando keystore e password sono
  disponibili. Senza quei dati la release resta non firmata
  (`app-release-unsigned.apk`), che è esattamente ciò che serve a F-Droid, che
  compila dai sorgenti e firma con la propria chiave. È stata verificata
  anche la compatibilità con la rimozione automatica della firma eseguita da
  `fdroidserver` (`remove_signing_keys`, in `common.py`): il blocco
  `signingConfigs { … }` e la riga `signingConfig = …` vengono eliminati con
  delle espressioni regolari, e il file che resta è ancora bilanciato e valido.
- **Metadati Fastlane.** Titolo e descrizioni in italiano e inglese con le note
  di rilascio in `changelogs/1.txt` per il `versionCode` 1.
- **Tag sorgente immutabile.** Il commit di release è identificato dal tag
  `v1.0`, così la richiesta può indicare uno snapshot preciso e F-Droid può
  seguire i tag per gli aggiornamenti.
- **Metadati di build proposti.** `docs/fdroiddata/com.aistudio.optishareqr.xkvpzr.yml`
  contiene la ricetta pronta per `fdroiddata` (`subdir: app`, `gradle: [yes]`,
  `commit: v1.0`), da allegare alla richiesta o da proporre con una merge request.
- **Verifica della build in CI.** `.github/workflows/build.yml` esegue
  `./gradlew assembleRelease` a ogni push e pubblica l'APK non firmato come
  artefatto, così l'esito della build è visibile e l'APK è confrontabile con
  quello prodotto da F-Droid.

## Da verificare prima di chiedere la pubblicazione

- **La build non è ancora stata eseguita.** In questo ambiente non sono
  disponibili JDK, Android SDK e accesso ai repository Maven, quindi nessuna
  compilazione è stata provata: la prima esecuzione utile è quella della CI su
  GitHub. F-Droid userà `gradlew-fdroid`, che legge
  `gradle/wrapper/gradle-wrapper.properties` e scarica Gradle 9.3.1 verificandone
  l'hash: la versione è presente nel _gradle-transparency-log_.
- **Toolchain.** L'app usa Android Gradle Plugin 9.1.1, `compileSdk 36` con
  `minorApiLevel = 1` e `targetSdk 36`. Se l'infrastruttura di F-Droid non ha
  la piattaforma SDK o un JDK abbastanza recente, la build fallirà e andrà adattata
  la ricetta (per esempio abbassando `compileSdk`).
- **Prova su dispositivi.** Nessun test su telefono o emulatore è stato fatto:
  F-Droid chiede che l'app funzioni davvero.
- **Icona e screenshot.** Mancano screenshot reali e un'icona dedicata nei
  metadati Fastlane; vanno aggiunti prima della pubblicazione
  (`fastlane/metadata/android/<locale>/images/`).
- **Politica di inclusione.** Il repository GitHub è pubblico ed è stato creato
  il 29 ottobre 2026; la
  [politica di inclusione](https://f-droid.org/docs/Inclusion_Policy/) richiede
  normalmente almeno tre mesi di sviluppo attivo, con alcune eccezioni (per
  esempio utenti dimostrabili). Vanno portate ai revisori le prove di
  sviluppo precedente o l'eccezione applicabile.
- **Application ID.** L'attuale ID è `com.aistudio.optishareqr.xkvpzr`. Va
  confermato prima della prima pubblicazione, perché cambiarlo in seguito crea
  un'app distinta. Il `namespace` Gradle è ancora `com.example`: si può
  riallineare senza conseguenze sull'identità dell'app, ma solo dopo aver
  ricompilato con successo.
- **Licenze e risorse.** Licenza MIT per il codice originale (`LICENSE`); i font
  in `app/src/main/res/font/` sono SIL OFL 1.1 con i testi in `licenses/fonts/`.
  Vanno verificate versione, provenienza ed eventuali modifiche dei font e
  delle altre risorse. F-Droid effettua una propria revisione.
- **File `metadata.json` e `.env.example`.** Sono descrizioni del progetto AI
  Studio e citano Firebase AI Logic e l'API Gemini, ma l'app non dichiara il
  permesso `INTERNET` e non contiene dipendenze di rete: va detto ai revisori,
  per evitare che vengano lette come traccianti.

## Metadati e invio

1. Completare le verifiche precedenti e provare l'app su dispositivi. Ogni
   release deve avere un tag immutabile pubblico.
2. Completare i metadati in `fastlane/metadata/android/` con icona e screenshot.
3. Aggiornare la [richiesta RFP #4700](https://gitlab.com/fdroid/rfp/-/work_items/4700)
   con quanto sistemato (il testo pronto è in [FDROID-RFP.md](FDROID-RFP.md)).
   Non aprire una seconda richiesta. In alternativa si può proporre una merge
   request a `fdroiddata` con i metadati di build verificati.
4. Rispondere all'eventuale revisione dei manutentori. L'inclusione e la
   pubblicazione dipendono dall'approvazione e dalla build di F-Droid.

Riferimenti ufficiali:

- [Guida alla sottomissione](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
- [Criteri di inclusione](https://f-droid.org/docs/Inclusion_Policy/)
- [Metadati, descrizioni e immagini](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
- [Riferimento dei metadati di build](https://f-droid.org/docs/Build_Metadata_Reference/)
- [FAQ per sviluppatori, incluse le firme](https://f-droid.org/docs/FAQ_-_App_Developers/)
