package edu.uwo.csd.dcsim.common;

/**
 * Defines a type that constructs instances of a given type
 *
 * @author Michael Tighe
 */
public interface ObjectBuilder<T> {

  public T build();
}
