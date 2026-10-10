{
  description = "Devshell for java development";

  inputs = {
    nixpkgs.url = "https://channels.nixos.org/nixpkgs-unstable/nixexprs.tar.zst";
  };

  outputs =
    { self, nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
    in
    {
      devShells."${system}".default = pkgs.mkShell {
        buildInputs = [
          pkgs.gradle_9
          pkgs.jdk25
        ];
        NIX_LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
          pkgs.stdenv.cc.cc
          pkgs.openssl
        ];
        NIX_LD = pkgs.lib.fileContents "${pkgs.stdenv.cc}/nix-support/dynamic-linker";
        VERTXWEB_ENVIRONMENT = "dev";
        VERTX_APPLICATION_PORT = "8083";
        packages = with pkgs; [
          pkgs.gradle_9
          pkgs.jdk25
        ];
      };

    };
}
