package ru.nsu.zenin.testapp;

import ru.nsu.zenin.list.ConcurrentLinkedList;
import ru.nsu.zenin.sorting.ConcurrentListSorter;
import ru.nsu.zenin.sorting.SyncronizedListSorter;
import ru.nsu.zenin.testapp.exception.UnknownSorterImplementationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import java.util.Scanner;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class App {
    private static Option jobs =
            Option.builder()
                    .argName("workerThreads")
                    .option("j")
                    .longOpt("jobs")
                    .hasArg(true)
                    .desc("sorter threads amount")
                    .build();
    private static Option inDelay =
            Option.builder()
                    .argName("n")
                    .option("i")
                    .longOpt("in-delay")
                    .hasArg(true)
                    .desc("delay inside sorting step")
                    .build();
    private static Option outDelay =
            Option.builder()
                    .argName("n")
                    .option("o")
                    .longOpt("out-delay")
                    .hasArg(true)
                    .desc("delay between sorting steps")
                    .build();
    private static Option implementation =
            Option.builder()
                    .argName("n")
                    .option("s")
                    .longOpt("sorter-impl")
                    .hasArg(true)
                    .desc("implementation of sorter. Either \"concurrent\" or \"synchronized\"")
                    .build();
    private static Option help =
            Option.builder()
                    .option("h")
                    .longOpt("help")
                    .hasArg(false)
                    .desc("display help message")
                    .build();
    private static Options options = new Options().addOption(jobs).addOption(inDelay).addOption(outDelay).addOption(implementation).addOption(help);

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

    private static AppConfig parseArgs(CommandLine cmd) throws ParseException, UnknownSorterImplementationException {
        AppConfig conf = new AppConfig();

        if (cmd.hasOption(implementation)) {
            String implementationRaw = cmd.getOptionValue(implementation);
            conf.setImplementation(SorterImplementation.fromString(implementationRaw));
        } else {
            conf.setImplementation(SorterImplementation.CONCURRENT_LIST);
        }

        if (cmd.hasOption(inDelay)) {
            String inDelayRaw = cmd.getOptionValue(inDelay);
            long inDelayArg = Long.parseLong(inDelayRaw);
            conf.setInDelay(inDelayArg);
        } else {
            conf.setInDelay(0L);
        }

        if (cmd.hasOption(outDelay)) {
            String outDelayRaw = cmd.getOptionValue(outDelay);
            long outDelayArg = Long.parseLong(outDelayRaw);
            conf.setOutDelay(outDelayArg);
        } else {
            conf.setOutDelay(0L);
        }

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
        switch (conf.getImplementation()) {
            case SorterImplementation.CONCURRENT_LIST:
                concurrentImpementation(conf);
                break;
            case SorterImplementation.SYNCHRONIZED_LIST:
                syncronizedImpementation(conf);
                break;
        }
    }

    private static void syncronizedImpementation(AppConfig conf) {
        List<String> li = Collections.synchronizedList(new ArrayList<String>());

        ThreadGroup sorters = new ThreadGroup("Sorters");
        for (int i = 0; i < conf.getWorkerThreads(); i++) {
            Thread sorter = new Thread(sorters, new SyncronizedListSorter<String>(li, conf.getOutDelay(), conf.getInDelay()));
            sorter.setDaemon(true);
            sorter.start();
        }

        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (line.isEmpty()) {
                synchronized (li) {
                    for (String s : li) {
                        System.out.println(s);
                    }
                }
            } else {
                li.add(line);
            }
        }
    }

    private static void concurrentImpementation(AppConfig conf) {
        ConcurrentLinkedList<String> li = new ConcurrentLinkedList<String>();

        ThreadGroup sorters = new ThreadGroup("Sorters");
        for (int i = 0; i < conf.getWorkerThreads(); i++) {
            Thread sorter = new Thread(sorters, new ConcurrentListSorter<String>(li, conf.getOutDelay(), conf.getInDelay()));
            sorter.setDaemon(true);
            sorter.start();
        }

        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
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
