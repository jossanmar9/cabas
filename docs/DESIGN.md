# Decisions inicials

## Propòsit

Cabàs facilita el registre conscient dels diners, la planificació i la reflexió mensual inspirada en Kakebo.

## Privacitat

Sense permís d’Internet, comptes ni serveis externs. Base Room en l’emmagatzematge privat de l’app, sense xifrat propi. `allowBackup=false` i les regles XML exclouen les dades del backup automàtic i de la transferència Android entre dispositius. Cal verificar estes regles en dispositius reals abans d’una versió estable. Referència: [documentació oficial de backup](https://developer.android.com/identity/data/autobackup). Encara no hi ha exportació: desinstal·lar l’app elimina els moviments.

## Tecnologia prevista

Kotlin 2.1.20, Compose BOM 2025.04.01, Room 2.7.1, AGP 8.9.2 i Gradle 8.11.1. Paquet inicial: `org.cabas.app` (no implica titularitat del domini). Android mínim: API 26; compile/target: API 35. La UI observa Room mitjançant Flow i ViewModel. Sense migracions destructives; es versiona l’esquema de Room.

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

## Preferències i informació

L’aspecte es guarda en SharedPreferences, amb el sistema com a valor inicial. La UI actualitza també el contrast de les barres del sistema. La versió es llig de BuildConfig per evitar text desactualitzat. Els enllaços de llicència i codi font s’obrin explícitament en un navegador extern i gestionen l’absència d’un navegador.

## Llengua i accessibilitat

Valencià com a llengua principal, amb textos en recursos traduïbles. Caldrà considerar lector de pantalla, contrast, grandària del text i controls accessibles.

## Identitat

Nom: **Cabàs**. Repositori: `cabas`. Lema: «Els teus diners, sota control.» Consulta la [identitat visual](../assets/branding/README.md).
