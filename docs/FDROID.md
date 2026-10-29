# Preparazione per F-Droid

Questa procedura riguarda il **catalogo ufficiale di F-Droid**, non un repository
F-Droid gestito in proprio. La presenza dei metadati in questo progetto non
significa che l'app sia stata inviata, approvata o pubblicata.

## Stato e decisioni necessarie

- Il repository GitHub è privato: F-Droid deve poter scaricare pubblicamente
  il codice sorgente completo e aggiornato. Il proprietario deve decidere se
  renderlo pubblico oppure pubblicare un repository sorgente pubblico equivalente.
- È stata aggiunta una licenza MIT per il codice originale (`LICENSE`), secondo
  la scelta del proprietario. Prima della distribuzione, i titolari dei diritti
  devono verificare che sia applicabile a tutti i contributi originali. La
  licenza e gli altri cambiamenti devono essere integrati nella versione pubblica.
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
3. Controllare che l'app non sia già presente nel catalogo o nella coda delle
   richieste. Presentare una richiesta di pacchettizzazione oppure una merge
   request a `fdroiddata` su GitLab con i metadati di build, indicando repository
   pubblico, licenza, versione, `versionCode` e commit/tag verificato.
4. Rispondere all'eventuale revisione dei manutentori. L'inclusione e la
   pubblicazione dipendono dall'approvazione e dalla build di F-Droid.

Riferimenti ufficiali:

- [Guida alla sottomissione](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
- [Criteri di inclusione](https://f-droid.org/docs/Inclusion_Policy/)
- [Metadati, descrizioni e immagini](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
- [FAQ per sviluppatori, incluse le firme](https://f-droid.org/docs/FAQ_-_App_Developers/)
