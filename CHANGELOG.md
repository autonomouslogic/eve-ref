EVE Ref Changelog

## [2.0.0](https://github.com/autonomouslogic/eve-ref/compare/1.0.0...2.0.0) (2023-05-16)


### ⚠ BREAKING CHANGES

* Basic publish setup (#129)

### Features

* Added LoggingInterceptor. ([f7c017c](https://github.com/autonomouslogic/eve-ref/commit/f7c017cc623ef377bf6a83259ad964c72e0d38e6))
* Basic Docker setup ([#46](https://github.com/autonomouslogic/eve-ref/issues/46)) ([b528209](https://github.com/autonomouslogic/eve-ref/commit/b528209e982e2282c1ced399d3ced7bc16239ae6))
* Basic publish setup ([#129](https://github.com/autonomouslogic/eve-ref/issues/129)) ([b46239a](https://github.com/autonomouslogic/eve-ref/commit/b46239ac35a06968774fe5fca03dab12a90ac93c))
* Basic structure for the Ref Data dataset ([#64](https://github.com/autonomouslogic/eve-ref/issues/64)) ([8967099](https://github.com/autonomouslogic/eve-ref/commit/8967099e5fde957f9e4f4b5e068d2ba94823c01d))
* Basic URL handling ([#34](https://github.com/autonomouslogic/eve-ref/issues/34)) ([c9a1bff](https://github.com/autonomouslogic/eve-ref/commit/c9a1bffe68e92c7329212420e8624d7cdfa7c94a))
* Changed index cache max-age and added it to market orders ([#62](https://github.com/autonomouslogic/eve-ref/issues/62)) ([5b3dca0](https://github.com/autonomouslogic/eve-ref/commit/5b3dca0de7473408facb964b72c99119dfa7838a))
* Chunked market history fetch ([#128](https://github.com/autonomouslogic/eve-ref/issues/128)) ([2fbd25f](https://github.com/autonomouslogic/eve-ref/commit/2fbd25fb5170022afd2408c27c1452aaafcc5e5d))
* Data indexer ([#28](https://github.com/autonomouslogic/eve-ref/issues/28)) ([dc2108a](https://github.com/autonomouslogic/eve-ref/commit/dc2108a0fa069153bbffb9ed383aba8061ea535d))
* Healthcheck reporting ([#47](https://github.com/autonomouslogic/eve-ref/issues/47)) ([74fe876](https://github.com/autonomouslogic/eve-ref/commit/74fe876cd2824a234a661388537113272c46004a))
* Initial setup ([#4](https://github.com/autonomouslogic/eve-ref/issues/4)) ([4efe521](https://github.com/autonomouslogic/eve-ref/commit/4efe521825f2b987dbc0fd19e1f5b1cb4d6d5a8c))
* Market history scraper ([#124](https://github.com/autonomouslogic/eve-ref/issues/124)) ([52b2b19](https://github.com/autonomouslogic/eve-ref/commit/52b2b19a88b20093a39a08cded1aad93befd4909))
* Market order scraper ([#40](https://github.com/autonomouslogic/eve-ref/issues/40)) ([a5f6be5](https://github.com/autonomouslogic/eve-ref/commit/a5f6be57e6c4a7563876a1f910d3ce788ecb793a))
* Public contract scrape ([#50](https://github.com/autonomouslogic/eve-ref/issues/50)) ([b4c952e](https://github.com/autonomouslogic/eve-ref/commit/b4c952e79c5f664e0f2285876f0a27b00bb1e63f))
* Reference data API ([#121](https://github.com/autonomouslogic/eve-ref/issues/121)) ([89d47c4](https://github.com/autonomouslogic/eve-ref/commit/89d47c425d5da245bef8c91ed98b61a38a473d87))
* Simple memory caching for UniverseEsi. ([0199097](https://github.com/autonomouslogic/eve-ref/commit/019909733100fe061714b8247aed00642743e10d))
* Slack reporting ([#48](https://github.com/autonomouslogic/eve-ref/issues/48)) ([277ed1b](https://github.com/autonomouslogic/eve-ref/commit/277ed1b36c1e56db6ece0c8230804c4517ab3044))
* Types reference data ([#65](https://github.com/autonomouslogic/eve-ref/issues/65)) ([cf41ccc](https://github.com/autonomouslogic/eve-ref/commit/cf41ccc75b8df8febd1c72df8ea74ba277cfefb4))


### Bug Fixes

* **deps:** update all non-major dependencies ([#33](https://github.com/autonomouslogic/eve-ref/issues/33)) ([ac6dd8b](https://github.com/autonomouslogic/eve-ref/commit/ac6dd8b74cf96c9577c468d44b4127821d17cebb))
* **deps:** update dependency software.amazon.awssdk:s3 to v2.20.28 ([#35](https://github.com/autonomouslogic/eve-ref/issues/35)) ([953ad07](https://github.com/autonomouslogic/eve-ref/commit/953ad071289b24b9aea466ad4e2098263d439e87))
* ESI 420 interceptor works properly ([#44](https://github.com/autonomouslogic/eve-ref/issues/44)) ([9a1ffd7](https://github.com/autonomouslogic/eve-ref/commit/9a1ffd7fdecdaf929c4d605bbd6dce7bac6004ee))
* ESI threading and concurrency ([#45](https://github.com/autonomouslogic/eve-ref/issues/45)) ([d21fe2d](https://github.com/autonomouslogic/eve-ref/commit/d21fe2ddabd156b13b92a80c6c6e6a6ca2bee3cd))
* More request logging. ([42aa7db](https://github.com/autonomouslogic/eve-ref/commit/42aa7db865fa367311050a8602394390bb23966a))
* Replaced Google Tag Manager with Goat Counter. ([e69ec3f](https://github.com/autonomouslogic/eve-ref/commit/e69ec3fbae2ca5bf144bb7998030bcb21baac2f7))
* RxJava error handler ([#58](https://github.com/autonomouslogic/eve-ref/issues/58)) ([7c665fe](https://github.com/autonomouslogic/eve-ref/commit/7c665fe8dc0a62945847b87cc5952d86ebccd2eb))


### Build System

* Code coverage ([#5](https://github.com/autonomouslogic/eve-ref/issues/5)) ([84eb96c](https://github.com/autonomouslogic/eve-ref/commit/84eb96c4ab4398cff40ff747ebe16c702e60b905))


### Continuous Integration

* Code climate. ([457cf04](https://github.com/autonomouslogic/eve-ref/commit/457cf041e9e41dd0f0587b6214d41ac9f67ac9f6))
* Fix Renovate Configuration ([#52](https://github.com/autonomouslogic/eve-ref/issues/52)) ([2baaa4c](https://github.com/autonomouslogic/eve-ref/commit/2baaa4ccd67e2af9dbc8228e3fd06023dc761ddc))
* Removed special package rules for Renovate ([#53](https://github.com/autonomouslogic/eve-ref/issues/53)) ([4b917ef](https://github.com/autonomouslogic/eve-ref/commit/4b917ef0140d7d745798b1221675ec63032885e3))
* Renovate config ([7082d4f](https://github.com/autonomouslogic/eve-ref/commit/7082d4f0c8191d7a55300d7c368bd0200f7e35ac))
* Sonarcloud. ([7888a79](https://github.com/autonomouslogic/eve-ref/commit/7888a79c4d7afe3e95cd93cf6b37ddf04fae56b1))


### Tests

* Fixed EsiHelperTest ([#49](https://github.com/autonomouslogic/eve-ref/issues/49)) ([b4e2cbb](https://github.com/autonomouslogic/eve-ref/commit/b4e2cbbbf4cf46039284f647daf7f05dde108001))
* Replace Mock Interceptor with Mockwebserver ([#57](https://github.com/autonomouslogic/eve-ref/issues/57)) ([da92129](https://github.com/autonomouslogic/eve-ref/commit/da92129ee4e768207e858c9bb667a8e02b970181))
* Testing for the rate limit interceptor ([#43](https://github.com/autonomouslogic/eve-ref/issues/43)) ([e696947](https://github.com/autonomouslogic/eve-ref/commit/e696947dfdeb02c0fcbe9dcb2add1147172ef53e))


### Dependency Updates

* **deps:** update actions/checkout action to v3 ([#55](https://github.com/autonomouslogic/eve-ref/issues/55)) ([8442149](https://github.com/autonomouslogic/eve-ref/commit/84421499c73356693d30ddc614ab19bf9bd2d3b0))
* **deps:** update all non-major dependencies ([#30](https://github.com/autonomouslogic/eve-ref/issues/30)) ([b629bd3](https://github.com/autonomouslogic/eve-ref/commit/b629bd30911a329c00b3a7788513c124e9915168))
* **deps:** update all non-major dependencies ([#54](https://github.com/autonomouslogic/eve-ref/issues/54)) ([568b964](https://github.com/autonomouslogic/eve-ref/commit/568b964f4b6ee377bd9101ded0f177d4e9192d4f))
* **deps:** update all non-major dependencies ([#75](https://github.com/autonomouslogic/eve-ref/issues/75)) ([5d2bda3](https://github.com/autonomouslogic/eve-ref/commit/5d2bda39a54558d458ff5252f0d98c78ebd7d08c))
* **deps:** update dependency gradle to v8 ([#31](https://github.com/autonomouslogic/eve-ref/issues/31)) ([2f52073](https://github.com/autonomouslogic/eve-ref/commit/2f5207314ba53368ff1e3ea3b0e6330a6f504dea))
* **deps:** update plugin io.freefair.lombok to v8 ([#36](https://github.com/autonomouslogic/eve-ref/issues/36)) ([2fa7400](https://github.com/autonomouslogic/eve-ref/commit/2fa7400174e80c82e8ea78e2dc865a3d77df2c32))
* **deps:** update plugin org.danilopianini.git-sensitive-semantic-versioning-gradle-plugin to v1 ([#32](https://github.com/autonomouslogic/eve-ref/issues/32)) ([656a58e](https://github.com/autonomouslogic/eve-ref/commit/656a58ec7a52eb86836bcf97dbab33f54911008a))


### Documentation

* Documentation ([#113](https://github.com/autonomouslogic/eve-ref/issues/113)) ([a48ec6b](https://github.com/autonomouslogic/eve-ref/commit/a48ec6b1f6c51f4f8fb26481ff43be65316a0088))
* ESI notes. ([a8dbf89](https://github.com/autonomouslogic/eve-ref/commit/a8dbf8983e2d4f1ec445d41e3e21fd80a7c80485))
* Readme. ([b7226d2](https://github.com/autonomouslogic/eve-ref/commit/b7226d2a4488a591df85c618b88386aa40035ef4))


### Code Refactoring

* Data access ([#123](https://github.com/autonomouslogic/eve-ref/issues/123)) ([b1e66f2](https://github.com/autonomouslogic/eve-ref/commit/b1e66f20a96b394f99dadfdc8daae54f3c5393d6))


### Miscellaneous Chores

* funding file ([5162042](https://github.com/autonomouslogic/eve-ref/commit/5162042f060d8d95726c41aeb2eedbf8d6e012c4))
* **release:** 2.0.0 [skip ci] ([c30e2a9](https://github.com/autonomouslogic/eve-ref/commit/c30e2a9933f74ecf88e7c4925fa1f8a627fbc4c6)), closes [#129](https://github.com/autonomouslogic/eve-ref/issues/129) [#46](https://github.com/autonomouslogic/eve-ref/issues/46) [#129](https://github.com/autonomouslogic/eve-ref/issues/129) [#64](https://github.com/autonomouslogic/eve-ref/issues/64) [#34](https://github.com/autonomouslogic/eve-ref/issues/34) [#62](https://github.com/autonomouslogic/eve-ref/issues/62) [#128](https://github.com/autonomouslogic/eve-ref/issues/128) [#28](https://github.com/autonomouslogic/eve-ref/issues/28) [#47](https://github.com/autonomouslogic/eve-ref/issues/47) [#4](https://github.com/autonomouslogic/eve-ref/issues/4) [#124](https://github.com/autonomouslogic/eve-ref/issues/124) [#40](https://github.com/autonomouslogic/eve-ref/issues/40) [#50](https://github.com/autonomouslogic/eve-ref/issues/50) [#121](https://github.com/autonomouslogic/eve-ref/issues/121) [#48](https://github.com/autonomouslogic/eve-ref/issues/48) [#65](https://github.com/autonomouslogic/eve-ref/issues/65) [#33](https://github.com/autonomouslogic/eve-ref/issues/33) [#35](https://github.com/autonomouslogic/eve-ref/issues/35) [#44](https://github.com/autonomouslogic/eve-ref/issues/44) [#45](https://github.com/autonomouslogic/eve-ref/issues/45) [#58](https://github.com/autonomouslogic/eve-ref/issues/58) [#5](https://github.com/autonomouslogic/eve-ref/issues/5) [#52](https://github.com/autonomouslogic/eve-ref/issues/52) [#53](https://github.com/autonomouslogic/eve-ref/issues/53) [#49](https://github.com/autonomouslogic/eve-ref/issues/49) [#57](https://github.com/autonomouslogic/eve-ref/issues/57) [#43](https://github.com/autonomouslogic/eve-ref/issues/43) [#55](https://github.com/autonomouslogic/eve-ref/issues/55) [#30](https://github.com/autonomouslogic/eve-ref/issues/30) [#54](https://github.com/autonomouslogic/eve-ref/issues/54) [#75](https://github.com/autonomouslogic/eve-ref/issues/75) [#31](https://github.com/autonomouslogic/eve-ref/issues/31) [#36](https://github.com/autonomouslogic/eve-ref/issues/36) [#32](https://github.com/autonomouslogic/eve-ref/issues/32) [#113](https://github.com/autonomouslogic/eve-ref/issues/113) [#123](https://github.com/autonomouslogic/eve-ref/issues/123)

## [2.0.0](https://github.com/autonomouslogic/eve-ref/compare/1.0.0...2.0.0) (2023-05-16)


### ⚠ BREAKING CHANGES

* Basic publish setup (#129)

### Features

* Added LoggingInterceptor. ([f7c017c](https://github.com/autonomouslogic/eve-ref/commit/f7c017cc623ef377bf6a83259ad964c72e0d38e6))
* Basic Docker setup ([#46](https://github.com/autonomouslogic/eve-ref/issues/46)) ([b528209](https://github.com/autonomouslogic/eve-ref/commit/b528209e982e2282c1ced399d3ced7bc16239ae6))
* Basic publish setup ([#129](https://github.com/autonomouslogic/eve-ref/issues/129)) ([b46239a](https://github.com/autonomouslogic/eve-ref/commit/b46239ac35a06968774fe5fca03dab12a90ac93c))
* Basic structure for the Ref Data dataset ([#64](https://github.com/autonomouslogic/eve-ref/issues/64)) ([8967099](https://github.com/autonomouslogic/eve-ref/commit/8967099e5fde957f9e4f4b5e068d2ba94823c01d))
* Basic URL handling ([#34](https://github.com/autonomouslogic/eve-ref/issues/34)) ([c9a1bff](https://github.com/autonomouslogic/eve-ref/commit/c9a1bffe68e92c7329212420e8624d7cdfa7c94a))
* Changed index cache max-age and added it to market orders ([#62](https://github.com/autonomouslogic/eve-ref/issues/62)) ([5b3dca0](https://github.com/autonomouslogic/eve-ref/commit/5b3dca0de7473408facb964b72c99119dfa7838a))
* Chunked market history fetch ([#128](https://github.com/autonomouslogic/eve-ref/issues/128)) ([2fbd25f](https://github.com/autonomouslogic/eve-ref/commit/2fbd25fb5170022afd2408c27c1452aaafcc5e5d))
* Data indexer ([#28](https://github.com/autonomouslogic/eve-ref/issues/28)) ([dc2108a](https://github.com/autonomouslogic/eve-ref/commit/dc2108a0fa069153bbffb9ed383aba8061ea535d))
* Healthcheck reporting ([#47](https://github.com/autonomouslogic/eve-ref/issues/47)) ([74fe876](https://github.com/autonomouslogic/eve-ref/commit/74fe876cd2824a234a661388537113272c46004a))
* Initial setup ([#4](https://github.com/autonomouslogic/eve-ref/issues/4)) ([4efe521](https://github.com/autonomouslogic/eve-ref/commit/4efe521825f2b987dbc0fd19e1f5b1cb4d6d5a8c))
* Market history scraper ([#124](https://github.com/autonomouslogic/eve-ref/issues/124)) ([52b2b19](https://github.com/autonomouslogic/eve-ref/commit/52b2b19a88b20093a39a08cded1aad93befd4909))
* Market order scraper ([#40](https://github.com/autonomouslogic/eve-ref/issues/40)) ([a5f6be5](https://github.com/autonomouslogic/eve-ref/commit/a5f6be57e6c4a7563876a1f910d3ce788ecb793a))
* Public contract scrape ([#50](https://github.com/autonomouslogic/eve-ref/issues/50)) ([b4c952e](https://github.com/autonomouslogic/eve-ref/commit/b4c952e79c5f664e0f2285876f0a27b00bb1e63f))
* Reference data API ([#121](https://github.com/autonomouslogic/eve-ref/issues/121)) ([89d47c4](https://github.com/autonomouslogic/eve-ref/commit/89d47c425d5da245bef8c91ed98b61a38a473d87))
* Simple memory caching for UniverseEsi. ([0199097](https://github.com/autonomouslogic/eve-ref/commit/019909733100fe061714b8247aed00642743e10d))
* Slack reporting ([#48](https://github.com/autonomouslogic/eve-ref/issues/48)) ([277ed1b](https://github.com/autonomouslogic/eve-ref/commit/277ed1b36c1e56db6ece0c8230804c4517ab3044))
* Types reference data ([#65](https://github.com/autonomouslogic/eve-ref/issues/65)) ([cf41ccc](https://github.com/autonomouslogic/eve-ref/commit/cf41ccc75b8df8febd1c72df8ea74ba277cfefb4))


### Bug Fixes

* **deps:** update all non-major dependencies ([#33](https://github.com/autonomouslogic/eve-ref/issues/33)) ([ac6dd8b](https://github.com/autonomouslogic/eve-ref/commit/ac6dd8b74cf96c9577c468d44b4127821d17cebb))
* **deps:** update dependency software.amazon.awssdk:s3 to v2.20.28 ([#35](https://github.com/autonomouslogic/eve-ref/issues/35)) ([953ad07](https://github.com/autonomouslogic/eve-ref/commit/953ad071289b24b9aea466ad4e2098263d439e87))
* ESI 420 interceptor works properly ([#44](https://github.com/autonomouslogic/eve-ref/issues/44)) ([9a1ffd7](https://github.com/autonomouslogic/eve-ref/commit/9a1ffd7fdecdaf929c4d605bbd6dce7bac6004ee))
* ESI threading and concurrency ([#45](https://github.com/autonomouslogic/eve-ref/issues/45)) ([d21fe2d](https://github.com/autonomouslogic/eve-ref/commit/d21fe2ddabd156b13b92a80c6c6e6a6ca2bee3cd))
* More request logging. ([42aa7db](https://github.com/autonomouslogic/eve-ref/commit/42aa7db865fa367311050a8602394390bb23966a))
* Replaced Google Tag Manager with Goat Counter. ([e69ec3f](https://github.com/autonomouslogic/eve-ref/commit/e69ec3fbae2ca5bf144bb7998030bcb21baac2f7))
* RxJava error handler ([#58](https://github.com/autonomouslogic/eve-ref/issues/58)) ([7c665fe](https://github.com/autonomouslogic/eve-ref/commit/7c665fe8dc0a62945847b87cc5952d86ebccd2eb))


### Miscellaneous Chores

* funding file ([5162042](https://github.com/autonomouslogic/eve-ref/commit/5162042f060d8d95726c41aeb2eedbf8d6e012c4))


### Build System

* Code coverage ([#5](https://github.com/autonomouslogic/eve-ref/issues/5)) ([84eb96c](https://github.com/autonomouslogic/eve-ref/commit/84eb96c4ab4398cff40ff747ebe16c702e60b905))


### Continuous Integration

* Code climate. ([457cf04](https://github.com/autonomouslogic/eve-ref/commit/457cf041e9e41dd0f0587b6214d41ac9f67ac9f6))
* Fix Renovate Configuration ([#52](https://github.com/autonomouslogic/eve-ref/issues/52)) ([2baaa4c](https://github.com/autonomouslogic/eve-ref/commit/2baaa4ccd67e2af9dbc8228e3fd06023dc761ddc))
* Removed special package rules for Renovate ([#53](https://github.com/autonomouslogic/eve-ref/issues/53)) ([4b917ef](https://github.com/autonomouslogic/eve-ref/commit/4b917ef0140d7d745798b1221675ec63032885e3))
* Renovate config ([7082d4f](https://github.com/autonomouslogic/eve-ref/commit/7082d4f0c8191d7a55300d7c368bd0200f7e35ac))
* Sonarcloud. ([7888a79](https://github.com/autonomouslogic/eve-ref/commit/7888a79c4d7afe3e95cd93cf6b37ddf04fae56b1))


### Tests

* Fixed EsiHelperTest ([#49](https://github.com/autonomouslogic/eve-ref/issues/49)) ([b4e2cbb](https://github.com/autonomouslogic/eve-ref/commit/b4e2cbbbf4cf46039284f647daf7f05dde108001))
* Replace Mock Interceptor with Mockwebserver ([#57](https://github.com/autonomouslogic/eve-ref/issues/57)) ([da92129](https://github.com/autonomouslogic/eve-ref/commit/da92129ee4e768207e858c9bb667a8e02b970181))
* Testing for the rate limit interceptor ([#43](https://github.com/autonomouslogic/eve-ref/issues/43)) ([e696947](https://github.com/autonomouslogic/eve-ref/commit/e696947dfdeb02c0fcbe9dcb2add1147172ef53e))


### Dependency Updates

* **deps:** update actions/checkout action to v3 ([#55](https://github.com/autonomouslogic/eve-ref/issues/55)) ([8442149](https://github.com/autonomouslogic/eve-ref/commit/84421499c73356693d30ddc614ab19bf9bd2d3b0))
* **deps:** update all non-major dependencies ([#30](https://github.com/autonomouslogic/eve-ref/issues/30)) ([b629bd3](https://github.com/autonomouslogic/eve-ref/commit/b629bd30911a329c00b3a7788513c124e9915168))
* **deps:** update all non-major dependencies ([#54](https://github.com/autonomouslogic/eve-ref/issues/54)) ([568b964](https://github.com/autonomouslogic/eve-ref/commit/568b964f4b6ee377bd9101ded0f177d4e9192d4f))
* **deps:** update all non-major dependencies ([#75](https://github.com/autonomouslogic/eve-ref/issues/75)) ([5d2bda3](https://github.com/autonomouslogic/eve-ref/commit/5d2bda39a54558d458ff5252f0d98c78ebd7d08c))
* **deps:** update dependency gradle to v8 ([#31](https://github.com/autonomouslogic/eve-ref/issues/31)) ([2f52073](https://github.com/autonomouslogic/eve-ref/commit/2f5207314ba53368ff1e3ea3b0e6330a6f504dea))
* **deps:** update plugin io.freefair.lombok to v8 ([#36](https://github.com/autonomouslogic/eve-ref/issues/36)) ([2fa7400](https://github.com/autonomouslogic/eve-ref/commit/2fa7400174e80c82e8ea78e2dc865a3d77df2c32))
* **deps:** update plugin org.danilopianini.git-sensitive-semantic-versioning-gradle-plugin to v1 ([#32](https://github.com/autonomouslogic/eve-ref/issues/32)) ([656a58e](https://github.com/autonomouslogic/eve-ref/commit/656a58ec7a52eb86836bcf97dbab33f54911008a))


### Documentation

* Documentation ([#113](https://github.com/autonomouslogic/eve-ref/issues/113)) ([a48ec6b](https://github.com/autonomouslogic/eve-ref/commit/a48ec6b1f6c51f4f8fb26481ff43be65316a0088))
* ESI notes. ([a8dbf89](https://github.com/autonomouslogic/eve-ref/commit/a8dbf8983e2d4f1ec445d41e3e21fd80a7c80485))
* Readme. ([b7226d2](https://github.com/autonomouslogic/eve-ref/commit/b7226d2a4488a591df85c618b88386aa40035ef4))


### Code Refactoring

* Data access ([#123](https://github.com/autonomouslogic/eve-ref/issues/123)) ([b1e66f2](https://github.com/autonomouslogic/eve-ref/commit/b1e66f20a96b394f99dadfdc8daae54f3c5393d6))
