# Production Plan (Phased)

Date baseline: 2026-03-12
Project: ecommerce-practice

## Phase 0: Engineering Baseline (1 week)

- [ ] Protect `main` branch, require PR + CI pass
- [ ] CI pipeline includes: build, unit tests, integration tests, `smoke`, `mq-smoke`
- [ ] Enforce code style and quality gates (Checkstyle/Spotless)
- [ ] Add dependency/image vulnerability scan (OWASP/Trivy)
- [ ] Reproducible image builds (fixed versions, immutable tags)

Acceptance:
- Any commit can be built and verified end-to-end in one pipeline run

## Phase 1: Observability (1-2 weeks)

- [ ] Metrics: QPS, error rate, p95/p99 latency, JVM, thread pools, DB pool, MQ backlog
- [ ] Structured logs (JSON) with `traceId/requestId` propagation
- [ ] Distributed tracing (OpenTelemetry + Jaeger/Tempo)
- [ ] Alerting for availability, latency, error ratio, queue backlog, slow SQL

Acceptance:
- For one production-like failure, root-cause direction can be identified within 10 minutes

## Phase 2: Reliability and Consistency (2 weeks)

- [ ] Timeout/retry/circuit-breaker/rate-limit policies for critical dependencies
- [ ] End-to-end idempotency for payment callback and message consumption
- [ ] Reliable event delivery (Outbox or equivalent)
- [ ] Dead-letter retry strategy and manual compensation runbook

Acceptance:
- Duplicate callback/message does not produce duplicated business side effects

## Phase 3: Performance and Capacity (2 weeks)

- [ ] Pressure test matrix: read-heavy, order flow, spike traffic, long-duration load
- [ ] Slow SQL governance and index optimization
- [ ] Tune connection pools and thread pools
- [ ] Redis hotspot and cache penetration/breakdown protections
- [ ] Capacity planning with at least 2x target peak headroom

Acceptance:
- Staging/pre-prod meets target SLO (example: error rate < 0.1%, key API p95 within target)

## Phase 4: Security and Compliance (1-2 weeks)

- [ ] Move secrets to secret manager (no secrets in repo/plain env files)
- [ ] Auth hardening: token rotation, least privilege, audit trail
- [ ] Vulnerability remediation SLA (critical 24h, high 7d)
- [ ] Input validation, sensitive field masking, API rate controls

Acceptance:
- No outstanding critical vulnerabilities, key operations are auditable

## Phase 5: Release and Operations (1-2 weeks)

- [ ] Canary/blue-green release strategy with automated health checks
- [ ] Automatic rollback policy
- [ ] DB migration workflow with rollback support (Flyway/Liquibase)
- [ ] Release runbook and incident response runbook

Acceptance:
- Failed release can be rolled back and recovered within 5 minutes

## Phase 6: Disaster Recovery and Drills (1 week)

- [ ] Backup and restore drills (full + incremental)
- [ ] Define and validate RTO/RPO targets
- [ ] Multi-replica or multi-zone deployment strategy
- [ ] Failure drills: DB down, MQ backlog, Redis failure, network jitter

Acceptance:
- Drill report demonstrates RTO/RPO targets are met

## Current Position (as of 2026-03-12)

- Phase 0: partially done
- Phase 1: partially done (basic health checks + logs)
- Phase 3: partially done (local load tests + order-flow load tests)
- Next recommended focus: Phase 1 (observability) with concrete daily tasks
