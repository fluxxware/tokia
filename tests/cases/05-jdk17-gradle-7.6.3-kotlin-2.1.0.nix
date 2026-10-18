{ pkgs ? import ../nixpkgs.nix { } }:
(import ../lib.nix { inherit pkgs; }) {
  name = "jdk17-gradle7.6.3-kotlin2.1.0";
  jdk = pkgs.temurin-bin-17;
  gradle = "7.6.3";
  kotlin = "2.1.0";
}
