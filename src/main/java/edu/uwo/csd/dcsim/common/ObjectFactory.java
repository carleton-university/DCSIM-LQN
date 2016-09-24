package edu.uwo.csd.dcsim.common;

/**
 * Defines a type that acts as an Abstract Factory for a class
 *
 * @author Michael Tighe
 */
public interface ObjectFactory<T> {

  public T newInstance();
}
