package ru.nsu.zenin.testapp;

import ru.nsu.zenin.list.ConcurrentLinkedList;
import ru.nsu.zenin.sorting.ConcurrentListSorter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import java.util.Scanner;

// TODO: Add delay options, add possibility to choose implementation
public class App {
    private static Option jobs =
            Option.builder()
                    .argName("workerThreads")
                    .option("j")
                    .longOpt("jobs")
                    .hasArg(true)
                    .desc("sorter threads amount")
                    .build();
    private static Option help =
            Option.builder()
                    .option("h")
                    .longOpt("help")
                    .hasArg(false)
                    .desc("display help message")
                    .build();
    private static Options options = new Options().addOption(jobs).addOption(help);

    public static void main(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            // If there is --help option - display help and exit
            if (cmd.hasOption(help)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("test_app [options]", options);
                return;
            }

            AppConfig conf = parseArgs(cmd);

            appMain(conf);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static AppConfig parseArgs(CommandLine cmd) throws ParseException {
        AppConfig conf = new AppConfig();

        if (cmd.hasOption(jobs)) {
            String workerThreadsRaw = cmd.getOptionValue(jobs);
            int workerThreads = Integer.parseInt(workerThreadsRaw);
            conf.setWorkerThreads(workerThreads);
        } else if (conf.getWorkerThreads() == null) {
            conf.setWorkerThreads(Runtime.getRuntime().availableProcessors());
        }

        return conf;
    }

    private static void appMain(AppConfig conf) {
        ConcurrentLinkedList<String> li = new ConcurrentLinkedList<String>();

        ThreadGroup sorters = new ThreadGroup("Sorters");
        for (int i = 0; i < conf.getWorkerThreads(); i++) {
            Thread sorter = new Thread(sorters, new ConcurrentListSorter<String>(li));
            sorter.start();
        }

        Scanner scanner = new Scanner(System.in);

        while (!Thread.interrupted()) {
            String line = scanner.nextLine();

            if (line.isEmpty()) {
                for (String s : li) {
                    System.out.println(s);
                }
            } else {
                li.add(line);
            }
        }
    }
}
