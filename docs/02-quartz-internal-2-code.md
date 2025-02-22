# quartz核心代码实现


##

org.quartz.core.QuartzSchedulerThread.run
```text
public void run() {
	int acquiresFailed = 0;

	while (!halted.get()) {
		try {
			// check if we're supposed to pause...
			synchronized (sigLock) {
				while (paused && !halted.get()) {
					try {
						// wait until togglePause(false) is called...
						sigLock.wait(1000L);
					} catch (InterruptedException ignore) {
					}

					// reset failure counter when paused, so that we don't
					// wait again after unpausing
					acquiresFailed = 0;
				}

				if (halted.get()) {
					break;
				}
			}

			// wait a bit, if reading from job store is consistently failing (e.g. DB is down or restarting)..
			if (acquiresFailed > 1) {
				try {
					long delay = computeDelayForRepeatedErrors(qsRsrcs.getJobStore(), acquiresFailed);
					Thread.sleep(delay);
				} catch (Exception ignore) {
				}
			}

			int availThreadCount = qsRsrcs.getThreadPool().blockForAvailableThreads();
			if(availThreadCount > 0) { // will always be true, due to semantics of blockForAvailableThreads...
				List<OperableTrigger> triggers;

				long now = System.currentTimeMillis();

				clearSignaledSchedulingChange();
				try {
				    // 重点1: acquire Trigger 
					triggers = qsRsrcs.getJobStore().acquireNextTriggers(now + idleWaitTime, Math.min(availThreadCount, qsRsrcs.getMaxBatchSize()), qsRsrcs.getBatchTimeWindow());
					acquiresFailed = 0;
					if (log.isDebugEnabled())
						log.debug("batch acquisition of " + (triggers == null ? 0 : triggers.size()) + " triggers");
				} catch (JobPersistenceException jpe) {
					if (acquiresFailed == 0) {
						qs.notifySchedulerListenersError("An error occurred while scanning for the next triggers to fire.", jpe);
					}
					if (acquiresFailed < Integer.MAX_VALUE)
						acquiresFailed++;
					continue;
				} catch (RuntimeException e) {
					if (acquiresFailed == 0) {
						getLog().error("quartzSchedulerThreadLoop: RuntimeException " +e.getMessage(), e);
					}
					if (acquiresFailed < Integer.MAX_VALUE)
						acquiresFailed++;
					continue;
				}

				if (triggers != null && !triggers.isEmpty()) {
					now = System.currentTimeMillis();
					long triggerTime = triggers.get(0).getNextFireTime().getTime();
					long timeUntilTrigger = triggerTime - now;
					while(timeUntilTrigger > 2) {
						synchronized (sigLock) {
							if (halted.get()) {
								break;
							}
							if (!isCandidateNewTimeEarlierWithinReason(triggerTime, false)) {
								try {
									// we could have blocked a long while
									// on 'synchronize', so we must recompute
									now = System.currentTimeMillis();
									timeUntilTrigger = triggerTime - now;
									if(timeUntilTrigger >= 1)
										sigLock.wait(timeUntilTrigger);
								} catch (InterruptedException ignore) {
								}
							}
						}
						if(releaseIfScheduleChangedSignificantly(triggers, triggerTime)) {
							break;
						}
						now = System.currentTimeMillis();
						timeUntilTrigger = triggerTime - now;
					}

					// this happens if releaseIfScheduleChangedSignificantly decided to release triggers
					if(triggers.isEmpty())
						continue;

					// set triggers to 'executing'
					List<TriggerFiredResult> bndles = new ArrayList<TriggerFiredResult>();

					boolean goAhead = true;
					synchronized(sigLock) {
						goAhead = !halted.get();
					}
					if(goAhead) {
						try {
						    // 重点12: Scheduler通知JobStore,fire指定的Trigger,更新相关对象的状态
							List<TriggerFiredResult> res = qsRsrcs.getJobStore().triggersFired(triggers);
							if(res != null)
								bndles = res;
						} catch (SchedulerException se) {
							qs.notifySchedulerListenersError(
									"An error occurred while firing triggers '"
											+ triggers + "'", se);
							//QTZ-179 : a problem occurred interacting with the triggers from the db
							//we release them and loop again
							for (int i = 0; i < triggers.size(); i++) {
							    // 重点3: release Trigger
								qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
							}
							continue;
						}
					}

					for (int i = 0; i < bndles.size(); i++) {
						TriggerFiredResult result =  bndles.get(i);
						TriggerFiredBundle bndle =  result.getTriggerFiredBundle();
						Exception exception = result.getException();

						if (exception instanceof RuntimeException) {
							getLog().error("RuntimeException while firing trigger " + triggers.get(i), exception);
							// 重点3: release Trigger
							qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
							continue;
						}

						// it's possible to get 'null' if the triggers was paused,
						// blocked, or other similar occurrences that prevent it being
						// fired at this time...  or if the scheduler was shutdown (halted)
						if (bndle == null) {
						    // 重点3: release Trigger
							qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
							continue;
						}

						JobRunShell shell = null;
						try {
							shell = qsRsrcs.getJobRunShellFactory().createJobRunShell(bndle);
							shell.initialize(qs);
						} catch (SchedulerException se) {
						    // 重点5: 异常场景,通知JobStore,job状态错误,更新相关对象的状态
							qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
							continue;
						}

                        // 重点4: 使用线程池中的可用线程执行任务
						if (qsRsrcs.getThreadPool().runInThread(shell) == false) {
							// this case should never happen, as it is indicative of the
							// scheduler being shutdown or a bug in the thread pool or
							// a thread pool being used concurrently - which the docs
							// say not to do...
							getLog().error("ThreadPool.runInThread() return false!");
							// 重点5: 通知JobStore,job执行完成,更新相关对象的状态
							qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
						}

					}

					continue; // while (!halted)
				}
			} else { // if(availThreadCount > 0)
				// should never happen, if threadPool.blockForAvailableThreads() follows contract
				continue; // while (!halted)
			}

			long now = System.currentTimeMillis();
			long waitTime = now + getRandomizedIdleWaitTime();
			long timeUntilContinue = waitTime - now;
			synchronized(sigLock) {
				try {
				  if(!halted.get()) {
					// QTZ-336 A job might have been completed in the mean time and we might have
					// missed the scheduled changed signal by not waiting for the notify() yet
					// Check that before waiting for too long in case this very job needs to be
					// scheduled very soon
					if (!isScheduleChanged()) {
					  sigLock.wait(timeUntilContinue);
					}
				  }
				} catch (InterruptedException ignore) {
				}
			}

		} catch(RuntimeException re) {
			getLog().error("Runtime error occurred in main trigger firing loop.", re);
		}
	} // while (!halted)

	// drop references to scheduler stuff to aid garbage collection...
	qs = null;
	qsRsrcs = null;
}
```




org.quartz.simpl.RAMJobStore.acquireNextTriggers

```text
public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow) {
	synchronized (lock) {
		List<OperableTrigger> result = new ArrayList<OperableTrigger>();
		Set<JobKey> acquiredJobKeysForNoConcurrentExec = new HashSet<JobKey>();
		Set<TriggerWrapper> excludedTriggers = new HashSet<TriggerWrapper>();
		long batchEnd = noLaterThan;
		
		// return empty list if store has no triggers.
		if (timeTriggers.size() == 0)
			return result;
		
		while (true) {
			TriggerWrapper tw;

			try {
				tw = timeTriggers.first();
				if (tw == null)
					break;
				timeTriggers.remove(tw);
			} catch (java.util.NoSuchElementException nsee) {
				break;
			}

			if (tw.trigger.getNextFireTime() == null) {
				continue;
			}

			if (applyMisfire(tw)) {
				if (tw.trigger.getNextFireTime() != null) {
					timeTriggers.add(tw);
				}
				continue;
			}

			if (tw.getTrigger().getNextFireTime().getTime() > batchEnd) {
				timeTriggers.add(tw);
				break;
			}
			
			// If trigger's job is set as @DisallowConcurrentExecution, and it has already been added to result, then
			// put it back into the timeTriggers set and continue to search for next trigger.
			JobKey jobKey = tw.trigger.getJobKey();
			JobDetail job = jobsByKey.get(tw.trigger.getJobKey()).jobDetail;
			if (job.isConcurrentExectionDisallowed()) {
				if (acquiredJobKeysForNoConcurrentExec.contains(jobKey)) {
					excludedTriggers.add(tw);
					continue; // go to next trigger in store.
				} else {
					acquiredJobKeysForNoConcurrentExec.add(jobKey);
				}
			}

			tw.state = TriggerWrapper.STATE_ACQUIRED;
			tw.trigger.setFireInstanceId(getFiredTriggerRecordId());
			OperableTrigger trig = (OperableTrigger) tw.trigger.clone();
			if (result.isEmpty()) {
				batchEnd = Math.max(tw.trigger.getNextFireTime().getTime(), System.currentTimeMillis()) + timeWindow;
			}
			result.add(trig);
			if (result.size() == maxCount)
				break;
		}

		// If we did excluded triggers to prevent ACQUIRE state due to DisallowConcurrentExecution, we need to add them back to store.
		if (excludedTriggers.size() > 0)
			timeTriggers.addAll(excludedTriggers);
		return result;
	}
}
```