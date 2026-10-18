{ pkgs ? import ../nixpkgs.nix { } }:
(import ../lib.nix { inherit pkgs; }) {
  name = "jdk8-gradle6.8.3-kotlin1.9.10";
  jdk = pkgs.temurin-bin-8;
  gradle = "6.8.3";
  kotlin = "1.9.10";
}
