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

---

## Aggiornamento da pubblicare come commento sulla richiesta #4700

Testo pronto, in inglese, da aggiungere come commento alla
[richiesta già aperta](https://gitlab.com/fdroid/rfp/-/work_items/4700), dopo
aver aggiornato l'eventuale commit e aver verificato la build in CI. Le parti
tra parentesi quadre vanno confermate o cancellate.

```text
Update from upstream:

* The Gradle wrapper is now complete: gradlew, gradlew.bat and
  gradle/wrapper/gradle-wrapper.jar, taken from the official Gradle 9.3.1
  distribution. gradle/wrapper/gradle-wrapper.properties pins 9.3.1 with its
  distributionSha256Sum, which matches the hash published in F-Droid's
  gradle-transparency-log, so gradlew-fdroid can build with it.
* The release build type no longer requires the AI Studio keystore: when the
  keystore or its passwords are missing the release APK is left unsigned
  (app-release-unsigned.apk), which is what F-Droid needs to sign the APK
  itself. This was checked against remove_signing_keys in fdroidserver, which
  strips signingConfigs from app/build.gradle.kts: the resulting file is still
  valid and balanced.
* Source snapshot: tagged release v1.0 (versionName 1.0, versionCode 1).
* Fastlane metadata for en-US and it now includes the changelog for
  versionCode 1.
* Proposed build metadata: docs/fdroiddata/com.aistudio.optishareqr.xkvpzr.yml
  (subdir: app, gradle: yes, commit: v1.0). No scanignore is needed:
  fdroidserver's scanner explicitly allows gradle/wrapper/gradle-wrapper.jar,
  gradlew and gradlew.bat.
* GitHub Actions runs ./gradlew assembleRelease on every push and publishes the
  unsigned APK as an artifact, so the build result is public.

Still open, and not yet verified:
* [no build has been run locally: no JDK, Android SDK or Maven access in the
  author's environment, so the first verification is the CI run]
* [the app is untested on real devices]
* [screenshots and a dedicated icon are missing from the Fastlane metadata]
* [the repository is younger than the three months of active development
  required by the inclusion policy]
* [font provenance and the other bundled assets still need review]
```
