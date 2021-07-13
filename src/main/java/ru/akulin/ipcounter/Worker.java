package ru.akulin.ipcounter;

import java.io.BufferedReader;

public class Worker implements Runnable {

    private final BufferedReader reader;
    private final Index index;

    public Worker(BufferedReader reader, Index index) {
        this.reader = reader;
        this.index = index;
    }

    @Override
    public void run() {
        String ip4Address;

        // Поток в любом случае надо обернуть в try-catch
        try {
            while ((ip4Address = reader.readLine()) != null) {
                index.insert(ip4Address);
            }
        }
        catch (Exception cause) {
            cause.printStackTrace(); // Выше пробрасывать некуда. Пишем ошибку
            System.exit(-1);   // и закрываем приложение
        }
    }
}
