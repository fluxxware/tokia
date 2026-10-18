# Pinned nixpkgs for the harness — matches the JDK flake's input (github:NixOS/nixpkgs/nixos-25.11).
# Using this everywhere keeps local `tests/run-all.sh` and CI on the same Temurin JDK builds,
# instead of the host's <nixpkgs> / the drifting nixos-unstable channel.
{ system ? builtins.currentSystem }:
import
  (builtins.fetchTarball "https://github.com/NixOS/nixpkgs/archive/refs/heads/nixos-25.11.tar.gz")
  { inherit system; }
