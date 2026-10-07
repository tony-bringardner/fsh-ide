# fsh-ide

An editor and debugger for shell scripts. It uses
[fsh](https://github.com/tony-bringardner/fsh), the FileSource Shell, as the
interpreter. fsh aims to be close to bash/POSIX, so the IDE works for those scripts
too. Scripts can be opened from and saved to any
[parley-files](https://github.com/tony-bringardner/parley-files) file system.

Formerly BjlShellIde (`us.bringardner:bjl_shell_ide`).

## Features

- Syntax-aware editor with templates, auto-completion and spell checking
- Run a script or just the selected lines, with output in an embedded console
- Breakpoints (optionally conditional), step into/over/return, and a variables
  view where values can be edited while paused
- Parse-tree views of the script

## Requirements

- Java 21
- fsh, which brings parley-files and its FTP and SFTP file systems

## Build and run

```sh
mvn package                 # jar in target/
mvn -Pjdbc package          # also bundle the JDBC file system
mvn -Pnative package        # GraalVM native binary named fsh-ide
java -cp ... us.bringardner.fsh.ide.FshIDE
```

## Settings

Templates and settings are kept in `~/.fsh-ide/Config.xml`; window positions and
recent files in Java preferences. Both are copied across from BjlShellIde's
locations the first time fsh-ide starts.

## License

Apache License 2.0. See [LICENSE](LICENSE).
