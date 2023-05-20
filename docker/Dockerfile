FROM ubuntu:20.04 as build
ARG EVE_REF_VERSION
RUN apt-get update && apt-get install -y zip
COPY build/distributions/eve-ref-${EVE_REF_VERSION}.tar /eve-ref.tar
RUN mkdir /eve-ref
RUN tar -x --strip-components=1 -f /eve-ref.tar -C /eve-ref
RUN unzip /eve-ref/lib/eve-ref-${EVE_REF_VERSION}.jar pug/* -d /eve-ref/

FROM eclipse-temurin:17.0.7_7-jre-focal
ARG EVE_REF_VERSION
ENV EVE_REF_VERSION=${EVE_REF_VERSION}
COPY --from=build /eve-ref /eve-ref
RUN chown -R 1000 /eve-ref
WORKDIR /eve-ref
COPY docker/entry.sh entry.sh
USER 1000
ENTRYPOINT ["./entry.sh"]
CMD ["placeholder"]
