package ru.akulin.ipcounter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class Application {

    private int MAX_CPU;
    private final List<String> fileNames;

    private Application() {
        MAX_CPU = 1;
        fileNames = new LinkedList<>();
    }

    private void run() {

        // Если список файлов пустой, то выводим справку и выходим
        if (fileNames.isEmpty()) {
            help();
            System.exit(0);
        }

        // Создаём общий экземпляр индекса
        Index index = new Index();

        // Бежим по всем файлам по очереди, последовательно читаем строки из каждого
        // Парсим и индексируем стрроки в несколько потоков
        // Если попадётся строка, не соответствующая шаблону IP адреса, то поток с ошибкой закроет приложение
        // Возможно стоит ошибки потоков обрабатывать как-то централизованно
        for (String fileName : fileNames) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName, StandardCharsets.UTF_8))) {

                List<Thread> workerThreads = new ArrayList<>(MAX_CPU);

                for (int c = 1; c <= MAX_CPU; c++) {
                    Thread workerThread = new Thread(new Worker(reader, index));
                    workerThreads.add(workerThread);
                    workerThread.start();
                }

                for (Thread workerThread : workerThreads) {
                    workerThread.join();
                }

            }
            catch (Exception cause) {
                cause.printStackTrace(); // Это же не веб-сервис, дальше пробрасывать некуда
                System.exit(-1);
            }
        }

        System.out.println(index.cardinality());
        System.exit(0);
    }

    public static void main(String[] args) {
        Application application = new Application();
        application.configure(args);
        application.run();
    }

    // Минимальный парсер командной строки
    // Последний попавшийся параметр --max-cpu побеждает
    // Все остальные аргументы трактуются как имена файлов
    private void configure(String[] args) {
        Pattern maxCpuPattern = Pattern.compile("^--max-cpu=\\d+$");

        for (String arg : args) {
            if (maxCpuPattern.matcher(arg).matches()) {
                String[] parts = arg.split("=");
                MAX_CPU = Integer.parseInt(parts[1]);
            }
            else {
                fileNames.add(arg);
            }
        }
    }

    private void help() {
        System.out.println("Usage: java -cp ipcounter.jar ru.akulin.ipcounter.Application [--max-cpu=N] [file [file [file...]]]");
    }
}
