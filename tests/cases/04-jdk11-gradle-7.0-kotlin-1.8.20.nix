{ pkgs ? import ../nixpkgs.nix { } }:
(import ../lib.nix { inherit pkgs; }) {
  name = "jdk11-gradle7.0-kotlin1.8.20";
  jdk = pkgs.temurin-bin-11;
  gradle = "7.0";
  kotlin = "1.8.20";
}
