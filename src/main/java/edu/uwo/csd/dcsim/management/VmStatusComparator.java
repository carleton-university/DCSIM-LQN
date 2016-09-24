package edu.uwo.csd.dcsim.management;

import java.util.Comparator;

/**
 * Compares VM status by a (non-empty) series of attributes or factors. The available factors are:
 * <p/>
 * + CPU_CORES:		requested number of cores; + CORE_CAP:		requested core capacity; +
 * MEMORY:		requested memory; + BANDWIDTH:		requested bandwidth; + CPU_IN_USE:	current CPU in use.
 *
 * @author Gaston Keller
 * @author Michael Tighe modified for VmStatus
 */
public enum VmStatusComparator implements Comparator<VmStatus> {

  CPU_CORES {
    public int compare(VmStatus o1, VmStatus o2) {
      return o1.getCores() - o2.getCores();
    }
  },
  CORE_CAP {
    public int compare(VmStatus o1, VmStatus o2) {
      return o1.getCoreCapacity() - o2.getCoreCapacity();
    }
  },
  MEMORY {
    public int compare(VmStatus o1, VmStatus o2) {
      return o1.getResourceInUse().memory() - o2.getResourceInUse().memory();
    }
  },
  BANDWIDTH {
    public int compare(VmStatus o1, VmStatus o2) {
      double
          compare =
          o1.getResourceInUse().bandwidth() - o2.getResourceInUse().bandwidth();
      if (compare < 0) {
        return -1;
      } else if (compare > 0) {
        return 1;
      }
      return 0;
    }
  },
  CPU_IN_USE {
    public int compare(VmStatus o1, VmStatus o2) {
      double compare = o1.getResourceInUse().cpu() - o2.getResourceInUse().cpu();
      if (compare < 0) {
        return -1;
      } else if (compare > 0) {
        return 1;
      }
      return 0;
    }
  };

  public static Comparator<VmStatus> getComparator(final VmStatusComparator... multipleOptions) {
    return new Comparator<VmStatus>() {
      public int compare(VmStatus o1, VmStatus o2) {
        for (VmStatusComparator option : multipleOptions) {
          int result = option.compare(o1, o2);
          if (result != 0) {
            return result;
          }
        }
        return 0;
      }
    };
  }

}
