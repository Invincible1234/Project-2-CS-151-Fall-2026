package test;

import f1.ui.ConsoleState;
import static test.ConsoleSessionFixture.check;
import static test.ConsoleSessionFixture.run;

public final class ConsoleIntegratedExitVerification {
    public static void main(String[] args) {
        String[] scripts = {
            "6\nEXIT\n4\n", "6\n1\nNEW\nEXIT\n4\n", "6\n4\nH1\nEXIT\n4\n",
            "7\n1\nNEW\nEXIT\n4\n", "7\n3\nTRACK1\nEXIT\n4\n", "8\nEXIT\n4\n",
            "9\nEXIT\n4\n", "9\n1\nNEWD\nEXIT\n4\n", "9\n1\nNEWD\nName\nLICENSE\nEXIT\n4\n",
            "9\n2\nD1\nEXIT\n4\n", "9\n5\nNEWE\nName\nHYBRID_POWER_UNIT\nEXIT\n4\n",
            "9\n6\nD1\nEXIT\n4\n", "9\n18\nEXIT\n4\n", "9\n12\nD1\nEXIT\n4\n",
            "9\n13\nE1\nEXIT\n4\n", "9\n15\nE1\nTIRES\nEXIT\n4\n", "9\n16\nEXIT\n4\n"
        };
        for (String script : scripts) {
            ConsoleState state = new ConsoleState();
            String output = run("4\n" + script, state);
            check(output.endsWith("Goodbye." + System.lineSeparator()), "Nested EXIT must end the whole application.");
            check(!output.contains("Demo IDs are already in use"), "Input after EXIT must not execute another action.");
            check(state.team().getCars().size() == 2 && state.team().getDrivers().size() == 2
                    && state.team().getEngineers().size() == 2 && state.getCircuits().size() == 1,
                    "Exiting an unfinished action must preserve the shared registries.");
            check(!output.contains("Exception") && !output.contains("at f1."), "Graceful exit must not expose stack traces.");
        }
        ConsoleState state = new ConsoleState();
        String output = run("4\n6\n0\n7\n0\n8\n0\n9\n0\n1\n3\nEXIT\n", state);
        check(output.contains("Team{teamId='DEMO'") && output.split("Main menu", -1).length >= 7,
                "Back must return from every menu to the same main session.");
        System.out.println("PASS: Integrated EXIT across " + scripts.length + " prompts and Back navigation");
    }
}
