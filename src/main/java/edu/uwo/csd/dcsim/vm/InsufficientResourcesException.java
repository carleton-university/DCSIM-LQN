package edu.uwo.csd.dcsim.vm;

import edu.uwo.csd.dcsim.host.Resource;

/**
 * @author Derek Hawker
 */
public class InsufficientResourcesException extends RuntimeException {

  public InsufficientResourcesException(Resource requestedResources,
                                        Resource availableResources) {
    super("Attempted to schedule more resources than is available: \n" +
          ((requestedResources.coreCapacity() > availableResources.coreCapacity()) ?
           "More CPU requested than is available (" + requestedResources.cpu() + ", " +
           availableResources.cpu() + ")\n" : "") +

          ((requestedResources.memory() > availableResources.memory()) ?
           "More memory requested than is available (" + requestedResources.memory() + ", " +
           +availableResources.memory() + ")\n" : "") +

          ((requestedResources.bandwidth() > availableResources.bandwidth()) ?
           "More bandwidth requested than is available (" +
           requestedResources.bandwidth() + ", " +
           availableResources.bandwidth() + ")\n" : "") +

          ((requestedResources.storage() > availableResources.storage()) ?
           "More storage requested than is available (" +
           requestedResources.storage() + ", " +
           availableResources.storage() + ")\n" : ""));
  }
}
