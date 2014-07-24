package com.connorsapps.snys;

/**
 * Callback for tasks showing progress
 * @author connor
 *
 */
public interface ProgressCallback
{
	/**
	 * Start indeterminant progress indicator in this activity's way
	 */
	public void startProgress();
	
	/**
	 * Stop indeterminent progress
	 */
	public void endProgress();
}
