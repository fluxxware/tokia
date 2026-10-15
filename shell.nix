{
  pkgs ? import <nixpkgs> { },
}:

let
  # The downloaded dart-sass is a generic Linux binary; on NixOS it needs the loader.
  libList = with pkgs; [
    stdenv.cc.cc.lib
    zlib
    glib
    nss
    nspr
    expat
  ];
in
pkgs.mkShell {
  buildInputs = with pkgs; [
    jdk17
  ];

  shellHook = ''
    # Pin JDK 17 so the system's stray Java 8 cannot win, and match the plugin's bytecode floor.
    export JAVA_HOME="${pkgs.jdk17}/lib/openjdk"
    export PATH="${pkgs.jdk17}/lib/openjdk/bin:${pkgs.jdk17}/bin:$PATH"
    export PATH="${pkgs.jdk17}/lib/openjdk/bin:${pkgs.jdk17}/bin:$PATH"

    export NIX_LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath libList}"
    export NIX_LD="${pkgs.stdenv.cc.libc}/lib/ld-linux-x86-64.so.2"
    export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath libList}:${pkgs.stdenv.cc.cc.lib}/lib:''${LD_LIBRARY_PATH:-}"

    echo "tokia-cmp dev shell ready: $(java -version 2>&1 | head -1)"
  '';
}
