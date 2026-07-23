import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * ConcurrentNavigableMap / ConcurrentSkipListMap — concurrent sorted map (TreeMap analog).
 * Supports approximate matches: floor, ceiling, subMap, etc.
 */
public class ConcurrentNavigableMapDemo {

    public static void main(String[] args) {
        ConcurrentNavigableMap<Integer, String> ages = new ConcurrentSkipListMap<>();
        ages.put(18, "teen");
        ages.put(25, "young");
        ages.put(40, "mid");
        ages.put(65, "senior");

        System.out.println("map=" + ages);
        System.out.println("floorKey(30)=" + ages.floorKey(30));     // ≤ 30 → 25
        System.out.println("ceilingKey(30)=" + ages.ceilingKey(30)); // ≥ 30 → 40
        System.out.println("subMap(20,50)=" + ages.subMap(20, 50));
        System.out.println("headMap(40)=" + ages.headMap(40));
    }
}
