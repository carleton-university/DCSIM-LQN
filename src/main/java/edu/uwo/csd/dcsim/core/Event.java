package edu.uwo.csd.dcsim.core;

import java.util.ArrayList;
import java.util.List;

public abstract class Event {

  protected Simulation simulation = null;
  private long time;
  private SimulationEventListener target;
  private long sendOrder;
  private List<EventCallbackListener> callbackListeners = new ArrayList<EventCallbackListener>();

  private int waitOnEvent = 0;
  //0 if we are waiting for another event to run, > 0 depending on number of events to wait for
  private boolean blockPostEvent = false;

  public Event(SimulationEventListener target) {
    this.target = target;
  }

  public final Event addCallbackListener(EventCallbackListener listener) {
    callbackListeners.add(listener);
    return this;
  }

  public final void addEventInSequence(Event nextEvent) {
    //flag that we are waiting to trigger postExecute, log, and callbackListeners until the next event is done
    waitOnEvent++;

    //add a listener to trigger methods once the next event completes. This can cause a cascade back through several sequenced events.
    nextEvent.addCallbackListener(new EventCallbackListener() {

      @Override
      public void eventCallback(Event e) {
        waitOnEvent--;
        postExecute();
        triggerCallback();
      }

    });
  }

  public final void cancelEventInSequence() {
    waitOnEvent--;
    postExecute();
    triggerCallback();
  }

  /**
   * Provides a hook to run any additional code after the event has been triggered and handled.
   */
  public void postExecute() {
    //default behaviour is to do nothing, designed to be overridden
  }

  public final void triggerPostExecute() {
    if (waitOnEvent == 0 && !blockPostEvent) {
      postExecute();
    }
  }


  public final void triggerCallback() {
    if (waitOnEvent == 0 && !blockPostEvent) {
      for (EventCallbackListener listener : callbackListeners) {
        listener.eventCallback(this);
      }
    }
  }

  /**
   * Provides a hook to run code just before this event is executed
   */
  public void preExecute() {
    //default behaviour is to do nothing
  }

  public final void initialize(Simulation simulation) {
    //only initialize if this is the first time the event has been sent
    if (this.simulation == null) {
      this.simulation = simulation;
    }
  }

  public final void setTime(long time) {
    this.time = time;
  }

  public final long getTime() {
    return time;
  }

  public final SimulationEventListener getTarget() {
    return target;
  }

  protected final void setSendOrder(long sendOrder) {
    this.sendOrder = sendOrder;
  }

  protected final long getSendOrder() {
    return sendOrder;
  }

  public final Simulation getSimulation() {
    return simulation;
  }

  public final boolean isPostEventBlocked() {
    return blockPostEvent;
  }

  public final void setBlockPostEvent(boolean blockPostEvent) {
    this.blockPostEvent = blockPostEvent;
  }

}
