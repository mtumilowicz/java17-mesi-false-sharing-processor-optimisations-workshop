# java17-mesi-false-sharing-processor-optimisations-workshop

* references
    * https://jenkov.com/tutorials/java-concurrency/false-sharing.html
    * https://www.baeldung.com/java-false-sharing-contended
    * [Cache Issues -- False Sharing -- Mike Bailey, Oregon State University](https://www.youtube.com/watch?v=dznxqe1Uk3E)
    * [JDD2019: Who ate my RAM?, Jarek Pałka](https://www.youtube.com/watch?v=bJAg_23ixmY)
    * [JDD 2018: new java.io.File("jdd.json"); is this really that simple? by Jarek Pałka](https://www.youtube.com/watch?v=cJBfQRXMBII)
    * [2023 - Krzysztof Ślusarski - Java vs CPU](https://www.youtube.com/watch?v=D96mSWuU-xc)
    * https://techexpertise.medium.com/cache-coherence-problem-and-approaches-a18cdd48ee0e
    * https://medium.com/@mallela.chandra76/cache-coherence-in-a-system-d9ba906b45f7
    * https://www.brainkart.com/article/The-MESI-protocol_7651/
    * https://www.geeksforgeeks.org/cache-coherence-protocols-in-multiprocessor-system/
    * https://stackoverflow.com/questions/49983405/what-is-the-benefit-of-the-moesi-cache-coherency-protocol-over-mesi
    * https://stackoverflow.com/questions/21126034/msi-mesi-how-can-we-get-read-miss-in-shared-state
    * https://stackoverflow.com/questions/10058243/mesi-cache-protocol
    * http://www.edwardbosworth.com/My5155_Slides/Chapter13/CacheCoherency.htm
    * https://fgiesen.wordpress.com/2014/07/07/cache-coherency/
    * https://github.com/melix/jmh-gradle-plugin
    * https://chat.openai.com/

## preface
* goals of these workshops
    * understanding modern cache architecture in multi-core environment
    * discussing cache write policies
    * explaining cache coherence problems
        * with presentation of most known cache coherence protocol: MESI
    * exemplifying cache performance hits
        * false sharing
        * loop order
    * showing processor optimisations
        * vectorization
* workshop plan
    * false sharing example
    * benchmarks
        * to trigger: `gradlew jmh`
        * vectorization
        * loop order

## prerequisite
* access to a cache by a processor involves one of two processes: read and write
    * each process can have two results
        * cache hit = processor accesses its private cache and finds the addressed item already in the cache
        * otherwise - cache miss

## cache coherence
* in modern CPUs (almost) all memory accesses go through the cache hierarchy
    * CPU core’s load/store (and instruction fetch) units normally can’t even access memory directly
        * physically impossible
            ![alt text](img/cache_coherence.png)
        * they talk to their L1 caches
            * at this point, there’s generally more cache levels involved
            * L1 cache doesn’t talk to memory directly anymore, it talks to a L2 cache – which in turns talks to memory
                * or maybe to a L3 cache
        * caches are organized into “lines”, corresponding to aligned blocks of memory
            * 32 bytes (older ARMs, 90s/early 2000s x86s/PowerPCs)
            * 64 bytes (newer ARMs and x86s)
            * 128 bytes (newer Power ISA machines)
* cache coherence
    * concern raised in a multi-core distributed caches
    * when multiple processors are operating on the same or nearby memory locations, they may end up sharing the same cache line
        * unit of granularity of a cache entry is 64 bytes (512 bits)
            * even if you read/write 1 byte you're writing 64 bytes
        * it’s essential to keep those overlapping caches consistent with each other
            * benefits of multithreading can disappear if the threads are competing for the same cache line
            * note that the problem really is that we have multiple caches, not that we have multiple cores
                * we could solve the entire problem by sharing all caches between all cores (L1)
                    * each cycle, the L1 picks one lucky core that gets to do a memory operation this cycle, and runs it
                        * problem: cores now spend most of their time waiting in line for their next turn at a L1 request
                            * processors do a lot of those, at least one for every load/store instruction
                                * slow
                            * solution: next best thing is to have multiple caches and then make them behave as if there was only one cache
                                * this is what cache coherency protocols are for
        * there are quite a few protocols to maintain the cache coherency between CPU cores
        * problem is not unique to parallel processing systems
            * strong resemblance to the "lost update" problem
        * general approach
            * getting read access to a cache line involves talking to the other cores
                * might cause them to perform memory transactions
            * writing to a cache line is a multi-step process
                * before you can write anything, you first need to acquire both exclusive ownership of the cache line and a copy of its existing contents
                    * "Read For Ownership" request
            * each line in a cache is identified and referenced by a cache tag (block number)
                * allows the determination of the primary memory address associated with each element in the cache
            * each individual cache must monitor the traffic in cache tags
                * corresponds to the blocks being read from and written to the shared primary memory
                * done by a snooping cache (or snoopy cache, after the Peanuts comic strip)
                    * basic idea behind snooping is that all memory transactions take place on a shared bus that’s visible to all cores
                        ![alt text](img/snoop_tags.png)
                    * caches don’t just interact with the bus when they want to do a memory transaction themselves
                        * instead, each cache continuously snoops on bus traffic to keep track of what the other caches are doing
                        * if one cache wants to read from or write to memory on behalf of its core, all the other cores notice
                            * that allows them to keep their caches synchronized
                            * one core writes to a memory location => other cores know that their copies of the corresponding cache line are now stale and hence invalid
                                * problem: write-back model
                                    * it’s not enough to broadcast just the writes to memory when they happen
                                        * physical write-back to memory can happen a long time after the core executed the corresponding store
                                            * for the intervening time, the other cores and their caches might themselves try to write to the same location, causing a conflict
                                    * if we want to avoid conflicts, we need to tell other cores about our intention to write before we start changing anything in our local copy
                    * memory itself is a shared resource
                        * memory access needs to be arbitrated
                            * only one cache gets to read data from, or write back to, memory in any given cycle
                * caches do not respond to bus events immediately
                    * reason: cache is busy doing other things (sending data to the core for example)
                        * it might not get processed that cycle
                    * invalidation queue
                        * place where bus message triggering a cache line invalidation sits for a while until the cache has time to process it
* cache write policies
    * write back
        * write operations are usually made only to the cache
        * main memory is only updated when the corresponding cache line is flushed from the cache
        * results in inconsistency
            * example: if two caches contain the same line, and the line is updated in one cache, the other cache will unknowingly have an invalid value
        * one fundamental implementation
            * MESI
                * each cache line can be in one of these four distinct states: Modified, Exclusive, Shared, or Invalid
                * key feature: delayed flush to main memory
                    * example: when no one reads the data there is no need to write main memory
                        * better to write only to cache as it is much faster
    * write through
        * all write operations are made to main memory as well as to the cache
        * ensures that main memory is always valid
        * has consistency issues
            * occur unless other cache monitor the memory traffic or receive some direct notification of the update.
        * two fundamental implementations
            1. with update protocol
                * after write to main memory message with the updated data is broadcast to all processor modules in the system
                    * each processor updates the contents of the affected cache block if this block is present in its cache
            1. with invalidation of copies
                * after write to main memory invalidation request is broadcast through the system
                    * all copies in other caches are invalidated
    * write-through caches are simpler, but write-back has some advantages
        * it can filter repeated writes to the same location
            * most of the cache line changes on a write-back => can issue one large memory transaction instead of several small ones
                * more efficient


## MESI protocol
* formal mechanism for controlling cache coherency using snooping techniques
* most widely used cache coherence protocol
* each line in an individual processors cache can exist in one of the four following states
    * (M)odified
        * result of a successful write hit on a cache line
            * its value is different from the main memory
        * indicates that the cache line is present in current cache only and is dirty
            * it must be in the I state for all other cores
        * modified line can be kept by a processor only as long as it's the only processor that has this copy
            * on a cache miss, the cache still needs to write-back data to memory if it is in the modified state
                * processor must signal "Dirty" and write the data back to the shared primary memory
                    * causing the other processor to abandon its memory fetch
            * it is necessary to first write in memory and then read it from there
                * reading cores can't directly read from the cache of the writing core
                    * it's more expensive
                    * example
                        1. suppose, both of A & B share that line and B got it directly from the cache line of A
                        1. C needs that line for a write
                        1. both A & B will have keep snooping that line
                            * shared copies grows
                            * greater impact on performance due to the snooping done by all the 'shared' processors
            * example
                1. let's processor A has that modified line
                1. processor B is trying to read that same cache (modified by A) line from main memory
                1. A's snooping read attempts for that line
                    * content in the main memory is invalid now (because A modified the content)
                1. A has to write it back to main memory
                    * in order to allow processor B (and others) to read that line
    * (E)xclusive
        * its value matches the main memory value
        * no other cache holds a copy of this line
        * main purpose: prevent the unnecessary broadcast of a Cache Invalidate signal on a write hit
            * reduces traffic on a shared bus
    * (S)hared
        * multiple caches may hold the line
        * main memory is up to date
        * if a core does not have exclusive access to a cache line when it wants to write, it first needs to send an "I want exclusive access" request to the bus
            * this tells all other cores to invalidate their copies of that cache line, if they have any
    * (I)nvalid
        * cache line does not contain valid data
        * it will reload that cache line the next time it needs to look at a value in it
            * even if that value is perfectly current => false sharing
* requires 2 bits per cache line to hold the state
    * 4 values: M, E, S, I
* transition between the states is controlled by memory accesses and bus snooping activity
    * suppose a requesting processor processing a write hit on its cache
        * by definition, any copy of the line in the caches of other processors must be in the Shared State
        * what happens depends on the state of the cache in the requesting processor
            1. Modified
                * protocol does not specify an action for the processor
            1. Shared
                * processor writes the data
                * marks the cache line as Modified
                * broadcasts a Cache Invalidate signal to other processors
            1. Exclusive
                * processor writes the data and marks the cache line as Modified
        * example

            |Step   |cache line A   |cache line B   |
            |---    |---            |---            |
            |1      |Exclusive      |-              |
            |2      |Shared         |Shared         |
            |3      |Invalid        |Modified       |
            |4      |Shared         |Shared         |

            1. Core A reads a value
            1. Core B reads a value from the same are of memory
            1. Core B writes into that value
            1. Core A tries to read a value from that same part of memory
                * Core B's cache line is forced back to memory
                * Core A's cache line is re-loaded from memory
    * simulation
        1. exclusive
            ![alt text](img/pt1_exclusive.png)
            * CPU 1
                * is the first (and only) processor to request block A from the shared memory
                * issues a BR (Bus Read) for the block and gets its copy
                    * neither CPU 2 nor CPU 3 respond to the BR
                * cache line containing block A is marked Exclusive
                    * subsequent reads to this block access the cached entry and not the shared memory
        1. shared
            ![alt text](img/pt2_shared.png)
            * CPU 2 requests the same block A
            * snoop cache on CPU 1 notes the request and CPU 1 broadcasts Shared, announcing that it has a copy of the block
                * CPU 3 does not respond to the BR
            * both copies of the block are marked as shared
                * indicates that the block is in two or more caches for reading
                * copy in the shared primary memory is up to date
        1. modified
            ![alt text](img/pt3_modified.png)
            * CPU 2 writes to the cache line it is holding in its cache
                * issues a BU (Bus Upgrade) broadcast, marks the cache line as Modified, and writes the data to the line
                * CPU 1 responds to the BU by marking the copy in its cache line as Invalid
                * CPU 3 does not respond to the BU
            * informally, CPU 2 can be said to "own the cache line"
        1. dirty
            ![alt text](img/pt4_dirty.png)
            * CPU 3 attempts to read block A from primary memory
            * CPU 1, the cache line holding that block has been marked as Invalid
                * CPU 1 does not respond to the BR (Bus Read) request
            * CPU 2 has the cache line marked as Modified
                * asserts the signal Dirty on the bus, writes the data in the cache line back to the shared memory, and marks the line Shared
                * informally, CPU 2 asks CPU 3 to wait while it writes back the contents of its modified cache line to the shared primary memory
            * CPU 3 waits and then gets a correct copy
            * The cache line in each of CPU 2 and CPU 3 is marked as Shared

## false sharing
* true sharing
    * CPUs are writing to the same variables stored within the same cache line
* CPUs are writing to independent variables stored within the same cache line
    * independent = each CPU doesn't really rely on the values written by the other CPU
    * steps
        1. first thread modifies the variables
            * cache line is invalidated in all CPU caches
        1. other CPUs reload the content of the invalidated cache line
            * even if they don't need the variable that was modified by first thread
* essence of problems with concurrent programming
    * more processors, more power, more electricity consumption but slower than single thread
* solution: padding
    * usually cache line has 16 slots
        * if you use padding you could move a value to the next cache line
    * JVM
        * don't use volatile
            * write to main memory will be delayed up to very end
            * assuming that threads are modifying not interlapping set of variables it's OK
            * example
                ```
                private static final int THREADS = 6
                private static int[] results = new int[THREADS] // each thread has it's own place to accumulate results
                ```
                however, if we modify `results` using volatile machinery, we have false sharing
                ```
                VH.setVolatile(results, offset, results[offset] + 1)
                ```
        * change data structures so the independent variables are no longer stored within the same cache line
            * `jdk.internal.vm.annotation.Contended`
                * annotation to prevent false sharing
                * introduced by Java 8 under `sun.misc` package
                    * repackaged later by Java 9
                * by default adds 128 bytes of padding
                    * cache line size in many modern processors is around 64/128 bytes
                    * configurable through the `-XX:ContendedPaddingWidth`
                * annotated field => JVM will add some paddings around it
                * annotated class => JVM will add the same padding before all the fields
                * `-XX:-RestrictContended`
                    * disable `@Contended` annotation
                * use cases
                    * `ConcurrentHashMap`
                        * https://github.com/openjdk/jdk/blob/f29d1d172b82a3481f665999669daed74455ae55/src/java.base/share/classes/java/util/concurrent/ConcurrentHashMap.java#L2565
                    * `ForkJoinPool`
                        * https://github.com/openjdk/jdk/blob/1e8806fd08aef29029878a1c80d6ed39fdbfe182/src/java.base/share/classes/java/util/concurrent/ForkJoinPool.java#L774
* note that false sharing doesn't cause incorrect results - just a performance hit
    * updates may be lost if writes are not atomic

## processor optimisations
* SIMD (Single Instruction, Multiple Data)
    * example: add four pairs of numbers together at once, rather than adding them sequentially
    * instructions supported by modern processors
    * allow a single instruction to operate on multiple data elements simultaneously
        * can significantly improve performance by exploiting parallelism at the instruction level
    * data should be aligned and contiguous in memory
* vectorization
    * involves identifying portions of code that can be executed in parallel using SIMD instructions
    * typically involves operations like arithmetic operations, array computations, and data processing loops
    * example: assembler on intel platform
        * `vpadd`
            * Vector Packed Add
            * used for adding packed integer or floating-point values stored in SIMD registers
            * vs `add` - typical instructions used for scalar operations
        * `vmovdqu`
            * Vector Move Unaligned
            * used for moving data between memory and SIMD registers
            * vs `mov` - typical instructions used for scalar operations