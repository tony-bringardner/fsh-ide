# Changelog

## 1.0.0 (unreleased)

First release as fsh-ide. Formerly BjlShellIde (`us.bringardner:bjl_shell_ide`).

### Changed
- Maven coordinates: `us.bringardner:bjl_shell_ide` → `us.bringardner:fsh-ide`.
- The sources are now the IDE that had grown inside BjlShell, which was ahead of
  this repository's copy (more templates, the recent-files menu, "Show Position",
  editable debug variables, output in an embedded console). It moved here from fsh.
- Package `us.bringardner.shell.ide` → `us.bringardner.fsh.ide`;
  `BjlShellIDE` → `FshIDE`, `BjlShellIDETextArea` → `FshIDETextArea`,
  `BjlShellTreeViewPanel` → `FshTreeViewPanel`.
- Depends on fsh and parley-files instead of bjl_shell and bjl_file_system.
- The debug-variables table has a Type column (from this repository's old copy).
- Fixed the `native` profile's main class (it named a package that didn't exist).

### Fixed
- The IDE no longer fails to start on Linux and Windows: it used the macOS-only
  screen-top menu bar without checking. Elsewhere the menu bar is now on the window.
- Saved settings and templates in `Config.xml` are read again (they were replaced by the
  defaults on every start). The file is written to a temporary file first.
- Saving no longer deletes non-ASCII characters; scripts are read and written as UTF-8.
  Only invisible characters (control characters, zero-width spaces, byte order marks) are
  removed, and a non-breaking space becomes a space.
- A failed save is reported and leaves the script marked as changed.
- Open, New, Reload and Open Recent ask before discarding unsaved changes. Reload works
  for scripts on remote file systems. A newly opened script no longer counts as changed.
- Breakpoints and compile-error markers stay on their line however the text is edited
  (they used to drift, or stop on the wrong line), and line numbers are always shown.
- A script run with no arguments no longer gets one empty argument.
- Redirect files for stdin, stdout and stderr are closed when the script ends.
- Debugging only the selected code highlights the right lines; the variables view no longer
  reads the script's variables while it's still running; a breakpoint condition that can't
  be evaluated is reported once per run instead of every time it's reached.
- Starting no longer fails on unreadable saved window bounds, and a window saved on a
  screen that's since been unplugged opens on the main screen.
- Ctrl+Shift+Add expands all folds (it collapsed them). Alt+F before Ctrl+F no longer fails.

### Still works
- Settings move from `~/.bjlshellIde/Config.xml` to `~/.fsh-ide/Config.xml`;
  the old file is copied across the first time.
- Preferences saved under the old package's node are copied to the new one on first
  start.
- Script argument markers (`#BjlIdeScriptArgs=` and friends) in saved scripts are
  unchanged, so existing scripts keep their settings.
