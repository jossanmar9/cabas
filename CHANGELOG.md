# Historial de canvis

Encara no hi ha cap versió publicada.

## Sense publicar

### Actualització 0.1.0-dev.4

- Còpia de seguretat completa en JSON, restauració validada i atòmica, i exportació CSV per a fulls de càlcul.
- Recordatori al resum quan fa més de 30 dies que no es fa cap còpia.
- Moviments recurrents mensuals amb dia ajustat als mesos curts, pausa i recuperació dels mesos pendents.
- Reflexió de final de mes amb les quatre preguntes del Kakebo.
- Edició de moviments en tocar-los i eliminació des del formulari.
- Selector de data amb calendari en lloc d’escriure-la.
- Resum de despeses per categoria amb percentatges.
- Opció de copiar el pla del mes anterior.
- Barra superior amb icones, llista de moviments agrupada per dies i botó per tornar al mes actual.
- Tema clar i fosc complets amb la paleta del logo, i fons de finestra correcte en mode fosc.
- Icona adaptativa del llançador.
- Room amb KSP en lloc de kapt; migració v3 → v4 que només afig taules.
- Compilació release amb R8, signatura des de secrets de CI i workflow de publicació per etiquetes.
- Metadades per a F-Droid.

### Actualització 0.1.0-dev.3

- Categories de despesa personalitzades i reanomenament de les incloses.
- Arxiu i restauració de categories sense eliminar l’historial; noms únics normalitzats.
- Selector de categories actives en registrar una despesa.
- Menú amb resum, categories, aspecte, ajuda, privacitat i informació de l’app.
- Tema del sistema, clar o fosc, amb preferència local.
- Versió i codi de compilació reals, llicència i enllaços al repositori.
- Migració Room v2 → v3 que conserva moviments, categories anteriors i plans.
- Proves de categories, integritat referencial, migracions i flux d’interfície.


### Actualització 0.1.0-dev.2

- Pressupost de despeses i objectiu d’estalvi per mes, editables i opcionals.
- Indicadors del pressupost restant, excés de despesa i progrés cap a l’objectiu.
- Imports zero explícits i camps buits per llevar límits o objectius.
- Migració Room v1 → v2 que conserva els moviments existents.
- Proves locals de migració i persistència amb Robolectric, i proves dels càlculs del pla.
- Codi de versió Android incrementat per instal·lar damunt de l’APK inicial.

### Afegit

- Base Android amb Kotlin, Compose, Room i Gradle Wrapper verificat amb SHA-256.
- Ingressos i despeses en EUR, categories Kakebo i dates civils.
- Resum i navegació mensual, persistència local i eliminació amb confirmació.
- Textos en valencià, logo i formulari amb validació d’imports i dates.
- Proves unitàries, prova instrumentada de Room i workflow Android.
- Sense permís d’Internet i amb backup automàtic desactivat.

- Documentació inicial dels principis i l’abast de Cabàs.
- Llicència AGPL-3.0-or-later i guia de contribució.
- Roadmap i decisions de disseny.
- Configuració d’edició i exclusions per a Android.
- Logo original aportat per l’usuari i integrat en el README.
