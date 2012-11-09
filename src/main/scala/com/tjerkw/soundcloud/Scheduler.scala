package com.tjerkw.soundcloud

import java.util.concurrent.{ScheduledExecutorService, TimeUnit, Executors}

object Scheduler {
  private var sched:ScheduledExecutorService = null

  /**
   * Schedule a call to a function in the future
   * @param f the future
   * @param time the amount of millis to wait until execution
   */
  def schedule(f: => Unit, time: Long) {
    if (sched == null) {
      sched = Executors.newSingleThreadScheduledExecutor()
    }
    sched.schedule(new Runnable {
      def run = f
    }, time, TimeUnit.MILLISECONDS)
  }

  /**
   * Cancel all scheduled calls
   */
  def cancel = if(sched!=null && !sched.isShutdown ) {
    sched.shutdownNow
    sched = null
  }
}
