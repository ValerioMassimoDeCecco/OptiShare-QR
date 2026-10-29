# Preparazione per F-Droid

Questa procedura riguarda il **catalogo ufficiale di F-Droid**, non un repository
F-Droid gestito in proprio. La presenza dei metadati in questo progetto non
significa che l'app sia stata approvata o pubblicata. Una richiesta è stata
aperta: [F-Droid RFP #4700](https://gitlab.com/fdroid/rfp/-/work_items/4700).

## Stato e decisioni necessarie

- Il repository GitHub è pubblico e contiene il codice sorgente. Il repository
  è stato creato il 29 ottobre 2026; la [politica di inclusione](https://f-droid.org/docs/Inclusion_Policy/)
  richiede normalmente almeno tre mesi di sviluppo attivo, con alcune eccezioni
  (per esempio utenti dimostrabili). Verificare se esistono prove di sviluppo
  precedente o se si applica un'eccezione, e comunicarlo ai revisori.
- È stata aggiunta una licenza MIT per il codice originale (`LICENSE`), secondo
  la scelta del proprietario. Prima della distribuzione, i titolari dei diritti
  devono verificare che sia applicabile a tutti i contributi originali.
- I font in `app/src/main/res/font/` indicano la licenza SIL OFL; i testi di
  licenza upstream e le attribuzioni sono in `licenses/fonts/`. Verificarne le
  versioni e possibili modifiche, oltre a provenienza e licenze delle altre
  risorse e dipendenze. F-Droid effettua una propria revisione.
- La configurazione Gradle prevede una firma di release per AI Studio. Occorre
  verificare una build di F-Droid senza le credenziali di quel keystore: il
  catalogo normalmente compila e firma gli APK con le proprie chiavi. Gli APK
  firmati con chiavi diverse non si aggiornano a vicenda; valutare la procedura
  per build riproducibili se serve distribuire l'APK firmato dall'autore.
- Il progetto contiene `gradle-wrapper.properties`, ma non gli script del
  wrapper né il relativo JAR. Documentare e verificare una build da riga di
  comando con le versioni corrette di Gradle, JDK e Android SDK. La build e la
  compatibilità dell'attuale toolchain con l'infrastruttura F-Droid non sono
  ancora state verificate.

## Metadati e invio

1. Completare le decisioni e le verifiche precedenti, provare l'app su dispositivi
   e preparare una release sorgente pubblica identificata da un tag immutabile.
   L'attuale application ID è `com.aistudio.optishareqr.xkvpzr`; verificarlo
   prima della prima pubblicazione, perché cambiarlo successivamente crea
   un'app distinta.
2. Completare i metadati in `fastlane/metadata/android/`: sono disponibili
   titolo e descrizioni in italiano e inglese. Aggiungere un'icona adatta,
   screenshot reali e note di rilascio in `changelogs/<versionCode>.txt`.
3. La [richiesta RFP #4700](https://gitlab.com/fdroid/rfp/-/work_items/4700)
   è stata aperta. Verificare eventuali duplicati nel catalogo e nella coda,
   completare la checklist richiesta e seguire le risposte dei manutentori. La
   [bozza originale](FDROID-RFP.md) è conservata come riferimento: non aprire
   una seconda richiesta. In alternativa si può proporre una merge request a
   `fdroiddata` con i metadati di build verificati.
4. Rispondere all'eventuale revisione dei manutentori. L'inclusione e la
   pubblicazione dipendono dall'approvazione e dalla build di F-Droid.

Riferimenti ufficiali:

- [Guida alla sottomissione](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
- [Criteri di inclusione](https://f-droid.org/docs/Inclusion_Policy/)
- [Metadati, descrizioni e immagini](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
- [FAQ per sviluppatori, incluse le firme](https://f-droid.org/docs/FAQ_-_App_Developers/)
