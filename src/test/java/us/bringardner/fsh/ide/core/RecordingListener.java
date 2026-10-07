package us.bringardner.fsh.ide.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import us.bringardner.fsh.ShellContext;

/** A DebugSession.Listener that records what it's told. */
class RecordingListener implements DebugSession.Listener {

	record Pause(int line, Map<String,Object> variables) {}

	final BlockingQueue<Pause> pauses = new LinkedBlockingQueue<>();
	final List<String> statements = new CopyOnWriteArrayList<>();
	final List<Exception> conditionErrors = new CopyOnWriteArrayList<>();
	volatile int resumes;
	volatile int terminates;

	@Override
	public void paused(int line, ShellContext ctx, Map<String, Object> variables) {
		pauses.add(new Pause(line, variables));
	}

	@Override
	public void statement(int line, String text) {
		statements.add(line+": "+text);
	}

	@Override
	public void conditionFailed(Breakpoint bp, int line, Exception error) {
		conditionErrors.add(error);
	}

	@Override
	public void resuming() {
		resumes++;
	}

	@Override
	public void terminateRequested() {
		terminates++;
	}
}
