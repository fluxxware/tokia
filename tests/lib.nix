{
  pkgs ? import <nixpkgs> { },
}:

# Shared builder for the atomic Tokia happy-path cases.
# Each case pins ONE (JVM, Gradle, Kotlin/KGP) combination and runs tests/happy_path.sh inside it.
{
  name,
  jdk,
  gradle,
  kotlin,
  pluginVersion ? "0.1.0",
}:

let
  # dart-sass is a generic Linux binary; on NixOS it needs the dynamic loader and runtime libs.
  runtimeLibs = with pkgs; [
    stdenv.cc.cc.lib
    zlib
    glib
    nss
    nspr
    expat
  ];
  # Temurin exposes `home`; fall back to the derivation root for other JDK packages.
  jdkHome = jdk.home or jdk;
in
pkgs.mkShell {
  name = "tokia-test-${name}";

  buildInputs = with pkgs; [
    jdk
    cacert
  ];

  shellHook = ''
    export JAVA_HOME="${jdkHome}"
    [ -x "$JAVA_HOME/bin/java" ] || export JAVA_HOME="${jdk}/lib/openjdk"
    export PATH="$JAVA_HOME/bin:$PATH"

    export NIX_LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath runtimeLibs}"
    export NIX_LD="${pkgs.stdenv.cc.libc}/lib/ld-linux-x86-64.so.2"
    export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath runtimeLibs}:${pkgs.stdenv.cc.cc.lib}/lib:''${LD_LIBRARY_PATH:-}"
    export SSL_CERT_FILE="${pkgs.cacert}/etc/ssl/certs/ca-bundle.crt"

    export TOKIA_TEST_GRADLE="${gradle}"
    export TOKIA_TEST_KOTLIN="${kotlin}"
    export TOKIA_TEST_PLUGIN_VERSION="${pluginVersion}"
    export REPO_ROOT="${toString ./..}"

    echo "tokia test shell: ${name} (gradle=${gradle}, kotlin=${kotlin})"
    java -version 2>&1 | head -1
  '';
}
