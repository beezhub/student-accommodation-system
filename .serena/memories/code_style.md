# Code Style & Architecture
- **Backend**: Uses a Facade pattern (`RoomAllocationService`, `ApplicationService`, `DocumentService`). Sub-services are package-private. No business logic in controllers. MapStruct for DTO mapping.
- **Database**: Flyway for migrations (no hbm2ddl updates). `tenant_id` used for multi-tenancy.
- **Frontend**: Lazy-loaded standalone components in Angular 21. Strong reliance on RxJS `BehaviorSubject`. Services own HTTP calls. No hardcoded API paths. Must write tests for services (aiming for 70% coverage).