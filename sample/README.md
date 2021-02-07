# Tuyalib sample

## How to run

1. Run `./gradlew run`.

1. Edit generated `devices.json` file and set your parameters. You will need:

    - device IP in your local network;
    - device ID;
    - AES secret key for local interactions (instructions **TBD**).

1. Once you configured your device, run `./gradlew run` again.

Your lamp will [blink](src/main/kotlin/main.kt) with long interval.
