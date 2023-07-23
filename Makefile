.PHONY: dist test format clean docker docs
EVE_REF_VERSION = $(shell ./gradlew properties | grep version | cut -d' ' -f 2)
DOCKER_TAG_BASE = autonomouslogic/eve-ref
DOCKER_TAG = $(DOCKER_TAG_BASE):$(EVE_REF_VERSION)
DOCKER_TAG_LATEST = $(DOCKER_TAG_BASE):latest

init: init-ui

init-ui:
	cd ui ; npm install

dist:
	./gradlew distTar

test-java:
	./gradlew test

test-ui:
	cd ui ; npm run test

test: test-java test-ui

lint:
	./gradlew spotlessCheck
	cd ui ; npm run lint

format:
	./gradlew spotlessApply
	cd ui ; npm run format

specs:
	./gradlew refDataSpec

dev-ui: specs
	cd ui ; npm run dev

build-ui: specs test-ui
	cd ui ; npm run build

docker: dist
	docker build \
		-f docker/Dockerfile \
		--tag $(DOCKER_TAG) \
		--tag $(DOCKER_TAG_LATEST) \
		--build-arg "EVE_REF_VERSION=$(EVE_REF_VERSION)" \
		.

docker-data-index: docker
	docker run -it --env-file local.env autonomouslogic/eve-ref:latest data-index

clean:
	./gradlew clean

version:
	echo $(EVE_REF_VERSION)

renovate-validate:
	npm install renovate
	node node_modules/renovate/dist/config-validator.js

dev-docs:
	cd docs ; npm run dev
