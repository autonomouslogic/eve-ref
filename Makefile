.PHONY: dist test clean docker
EVE_REF_VERSION = $(shell ./gradlew properties | grep version | cut -d' ' -f 2)
DOCKER_TAG = autonomouslogic/eve-ref:$(EVE_REF_VERSION)

dist:
	./gradlew distTar

test:
	./gradlew test

docker: dist
	docker build \
		-f docker/Dockerfile \
		--tag $(DOCKER_TAG) \
		--build-arg "EVE_REF_VERSION=$(EVE_REF_VERSION)" \
		.

clean:
	./gradlew clean

version:
	echo $(EVE_REF_VERSION)
