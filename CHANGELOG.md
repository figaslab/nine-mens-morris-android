# Changelog

## [1.1.0] - 2026-04-20

### Added
- Cross-game promotion cards in P2P lobby and tournaments
- Optional cryptography in P2P communication (disabled for game moves, reduces packet size ~85%)
- 15s per-turn countdown timer in Practice mode (color shifts at 10s/5s, vibration on final 5s, auto-loss on timeout); network turn timeout reduced 30s → 15s
- Android 16 KB page size compliance for native libraries (shared submodules)

### Fixed
- Duplicate variant selection dialog in WiFi/Bluetooth lobby
