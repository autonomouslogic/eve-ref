FROM ubuntu:22.04 as build
ARG EVE_REF_VERSION
COPY build/distributions/eve-ref-${EVE_REF_VERSION}.tar /eve-ref.tar
RUN tar -xf /eve-ref.tar

FROM eclipse-temurin:17-jre-focal
ARG EVE_REF_VERSION
ENV EVE_REF_VERSION=${EVE_REF_VERSION}
COPY --from=build /eve-ref-${EVE_REF_VERSION} /eve-ref
RUN chown -R 1000 /eve-ref
COPY docker/entry.sh /entry.sh
WORKDIR /
USER 1000
ENTRYPOINT ["./entry.sh"]
CMD ["placeholder"]
