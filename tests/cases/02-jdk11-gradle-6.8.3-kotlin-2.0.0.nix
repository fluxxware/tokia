{ pkgs ? import ../nixpkgs.nix { } }:
(import ../lib.nix { inherit pkgs; }) {
  name = "jdk11-gradle6.8.3-kotlin2.0.0";
  jdk = pkgs.temurin-bin-11;
  gradle = "6.8.3";
  kotlin = "2.0.0";
}
