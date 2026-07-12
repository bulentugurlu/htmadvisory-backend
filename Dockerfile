# ─────────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Maven wrapper files and pom.xml first — lets Docker cache the
# dependency download layer separately from the source code, so a source-only
# change doesn't re-download all dependencies on the next build.
COPY pom.xml .

# Download dependencies (cached layer — only invalidated when pom.xml changes)
# Uses the system mvn; no Maven wrapper in this repo (run-dev.sh is local-only).
RUN apt-get update && apt-get install -y maven && \
    mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build the fat JAR, skipping tests (tests run in CI, not here)
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# ─────────────────────────────────────────────
# Stage 2: Runtime
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

# Install Playwright browser dependencies and Node.js to run playwright install
# Ubuntu Jammy does not have a 'chromium' apt package — Playwright manages its own browser
RUN apt-get update && apt-get install -y --no-install-recommends \
    nodejs \
    npm \
    wget \
    ca-certificates \
    fonts-liberation \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libc6 \
    libcairo2 \
    libcups2 \
    libdbus-1-3 \
    libexpat1 \
    libfontconfig1 \
    libgbm1 \
    libgcc1 \
    libglib2.0-0 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libstdc++6 \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxi6 \
    libxrandr2 \
    libxrender1 \
    libxss1 \
    libxtst6 \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 20 (Ubuntu Jammy default is 12 which is too old for Playwright)
# Then install Playwright CLI and download Chromium browser
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN npm install -g n && n 20 &&     npm install -g playwright@1.44.0 &&     playwright install chromium &&     playwright install-deps chromium

# Run as non-root for security — Cloud Run is fine with non-root containers
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

# Copy only the fat JAR from the build stage — no JDK, no Maven, no source
COPY --from=builder /build/target/platform-backend-0.0.1-SNAPSHOT.jar app.jar

# MaxMind GeoLite2 databases — mounted at runtime via Cloud Run volume or
# baked in here if bundled. The app degrades gracefully if these are absent
# (city/serviceProvider fields will be null on Visit records).
# Placeholder directory; actual .mmdb files are NOT committed to the repo.
RUN mkdir -p /app/geoip && chown -R appuser:appgroup /app

USER appuser

# Cloud Run always routes to 8080 — do not change this port.
EXPOSE 8080

# JVM tuning for Cloud Run containers:
# -XX:+UseContainerSupport   — respect container memory limits (not host RAM)
# -XX:MaxRAMPercentage=75.0  — use up to 75% of container RAM for heap;
#                              leaves headroom for metaspace, thread stacks,
#                              and the MaxMind DB file buffers
# -Djava.security.egd=...    — faster startup: avoids blocking on /dev/random
#                              for SecureRandom seeding (safe for web apps)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
