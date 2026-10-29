# MLGPSTest

App Android per testare il GPS/GNSS, in italiano e con interfaccia Material You (colori dinamici).
Kotlin + Jetpack Compose.

## Funzioni

- **Stato**: stato GNSS (fix / nessun fix), precisione, satelliti in vista e in uso,
  grafico C/N0 per satellite, SNR medio, riquadri rapidi (cielo, posizione, bussola, velocità, tempo al primo fix)
- **Satelliti**: coordinate, altitudine (ellissoide e s.l.m.), velocità, DOP, ora, TTPL, precisione,
  e tabella con ID, costellazione, banda (L1/E1/B1I...), C/N0, dati A/E/U, elevazione e azimut. Ordina e filtra per costellazione
- **Cielo**: skyplot con forma per costellazione (GPS, GLONASS, Galileo, BeiDou, QZSS, NavIC, SBAS) e colore per intensità
- **Bussola**: nord vero / magnetico, declinazione e velocità (km/h, m/s, mph, nodi)
- **Log**: registrazione in GPX, NMEA e CSV (satelliti), con condivisione
- **A-GPS**: cancella / aggiorna i dati ausiliari (menu ⋮)
- Impostazioni: tema, colori dinamici, unità, formato coordinate, schermo sempre acceso

## Build

L'APK viene compilato da GitHub Actions a ogni push su `main` e pubblicato nelle **Releases**.

Per firmare con la tua chiave (consigliato, così gli aggiornamenti si installano sopra) aggiungi in
*Settings → Secrets and variables → Actions*: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

Con Android Studio: apri la cartella e premi Run (serve JDK 17).

## Aggiornamenti

Dalle impostazioni dell app: *Aggiornamenti → Cerca aggiornamenti*. Scarica l ultima Release di GitHub e la installa.
