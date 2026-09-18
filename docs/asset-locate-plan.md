# Asset Locate Plan

> BLE-only indoor asset management for ble-toolkit.
> Epic: [#61](https://github.com/gary-quinn/ble-toolkit/issues/61)

## Overview

Asset Locate adds room-level BLE asset tracking to ble-toolkit:

- **Mobile (iOS/Android)**: pin assets, go-find with RSSI, optional walk survey
- **Guardian (Linux JVM)**: fixed anchor scanning 24/7 via kmp-ble BlueZ (Phase 3)
- **Shared (`asset-core`)**: registry, fusion, sync protocol

## Module layout

```
ble-toolkit/
├── composeApp/     # Mobile UI
├── asset-core/     # Shared KMP domain logic
└── guardian/       # Linux JVM daemon (Phase 3)
```

## Phases

### Phase 1 - Mobile MVP

Ship usable go-find without room map or guardian.

| Issue | Title |
|-------|-------|
| [#63](https://github.com/gary-quinn/ble-toolkit/issues/63) | Scaffold `:asset-core` KMP module |
| [#66](https://github.com/gary-quinn/ble-toolkit/issues/66) | Assets tab: My Assets registry |
| [#62](https://github.com/gary-quinn/ble-toolkit/issues/62) | Go-find: RSSI warmer/colder mode |
| [#65](https://github.com/gary-quinn/ble-toolkit/issues/65) | Docs + README update |

**Exit criteria:** pin device from scanner, open go-find, warmer/colder on physical devices.

### Phase 2 - Room + fusion

Add anchor config, walk survey, and LAN sync protocol.

| Issue | Title |
|-------|-------|
| [#67](https://github.com/gary-quinn/ble-toolkit/issues/67) | Room setup: anchor position |
| [#68](https://github.com/gary-quinn/ble-toolkit/issues/68) | Walk survey + RSSI fusion |
| [#69](https://github.com/gary-quinn/ble-toolkit/issues/69) | Guardian sync protocol (WebSocket) |

**Exit criteria:** fused asset dots on 2D map (~1-3m RSSI accuracy), mock guardian streams to app.

### Phase 3 - Guardian daemon

Linux fixed anchor using kmp-ble JVM BlueZ.

| Issue | Title |
|-------|-------|
| [#70](https://github.com/gary-quinn/ble-toolkit/issues/70) | Scaffold `:guardian` JVM module |
| [#71](https://github.com/gary-quinn/ble-toolkit/issues/71) | Integrate kmp-ble BlueZScanner |
| [#72](https://github.com/gary-quinn/ble-toolkit/issues/72) | Room presence dashboard |

**Blocked by:** [kmp-ble JVM desktop BLE RFC](https://github.com/gary-quinn/kmp-ble/tree/docs/jvm-desktop-ble-architecture)

**Exit criteria:** RPi/PC guardian publishes observations; NEW_DEVICE alerts; last-seen in app.

### Phase 4 - Map polish

| Issue | Title |
|-------|-------|
| [#73](https://github.com/gary-quinn/ble-toolkit/issues/73) | 2D room map UI |
| [#74](https://github.com/gary-quinn/ble-toolkit/issues/74) | Map-assisted go-find arrow |

## Dependencies

- [kmp-ble](https://github.com/gary-quinn/kmp-ble) mobile Scanner (available now)
- kmp-ble JVM BlueZ scanner (Phase 3 blocker)

## Out of scope (v1)

- NFC / UWB
- macOS/Windows guardian
- Enterprise CMMS backend
- Sub-meter positioning guarantees
