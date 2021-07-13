package ru.akulin.ipcounter;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

// Трехуровневое дерево. Из-за устройства IP адреса само собой всегда будет сбалансированным
// Первый уровень содержит корень из значений самого левого октета IP адреса
// Второй - ветви второго слева октета
// На третьем уровне лежат BitSet-ы по 65536 бит
// Если IP адресов окажется максимальное количество, то вся структура займёт в памяти примерно 600мб
// Чем-то напоминает двоичные индексы Oracle
public class Index {

    private final HashMap<Byte, HashMap<Byte, BitSet>> ixRoot;
    private final Object ixSync = new Object();
    private static final Pattern IP4_PATTERN = Pattern.compile("^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$");

    public Index() {
        ixRoot = new LinkedHashMap<>();
    }

    // Вставляем элемент в индекс
    public void insert(String ipAddress) {

        // Если строка не соответствует шаблону IP адреса, то выбрасываем исключение
        // Можно было бы определить собственный тип исключения, но надо ли это на таком небольшом проекте
        if ((ipAddress != null) && (!IP4_PATTERN.matcher(ipAddress).matches())) {
            throw new RuntimeException("Text does not matching IPv4 address pattern: " + ipAddress);
        }

        // Разделим строчку на части и поместим в массив байт
        // Здесь компилятор предупреждает про возможный NullPointerException, но как-бы вроде ему уже неоткуда взяться
        String[] ipAddressParts = ipAddress.split("\\.");
        byte[] ipAddressBytes = new byte[4];

        for (byte n = 0; n <= 3; n++) {
            ipAddressBytes[n] = (byte) Short.parseShort(ipAddressParts[n]);
        }

        // Посчитаем смещение адреса внутри набора бит
        // Это можно сделать за пределами критических секций
        int bitPos = (Byte.toUnsignedInt(ipAddressBytes[2]) << 8) | (Byte.toUnsignedInt(ipAddressBytes[3]));

        HashMap<Byte, BitSet> ixSubRoot;

        // Блокируем корень индекса
        synchronized (ixSync) {
            // Создаем ветку индекса, если еще не существует
            if (!ixRoot.containsKey(ipAddressBytes[0])) {
                ixRoot.put(ipAddressBytes[0], new LinkedHashMap<>());
            }
        }

        ixSubRoot = ixRoot.get(ipAddressBytes[0]);
        BitSet bs;

        // Блокируем только одну ветку, чтобы получить нужный BitSet
        synchronized (ixSubRoot) {
            if (!ixSubRoot.containsKey(ipAddressBytes[1])) {
                ixSubRoot.put(ipAddressBytes[1], new BitSet(65536));
            }

            bs = ixSubRoot.get(ipAddressBytes[1]);
        }

        // Блокируем только конкретный лист на пути Root -> SubRoot -> BitSet
        synchronized (bs) {
            bs.set(bitPos);
        }
    }

    // Подсчитываем число установленных бит
    // Можно было бы считать кардинальность налету, при добавлении элементов, было бы еще немного быстрее
    public long cardinality() {

        long cardinality = 0;

        for (Byte rk : ixRoot.keySet()) {
            HashMap<Byte, BitSet> sr = ixRoot.get(rk);
            for (Byte srk : sr.keySet()) {
                BitSet bs = sr.get(srk);
                cardinality += bs.cardinality();
            }
        }

        return cardinality;
    }
}
