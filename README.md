<p align="center"><img src="assets/branding/cabas-logo.png" alt="Cabàs: un cabàs amb una moneda i rebuts" width="220"></p>

# Cabàs

**Els teus diners, sota control.**

Cabàs és un projecte d’app Android lliure i gratuïta de finances personals, inspirada en Kakebo, amb el valencià com a llengua principal.

## Estat

Primera base Android en desenvolupament (`0.1.0-dev.3`). Inclou registre manual d’ingressos i despeses, categories Kakebo, consulta per mesos, balanç mensual, pressupost de despeses i objectiu d’estalvi independents per mes, categories editables, menú, informació de l’app i preferència d’aspecte. Els moviments es guarden en una base de dades Room local. Encara no és una versió publicada; la reflexió mensual, l’exportació i les còpies de seguretat estan pendents.

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

## Categories i menú

En **Menú → Categories**, crea categories de despesa i reanomena tant les personalitzades com les quatre incloses. Els noms admeten fins a 40 caràcters i no es poden repetir, tampoc si la categoria està arxivada.

Arxivar una categoria l’amaga de les noves despeses i conserva la relació amb els moviments antics. Activa **Mostrar categories arxivades** per restaurar-la. Reanomenar-la actualitza el nom que es mostra també en l’historial. Si no queda cap categoria activa, cal crear-ne o restaurar-ne una abans de registrar una despesa.

El menú inclou també **Aspecte** (sistema, clar o fosc, conservat localment), **Com funciona**, **Privacitat i dades** i **Quant a Cabàs**, amb la versió real de la compilació, llicència i enllaç al codi font. Els enllaços s’obrin al navegador a petició de l’usuari; l’app continua sense permís d’Internet.

## Pla mensual

En cada mes, prem **Planificar mes** per definir un pressupost de despeses i un objectiu d’estalvi. Després pots modificar-los amb **Editar pla**.

- El pressupost mostra el que queda per gastar o quant s’ha superat.
- L’objectiu es compara amb els ingressos menys les despeses registrats d’eixe mes. No representa diners apartats ni un saldo bancari.
- Els camps són independents: buit significa sense definir; zero és un límit o objectiu explícit. Per llevar el pla, deixa els dos camps buits i guarda.
- Els plans no es copien automàticament a altres mesos i no creen moviments.

### Provar l’actualització

Instal·la la nova APK damunt de l’anterior, **sense desinstal·lar l’app ni esborrar-ne les dades**. Manté el paquet i la clau de desenvolupament i incrementa el codi de versió. Les migracions Room v1 → v2 → v3 conserven els moviments i plans existents. La v3 vincula les quatre categories antigues a categories editables i afig una taula amb identificadors estables.

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

Les proves locals cobreixen imports, dates, balanços, pressupostos i objectius. Amb Robolectric s’executen les migracions Room v1 → v3 i v2 → v3 conservant moviments i plans, la persistència dels plans, l’aïllament entre mesos, les categories i les seues relacions. També hi ha una prova del menú, les pantalles d’informació i el canvi de tema. El formulari de categories cal verificar-lo manualment al dispositiu: l’animació del cursor impedix estabilitzar eixa prova en Robolectric. La prova instrumentada addicional cobreix persistència dels moviments després de reobrir la base de dades, límits mensuals i eliminació. El workflow de GitHub executa compilació, proves unitàries i lint; les proves instrumentades requereixen un dispositiu.

**Dades:** la primera versió utilitza EUR, amb imports positius de fins a 999.999.999,99 € i dos decimals. Accepta coma o punt decimal, sense separadors de milers. L’app no demana permís d’Internet i desactiva les còpies de seguretat automàtiques d’Android. Encara no disposa d’exportació ni xifrat propi: desinstal·lar-la elimina les dades. No l’utilitzes encara com a únic registre dels teus comptes.

- [Guia de contribució](CONTRIBUTING.md)
- [Roadmap](docs/ROADMAP.md)
- [Decisions de disseny](docs/DESIGN.md)
- [Historial de canvis](CHANGELOG.md)
- [Identitat visual](assets/branding/README.md)

## Llicència

Copyright (C) 2026 Cabàs contributors.

GNU Affero General Public License, versió 3 o qualsevol versió posterior (`AGPL-3.0-or-later`). Consulta [LICENSE](LICENSE).
