<p align="center"><img src="assets/branding/cabas-logo.png" alt="Cabàs: un cabàs amb una moneda i rebuts" width="220"></p>

# Cabàs

**Els teus diners, sota control.**

Cabàs és un projecte d’app Android lliure i gratuïta de finances personals, inspirada en Kakebo, amb el valencià com a llengua principal.

## Estat

Primera base Android en desenvolupament (`0.1.0-dev.2`). Inclou registre manual d’ingressos i despeses, categories Kakebo, consulta per mesos, balanç mensual, pressupost de despeses i objectiu d’estalvi independents per mes, i eliminació amb confirmació. Els moviments es guarden en una base de dades Room local. Encara no és una versió publicada; la reflexió mensual, l’exportació i les còpies de seguretat estan pendents.

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

## Pla mensual

En cada mes, prem **Planificar mes** per definir un pressupost de despeses i un objectiu d’estalvi. Després pots modificar-los amb **Editar pla**.

- El pressupost mostra el que queda per gastar o quant s’ha superat.
- L’objectiu es compara amb els ingressos menys les despeses registrats d’eixe mes. No representa diners apartats ni un saldo bancari.
- Els camps són independents: buit significa sense definir; zero és un límit o objectiu explícit. Per llevar el pla, deixa els dos camps buits i guarda.
- Els plans no es copien automàticament a altres mesos i no creen moviments.

### Provar l’actualització

Instal·la la nova APK damunt de l’anterior, **sense desinstal·lar l’app ni esborrar-ne les dades**. Manté el paquet i la clau de desenvolupament i incrementa el codi de versió. La migració a Room v2 afig la taula dels plans sense modificar els moviments.

Amb un ingrés de 1.000 € i una despesa de 25,50 €, un pressupost de 500 € mostrarà 474,50 € disponibles per gastar, i un objectiu de 1.000 € mostrarà que falten 25,50 €. Canvia de mes per comprovar que el pla és independent i torna a obrir l’app per verificar que es conserva.

## Tecnologia

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

Les proves locals cobreixen imports, dates, balanços, pressupostos i objectius. Amb Robolectric s’executen la migració Room v1 → v2 conservant moviments, la persistència dels plans, l’edició, l’eliminació del pla i l’aïllament entre mesos. La prova instrumentada addicional cobreix persistència dels moviments després de reobrir la base de dades, límits mensuals i eliminació. El workflow de GitHub executa compilació, proves unitàries i lint; les proves instrumentades requereixen un dispositiu.

**Dades:** la primera versió utilitza EUR, amb imports positius de fins a 999.999.999,99 € i dos decimals. Accepta coma o punt decimal, sense separadors de milers. L’app no demana permís d’Internet i desactiva les còpies de seguretat automàtiques d’Android. Encara no disposa d’exportació ni xifrat propi: desinstal·lar-la elimina les dades. No l’utilitzes encara com a únic registre dels teus comptes.

- [Guia de contribució](CONTRIBUTING.md)
- [Roadmap](docs/ROADMAP.md)
- [Decisions de disseny](docs/DESIGN.md)
- [Historial de canvis](CHANGELOG.md)
- [Identitat visual](assets/branding/README.md)

## Llicència

Copyright (C) 2026 Cabàs contributors.

GNU Affero General Public License, versió 3 o qualsevol versió posterior (`AGPL-3.0-or-later`). Consulta [LICENSE](LICENSE).
