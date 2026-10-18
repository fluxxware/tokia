{ pkgs ? import ../nixpkgs.nix { } }:
(import ../lib.nix { inherit pkgs; }) {
  name = "jdk21-gradle9.6-kotlin2.4.0";
  jdk = pkgs.temurin-bin-21;
  gradle = "9.6.0";
  kotlin = "2.4.0";
}
