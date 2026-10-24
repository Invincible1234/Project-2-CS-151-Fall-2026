package test;

import static test.StrategyCompletionFixture.check;

import f1.model.ClassicRaceCar;
import f1.model.Team;

import ui.CarMenu;
import ui.ConsoleInput;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public final class ConsoleClockVerification {
    public static void main(String[] args) {
        Clock clock = Clock.fixed(Instant.parse("2035-02-03T12:00:00Z"), ZoneOffset.UTC);
        Team team = new Team("CLOCK-TEAM", "Apex", 2, 0);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleInput input =
                new ConsoleInput(
                        new StringReader("1\nCLOCK-H\nHybrid\n2035\n4\n3\n350\n0\n"),
                        new PrintStream(bytes),
                        clock);
        input.run(() -> new CarMenu(input, team).run());
        check(
                team.getCars().size() == 1 && team.getCars().get(0).getYear() == 2035,
                "Console year validation and model construction must share the supplied clock.");
        ClassicRaceCar classic =
                new ClassicRaceCar("CLOCK-C", "Apex", "Classic", 1980, 90, 2035, 250, clock);
        classic.performService("Date-controlled service");
        check(
                classic.getLastServiceDate().equals("2035-02-03"),
                "Service records must use the model's supplied clock.");
        ConsoleInput dates =
                new ConsoleInput(
                        new StringReader("2035-02-04\n2035-02-03\n"),
                        new PrintStream(bytes),
                        clock);
        check(
                dates.today().equals(LocalDate.of(2035, 2, 3))
                        && dates.pastOrPresentDate("Date").equals(dates.today())
                        && bytes.toString().contains("not in the future"),
                "Date prompts must reject tomorrow and accept today relative to the supplied"
                        + " clock.");
        System.out.println("Console clock verification passed.");
    }
}
