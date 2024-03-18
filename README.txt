# java17-mesi-false-sharing-vectorization-jmh-workshop

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



## cache coherence
* cache coherence
    * concern raised in a multi-core distributed caches
    * when multiple processors are operating on the same or nearby memory locations, they may end up sharing the same cache line
        * unit of granularity of a cache entry is 64 bytes (512 bits)
            * even if you read/write 1 byte you're writing 64 bytes
        * it’s essential to keep those overlapping caches in different cores consistent with each other
            * benefits of multithreading can disappear if the threads are competing for the same cache line
        * there are quite a few protocols to maintain the cache coherency between CPU cores
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


## MESI protocol
* when a cache's state is Invalid (I), it will reload that cache the next time it needs
    to look at a value in it
        * this is true, even if that value is perfectly current
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
* In modern CPUs (almost) all memory accesses go through the cache hierarchy
    * The CPU core’s load/store (and instruction fetch) units normally can’t even access memory directly – it’s physically impossible
        * they talk to their L1 caches which are supposed to handle it
        * At this point, there’s generally more cache levels involved;
            * this means the L1 cache doesn’t talk to memory directly anymore, it talks to a L2 cache – which in turns talks to memory. Or maybe to a L3 cache
    * Caches are organized into “lines”, corresponding to aligned blocks of either 32 (older ARMs, 90s/early 2000s x86s/PowerPCs), 64 (newer ARMs and x86s) or 128 (newer Power ISA machines) bytes of memory
* Cache Coherence Problem
    * In multiprocessor system where many processes needs a copy of same memory block, the maintenance of consistency among these copies raises a problem
    ![alt text](img/cache_coherence.png)
    * We first note that this problem is not unique to parallel processing systems.  Those students who have experience with database design will note the strong resemblance to the “lost update” problem.
    * Each line in a cache is identified by a cache tag (block number), which allows the determination of the primary memory address associated with each element in the cache.

      Cache blocks are identified and referenced by their memory tags.
      * In order to maintain coherency, each individual cache must monitor the traffic in cache tags, which corresponds to the blocks being read from and written to the shared primary memory.
      * This is done by a snooping cache (or snoopy cache, after the Peanuts comic strip), which is just another port into the cache memory from the shared bus.
        ![alt text](img/snoop_tags.png)
* formal mechanism for controlling cache coherency using snooping techniques
* acronym stands for modified, exclusive, shared, invalid and refers to the states that cached data can take
* Transition between the states is controlled by memory accesses and bus snooping activity
* most widely used cache coherence protocol
* On a cache miss, the cache still needs to writeback data to memory if it is in the modified state.
* Let's processor A has that modified line. Now processor B is trying to read that same cache (modified by A) line from main memory. Since the content in the main memory is invalid now (because A modified the content), A's snooping the any other read attempts for that line. So in order to allow processor B (and others) to read that line, A has to write it back to main memory.
* why not keep the data in the cache memory, and keep updating the data in the cache
    * You are right and this is what usually done. But here, that's not the case. Someone else (processor B in our example) is trying to read. So A has to write it back and make the cache line status to shared because both A and B are sharing the cache line now.
* What I meant by not writing to the memory was that can't we do such that the cache coherence mechanism reads data from the cache of the writing core and convey it to those reading it. Is it necessary to first write in memory and then read it from there. Can't the reading cores directly read from the cache of the writing core?
    * A modified line can be kept by a processor only as long as it's the only processor that has this copy (MESI). What you are saying can't done, because it's more expensive. Suppose, both of A & B share that line and B got it directly from the cache line of A and some one else, say C, needs that line for a write, then both A & B will have keep snooping that line. As the shared copies grows, then it will have greater impact on performance due to the snooping done by all the 'shared' processors. That's why this is not done
    * MESI uses write-back cache to reduce the writes back to main memory whenever possible
* So actually I don't think the reading cache has to go to main memory. In MESI, when a processor request a block modified by one of it's peers, it issue a read miss on the bus (or any interconnect medium), which is broadcasted to every processor.

  The processor which hold the block in the "Modified" state catch the call, and issue a copy back on the bus - holding the block ID and the value - while changing it's own copy state to "shared". This copy back is received by the requesting processor, which will write the block in it's local cache and tag it as "shared".

  It depends upon the implementation if the copy back issued by the owning processor goes to main memory, or not.
* MESI only requires 2 bits per cache line to hold the state
Modified –
This indicates that the cache line is present in current cache only and is dirty i.e its value is different from the main memory. The cache is required to write the data back to main memory in future, before permitting any other read of invalid main memory state.
Exclusive –
This indicates that the cache line is present in current cache only and is clean i.e its value matches the main memory value.
Shared –
It indicates that this cache line may be stored in other caches of the machine.
Invalid –
It indicates that this cache line is invalid.
* In the write–through strategy, all changes to the cache memory were immediately copied to the main memory.  In this simpler strategy, memory writes could be slow.

  In the write–back strategy, changes to the cache were not propagated back to the main memory until necessary in order to save the data.  This is more complex, but faster.
* Access to a cache by a processor involves one of two processes: read and write.  Each process can have two results: a cache hit or a cache miss.

  Recall that a cache hit occurs when the processor accesses its private cache and finds the addressed item already in the cache.  Otherwise, the access is a cache miss.
* Each line in an individual processors cache can exist in one of the four following states:

      1.   Invalid           The cache line does not contain valid data.

      2.   Shared           Multiple caches may hold the line; the shared memory is up to date.

      3.   Exclusive      No other cache holds a copy of this line;
                                  the shared memory is up to date.

                                  The main purpose of the Exclusive state is to prevent the unnecessary broadcast of a Cache Invalidate signal on a write hit.  This reduces traffic on a shared bus.

      4.   Modified       The line in this cache is valid; no copies of the line exist in
                                  other caches; the shared memory is not up to date.

                                  As a result of a successful write hit on a cache line, that cache line is always
                                  marked as Modified.
* Suppose a requesting processor processing a write hit on its cache.  By definition, any copy of the line in the caches of other processors must be in the Shared State.  What happens depends on the state of the cache in the requesting processor.

      1.   Modified       The protocol does not specify an action for the processor.

      2.   Shared           The processor writes the data, marks the cache line as Modified,
                                  and broadcasts a Cache Invalidate signal to other processors.

      3.   Exclusive      The processor writes the data and marks the cache line as Modified.
* If a line in the cache of an individual processor is marked as “Modified” and another processor attempts to access the data copied into that cache line, the individual processor must signal “Dirty” and write the data back to the shared primary memory.
    * Consider the following scenario, in which processor P1 has a write miss on a cache line.

          1.   P1 fetches the block of memory into its cache line, writes to it, and marks it Dirty.

          2.   Another processor attempts to fetch the same block from the shared main memory.

          3.   P1’s snoop cache detects the memory request.  P1 broadcasts a message “Dirty” on
                the shared bus, causing the other processor to abandon its memory fetch.

          4.   P1 writes the block back to the share memory and the other processor can access it.

* flow
    1. overview of architecture
        ![alt text](img/architecture_overview.png)
    1. exclusive
        ![alt text](img/pt1_exclusive.png)
        * CPU 1
            * is the first (and only) processor to request block A from the shared memory.
            * It issues a BR (Bus Read) for the block and gets its copy.
                * Neither CPU 2 nor CPU 3 respond to the BR.
            * The cache line containing block A is marked Exclusive.
                * Subsequent reads to this block access the cached entry and not the shared memory.
    1. shared
        ![alt text](img/pt2_shared.png)
        * CPU 2 requests the same block A
        * snoop cache on CPU 1 notes the request and CPU 1 broadcasts “Shared”, announcing that it has a copy of the block
            * CPU 3 does not respond to the BR.
        * Both copies of the block are marked as shared.
            * indicates that the block is in two or more caches for reading and
              that the copy in the shared primary memory is up to date
    1. modified
        ![alt text](img/pt3_modified.png)
        * CPU 2 writes to the cache line it is holding in its cache
            * It issues a BU (Bus Upgrade) broadcast, marks the cache line as Modified, and writes the data to the line
            * CPU 1 responds to the BU by marking the copy in its cache line as Invalid.
            * CPU 3 does not respond to the BU.
        * Informally, CPU 2 can be said to “own the cache line”.
    1. dirty
        ![alt text](img/pt4_dirty.png)
        * CPU 3 attempts to read block A from primary memory
        * CPU 1, the cache line holding that block has been marked as Invalid
            * CPU 1 does not respond to the BR (Bus Read) request.
        * CPU 2 has the cache line marked as Modified
            * It asserts the signal “Dirty” on the bus, writes the data in the cache line back to the shared memory, and marks the line “Shared”.
            * Informally, CPU 2 asks CPU 3 to wait while it writes back the contents of its modified cache line to the shared primary memory.
        * CPU 3 waits and then gets a correct copy.
        * The cache line in each of CPU 2 and CPU 3 is marked as Shared.

* Write-through caches are simpler, but write-back has some advantages: it can filter repeated writes to the same location, and if most of the cache line changes on a write-back, it can issue one large memory transaction instead of several small ones, which is more efficient.
* Note that the problem really is that we have multiple caches, not that we have multiple cores. We could solve the entire problem by sharing all caches between all cores: there’s only one L1$, and all processors have to share it. Each cycle, the L1$ picks one lucky core that gets to do a memory operation this cycle, and runs it.
    * This works just fine. The only problem is that it’s also slow, because cores now spend most of their time waiting in line for their next turn at a L1$ request (and processors do a lot of those, at least one for every load/store instruction)
    * We know that one set of caches works, but when that’s too slow, the next best thing is to have multiple caches and then make them behave as if there was only one cache.
        * This is what cache coherency protocols are for: as the name suggests, they ensure that the contents of multiple caches stay coherent.
* The basic idea behind snooping is that all memory transactions take place on a shared bus that’s visible to all cores: the caches themselves are independent, but memory itself is a shared resource, and memory access needs to be arbitrated: only one cache gets to read data from, or write back to, memory in any given cycle. Now the idea in a snooping protocol is that the caches don’t just interact with the bus when they want to do a memory transaction themselves; instead, each cache continuously snoops on bus traffic to keep track of what the other caches are doing. So if one cache wants to read from or write to memory on behalf of its core, all the other cores notice, and that allows them to keep their caches synchronized. As soon as one core writes to a memory location, the other cores know that their copies of the corresponding cache line are now stale and hence invalid.
    * But if there are write-back caches in the mix, this doesn’t work, since the physical write-back to memory can happen a long time after the core executed the corresponding store – and for the intervening time, the other cores and their caches are none the wiser, and might themselves try to write to the same location, causing a conflict.
    * So with a write-back model, it’s not enough to broadcast just the writes to memory when they happen; if we want to avoid conflicts, we need to tell other cores about our intention to write before we start changing anything in our local copy.
* Modified lines are dirty; they have been locally modified. If a line is in the M state, it must be in the I state for all other cores, same as E. In addition, modified cache lines need to be written back to memory when they get evicted or invalidated – same as the regular dirty state in a write-back cache.
* E state denoting exclusive access. This state solves the “we need to tell other cores before we start modifying memory” problem: each core may only write to cache lines if their caches hold them in the E or M states
    * i.e. they’re exclusively owned
    * If a core does not have exclusive access to a cache line when it wants to write, it first needs to send an “I want exclusive access” request to the bus.
    * This tells all other cores to invalidate their copies of that cache line, if they have any.
    * Only once that exclusive access is granted may the core start modifying data – and at that point, the core knows that the only copies of that cache line are in its own caches, so there can’t be any conflicts.
    * Conversely, once some other core wants to read from that cache line (which we learn immediately because we’re snooping the bus), exclusive and modified cache lines have to revert back to the “shared” (S) state. In the case of modified cache lines, this also involves writing their data back to memory first.
* As a software developer, you’ll get pretty far knowing only two things:

  Firstly, in a multi-core system, getting read access to a cache line involves talking to the other cores, and might cause them to perform memory transactions.
  Writing to a cache line is a multi-step process: before you can write anything, you first need to acquire both exclusive ownership of the cache line and a copy of its existing contents (a so-called “Read For Ownership” request).
* Caches do not respond to bus events immediately. If a bus message triggering a cache line invalidation arrives while the cache is busy doing other things (sending data to the core for example), it might not get processed that cycle. Instead, it will enter a so-called “invalidation queue”, where it sits for a while until the cache has time to process it.

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

## jmh