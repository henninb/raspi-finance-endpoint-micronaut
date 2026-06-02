# Missing Features — Micronaut vs Reference

Tracked against `/home/henninb/projects/github.com/henninb/raspi-finance-endpoint`.

## Status Legend
- [ ] Not started
- [x] Done

---

## 1. Missing Utility Classes

| Status | File | Description |
|--------|------|-------------|
| [x] | `utils/TransactionTypeConverter.kt` | JPA converter for `TransactionType` enum |
| [x] | `utils/IpAddressValidator.kt` | IP extraction with trusted-proxy protection |
| [x] | `utils/OptionalExtensions.kt` | `Optional<T>.orThrowNotFound()` extension |

---

## 2. Missing Domain Extensions

| Status | File | Description |
|--------|------|-------------|
| [x] | `domain/ServiceResultExtensions.kt` | Map `ServiceResult<T>` → Micronaut `HttpResponse` |

---

## 3. Missing Controller Endpoints

### TransactionController (`/api/transaction`)
| Status | Method | Path | Description |
|--------|--------|------|-------------|
| [x] | GET | `/active` | List all active transactions |
| [x] | GET | `/date-range` | Filter transactions by date range |
| [x] | GET | `/account/select/{accountNameOwner}/paged` | Paginated transactions by account |
| [x] | POST | `/future` | Insert a future transaction |
| [x] | DELETE | `/receipt/image/{guid}` | Detach receipt image from transaction |
| [x] | GET | `/category/{category_name}` | Filter transactions by category |
| [x] | GET | `/description/{description_name}` | Filter transactions by description |
| [x] | GET | `/account/bonus-progress/{accountNameOwner}` | Bonus progress for account |

### AccountController (`/api/account`)
| Status | Method | Path | Description |
|--------|--------|------|-------------|
| [x] | GET | `/validation/refresh` | Refresh validation dates for all accounts |
| [x] | PUT | `/deactivate/{accountNameOwner}` | Deactivate an account |
| [x] | PUT | `/activate/{accountNameOwner}` | Activate an account |

### PendingTransactionController (`/api/pending/transaction`)
| Status | Method | Path | Description |
|--------|--------|------|-------------|
| [x] | GET | `/select/{pendingTransactionId}` | Find pending transaction by ID |
| [x] | PUT | `/update/{pendingTransactionId}` | Update pending transaction |

### TransferController (`/api/transfer`)
| Status | Method | Path | Description |
|--------|--------|------|-------------|
| [x] | GET | `/select/{transferId}` | Find transfer by ID |
| [x] | PUT | `/update/{transferId}` | Update transfer |

### ValidationAmountController (`/api/validation/amount`)
| Status | Method | Path | Description |
|--------|--------|------|-------------|
| [x] | GET | `/active` | List all active validation amounts |
| [x] | GET | `/select/{validationId}` | Find by ID |
| [x] | PUT | `/update/{validationId}` | Update |
| [x] | DELETE | `/delete/{validationId}` | Delete by ID |

### FamilyMemberController (`/api/family-members`)
| Status | Method | Path | Description |
|--------|--------|------|-------------|
| [x] | GET | `/owner/{owner}` | List family members by owner |
| [x] | GET | `/owner/{owner}/relationship/{relationship}` | List by owner + relationship |

---

## 4. Configurations (Micronaut equivalents needed)

| Status | Reference Class | Description |
|--------|----------------|-------------|
| [x] | `CorrelationIdFilter` | Propagate correlation ID on every request |
| [ ] | `RateLimitingFilter` | Per-IP rate limiting (future work) |
| [x] | `RequestLoggingFilter` | HTTP request/response logging |
| [x] | `SecurityAuditFilter` | Security event audit logging |
| [x] | `SqlQueryLoggingInterceptor` | SQL query logging via AOP interceptor |

---

## Notes
- GraphQL: The Micronaut project already has `schema.graphqls` + fetchers — this is ahead of the reference.
- Excel: `ExcelFileController` exists only in Micronaut — no gap here.
- `CsrfController` in reference is Spring Security specific; Micronaut handles CSRF differently — skip unless needed.
- `TenantContext` in reference uses Spring `SecurityContextHolder`; Micronaut uses `SecurityService` — port only if multi-tenancy is needed.
