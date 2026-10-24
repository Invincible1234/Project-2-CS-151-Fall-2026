# F1 Team Operations

## Overview

A Java console application for managing Formula 1 teams, hybrid and classic
cars, personnel, circuits, race strategies, inventory, and results. Users can
prepare cars, plan races, manage resources, and track championship points.
Data stays in memory and resets when the program closes.

## Features

- Register teams, cars, drivers, engineers, and circuits with unique IDs.
- Track servicing and sessions, hybrid battery energy, and classic restoration,
  preservation, and ownership transfers.
- Plan strategies using weather, tires, car setup, energy, and required parts.
- Purchase inventory, manage budgets, and fulfill engineer parts requests.
- Record results, correct finishing positions, void results, and view standings.
- Restrict protected actions and personnel information by session role.

## Developers and contributions

- **Sharif:** `F1Car`, `HybridRaceCar`, `ClassicRaceCar`, `Circuit`,
  `RaceResult`, and their menus/tests.
- **Jeet:** `Team`, `RaceStrategy`, budgets, inventory, and the main
  application.
- **Rishit:** `Driver`, `Engineer`, personnel menus, validation, and privacy
  controls.

## Design

The model classes hold business rules; console menus collect input and invoke
those operations. This keeps validation in the model even when an operation
is called outside its menu.

`F1Car` is abstract and implements `Raceable`. Its subclasses provide different
performance and compliance behavior. Team collections store `F1Car` references,
and performance predictions call the appropriate subclass method at runtime.
Private fields, validated operations, business identities, and custom exceptions
protect state. Result corrections and voids update previously applied points.
Teams connect cars, personnel, inventory, strategies, and results. Strategies
reserve resources when ready, consume them on completion, and release
reservations when cancelled.

UML: [one-page PDF](uml-class-diagram.pdf) and [PNG](uml-class-diagram.png).
The PDF uses A0 landscape poster size; zoom in to read individual classes.

## Installation

Install JDK 17 or newer, with `java` and `javac` available in your terminal.
Use a shell that supports `.sh` scripts, such as a macOS/Linux terminal or
Git Bash. From the repository root, run:

```sh
./verify.sh
java -cp out f1.ui.ConsoleApplication
```

`verify.sh` compiles the application and tests into `out/`, then runs the
verification programs. Maven and external libraries are unnecessary.
To compile and run only the application:

```sh
mkdir -p out
find src/main/java -name '*.java' > out/sources.txt
javac --release 17 -d out @out/sources.txt
java -cp out f1.ui.ConsoleApplication
```

## Usage

The main menu provides these actions:

| Option | Area | Common actions |
| --- | --- | --- |
| 1 | Teams | Create/select a team and view its summary or performance |
| 2 | Strategies | Configure plans, mark ready, start, complete, or cancel |
| 3 | Inventory and budget | Manage resources and fulfill engineer requests |
| 4 | Load demo | Load a sample team, cars, crew, tires, and circuit |
| 5 | Session role | Select viewer, driver, engineer, or team principal |
| 6 | Cars | Create, prepare, service, upgrade, and manage car operations |
| 7 | Circuits | Manage tracks, weather, corner notes, and historical laps |
| 8 | Race results | Record results, correct/void them, and view standings |
| 9 | Personnel | Register personnel and manage assignments and workflows |
| 0 | Exit | Close the application |

For a quick start, choose **4 Load demo**. This selects team `DEMO` with hybrid
car `H1`, classic car `C1`, drivers `D1`/`D2`, engineers `E1`/`E2`, and circuit
`TRACK1`. Choose **6 Cars**, then **3 List**, to inspect the cars. Load the demo
once per run; repeating it is rejected because its IDs are already registered.

Choose **5 Session role** for protected budget/inventory actions. Role selection
demonstrates permissions; it is not a login system. Personnel workflows may
also require selecting the acting driver or engineer.

Use **2 Strategies** to create a plan, set weather/tires/aero, add hybrid energy
and required parts, finalize its estimate, mark it ready, then start and
complete or cancel it. Use **8 Race results** to record a completed race and
view standings or correct/void results. **6 Cars → 11 Advanced operations**
covers energy, restoration, preservation, ownership transfers, and performance
predictions. Engineer parts requests are purchased through **3 Inventory and
budget → 6 Engineer requests**.

Invalid input prompts again, and rejected operations display an error without
ending the session. Enter **EXIT** at any prompt to quit; end of input also
exits safely.

## Tests and project structure

- `src/main/java/`: application models, services, console menus, and utilities.
- `src/test/java/test/` and `test/`: verification programs and shared fixtures.
- `verify.sh`: compiles with Java 17 compatibility and treats compiler warnings
  as errors, then runs each verification program in a separate JVM.
- `out/`: generated classes and source lists; excluded from version control.

Tests are standalone Java verification programs.
They cover capacity boundaries, duplicate identities, permissions, car energy
and maintenance, restoration/preservation, strategy resource handling, result
accounting, and console workflows. A successful run ends with
`All verification programs passed.`

## Limits and future work

A shared constant limits successful creation to 100 instances per managed
concrete class during one run; team collections also enforce their capacities.
Duplicate business IDs are rejected across registered teams. Removing an object
does not reset its class's creation counter for that run. Performance
predictions are estimates from the project's model. Persistence, authenticated
accounts, and a graphical interface are possible future improvements.
