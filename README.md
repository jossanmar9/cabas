<p align="center"><img src="assets/branding/cabas-logo.png" alt="Cabàs: un cabàs amb una moneda i rebuts" width="220"></p>

# Cabàs

**Els teus diners, sota control.**

Cabàs és un projecte d’app Android lliure i gratuïta de finances personals, inspirada en Kakebo, amb el valencià com a llengua principal.

## Estat

Primera base Android en desenvolupament (`0.1.0-dev`). Inclou registre manual d’ingressos i despeses, categories Kakebo, consulta per mesos, balanç mensual i eliminació amb confirmació. Els moviments es guarden en una base de dades Room local. Encara no és una versió publicada; pressupostos, objectius d’estalvi i exportació estan pendents.

## Principis

- Funcionament offline i dades al dispositiu.
- Sense publicitat, telemetria, rastreig ni comptes d’usuari.
- Sense connexió bancària: registre manual i conscient dels moviments.
- Programari lliure i gratuït amb llicència AGPL-3.0-or-later.
- Interfície senzilla, accessible i en valencià.

## Funcions previstes

- Registre d’ingressos i despeses per data i categoria.
- Pressupost mensual i objectiu d’estalvi.
- Categories inspirades en Kakebo: necessitats, desitjos, cultura i imprevistos.
- Resum i reflexió de final de mes.
- Exportació, importació i còpies de seguretat locals controlades per l’usuari.

## Tecnologia prevista

Kotlin 2.1.20, Jetpack Compose (BOM 2025.04.01) i Room 2.7.1. Android 8.0 (API 26) o posterior, compilació i target API 35. AGP 8.9.2 i Gradle 8.11.1.

## Com començar

```sh
git clone https://github.com/jossanmar9/cabas.git
cd cabas
```

Obri el directori en Android Studio o utilitza JDK 17 (JDK 21 també és compatible amb Gradle 8.11.1) i l’SDK d’Android amb `platforms;android-35` i `build-tools;35.0.0`. Configura `ANDROID_HOME` o `sdk.dir` en `local.properties` (no es versiona).

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
# Amb un emulador o dispositiu Android connectat:
./gradlew connectedDebugAndroidTest
```

APK de desenvolupament: `app/build/outputs/apk/debug/app-debug.apk`. Per instal·lar-lo: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Les proves unitàries cobreixen imports, dates i balanços. La prova instrumentada cobreix persistència després de reobrir la base de dades, límits mensuals i eliminació. El workflow de GitHub executa compilació, proves unitàries i lint; les proves instrumentades requereixen un dispositiu.

**Dades:** la primera versió utilitza EUR, amb imports positius de fins a 999.999.999,99 € i dos decimals. Accepta coma o punt decimal, sense separadors de milers. L’app no demana permís d’Internet i desactiva les còpies de seguretat automàtiques d’Android. Encara no disposa d’exportació ni xifrat propi: desinstal·lar-la elimina les dades. No l’utilitzes encara com a únic registre dels teus comptes.

- [Guia de contribució](CONTRIBUTING.md)
- [Roadmap](docs/ROADMAP.md)
- [Decisions de disseny](docs/DESIGN.md)
- [Historial de canvis](CHANGELOG.md)
- [Identitat visual](assets/branding/README.md)

## Llicència

Copyright (C) 2026 Cabàs contributors.

GNU Affero General Public License, versió 3 o qualsevol versió posterior (`AGPL-3.0-or-later`). Consulta [LICENSE](LICENSE).
