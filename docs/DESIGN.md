# Decisions inicials

## Propòsit

Cabàs facilita el registre conscient dels diners, la planificació i la reflexió mensual inspirada en Kakebo.

## Privacitat

Sense permís d’Internet, comptes ni serveis externs. Base Room en l’emmagatzematge privat de l’app, sense xifrat propi. `allowBackup=false` i les regles XML exclouen les dades del backup automàtic i de la transferència Android entre dispositius. Cal verificar estes regles en dispositius reals abans d’una versió estable. Referència: [documentació oficial de backup](https://developer.android.com/identity/data/autobackup). Encara no hi ha exportació: desinstal·lar l’app elimina els moviments.

## Tecnologia prevista

Kotlin 2.1.20, Compose BOM 2025.04.01, Room 2.7.1, AGP 8.9.2 i Gradle 8.11.1. Paquet inicial: `org.cabas.app` (no implica titularitat del domini). Android mínim: API 26; compile/target: API 35. La UI observa Room mitjançant Flow i ViewModel. Sense migracions destructives; es versiona l’esquema de Room.

## Imports i dates

La primera versió admet EUR. Cada moviment guarda un enter Long en cèntims, amb màxim de 99.999.999.999 cèntims. La conversió utilitza BigDecimal, rebutja zero, negatius i més de dos decimals. Les dates es guarden com a dia civil (epochDay), sense conversions de zona horària. Els intervals mensuals inclouen el primer dia i exclouen el primer dia del mes següent.

## Llengua i accessibilitat

Valencià com a llengua principal, amb textos en recursos traduïbles. Caldrà considerar lector de pantalla, contrast, grandària del text i controls accessibles.

## Identitat

Nom: **Cabàs**. Repositori: `cabas`. Lema: «Els teus diners, sota control.» Consulta la [identitat visual](../assets/branding/README.md).
