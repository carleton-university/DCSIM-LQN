package edu.uwo.csd.dcsim.host;

import com.google.auto.value.AutoValue;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a set of resources
 *
 * @author Michael Tighe
 * @author Derek Hawker
 */
@AutoValue
public abstract class Resource {

  private static Resource emptyResource;

  public abstract int numCpus();

  public abstract int numCores();

  public abstract int cpu();

  public abstract int memory();

  public abstract int bandwidth();

  public abstract int storage();

  /**
   * Pseudonym for cpu
   */
  public int coreCapacity() {
    return cpu();
  }

  public static Resource create(int numCpu,
                                int numCores,
                                int cpu,
                                int memory,
                                int bandwidth,
                                int storage) {
    checkArgument(numCpu > 0, "Number of cpu must be > 0");
    checkArgument(numCores > 0, "Number of cores must be > 0");
    checkArgument(cpu >= 0, "cpu must be >= 0");
    checkArgument(memory >= 0, "memory must be >= 0");
    checkArgument(bandwidth >= 0, "bandwidth must be >= 0");
    checkArgument(storage >= 0, "storage must be >= 0");

    return new AutoValue_Resource(numCpu, numCores, cpu, memory, bandwidth, storage);
  }

  public Resource add(Resource other) {
    int cpu = (cpu() + other.cpu());
    int bw = (bandwidth() + other.bandwidth());
    int memory = (memory() + other.memory());
    int storage = (storage() + other.storage());

    return Resource.create(numCpus(), numCores(), cpu, memory, bw, storage);
  }

  public Resource subtract(Resource other) {
    int cpu = (cpu() - other.cpu());
    int bw = (bandwidth() - other.bandwidth());
    int memory = (memory() - other.memory());
    int storage = (storage() - other.storage());

    return Resource.create(numCpus(), numCores(), cpu, memory, bw, storage);
  }

  public Resource withCpu(int cpu) {
    return Resource.create(numCpus(), numCores(), cpu, memory(), bandwidth(), storage());
  }

  public Resource withMemory(int memory) {
    return Resource.create(numCpus(), numCores(), cpu(), memory, bandwidth(), storage());
  }

  public Resource withBandwidth(int bandwidth) {
    return Resource.create(numCpus(), numCores(), cpu(), memory(), bandwidth, storage());
  }

  public Resource withStorage(int storage) {
    return Resource.create(numCpus(), numCores(), cpu(), memory(), bandwidth(), storage);
  }

  public static Resource createMicroInstance() {
    return Resource.create(1, 1, 1350, 16384/4, 1, 1);
  }

  public static Resource empty() {
    return emptyResource;
  }

  static {
    emptyResource = Resource.create(1, 1, 0, 0, 0, 0);
  }

  public static Resource copyOf(Resource other) {
    return Resource.create(other.numCpus(), other.numCores(),
                           other.cpu(), other.memory(),
                           other.bandwidth(), other.storage());
  }

  public static Resource createLumped(int cpu,
                                      int memory,
                                      int bandwidth,
                                      int storage) {
    return Resource.create(1, 1, cpu, memory, bandwidth, storage);
  }

  public int grossCpu() {
    return coreCapacity() * numCores() * numCpus();
  }

  public boolean fitsIn(Resource target) {
    return target.grossCpu() >= grossCpu() &&
           target.memory() >= memory() &&
           target.bandwidth() >= bandwidth() &&
           target.storage() >= storage();
  }

  public Resource withLumped(int cpu,
                             int memory,
                             int bandwidth,
                             int storage) {
    return Resource.create(numCpus(), numCores(), cpu, memory, bandwidth, storage);
  }
}
