# Releasing

`0.1.0` is published. Publishing runs through `com.vanniktech.maven.publish` 0.37.0
against the Central Portal, the `com.sinebloc` namespace is verified, and the signing
key exists. What follows is the setup as it was done — needed again only on a fresh
machine or by a second maintainer.

## One-time setup (already done for this project)

1. **Central Portal account** — https://central.sonatype.com

2. **Verify the `com.sinebloc` namespace** — done, via a DNS TXT record on
   `sinebloc.com`. To repeat it elsewhere: add the TXT record the Portal
   gives you to the exact domain the namespace names — the Portal checks that domain
   and does not try variations. Usually verifies in minutes.

   If DNS is not available, the fallback is `io.github.<user>`, verified by creating a
   public repo named after the verification key — but then change `coordinates(...)` in
   `build.gradle.kts` to match, because the group must sit inside a verified namespace.

3. **A GPG key.** Central rejects unsigned artifacts. Generate a pair and publish the
   public half to a keyserver.

4. **Credentials, in `~/.gradle/gradle.properties` — never in this repo:**

   ```properties
   mavenCentralUsername=<Portal user token name>
   mavenCentralPassword=<Portal user token>

   signing.keyId=<last 8 chars of the key id>
   signing.password=<key password>
   signing.secretKeyRingFile=/Users/<you>/.gnupg/secring.gpg
   ```

   For CI, use the environment form instead: `ORG_GRADLE_PROJECT_mavenCentralUsername`,
   `ORG_GRADLE_PROJECT_mavenCentralPassword`, and `ORG_GRADLE_PROJECT_signingInMemoryKey`
   (the ascii-armored private key) with `...KeyId` and `...KeyPassword`.

   The build only calls `signAllPublications()` when it can see a signing key, so
   `publishToMavenLocal` keeps working on a machine without one. It logs a warning when
   it skips signing — if you see that warning during a real release, stop: Central will
   reject the upload.

## Cutting a release

Bump `coordinates(...)` in `build.gradle.kts` and the two version strings in
`README.md` (the dependency snippet and the version-catalog entry), then:

```bash
./gradlew publishToMavenLocal          # sanity check: artifacts, POM, sources, javadoc
./gradlew publishAndReleaseToMavenCentral -PuseGpgCmd=true
```

`-PuseGpgCmd=true` is what 0.1.0 was signed with — see the note on it in
`build.gradle.kts`. Drop it if your key is one Gradle's bundled BouncyCastle can read.

Use `publishToMavenCentral` instead if you want to inspect the staged deployment in the
Portal and release it by hand.

Then tag it:

```bash
git tag -a v0.1.0 -m "quiet-motion 0.1.0" && git push origin v0.1.0   # 0.1.0: tagged
```

## Two things that cannot be undone

**A released version is immutable.** Central will not let you delete or overwrite
`0.1.0`. A mistake costs a new version number, so run `publishToMavenLocal` and look at
what comes out first.

**A published KMP artifact's target list is frozen per version.** Adding a target later
does not help anyone already unable to resolve the old version. Decide the target list
before the upload, not after.
