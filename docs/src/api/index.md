# API

The EVE Ref API is available at `api.everef.net` and an [OpenAPI spec](https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml) is available.
Use [Swagger Codegen](https://swagger.io/tools/swagger-codegen/) to generate a client for your language.

## Running Locally
The API can run locally on port 8080 without any special configuration or local dependencies:
```bash
docker run -it --rm autonomouslogic/eve-ref:latest api
```
