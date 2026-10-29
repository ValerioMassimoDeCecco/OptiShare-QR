# Bozza richiesta di pacchettizzazione F-Droid

Questa è la bozza usata per la richiesta già aperta:
[F-Droid RFP #4700](https://gitlab.com/fdroid/rfp/-/work_items/4700). Non
aprire una seconda richiesta. Completare la checklist ufficiale solo dopo aver
verificato ciascun punto. Non è stato possibile verificare l'intera coda delle
richieste per individuare eventuali duplicati.

---

**Title:** OptiShare QR

**Application ID:** `com.aistudio.optishareqr.xkvpzr`

**Source code:** <https://github.com/ValerioMassimoDeCecco/OptiShare-QR>

**Source snapshot:** `a9330eed43805be848ac170ccd5b6e143334b012` (current public
main at the time of drafting; there is no tagged release yet)

**License:** MIT for the original project code; bundled fonts are under the
SIL Open Font License 1.1. See `LICENSE` and `licenses/fonts/` in the repository.

**Suggested category:** Connectivity

**Summary:** Transfer files offline using sequences of QR codes

**Description:** OptiShare QR transfers files between devices using sequences
of QR codes without a network connection. It uses GZIP compression, CRC32
packet checks, SHA-256 file integrity verification and XOR parity packets to
help recover missing data. Transfers are kept in a local archive. QR codes are
not encrypted.

**Version:** 1.0 (`versionCode` 1), as declared in `app/build.gradle.kts`.

**Build information:** Android/Gradle project; app module is `app`. The
repository contains Fastlane descriptions in English and Italian. The project
specifies Gradle 9.3.1 and Android Gradle Plugin 9.1.1, but the Gradle wrapper
scripts and JAR are not included. The release build configures an upstream
keystore; F-Droid's signing removal and the resulting unsigned build have not
yet been tested. No source release tag has been created. Font provenance and
other dependencies still require review. The app author requested submission.

---

Non dichiarare verificati conformità, assenza di duplicati, compilazione o test
su dispositivi finché non sono stati effettivamente controllati. L'accettazione
è soggetta alla revisione dei manutentori F-Droid.
