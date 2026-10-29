<p align="center"><img src="assets/branding/cabas-logo.png" alt="Cabàs: un cabàs amb una moneda i rebuts" width="220"></p>

# Cabàs

**Els teus diners, sota control.**

Cabàs és un projecte d’app Android lliure i gratuïta de finances personals, inspirada en Kakebo, amb el valencià com a llengua principal.

## Estat

Versió **1.0.0**. Inclou registre i edició d’ingressos i despeses amb calendari i suggeriments, categories Kakebo editables amb pressupost mensual, resum mensual, pla i objectiu d’estalvi, moviments recurrents, reflexió de final de mes, cerca, evolució anual, còpies de seguretat manuals i automàtiques, exportació CSV, bloqueig opcional i recordatori diari. Disponible en valencià, castellà i anglés.

## Principis

- Funcionament offline i dades al dispositiu.
- Sense publicitat, telemetria, rastreig ni comptes d’usuari.
- Sense connexió bancària: registre manual i conscient dels moviments.
- Programari lliure i gratuït amb llicència AGPL-3.0-or-later.
- Interfície senzilla, accessible i en valencià.

## Funcions

- Registre d’ingressos i despeses per data (amb calendari) i categoria, amb suggeriments dels conceptes ja usats. Toca un moviment per editar-lo o eliminar-lo.
- Resum mensual: balanç, despeses per categoria (amb pressupost si n’hi ha) i llista de moviments agrupada per dies.
- Pressupost mensual i objectiu d’estalvi, amb opció de copiar el pla del mes anterior.
- Categories inspirades en Kakebo: necessitats, desitjos, cultura i imprevistos, editables, ampliables i amb pressupost propi.
- Moviments recurrents: es registren sols cada mes el dia indicat.
- Reflexió de final de mes amb les quatre preguntes del Kakebo.
- Cerca en tots els mesos i pantalla d’evolució anual amb gràfic.
- Còpia de seguretat en JSON (a mà o automàtica cada setmana), restauració validada i exportació CSV.
- Bloqueig amb l’empremta o el PIN del mòbil, recordatori diari i accés directe «Afegir despesa».
- Valencià, castellà i anglés.

## Categories i menú

En **Menú → Categories**, crea categories de despesa i reanomena tant les personalitzades com les quatre incloses. Els noms admeten fins a 40 caràcters i no es poden repetir, tampoc si la categoria està arxivada.

Arxivar una categoria l’amaga de les noves despeses i conserva la relació amb els moviments antics. Activa **Mostrar categories arxivades** per restaurar-la. Reanomenar-la actualitza el nom que es mostra també en l’historial. Si no queda cap categoria activa, cal crear-ne o restaurar-ne una abans de registrar una despesa.

La lupa de la barra superior obri la **Cerca**. El menú inclou també **Evolució**, **Moviments recurrents**, **Còpies i exportació**, **Configuració** (aspecte, bloqueig i recordatori) (sistema, clar o fosc, conservat localment), **Com funciona**, **Privacitat i dades** i **Quant a Cabàs**, amb la versió real de la compilació, llicència i enllaç al codi font. Els enllaços s’obrin al navegador a petició de l’usuari; l’app continua sense permís d’Internet.

## Pla mensual

En cada mes, prem **Planificar mes** per definir un pressupost de despeses i un objectiu d’estalvi. Després pots modificar-los amb **Editar pla**.

- El pressupost mostra el que queda per gastar o quant s’ha superat.
- L’objectiu es compara amb els ingressos menys les despeses registrats d’eixe mes. No representa diners apartats ni un saldo bancari.
- Els camps són independents: buit significa sense definir; zero és un límit o objectiu explícit. Per llevar el pla, deixa els dos camps buits i guarda.
- Els plans no es copien automàticament a altres mesos i no creen moviments.

Cada categoria pot tindre un **pressupost mensual** (botó **Pressupost** en Menú → Categories). El resum mostra quant en queda, avisa a partir del 90 % i marca en roig quan se supera.

## Moviments recurrents

En **Menú → Moviments recurrents**, afig els moviments que es repetixen cada mes amb l’import, el dia (1–31), el concepte i la categoria. Cabàs els registra automàticament quan arriba el dia, també els mesos que hages passat sense obrir l’app (fins a 120). En els mesos curts s’utilitza l’últim dia. Pots posar-los en pausa o eliminar-los; els moviments ja registrats es conserven i es poden editar si l’import d’un mes canvia.

## Reflexió mensual

Des del resum, **Escriure la reflexió** mostra les quatre preguntes del Kakebo: quant has ingressat, quant volies estalviar, quant has gastat i quant has estalviat, calculades amb els moviments del mes. Després pots escriure què ha anat bé i què vols millorar.

## Còpies de seguretat i exportació

En **Menú → Còpies i exportació**:

- **Fer una còpia de seguretat** guarda tots els moviments, categories, plans, recurrents i reflexions en un fitxer JSON on tu tries (carpeta del mòbil, ordinador o núvol). Els imports es guarden en cèntims i les dates en format ISO.
- **Restaurar una còpia** valida el fitxer sencer abans de canviar res i, després de confirmar, substituïx totes les dades en una sola transacció. Si el fitxer no és vàlid, no es toca res.
- **Exportar moviments (CSV)** genera un full de càlcul amb punt i coma i coma decimal, preparat per a LibreOffice o Excel.

- **Còpia automàtica**: tria una carpeta i Cabàs hi guardarà una còpia cada setmana (`cabas-auto-AAAA-MM-DD.json`), conservant les quatre més recents. Els altres fitxers de la carpeta no es toquen.

Si fa més de 30 dies que no fas cap còpia (i no tens la còpia automàtica activada), o si la còpia automàtica falla, el resum ho recorda.

## Bloqueig, recordatori i idiomes

- **Bloqueig** (Menú → Configuració): demana l’empremta, la cara o el PIN del mòbil en obrir l’app i en tornar després d’un minut. Cabàs no guarda cap contrasenya. Amb el bloqueig, l’app s’amaga a les recents i no permet captures.
- **Recordatori diari** a les 20, 21 o 22 h, només si aquell dia encara no has apuntat res. En Android 13+ demana permís de notificacions en activar-lo.
- **Accés directe**: mantín premuda la icona per obrir directament el formulari.
- **Idiomes**: segueix l’idioma del mòbil; en Android 13+ també es pot triar en Configuració del sistema → Aplicacions → Cabàs → Idioma. Les categories inicials es creen en l’idioma de l’app.

### Provar l’actualització

Instal·la la nova APK damunt de l’anterior, **sense desinstal·lar l’app ni esborrar-ne les dades**. Manté el paquet i la clau de desenvolupament i incrementa el codi de versió. Les migracions Room v1 → v2 → v3 → v4 → v5 conserven els moviments, categories i plans existents. La v3 vincula les quatre categories antigues a categories editables; la v4 afig les taules de moviments recurrents i reflexions; la v5, el pressupost per categoria.

Amb un ingrés de 1.000 € i una despesa de 25,50 €, un pressupost de 500 € mostrarà 474,50 € disponibles per gastar, i un objectiu de 1.000 € mostrarà que falten 25,50 €. Canvia de mes per comprovar que el pla és independent i torna a obrir l’app per verificar que es conserva.

## Tecnologia

Kotlin 2.1.20, Jetpack Compose (BOM 2025.04.01), Room 2.7.1 amb KSP, WorkManager i AndroidX Biometric. Android 8.0 (API 26) o posterior, compilació i target API 35. AGP 8.9.2 i Gradle 8.11.1.

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

APK de desenvolupament: `app/build/outputs/apk/debug/app-debug.apk`. Se signa amb la clau pública `app/debug.keystore`, que només servix per a proves: així cada APK de prova s’instal·la damunt de l’anterior. Cada vegada que es puja una branca, GitHub publica l’APK a la versió **prova** de Releases. Per instal·lar-lo: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Les proves locals cobreixen imports, dates, evolució anual, cerca sense accents, suggeriments, estat dels pressupostos, hora del recordatori, rotació de còpies automàtiques, balanços, pressupostos, objectius, moviments recurrents (dies curts, mesos pendents, sense repeticions), el resum per categories, la còpia de seguretat (anada i tornada exacta, rebuig de fitxers no vàlids sense tocar dades) i l’escapament del CSV. Amb Robolectric s’executen les migracions Room fins a la v5 des de les versions 1, 2, 3 i 4 conservant moviments i plans, la persistència dels plans, l’aïllament entre mesos, les categories i les seues relacions. També hi ha una prova del menú, les pantalles d’informació i el canvi de tema. El formulari de categories cal verificar-lo manualment al dispositiu: l’animació del cursor impedix estabilitzar eixa prova en Robolectric. La prova instrumentada addicional cobreix persistència dels moviments després de reobrir la base de dades, límits mensuals i eliminació. El workflow de GitHub executa compilació de depuració i release (amb R8), proves unitàries i lint; les proves instrumentades requereixen un dispositiu. Per publicar versions signades, consulta [Publicar una versió](docs/RELEASING.md).

**Dades:** la primera versió utilitza EUR, amb imports positius de fins a 999.999.999,99 € i dos decimals. Accepta coma o punt decimal, sense separadors de milers. L’app no demana permís d’Internet (només el de notificacions, per al recordatori opcional) i desactiva les còpies de seguretat automàtiques d’Android: les còpies les fas tu des de l’app. No hi ha xifrat propi, ni en la base de dades ni en els fitxers exportats. Desinstal·lar-la elimina les dades que no tingues copiades.

- [Guia de contribució](CONTRIBUTING.md)
- [Roadmap](docs/ROADMAP.md)
- [Decisions de disseny](docs/DESIGN.md)
- [Publicar una versió](docs/RELEASING.md)
- [Historial de canvis](CHANGELOG.md)
- [Identitat visual](assets/branding/README.md)

## Llicència

Copyright (C) 2026 Cabàs contributors.

GNU Affero General Public License, versió 3 o qualsevol versió posterior (`AGPL-3.0-or-later`). Consulta [LICENSE](LICENSE).
