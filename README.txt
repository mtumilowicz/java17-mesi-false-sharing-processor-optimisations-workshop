java17-jmh-false-sharing-vectorization-cacheline-mesi-workshop

* references
    * https://jenkov.com/tutorials/java-concurrency/false-sharing.html
    * [Cache Issues -- False Sharing -- Mike Bailey, Oregon State University](https://www.youtube.com/watch?v=dznxqe1Uk3E)
    * [JDD2019: Who ate my RAM?, Jarek Pałka](https://www.youtube.com/watch?v=bJAg_23ixmY)
    * [JDD 2018: new java.io.File("jdd.json"); is this really that simple? by Jarek Pałka](https://www.youtube.com/watch?v=cJBfQRXMBII)
    * [2023 - Krzysztof Ślusarski - Java vs CPU](https://www.youtube.com/watch?v=D96mSWuU-xc)




* true sharing



# cache false sharing
* example: https://www.youtube.com/watch?v=D96mSWuU-xc
  * przydzielenie oddzielnego miejsca dla resultow per watek
    ```
    private static final ARRAY_A = new int[SIZE];
    private static int[] results = new int[THREADS]; // tu jest zapis wynikow

    zadanie: policzyc ile jest parzystych w ARRAY_A wielowatkowo

    jesli uzyjemy atomica do incrementacji: bedzie wolno
    bardzo szybko, bo java opozni zapis do glownej pamieci az do konca
    jesli wymusimy zapis do glownej pamieci: bedzie bardzo wolno
    ```
  * writer i reader w innych watkach
    ```
    tuple { volatile int read; volatile int write; }

    i tylko dwa watki: jeden inrementuje write, a drugi odczytuje read
    ```
    jak usuniemy write to bedzie duzo szybciej
* true sharing: jak watki pracuja na DOKLADNIE tym samym polu
  * false sharing: kiedy operuja na tej samej cache line ale na innych polach
* the benefits of multithreading can disappear if the threads are
competing for the same cache line
* each core has its own separate l2 cache, but a write by one can possibly
impact the state of the others
* each core's L2 cache has 4 states (MESI):
    * Modified
        * exclusively owned by that core, and modified (dirty)
    * Exclusive
        * exclusively owned by that core, but not modified
    * Shared
        * shared read-only with other cores
    * Invalid
        * cache line not used
* overview

    |Step   |cache line A   |cache line B   |
    |---    |---            |---            |
    |1      |Exclusive      |-              |
    |2      |Shared         |Shared         |
    |2      |Invalid        |Modified       |
    |4      |Shared         |Shared         |

    1. Core A reads a value
        * those values are brought into its cache
        * that cache line is now tagged Exclusive
    1. Core B reads a value from the same are of memory
        * those values are brought into its cache, and now both
        cache lines are re-tagged Shared
    1. If Core B writes into that value
        * its cache line is re-tagged Modified and Core A's cache
        line is re-tagged Invalid
    1. Core A tries to read a value from that same part of memory
        * but its cache line is tagged Invalid
        * so, Core B's cache line is forced back to memory and then Core A's
        cache line is re-loaded from memory
        * both cache lines are now tagged Shared
    * this is a huge performance hit, and is referred to as False Sharing
    * event if CoreA and CoreB are writing to separate memory locations in
    in the same cache line the fact that they were the same cache line causes this
    to happen
* note that False Sharing doesn't cause incorrect results - just a performance hit!
* usually cache line has 16 slots, if you use padding you could move a value to the next
cache line
    * example: 4 first - XXXX, next 12 - 0000...
    * padding 7: X - 0000000(7) - X - 0000000(7) and in the next line the same
* summary
    * each l2 cache has four states: Modified, Exclusive, Shared, Invalid (MESI)
    * when a cache's state is Invalid (I), it will reload that cache the next time it needs
    to look at a value in it
        * this is true, even if that value is perfectly current
        * example: modifying array (4 fields, 4 cache slots)
    * benefits of multithreading can disappear if threads are competing
    for the same cache line
* unit of granularity of a cache entry is 64 bytes (512 bits)
    * even if you read/write 1 byte you're writing 64 bytes
* false sharing
    * two cores trying to write to bytes in the same cache-line will trash
        * example:
            * Thread 1: data[0] = 'A'
            * Thread 2: data[7] = 'B'
        * first thread will try to acquire exclusive ownership of cache line
        * second thread (on different core) will try to do the same
        * updates may be lost if writes are not atomic
        * performance will suffer when cache line repeatedly moved
        * avoid by padding to at least cacheline size * 2 (128 bytes) for writes
