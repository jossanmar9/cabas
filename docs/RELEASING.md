# Publicar una versió

Les versions es compilen i se signen a GitHub Actions quan es puja una etiqueta `v…`. La clau de signatura no entra mai al repositori: només viu en els secrets de GitHub i en la teua còpia privada.

## 1. Crear la clau de signatura (una sola vegada)

Necessites `keytool`, que ve amb qualsevol JDK (també amb Android Studio). En Linux o en la terminal de Windows:

```sh
keytool -genkeypair -v -keystore cabas-release.jks -alias cabas \
  -keyalg RSA -keysize 4096 -validity 10000
```

Apunta les contrasenyes. **Guarda `cabas-release.jks` i les contrasenyes en un lloc segur i fora del repositori** (per exemple, en un gestor de contrasenyes i una còpia en una memòria USB). Si la perds, no podràs publicar actualitzacions que s’instal·len damunt de les anteriors.

## 2. Afegir els secrets a GitHub (una sola vegada)

En GitHub: **Settings → Secrets and variables → Actions → New repository secret**.

| Secret | Valor |
| --- | --- |
| `CABAS_KEYSTORE_BASE64` | el fitxer `.jks` en base64 (vegeu baix) |
| `CABAS_KEYSTORE_PASSWORD` | contrasenya del magatzem |
| `CABAS_KEY_ALIAS` | `cabas` |
| `CABAS_KEY_PASSWORD` | contrasenya de la clau |

Per obtindre el base64:

```sh
# Linux
base64 -w0 cabas-release.jks
# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("cabas-release.jks"))
```

Amb la CLI de GitHub també es pot fer: `gh secret set CABAS_KEYSTORE_BASE64 < fitxer-base64.txt`.

## 3. Publicar

1. Actualitza `versionCode` (sempre +1) i `versionName` en `app/build.gradle.kts`.
2. Mou les entrades de «Sense publicar» del `CHANGELOG.md` a la nova versió i afig `fastlane/metadata/android/ca/changelogs/<versionCode>.txt`.
3. Fes commit a `main` i crea l’etiqueta amb el mateix nom que `versionName`:

```sh
git tag v0.1.0
git push origin v0.1.0
```

El workflow **Release** comprova que l’etiqueta i `versionName` coincidixen, passa les proves, compila l’APK amb R8 i el publica a **Releases** amb el seu SHA-256.

## Canvi de l’APK de desenvolupament a la versió signada

L’APK de desenvolupament (`app-debug.apk`) està signat amb una clau diferent. Android no deixa instal·lar la versió signada damunt sense desinstal·lar. Abans de canviar:

1. En Cabàs: **Menú → Còpies i exportació → Fer una còpia de seguretat**.
2. Desinstal·la l’app de desenvolupament i instal·la l’APK de Releases.
3. **Restaurar una còpia** amb el fitxer guardat.

## F-Droid

Cabàs complix els requisits d’F-Droid: codi lliure, sense dependències privatives, sense Internet ni rastreig. Les descripcions per a la botiga estan en `fastlane/metadata/android/ca`. Per proposar-la cal obrir una petició al repositori [fdroiddata](https://gitlab.com/fdroid/fdroiddata) seguint la [guia d’inclusió](https://f-droid.org/docs/Inclusion_How-To/). F-Droid compila l’app des del codi font i la signa amb la seua pròpia clau.
