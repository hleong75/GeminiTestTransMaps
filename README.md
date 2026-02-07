# GeminiTestTransMaps

Clone open-source de l'application Transito, conçu pour Android natif en Kotlin avec Jetpack Compose.

## Objectif

- 100% Offline : aucune API serveur, toutes les opérations se font en local.
- Données importées par l'utilisateur :
  - fichier `.zip` (GTFS) pour les horaires.
  - fichier `.mbtiles` (OSM) pour la carte.

## Tech Stack

- UI : Material Design 3 + Jetpack Compose.
- Carte : MapLibre GL Native pour Android (tuiles vectorielles).
- Base de données : Room (SQLite) pour stocker et requêter le GTFS.
- Injection de dépendance : Hilt.
- Asynchrone : Coroutines & Flow.

## Structure de dossiers proposée (Clean Architecture)

```
app/
  src/main/java/<package>/
    data/
    domain/
    ui/
    di/
```
