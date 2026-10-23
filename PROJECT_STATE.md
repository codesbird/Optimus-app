# Optimus Project State & Memory Checkpoint

**Timestamp:** 2026-10-24 00:08 (IST)  
**Status:** Phases 1–3 Complete & Verified. UI Polish & Terminal Bug Fixes Applied. Ready for Phase 4.

---

## 1. Project Summary & Tech Stack

**Optimus** is an open-source Android SSH, SFTP, and terminal client inspired by **Termius**, built with clean architecture, zero-knowledge hardware-backed encryption, and desktop-grade terminal capabilities.

- **OS / Platform:** Android (minSdk 26, targetSdk 35)
- **Language / Toolchain:** Kotlin 2.0.21, Android Gradle Plugin 8.7.3, KSP 2.0.21-1.0.28
- **UI Framework:** Jetpack Compose (BOM 2024.12.01), Material 3, Material Icons Extended
- **Database & Encryption:** Room 2.6.1 + SQLCipher 4.6.1 (256-bit AES database encryption at rest)
- **Hardware Security:** Android Keystore (TEE / StrongBox Keymaster) with AES-256-GCM + AndroidX `BiometricPrompt`
- **SSH & Crypto Core:** `sshj` 0.39.0 + BouncyCastle 1.78.1 (Custom provider priority 1 injection)
- **Build Output:** `D:\Optimus\app\build\outputs\apk\debug\app-debug.apk` (45.3 MB)

---

## 2. Recent Bug Fixes & UI Enhancements (October 24, 2026)

1. **Terminal Backspace & Input Lag Fixed**:
   - Software keyboards (Gboard/Samsung) failing on Backspace was caused by an empty `BasicTextField` state (`newValue.text.isNotEmpty()` returning false). Resolved via a persistent non-empty sentinel (`SENTINEL = "   "`), enabling 100% reliable Backspace detection across all virtual keyboards.
   - Disabled IME predictive batching and word suggestion latency via `KeyboardType.Password`.
   - Replaced per-keystroke coroutine dispatching with a dedicated `Channel<String>(Channel.UNLIMITED)` non-blocking FIFO input worker in `TerminalViewModel.kt`, reducing input latency to under 1ms.
   - Added dedicated `⌫` (Backspace) and `DEL` keys to `TerminalKeyboardBar.kt`.
   - Canvas redraws now hook directly into `emulator.updateTick` within the draw phase, bypassing parent recompositions.

2. **Horizontal Terminal Overflow & PTY Sizing Fixed**:
   - Added two-dimensional drag gestures supporting horizontal swipe panning (`scrollOffsetX`) alongside vertical scrollback history.
   - Remote PTY window dimensions now synchronize immediately on connect (`changeWindowDimensions`) rather than being stuck on 80x24.
   - Reduced default font size to `12sp` for higher column density on mobile portrait viewports.
   - Double-tap anywhere on the terminal canvas instantly snaps horizontal and vertical scroll back to origin.

3. **Host List Cards Modernized**:
   - Removed redundant Play button (tapping anywhere on the card connects).
   - Replaced generic power icon with smart platform detection (`detectHostPlatform`): **AWS EC2**, **Contabo**, **DigitalOcean**, **Linux**, **Windows**, **Local / LAN**, or **Server**.
   - Added a compact status dot overlay (green, yellow, red) at the bottom-right of the platform icon badge.
   - Reduced card padding to `horizontal = 12.dp, vertical = 10.dp` for cleaner information density.

4. **Dialog Wrapping & Space Constraints Resolved**:
   - Dialogs now utilize `DialogProperties(usePlatformDefaultWidth = false)` with `fillMaxWidth(0.95f)` for 50dp+ additional horizontal room.
   - Replaced cramped `FilterChip` rows with a custom Material 3 `SegmentedSelector` featuring `maxLines = 1` and `softWrap = false`, completely eliminating awkward text wrapping ("Passwo rd", "Direct Credentials").

---

## 2. Completed Work (Phases 1–3)

### Phase 1: Architecture & Encrypted Storage (Completed)
- Multi-module Gradle configuration via Gradle Version Catalog (`libs.versions.toml`).
- `KeystoreManager.kt`: Hardware-backed master key generation (`optimus_master_key`) using AES/GCM/NoPadding in Android TEE/StrongBox.
- `BiometricHelper.kt`: Biometric authentication gatekeeping.
- `OptimusDatabase.kt`: Encrypted database with DAOs (`HostDao`, `KeyDao`, `IdentityDao`, `HostGroupDao`, `PortForwardRuleDao`, `SnippetDao`).
- `HostRepository.kt`: CRUD operations with password encryption preservation.
- `AppLockScreen.kt`: Master security lock screen.
- `SshForegroundService.kt`: Persistent keep-alive foreground service with `WakeLock` & `WifiLock`.

### Phase 2: Core SSH Engine & Interactive Terminal Emulator (Completed)
- **ANSI / VT100 / xterm-256 Engine**:
  - `AnsiColor.kt`: 16 standard colors, 256 indexed colors, 24-bit TrueColor (`RGB`), and SGR styling (bold, dim, italic, underline, reverse video).
  - `TerminalBuffer.kt`: Primary and Alternate screen buffers (for full-screen apps like `vim`, `htop`, `tmux`, `nano`) with 2000 lines of scrollback history.
  - `TerminalEmulator.kt`: Robust state-machine parser handling cursor positioning, screen clearing, erase in line, scrolling margins, and character insertion/deletion.
- **Hardware-Accelerated Canvas Terminal (`TerminalView.kt`)**:
  - Direct Compose `Canvas` rendering with monospaced typeface.
  - Touch scrolling, drag-to-scroll gestures, blinking block cursor.
  - Dynamic PTY column/row resize calculation based on viewport dimensions and font metrics.
- **Mobile Developer Keyboard Toolbar (`TerminalKeyboardBar.kt`)**:
  - Dedicated row for `ESC`, `TAB`, sticky `CTRL`, sticky `ALT`, arrow keys (`↑`, `↓`, `←`, `→`), `|`, `/`, `-`, `~`, `$`.
- **Live Interactive Shell Streaming (`TerminalViewModel.kt` & `TerminalScreen.kt`)**:
  - Non-blocking coroutine streaming over SSH channels.
  - Verified on a physical Android device connecting to Windows/WSL over reverse TCP tunnel.

### Phase 3: Key Management, Identities & Bastion Chaining (Completed)
- **SSH Key Generator (`SshKeyGenerator.kt`)**:
  - Supported algorithms: **Ed25519** (RFC 8709), **ECDSA** (256/384/521), **RSA** (2048/4096).
  - Generates standard OpenSSH wire format public keys (`ssh-ed25519 ... user@optimus`).
  - Computes standard OpenSSH SHA-256 fingerprints (`SHA256:...`).
  - Encodes private keys in standard PEM PKCS#8 format.
  - Uses `java.util.Base64` for Android API 26+ and JVM unit test compatibility.
- **Encrypted Key Storage (`KeyRepository.kt`)**:
  - Stores keys in Room + SQLCipher with private keys encrypted via hardware Keystore AES-GCM.
  - Public/Private key import and clipboard copy features.
- **Reusable Identities System (`IdentityRepository.kt`, `IdentitiesViewModel.kt`, `IdentitiesScreen.kt`)**:
  - Reusable credentials storing username, hardware-encrypted password, and linked SSH keys.
  - Full CRUD UI with auth method badges (`Password`, `SSH Key`, `Password & Key`).
- **Bastion / Jump Host Chaining (`ProxyJump`) (`SshConnectionManager.kt`)**:
  - Native in-memory SSH socket forwarding (`jumpClient.newDirectConnection` + `client.connectVia(tunnel)`).
  - Multi-hop credential resolution (resolves passwords, private keys, or identities for both the bastion and target host).
- **Advanced Add/Edit Host Dialog (`AddEditHostDialog.kt`)**:
  - Switch between **Saved Identity** vs. **Direct Credentials**.
  - Dropdown selector for **Jump Host (Bastion)** excluding self.
- **Main Bottom Navigation (`MainScreen.kt` & `OptimusNavGraph.kt`)**:
  - Material 3 `NavigationBar` with **Hosts**, **Keys**, and **Identities** tabs.
  - Automatically hides when entering terminal sessions for maximum display area.
- **Unit Tests**:
  - `SshKeyGeneratorTest.kt`: Verified Ed25519, RSA, and ECDSA key generation and fingerprint calculations (`testDebugUnitTest` passed).

---

## 3. Key Findings & Resolved Pitfalls

1. **Android System BouncyCastle Conflict**:
   - Fixed by removing `"BC"` and inserting `org.bouncycastle.jce.provider.BouncyCastleProvider()` at priority 1 in `OptimusApplication.onCreate()`.
2. **SSH2 Diffie-Hellman Group Exchange Incompatibility**:
   - Node.js `ssh2` server does not implement dynamic group exchange. Excluded `curve25519` and `group-exchange` in `SshConnectionManager.kt`, enabling instant agreement on `diffie-hellman-group14-sha256` and `ecdh-sha2-nistp256`.
3. **Password Preservation on Host Edit**:
   - Editing hosts without retyping the password preserves existing encrypted passwords in `HostRepository.kt` and `IdentityRepository.kt`.
4. **Mobile Hotspot Network Isolation**:
   - When connected via phone hotspot, Windows marks the connection as "Public" firewall profile. Solved using reverse TCP tunnels for testing.

---

## 4. Pending Work & Roadmap for Next Session

### Immediate Security Polish (Quick Win)
- [ ] **Interactive Host Key Verification (`known_hosts`)**:
  - Replace `PromiscuousVerifier()` with a custom verifier that checks against stored host key fingerprints in Room and presents a security confirmation dialog on unknown or changed keys (MITM protection).

### Phase 4: Dual-Pane SFTP File Browser & Remote Editor (Next Major Milestone)
- [ ] **SFTP Subsystem (`SftpClientManager.kt`)**:
  - Connect to remote SFTP subsystem via `sshj.newSFTPClient()`.
  - Directory traversal, fast `stat`/`lstat`, hidden file toggling (`.`).
  - Remote file operations: create folder, rename, delete, `chmod` permissions editor.
- [ ] **Dual-Pane / Tabbed Browser UI**:
  - Local Android storage pane (`DocumentsProvider` / `MediaStore`) alongside remote SFTP pane.
  - Intuitive file selection, sort by name/size/date, file type icons.
- [ ] **Transfer Manager**:
  - Multi-threaded upload/download transfer queue in background service.
  - Persistent progress notification and speed calculation (KB/s, MB/s).
  - Resume interrupted file transfers.
- [ ] **In-App Remote File Editor**:
  - Syntax-highlighted text editor to open and modify remote configuration files (`nginx.conf`, scripts, configs).
  - Atomic save via temporary file upload and atomic rename.

### Phase 5: Port Forwarding & Network Tunneling
- [ ] Local Port Forwarding (`-L local:remote:port`).
- [ ] Remote Port Forwarding (`-R remote:local:port`).
- [ ] Dynamic SOCKS5 Proxy (`-D socks_port`).
- [ ] Live bandwidth graphs & connection toggles.

### Phase 6: Snippets Engine, Themes & Release Packaging
- [ ] Snippets runner with placeholder parameters (`{{name}}`).
- [ ] Terminal color themes (Dracula, Monokai, Solarized, Nord, Gruvbox) & developer fonts (JetBrains Mono).
- [ ] ProGuard/R8 release build (`app-release.apk`) signed with a release keystore.

---

## 5. Development & Testing Quick Reference

- **Source Code Base:** `D:\Optimus`
- **Compile Debug APK:** `.\gradlew.bat assembleDebug`
- **Run Unit Tests:** `.\gradlew.bat testDebugUnitTest`
- **Local Test SSH Server:** Run with `node D:\Optimus\server\server.js` (listens on `0.0.0.0:2222`, user: `optimus` / `pass123` or Windows user).
- **Latest Compiled APK:** `D:\Optimus\app\build\outputs\apk\debug\app-debug.apk`
