# Decisions inicials

## Propòsit

Cabàs facilita el registre conscient dels diners, la planificació i la reflexió mensual inspirada en Kakebo.

## Privacitat

Sense permís d’Internet, comptes ni serveis externs. Base Room en l’emmagatzematge privat de l’app, sense xifrat propi. `allowBackup=false` i les regles XML exclouen les dades del backup automàtic i de la transferència Android entre dispositius. Cal verificar estes regles en dispositius reals abans d’una versió estable. Referència: [documentació oficial de backup](https://developer.android.com/identity/data/autobackup). Les còpies les fa l’usuari des de l’app amb el selector de fitxers del sistema (Storage Access Framework), sense permisos d’emmagatzematge.

## Tecnologia prevista

Kotlin 2.1.20, Compose BOM 2025.04.01, Room 2.7.1 amb KSP, AGP 8.9.2 i Gradle 8.11.1. Paquet inicial: `org.cabas.app` (no implica titularitat del domini). Android mínim: API 26; compile/target: API 35. La UI observa Room mitjançant Flow i ViewModel. Sense migracions destructives; es versiona l’esquema de Room.

## Imports i dates

La primera versió admet EUR. Cada moviment guarda un enter Long en cèntims, amb màxim de 99.999.999.999 cèntims. La conversió utilitza BigDecimal, rebutja zero, negatius i més de dos decimals. Les dates es guarden com a dia civil (epochDay), sense conversions de zona horària. Els intervals mensuals inclouen el primer dia i exclouen el primer dia del mes següent.

## Pla mensual i migració v2

La taula `monthly_plans` té una fila per mes (`YYYY-MM`), amb pressupost de despeses i objectiu d’estalvi opcionals en cèntims. Un valor nul significa sense definir; zero és un valor explícit. Guardar els dos camps buits elimina el pla d’eixe mes. No s’arrosseguen plans ni balanços entre mesos.

El pressupost restant és el pressupost menys les despeses; un resultat negatiu indica excés. L’import que falta per a l’objectiu és `max(objectiu − (ingressos − despeses), 0)`. El progrés visual es limita entre 0 i 1 i gestiona els objectius de zero sense divisió. Els càlculs monetaris es fan amb enters; només la representació de les barres usa coma flotant.

La migració explícita v1 → v2 només crea la nova taula. No elimina ni modifica `movements`. La prova amb Robolectric recrea l’esquema v1 versionat, inserix un moviment, obri Room v2 amb validació d’esquema i comprova la conservació de les dades i la persistència dels plans després de reobrir.

## Categories editables i migració v3

`expense_categories` guarda identificadors estables, nom visible, clau normalitzada única, origen inclòs/personalitzat i estat d’arxiu. La clau es calcula amb normalització Unicode NFC, espais reduïts i minúscules independents de la configuració regional. El límit és de 40 unitats de text Kotlin; no s’admeten noms buits, caràcters de control o de format invisibles.

`movements.categoryId` té una clau forana amb eliminació restringida. La columna enum antiga es conserva per compatibilitat, però els moviments nous utilitzen l’identificador editable. No s’oferix eliminació definitiva de categories: l’arxiu conserva les referències i permet restaurar-les. El nom es resol al llegir, de manera que un canvi de nom es veu també en els moviments antics.

La migració v2 → v3 crea les quatre categories inicials i assigna cada despesa antiga a l’identificador corresponent; els ingressos mantenen categoria nul·la. No altera imports, dates, conceptes ni plans mensuals. Les instal·lacions noves reben les mateixes categories inicials mitjançant el callback de Room.

## Moviments recurrents, reflexions i migració v4

`recurring_movements` guarda plantilles amb import en cèntims, tipus, categoria opcional, dia del mes (1–31), concepte, mes d’inici i últim mes generat. En obrir l’app (i en tornar-hi) es generen, dins d’una transacció, els moviments dels mesos des del següent a l’últim generat fins al mes actual, només si la data ja ha arribat. El dia es limita a la llargària del mes. Com a màxim es recuperen 120 mesos d’una vegada. Editar una plantilla no modifica els moviments ja creats; reactivar-la no recupera els mesos en pausa. No hi ha clau forana a les categories: les categories no s’eliminen mai fora d’una restauració, que es valida sencera.

`monthly_reflections` guarda, per mes, dos textos de fins a 1.000 caràcters. Les respostes numèriques del Kakebo es calculen sempre a partir dels moviments i del pla.

La migració v3 → v4 només crea les dues taules.

## Còpies de seguretat

Format JSON versionat (`"app": "cabas", "format": 1`) amb totes les taules, imports en cèntims enters i dates ISO. La importació es fa en dues fases: primer es llig (màxim 20 MB) i es valida tot (tipus, imports, dates, identificadors únics, noms de categoria, referències); després de confirmar, es buiden i s’omplin totes les taules en una sola transacció Room. Les preferències (tema, data de l’última còpia) no formen part de la còpia.

El CSV usa punt i coma, coma decimal, signe negatiu per a les despeses i BOM UTF-8. Els camps que començarien una fórmula (`=`, `+`, `-`, `@`) es prefixen amb un apòstrof.

## Versió 1.0: pressupostos, cerca, evolució i treball en segon pla

**Room v5** afig `expense_categories.monthlyBudgetCents` (nul·lable, igual per a tots els mesos). La migració v4 → v5 només fa `ALTER TABLE … ADD COLUMN`. L’estat del pressupost (`OK`, `NEAR` a partir del 90 %, `OVER`) es calcula amb enters.

**Format de còpia 2**: afig `monthlyBudgetCents` a cada categoria i és obligatori (pot ser `null`). El format 1 s’accepta i es restaura sense pressupostos.

**Cerca** i **evolució** treballen sobre tots els moviments en memòria (l’ús personal té pocs milers de files). La cerca normalitza a minúscules sense diacrítics (NFD) i exigix que totes les paraules apareguen en el concepte, la categoria o el tipus. El gràfic de barres té una descripció accessible i una taula amb les xifres exactes.

**Suggeriments**: una consulta agrupa els conceptes per freqüència i, gràcies a les columnes nues de SQLite amb `MAX(id)`, retorna el tipus i la categoria de l’últim ús.

**Treball en segon pla** amb WorkManager (no necessita Internet):
- Còpia automàtica: treball periòdic de 7 dies més un d’immediat en activar-la. Escriu en una carpeta triada amb `ACTION_OPEN_DOCUMENT_TREE` i permís persistent, i esborra només els fitxers `cabas-auto-*.json` més antics que els quatre darrers. Si falla, ho reintenta i el resum ho avisa.
- Recordatori: treball periòdic diari amb retard inicial fins a l’hora triada. No notifica si ja hi ha moviments d’eixe dia. En Android 13+ requerix `POST_NOTIFICATIONS`, demanat en activar-lo.

**Bloqueig** amb `BiometricPrompt` i `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`, compatible des d’Android 8: Cabàs no guarda cap PIN. Es bloqueja en obrir l’app i en tornar després de 60 s en segon pla; mentre està bloquejada no es compon cap pantalla ni diàleg. Amb el bloqueig s’activa `FLAG_SECURE`.

**Idiomes**: recursos per defecte en valencià (AVL), `values-es` i `values-en`. El locale, el patró del mes i el del dia es lligen dels recursos, perquè els imports i les dates seguisquen la llengua de la interfície. `generateLocaleConfig` publica els idiomes per al selector per app d’Android 13+.

**Accés directe** estàtic amb l’acció `org.cabas.app.action.ADD`; l’activitat és `singleTop` i el ViewModel guarda la petició fins que l’app està desbloquejada.

Permisos resultants: `POST_NOTIFICATIONS`, `USE_BIOMETRIC` (AndroidX Biometric) i els que afig WorkManager (`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE`). Cap permet accedir a Internet.

## Preferències i informació

L’aspecte es guarda en SharedPreferences, amb el sistema com a valor inicial. La UI actualitza també el contrast de les barres del sistema. La versió es llig de BuildConfig per evitar text desactualitzat. Els enllaços de llicència i codi font s’obrin explícitament en un navegador extern i gestionen l’absència d’un navegador.

## Llengua i accessibilitat

Valencià com a llengua principal, amb textos en recursos traduïbles. Caldrà considerar lector de pantalla, contrast, grandària del text i controls accessibles.

## Identitat

Nom: **Cabàs**. Repositori: `cabas`. Lema: «Els teus diners, sota control.» Consulta la [identitat visual](../assets/branding/README.md).
