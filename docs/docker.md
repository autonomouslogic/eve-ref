# Docker

EVE Ref can be run locally using
```bash
docker run autonomouslogic/eve-ref:latest command
```

## Configuration

The Docker container is configured exclusively using environment variables.
These can be set through Docker using the `-e` flag:
```
-e "VARIABLE=value"
```
or using an `.env` file: using the `--env-file` flag.

For a list of support configuration options in EVE Ref, see [Configs.java](https://github.com/autonomouslogic/eve-ref/blob/main/src/main/java/com/autonomouslogic/everef/config/Configs.java).

## Build

To build the Docker image, run:
```bash
make docker
```
