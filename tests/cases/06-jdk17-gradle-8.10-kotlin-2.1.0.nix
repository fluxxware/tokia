{ pkgs ? import ../nixpkgs.nix { } }:
(import ../lib.nix { inherit pkgs; }) {
  name = "jdk17-gradle8.10-kotlin2.1.0";
  jdk = pkgs.temurin-bin-17;
  gradle = "8.10";
  kotlin = "2.1.0";
}
