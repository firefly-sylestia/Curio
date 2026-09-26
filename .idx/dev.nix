# To learn more about how to use Nix to configure your environment
# see: https://firebase.google.com/docs/studio/customize-workspace
{ pkgs, ... }:
let
  python = pkgs.python3;
in
{
  # Which nixpkgs channel to use.
  channel = "stable-24.05"; # or "unstable"

  # Use https://search.nixos.org/packages to find packages
  packages = [
    # --- Python — the CI readers in .github/scripts/ are Python ------------
    python
    pkgs.python3Packages.pip
    pkgs.python3Packages.requests
    pkgs.python3Packages.pyyaml

    # --- Node / web tooling (web/ is Vite + TS; auth-web/ is dependency-free)
    pkgs.nodejs_20
    pkgs.nodePackages.pnpm # auth-web/ ships a pnpm lockfile

    # --- Android / Gradle toolchain — CI builds with Temurin JDK 17 -------
    pkgs.jdk17
    pkgs.gradle

    # --- Android command-line tools --------------------------------------
    # adb/fastboot, the SDK manager, and the APK inspection tools. Note that
    # Gradle builds are CI-only in this workspace (see root AGENTS.md), so
    # these are for inspecting devices and reading built artifacts, not for
    # assembling the app.
    pkgs.android-tools # adb, fastboot
    pkgs.sdkmanager # sdkmanager / avdmanager
    pkgs.aapt # resource table dumps
    pkgs.apksigner
    pkgs.apktool # decode/rebuild an APK
    pkgs.jadx # decompile to Java
    pkgs.kotlin # standalone kotlinc
    pkgs.ktlint
    pkgs.imagemagick # resize/prepare store + design assets

    # --- Everyday CLI the agent workflow relies on ------------------------
    pkgs.git
    pkgs.gh # `gh run list` / `gh run view --log-failed` for CI checks
    pkgs.jq
    pkgs.curl
    pkgs.wget
    pkgs.unzip
    pkgs.zip
    pkgs.openssl
    pkgs.sqlite # inspect local .db data
    pkgs.ripgrep
    pkgs.fd
  ];

  # Sets environment variables in the workspace.
  # nixpkgs' python3 ships `python3`/`pip3`; JAVA_HOME is wired to the JDK so
  # Gradle tooling launched from the workspace finds it.
  env = {
    JAVA_HOME = pkgs.jdk17.home;
  };
  idx = {
    # Search for the extensions you want on https://open-vsx.org/ and use "publisher.id"
    extensions = [
      # "vscodevim.vim"
    ];

    # Enable previews
    previews = {
      enable = true;
      previews = {
        # web = {
        #   # Example: run "npm run dev" with PORT set to IDX's defined port for previews,
        #   # and show it in IDX's web preview panel
        #   command = ["npm" "run" "dev"];
        #   manager = "web";
        #   env = {
        #     # Environment variables to set for your server
        #     PORT = "$PORT";
        #   };
        # };
      };
    };

    # Workspace lifecycle hooks
    workspace = {
      # Runs when a workspace is first created
      onCreate = {
        # Example: install JS dependencies from NPM
        # npm-install = "npm install";
      };
      # Runs when the workspace is (re)started
      onStart = {
        # Example: start a background task to watch and re-build backend code
        # watch-backend = "npm run watch-backend";
      };
    };
  };
}
