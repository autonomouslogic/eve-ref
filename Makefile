.PHONY: dist test format clean docker docs
EVE_REF_VERSION = $(shell ./gradlew properties | grep 'version:' | cut -d' ' -f 2)
DOCKER_TAG_BASE = autonomouslogic/eve-ref
DOCKER_TAG = $(DOCKER_TAG_BASE):$(EVE_REF_VERSION)
DOCKER_TAG_LATEST = $(DOCKER_TAG_BASE):latest

init: init-ui

init-ui:
	cd ui ; npm install

dist: generate-database
	./gradlew distTar --stacktrace

test-java: generate-database
	./gradlew test --stacktrace

test-ui:
	cd ui ; npm run test

test: test-java test-ui

lint:
	./gradlew spotlessCheck --stacktrace
	cd ui ; npm run lint

format:
	./gradlew spotlessApply --stacktrace
	cd ui ; npm run format

specs: generate-database
	./gradlew refDataSpec --stacktrace

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

docker-push: docker
	docker push $(DOCKER_TAG)
	docker push $(DOCKER_TAG_LATEST)

docker-placeholder: docker
	docker run -it --env-file local.env $(DOCKER_TAG) placeholder

docker-data-index: docker
	docker run -it --env-file local.env $(DOCKER_TAG) data-index

clean:
	./gradlew clean --stacktrace
	rm -R ui/.nuxt || true
	rm -R ui/.nitro || true
	rm -R ui/.cache || true
	rm -R ui/.output || true
	rm -R ui/.env || true
	rm -R ui/dist || true

version:
	echo $(EVE_REF_VERSION)

renovate-validate:
	npm install renovate
	node node_modules/renovate/dist/config-validator.js

dev-docs:
	cd docs ; npm run dev

update-esi-swagger:
	curl -s https://esi.evetech.net/latest/swagger.json | jq . > spec/esi-swagger.json

generate-database:
	./gradlew database:compileJava
	./gradlew database:generateJooq

postgres-start:
	docker run -d --rm \
		--name eve-ref-postgres \
		-p 5432:5432 \
		-e POSTGRES_DB=everef \
		-e POSTGRES_USER=everef \
		-e POSTGRES_PASSWORD=password1 \
		postgres:15

postgres-stop:
	docker stop eve-ref-postgres

postgres-migrate-test:
	make postgres-start
	./gradlew postgresMigrate
	make postgres-stop
