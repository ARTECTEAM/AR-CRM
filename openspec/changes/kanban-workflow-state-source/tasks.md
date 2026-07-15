# Tasks: Kanban Workflow State Source

## 1. Domain

- [x] 1.1 Add/adjust `TableroTest` expectations for TRATOS default columns as pipeline stages.
- [x] 1.2 Update `Tablero` default TRATOS columns to stage labels, preserving TAREAS workflow labels.

## 2. Application

- [x] 2.1 Add RED tests for `MoverColumnaFichaService`: TAREA→TAREAS allowed, TAREA→TRATOS rejected, TRATO→TRATOS allowed, TRATO→TAREAS rejected, TRATO move does not call outcome services.
- [x] 2.2 Add a granular output port for target column workflow compatibility if no existing port fits.
- [x] 2.3 Update `MoverColumnaFichaService` to validate target compatibility before saving.
- [x] 2.4 Add/adjust exception type for incompatible movement with Spanish message.

## 3. Infrastructure

- [x] 3.1 Add RED adapter test for resolving whether a target column is assigned to a tablero of a given type.
- [x] 3.2 Implement the compatibility port in the appropriate repository adapter.
- [x] 3.3 Wire the new port in `boot` if constructor composition requires it.
- [x] 3.4 Correct misleading schema/comment docs that imply `Columna` stores semantic state.

## 4. Verification

- [ ] 4.1 Run targeted backend tests for domain/application/infrastructure modules. Do not run package/build. BLOCKED: Maven wrapper runs with Java 25.0.2 and fails during Lombok/javac testCompile before tests (`TypeTag :: UNKNOWN`); project config expects Java 21.
